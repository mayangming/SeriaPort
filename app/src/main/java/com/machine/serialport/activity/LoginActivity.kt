package com.machine.serialport.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import cn.trinea.android.common.util.ToastUtils
import com.machine.serialport.R
import com.machine.serialport.contact.ContactsCmd
import com.machine.serialport.service.ServiceDataIpc
import com.machine.serialport.service.ServiceSerialPort
import com.machine.serialport.util.LogUtils

//登录页面
class LoginActivity : BaseActivity(), ServiceDataIpc {
    private var serviceSerialPort: ServiceSerialPort? = null
    private var mBound = false
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
            serviceSerialPort = binder.getService()
            serviceSerialPort?.setSerialPortCallBack(this@LoginActivity)
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        initView()
        bindSerialPortService()
        LogUtils.logE("YM","程序启动")
    }

    //初始化控件
    private fun initView(){
        findViewById<View>(R.id.login).setOnClickListener(this::onClick)
        findViewById<View>(R.id.login_face).setOnClickListener(this::onClick)
        findViewById<View>(R.id.open_putter).setOnClickListener(this::onClick)
        findViewById<View>(R.id.close_putter).setOnClickListener(this::onClick)
        findViewById<View>(R.id.stop_putter).setOnClickListener(this::onClick)
        findViewById<View>(R.id.start_alarm).setOnClickListener(this::onClick)
        findViewById<View>(R.id.stop_alarm).setOnClickListener(this::onClick)
        findViewById<View>(R.id.read_status).setOnClickListener(this::onClick)
    }

    private fun onClick(v: View){
        when(v.id){
            R.id.login -> {
                unLock()
                goMain()
            }
            R.id.login_face -> {
                startActivity(
                    Intent(
                        v.context,
                        LoginByFaceActivity::class.java
                    )
                )
            }
            R.id.open_putter -> {
                openPutter()
            }
            R.id.close_putter -> {
                closePutter()
            }
            R.id.stop_putter -> {
                stopPutter()
            }
            R.id.start_alarm -> {
                starAlarm()
            }
            R.id.stop_alarm -> {
                stopAlarm()
            }
            R.id.read_status -> {
                readStatus()
            }
        }
    }
    private fun unLock(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.UN_LOCK_BROADCAST)
    }

    private fun openPutter(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.Close_Putter_BROADCAST)
    }

    private fun closePutter(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.open_Putter_BROADCAST)
    }

    private fun stopPutter(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.STOP_Putter_BROADCAST)
    }
    private fun starAlarm(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.ALARM_OPEN)
    }
    private fun stopAlarm(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.ALARM_CLOSE)
    }

    private fun readStatus(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        serviceSerialPort?.writeHex(ContactsCmd.TEMPORARY_STORAGE_STATUS)
    }


    //绑定服务
    private fun bindSerialPortService() {
        // Bind to LocalService
        val intent = Intent(this, ServiceSerialPort::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    //
    private fun goMain(){
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )
        finish()
    }

    //解绑服务
    private fun unBindService() {
        unbindService(connection)
        mBound = false
    }

    override fun onDataReceived(buffer: ByteArray, size: Int) {
        //接收的数据
        val receiverContent = String(buffer, 0, size)
//        LogUtils.logE(mess = "接收的内容${ HexUtil.encodeHexStr(buffer)}")

    }

    override fun onError(resourceId: Int) {
        //错误信息
        DisplayError(resourceId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unBindService()
    }

}