package com.machine.serialport.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import cn.trinea.android.common.util.ToastUtils
import com.machine.serialport.R
import com.machine.serialport.contact.Contacts
import com.machine.serialport.dialog.SimpleDialog
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.model.UserModel
import com.machine.serialport.service.ServiceDataIpc
import com.machine.serialport.service.ServiceSerialPort
import com.machine.serialport.util.*
import http.OkHttpUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import java.util.concurrent.locks.ReentrantLock

//二维码扫描页面
class ScanCodeActivity : BaseActivity(), ServiceDataIpc {
    private var serviceSerialPort: ServiceSerialPort? = null
    private var mBound = false
    private var isDestroy = false
    private var isDeliver = false//数据是否处理中
    private val dialog = SimpleDialog()
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
            serviceSerialPort = binder.getService()
            serviceSerialPort?.setSerialPortCallBack(this@ScanCodeActivity)
            mBound = true
//            serviceSerialPort?.startInventory()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_code)
        initDialog()
        bindSerialPortService()
        findViewById<View>(R.id.back_green).setOnClickListener {
            finish()
        }
    }
    private fun initDialog(){
        val argument = Bundle()
        argument.putString("title","系统提示")
        argument.putString("content","请选择开启哪个门")
        argument.putString("negativeTitle","主柜")
        argument.putString("positiveTitle","辅柜")
        argument.putBoolean("isCancelable",false)
        argument.putBoolean("isShowCancelBtn",true)
        dialog.arguments = argument
    }

    //绑定服务
    private fun bindSerialPortService() {
        // Bind to LocalService
        val intent = Intent(this, ServiceSerialPort::class.java)
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    //解绑服务
    private fun unBindService() {
        try {
            if (ServiceUtils.isServiceRunning(this,ServiceSerialPort::class.java.name)){
                unbindService(connection)
                mBound = false
//                serviceSerialPort?.stopInventory()
            }
        }catch (e: Exception){

        }
    }

    private fun qrCodeLogin(code: String){
        LogUtils.logE(mess = "开始请求内容物流人员信息")
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.QRCODE_LOGIN))
            .addParams("code",code)
            .build().execute(object : SerialPortHttpCallBack<UserModel>(){
                override fun onError(call: Call?, e: Exception?, id: Int) {
                    ToastUtils.show(this@ScanCodeActivity,"网络，请检查网络!")
//                    isDeliver = false
                }

                override fun onResponse(response: HttpBaseResponseMode<UserModel>, id: Int) {
                    if (!response.success){
                        ToastUtils.show(this@ScanCodeActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                        return
                    }
                    val data = response.data
                    GlobalScope.launch {
                        DataStoreUtils.getInstance().putInventoryToken(data!!.token)
                        runOnUiThread {
                            chooseOpenDoor()
                        }
                    }
//                    isDeliver = false
                    try {
                        lock.unlock()
                    }catch (e : Exception){

                    }
                }
            })
    }
    //选择开哪个门
    private fun chooseOpenDoor(){
        if(AppUtils.isDistribute(packageName)){
            SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_BROADCAST)
            startActivity(
                Intent(
                    this@ScanCodeActivity,
                    MainActivity::class.java
                )
            )
            finish()
            openDoor()
            return
        }

        dialog.setOnNegativeClick {
            Log.e("YM","取消按钮")
            openCheckDoor(0)
        }
        dialog.setOnPositiveClick {
            Log.e("YM","确定按钮")
            openCheckDoor(1)
        }
        dialog.show(supportFragmentManager.beginTransaction(),"tag")
//        MaterialDialog(this).show {
//            title(text = "系统提示")
//            message(text = "请选择开启哪个门!")
//            negativeButton(text = "主柜"){
//                openCheckDoor(0)
//            }
//            positiveButton(text = "辅柜"){
//                openCheckDoor(1)
//            }
//        }
    }

    //0 主柜 1辅柜
    private fun openCheckDoor(doorType: Int){
        if(!AppUtils.isDistribute(packageName)){//满桶且是回收柜,设置开启辅助柜子的门
            if (doorType == 0){
                LogUtils.logE(mess = "设置场景为主机场景")
                SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_MASTER)
                Contacts.testDeviceId = Contacts.testRecoveryMasterDeviceId
            }else{
                LogUtils.logE(mess = "设置场景为从机场景")
                SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_SLAVE)
                Contacts.testDeviceId = Contacts.testRecoverySlaveDeviceId
            }
        }else{
            SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_BROADCAST)
            Contacts.testDeviceId = Contacts.testDistributeDeviceId
        }
        startActivity(
            Intent(
                this@ScanCodeActivity,
                MainActivity::class.java
            )
        )
        finish()
        openDoor()
    }

    //开门
    private fun openDoor(){//因为回收柜还是分法柜都是开锁
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
//        serviceSerialPort?.writeHex(ContactsCmd.UN_LOCK_BROADCAST)
        serviceSerialPort?.writeHex(SerialPortCmdUtil.getUnlockCmd())
    }
    private val lock = ReentrantLock()
    override fun onDataReceived(buffer: ByteArray, size: Int) {
        if (isDestroy){
            return
        }
        GlobalScope.launch {
//            Log.e(TAG, "接收的值mReceptionS是否为null: " + (null == mReception))
//            if (mReception != null) {
            var receiverContent = String(buffer, 0, size)
            receiverContent = receiverContent.trim()
//                val status = HexUtils.toHex(buffer)

//            addData(receiverContent)
//            parserReceiverData(status)
            lock.lock()
            LogUtils.logE(mess = "二维码页面是否销毁------>$isDestroy")
            if(isDestroy){
                return@launch
            }
//                LogUtils.logE(mess = "接收的内容二维码数据十六进制status------>$status")
                LogUtils.logE(mess = "接收的内容二维码数据文本格式status------>$receiverContent")
                parserReceiverData(receiverContent)
//            lock.unlock()
        }
//        runOnUiThread {
////            Log.e(TAG, "接收的值mReceptionS是否为null: " + (null == mReception))
////            if (mReception != null) {
////            val receiverContent = String(buffer, 0, size)
//            val status = HexUtils.toHex(buffer)
//            LogUtils.logE(mess = "接收的内容二维码数据status------>$status")
////            addData(receiverContent)
////            parserReceiverData(status)
//            parserReceiverData(Contacts.faceCode)
//        }
    }


    private fun parserReceiverData(status: String) {
        LogUtils.logE(mess = "是否在处理数据中-->$isDeliver")
        if (isDeliver){
            return
        }
        isDeliver = true
        LogUtils.logE(mess = "111111111")
//        if (!status.startsWith("B6M8FlVOc")){
////            isDeliver = false
//            return
//        }
        LogUtils.logE(mess = "开始请求数据")
       qrCodeLogin(status)
    }

    override fun onError(resourceId: Int) {
        DisplayError(resourceId)
    }
    override fun onDestroy() {
        super.onDestroy()
        isDestroy = true
        unBindService()
    }

    override fun onBackPressed() {
        isDestroy = true
        unBindService()
        super.onBackPressed()
    }
}