package com.machine.serialport.activity

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.RequiresApi
import cn.trinea.android.common.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.machine.serialport.R
import com.machine.serialport.SerialPortApplication
import com.machine.serialport.data.TestData
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.AppMsgModel
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.util.LogUtils
import http.OkHttpUtil
import okhttp3.Call

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SplashActivity: BaseActivity() {
    private var splashImg: ImageView ?= null
    private var mGlide: RequestManager? = null
    private val netWorkImpl = NetWorkImpl()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        initPermission()
        initView()
        initData()
        netWorkListener()
    }
    private fun netWorkListener(){
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()
        val request: NetworkRequest = builder.build()
        val connMgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connMgr?.registerNetworkCallback(request, netWorkImpl)
    }
    private fun initPermission(){

        XXPermissions.with(this)
            .permission(Permission.SYSTEM_ALERT_WINDOW)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
//                        startFaceService()
                        SerialPortApplication.getInstance().startSmartFaceService()
                        ToastUtils.show(this@SplashActivity, "???????????????")
                    } else {
                        ToastUtils.show(this@SplashActivity, "?????????????????????????????????????????????????????????")

                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        ToastUtils.show(this@SplashActivity, "??????????????????????????????????????????????????????")
                        // ??????????????????????????????????????????????????????????????????
                        XXPermissions.startPermissionActivity(this@SplashActivity, permissions)
                    } else {
                        ToastUtils.show(this@SplashActivity, "???????????????????????????")
                    }
                }
            })
    }
    private fun initView(){
        splashImg = findViewById(R.id.splash_img)
        splashImg?.setOnClickListener(this::onClick)
        findViewById<View>(R.id.root)?.setOnClickListener(this::onClick)
    }

    private fun initData(){
        mGlide = Glide.with(this)
        mGlide?.load(TestData.SPLASH_SRC)?.into(splashImg!!)
//        val imie = DeviceIdUtils.getDeviceId(this)
//        LogUtils.logE(mess = "??????ID:$imie")
        splashImg?.postDelayed({
            getAppBg()
        },500)
    }

    //??????app???????????????
    private fun getAppBg(){
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.DEVICE_PREVIEW))
            .build().execute(object : SerialPortHttpCallBack<AppMsgModel>(){
                override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                    ToastUtils.show(this@SplashActivity,"????????????????????????!")
                }

                override fun onResponse(
                    response: HttpBaseResponseMode<AppMsgModel>,
                    id: Int
                ) {
                    if (!response.success){
                        ToastUtils.show(this@SplashActivity,"????????????????????????????????????????????????:${response.msg}")
                        return
                    }
                    val data = response.data
                    LogUtils.logE(mess = "?????????????????????:${data.toString()}")
                    LogUtils.logE(mess = "Glide????????????:${null == mGlide}")
                    mGlide?.load(data!!.url)?.into(splashImg!!)
                }
            })
    }

    private fun onClick(v: View){
        when(v.id){
            R.id.splash_img -> {
                startActivity(
                    Intent(
                        v.context,
                        HomeActivity::class.java
                    )
                )
            }
            R.id.root -> {
                startActivity(
                    Intent(
                        v.context,
                        HomeActivity::class.java
                    )
                )
            }
        }
    }
    inner class NetWorkImpl:ConnectivityManager.NetworkCallback(){
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            LogUtils.logE(mess = "????????????")
            getAppBg()
        }

        override fun onUnavailable() {
            super.onUnavailable()
            LogUtils.logE(mess = "????????????")
        }
    }
}