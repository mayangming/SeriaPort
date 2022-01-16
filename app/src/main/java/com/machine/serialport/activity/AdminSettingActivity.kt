package com.machine.serialport.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import cn.trinea.android.common.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.models.album.entity.Photo
import com.machine.serialport.R
import com.machine.serialport.contact.Contacts
import com.machine.serialport.contact.Contacts.testDeviceId
import com.machine.serialport.contact.SmartFaceContacts
import com.machine.serialport.dialog.HospitalLocationDialog
import com.machine.serialport.dialog.TYPE_RECYCLE
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.HttpApi.getURL
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.DepartmentModel
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.model.UserFaceModel
import com.machine.serialport.model.UserModel
import com.machine.serialport.service.MipsIDFaceProService
import com.machine.serialport.util.*
import http.OkHttpUtil
import http.callback.StringCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import java.lang.Exception
import java.lang.reflect.Type
import java.util.*
import kotlin.random.Random

class AdminSettingActivity : BaseActivity(), ServiceConnection {
    private val photoRequestCode: Int = 101 // 获取文件的请求码
    private var addVip: ImageView ?= null
    private var mGlide: RequestManager? = null
    private var mIntent: Intent ?= null
    private var mipsFaceService: MipsIDFaceProService? = null
    private var vipPhoto: Photo ?= null //临时保存的VIP图片信息
    private var faceUserCache: MutableList<UserFaceModel> = mutableListOf()

