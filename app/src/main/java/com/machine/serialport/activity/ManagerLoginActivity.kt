package com.machine.serialport.activity

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import cn.trinea.android.common.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.machine.serialport.R
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.model.UserModel
import com.machine.serialport.util.DataStoreUtils
import com.machine.serialport.util.LogUtils
import http.OkHttpUtil
import http.callback.StringCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType
import java.lang.Exception
import java.lang.reflect.Type

//管理员登录页面
class ManagerLoginActivity : BaseActivity() {
    private var account: TextView ?= null
    private var pwd: TextView ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manager_login)
        initView()
        initData()
    }

    private fun initView(){
        findViewById<View>(R.id.admin_login).setOnClickListener(this::onClick)
        account = findViewById(R.id.admin_account_input)
        pwd = findViewById(R.id.admin_pwd_input)
        findViewById<View>(R.id.back_green).setOnClickListener(this::onClick)
    }

    private fun initData(){

    }

    private fun onClick(v: View){
        when(v.id){
            R.id.admin_login -> {
                if(!checkParams()){
                    return
                }
                managerLogin()
            }
            R.id.back_green -> {
                finish()
            }
        }
    }

    private fun checkParams():Boolean{
        val accountValue = account?.text.toString()
        val pwdValue = pwd?.text.toString()
        if (TextUtils.isEmpty(accountValue)){
            ToastUtils.show(this,"账号不能为空!")
            return false
        }
        if (TextUtils.isEmpty(pwdValue)){
            ToastUtils.show(this,"密码不能为空!")
            return false
        }
        return true
    }

    private fun managerLogin(){
        val params = mutableMapOf<String,String>()
        params["username"] = account?.text.toString()
        params["password"] = pwd?.text.toString()
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.USER_LOGIN))
            .addParams(params)
            .build().execute(object : SerialPortHttpCallBack<UserModel>(){
                override fun onError(call: Call?, e: Exception?, id: Int) {
                    ToastUtils.show(this@ManagerLoginActivity,"网络，请检查网络!")
                }

                override fun onResponse(response: HttpBaseResponseMode<UserModel>, id: Int) {
                    if (!response.success){
                        ToastUtils.show(this@ManagerLoginActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                        return
                    }
                    val data = response.data
                    GlobalScope.launch {
                        DataStoreUtils.getInstance().putInventoryToken(data!!.token)
                        startActivity(
                            Intent(
                                this@ManagerLoginActivity,
                                AdminSettingActivity::class.java
                            )
                        )
                        finish()
                    }
                }
            })
    }

}