    private var name: EditText ?= null
    private var phone: EditText ?= null
    private var job: EditText ?= null
    private var deviceLocation: EditText ?= null
    val faceUserModel = UserFaceModel()
    private var deviceInfo = DepartmentModel()//设备信息
    private val hospitalDialog = HospitalLocationDialog()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_setting)
        initView()
        initData()
    }

    private fun initView(){
        findViewById<View>(R.id.back_green).setOnClickListener(this::onClick)
        findViewById<View>(R.id.next).setOnClickListener(this::onClick)
        name = findViewById(R.id.name_edt)
        phone = findViewById(R.id.phone_edt)
        job = findViewById(R.id.position_edt)
        deviceLocation = findViewById(R.id.device_location)
        addVip = findViewById(R.id.add_vip)
        addVip?.setOnClickListener(this::onClick)
        deviceLocation?.setOnClickListener(this::onClick)
    }

    private fun initData(){
        mGlide = Glide.with(this)
        startSmartFaceService()
        initFaceCacheData()
        hospitalDialog.selectDeviceCallBack = {
            deviceInfo = it
            deviceLocation?.setText(it.name)
            Contacts.testDeviceId = it.value
        }
    }

    private fun initFaceCacheData(){
        GlobalScope.launch {
            val faceData = DataStoreUtils.getInstance().getFaceDataCache()
            Log.e(">>>>>>>>>>>>", "获取数据:$faceData-->数据是否为空:${TextUtils.isEmpty(faceData)}")
            if (TextUtils.isEmpty(faceData)){
                return@launch
            }
            val type: Type = object : TypeToken<ArrayList<UserFaceModel>>() {}.type
            faceUserCache = Gson().fromJson(faceData,type)
        }

    }

    private fun onClick(v: View){
        when(v.id){
            R.id.back_green -> {
                finish()
            }
            R.id.next -> {
                if (!checkParams()){
                    return
                }
                addVip()
            }
            R.id.add_vip -> {
                EasyPhotos.createAlbum(this, true, false, GlideEngine.getInstance())
                    .setFileProviderAuthority("$packageName.fileprovider")
                    .setPuzzleMenu(false)
                    .setCleanMenu(false)
                    .start(photoRequestCode) //也可以选择链式调用写法

            }
            R.id.device_location -> {
                Log.e("YM---->","--->弹出对话框:")
                hospitalDialog.show(supportFragmentManager,"")
            }
        }
    }
    private fun updateVipImage(){
        LogUtils.logE(mess = vipPhoto.toString())
        if (null != vipPhoto){
            mGlide?.load(vipPhoto!!.uri)?.into(addVip!!)
        }else{
            mGlide?.load(R.mipmap.default_vip_img)?.into(addVip!!)
        }

    }

    //检查参数
    private fun checkParams():Boolean{
        val faceName = name?.text.toString()
        var facePhone = phone?.text.toString()
        val faceJob = job?.text.toString()
        val deviceId = deviceLocation?.text.toString()
        if (TextUtils.isEmpty(faceName)){
            Toast.makeText(applicationContext, "姓名不能为空!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (TextUtils.isEmpty(facePhone)){
            Toast.makeText(applicationContext, "手机号不能为空!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (TextUtils.isEmpty(faceJob)){
            Toast.makeText(applicationContext, "职务不能为空!", Toast.LENGTH_SHORT).show()
            return false
        }
        if (TextUtils.isEmpty(deviceId)){
            Toast.makeText(applicationContext, "设备位置信息不能设置为空!", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun addVip(){
        if (null == vipPhoto){
            return
        }
        var ret = -1;
        val vipCount = mipsFaceService!!.mipsGetDbFaceCnt()
        ret = mipsFaceService!!.mipsAddVipFace(this, vipPhoto?.path, vipCount, true)
        if (ret < 0) {
            Toast.makeText(applicationContext, "添加VIP失败", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "添加VIP成功", Toast.LENGTH_SHORT).show()
            upLoadFaceData(vipCount)
        }
        vipPhoto = null
        updateVipImage()

        saveDeviceId()
    }

    //保存设备id的话把所有id保存为一致
    private fun saveDeviceId(){
//        val deviceId = deviceId?.text.toString()
//        DeviceIdUtils.setDeviceId(this,deviceId)
//
//        Contacts.testDeviceId = deviceId
//        Contacts.testDistributeDeviceId = deviceId
//        Contacts.testRecoveryMasterDeviceId = deviceId
//        Contacts.testRecoverySlaveDeviceId = deviceId
        GlobalScope.launch(context = Dispatchers.IO) {
            DataStoreUtils.getInstance().putDeviceId(testDeviceId)
//            val subDeviceId = deviceInfo.subCabinetList
            val type = deviceInfo.type
            if (type == TYPE_RECYCLE){//回收柜
                val subInfoList = deviceInfo.subCabinetList
                if (subInfoList.isNotEmpty()){
                    val subInfo = subInfoList[0]
                    DataStoreUtils.getInstance().putSubDeviceId(subInfo.id)
                }
            }
        }
    }

    private fun saveFaceData(){

        faceUserCache.add(faceUserModel)
        val cacheJson = Gson().toJson(faceUserCache)
        GlobalScope.launch {
            Log.e(">>>>>>>>>>>>", "保存人脸注册数据:$cacheJson")
            DataStoreUtils.getInstance().putFaceDataCache(cacheJson)
        }
    }
    private fun saveUserModel(faceId: Int){
//        val uuid = UUID.randomUUID()
//        val random = Random(100).nextInt()
        val faceName = name?.text.toString()
        var facePhone = phone?.text.toString()
        val faceJob = job?.text.toString()
        facePhone = "$facePhone"
        faceUserModel.faceId = faceId
        faceUserModel.faceName = faceName
        faceUserModel.facePhone = facePhone
        faceUserModel.faceJob = faceJob
    }
   private fun upLoadFaceData(faceId: Int){
       saveUserModel(faceId)
       OkHttpUtil.postJson()
           .url(HttpApi.getURL(HttpApi.USER_FACE_REG))
           .addParams("mobile",faceUserModel.facePhone)
           .addParams("realName",faceUserModel.faceName)
           .addParams("position",faceUserModel.faceJob)
           .build().execute(object : SerialPortHttpCallBack<UserModel>(){
               override fun onError(call: Call?, e: Exception?, id: Int) {
                   ToastUtils.show(this@AdminSettingActivity,"网络，请检查网络!")
//                    isDeliver = false
               }

               override fun onResponse(response: HttpBaseResponseMode<UserModel>, id: Int) {
                   if (!response.success){
                       ToastUtils.show(this@AdminSettingActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                       return
                   }
                   val data = response.data
                   saveFaceData()
//                    isDeliver = false
               }
           })
   }

    private fun startSmartFaceService(){
        mIntent = Intent(this, MipsIDFaceProService::class.java)
        if (!ServiceUtils.isServiceRunning(this, MipsIDFaceProService::class.java.name)){//假如服务没有启动则进行启动
            startService(mIntent)
        }
        bindService(mIntent, this, BIND_AUTO_CREATE)
    }

    private fun stopSmartFaceService(){
        //解除绑定
        if (ServiceUtils.isServiceRunning(this, MipsIDFaceProService::class.java.name)){
            unbindService(this)
            LogUtils.logE("YM-->管理员设置页面", "人脸识别服务已关闭")
        }else{
            LogUtils.logE("YM-->管理员设置页面", "人脸识别服务没有运行")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        stopSmartFaceService()
    }
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MipsIDFaceProService.Binder
        mipsFaceService = binder.service
        Thread{
            mipsFaceService?.initMips( applicationContext,
                SmartFaceContacts.licPath,
                SmartFaceContacts.camera_orientation,
                assets,
                SmartFaceContacts.choose_alg.toString())
                LogUtils.logE(mess = "人脸识别服务创建成功")
        }.start()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (RESULT_OK != resultCode) {
            return
        }
        //相机或相册回调
        if (requestCode == 101) {
            //返回对象集合：如果你需要了解图片的宽、高、大小、用户是否选中原图选项等信息，可以用这个
            val resultPhotos: ArrayList<Photo>? =
                data?.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS)
            resultPhotos?.let {
                vipPhoto = it[0]
                updateVipImage()
            }
            return
        }
    }
}