package com.machine.serialport.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import cn.trinea.android.common.util.ToastUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.machine.serialport.R
import com.machine.serialport.SerialPortApplication
import com.machine.serialport.contact.Contacts
import com.machine.serialport.service.ServiceDataIpc
import com.machine.serialport.service.ServiceSerialPort
import com.machine.serialport.util.*
import com.smdt.deviceauth.HexUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception
import java.lang.reflect.Type
import java.util.*

//首页启动更改
class HomeActivity : BaseActivity(), ServiceDataIpc {

    private var serviceSerialPort: ServiceSerialPort? = null
    private var mBound = false
    private var isDestroy = false
    private var isDeliver = false//数据是否处理中
    private var labelCache: MutableList<String> = mutableListOf()//标签缓存，如果同一个标签读取多次的话，则不再进行处理

    private var medical_workers_iv: View ?= null//放布草
    private var logistics_personnel_iv: View ?= null//取布草
    private var medical_workers_tv: View ?= null
    private var logistics_personnel_tv: View ?= null
    private var totalCount: TextView ?= null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
            serviceSerialPort = binder.getService()
            serviceSerialPort?.setSerialPortCallBack(this@HomeActivity)
            mBound = true
            serviceSerialPort?.startInventory()
            LogUtils.logE(mess = "服务绑定成功")
            if (!AppUtils.isDistribute(packageName)) {//假如是回收柜的话发送指令去检测是否满桶
                LogUtils.logE(mess = "服务绑定成功后开始进行状态监测")
                checkIsFill()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        initScene()
        initView()
        initData()
        initDeviceId()
//        initPermission()
//        bindSerialPortService()
    }

    override fun onStart() {
        super.onStart()
        LogUtils.logE(mess = "页面生命周期:onStart")
        parserInventoryLabelDescription()
        if (!AppUtils.isDistribute(packageName)) {//假如是回收柜的话发送指令去检测是否满桶
//            checkIsFill()
//            serviceSerialPort?.startInventory()
            bindSerialPortService()
        }
    }

    //初始化设备Id
    private fun initDeviceId(){
//        val deviceId = DeviceIdUtils.getDeviceId(this)
//        if ("-1" != deviceId){
//            Contacts.testDeviceId = deviceId
//            Contacts.testDistributeDeviceId = deviceId
//            Contacts.testRecoveryMasterDeviceId = deviceId
//            Contacts.testRecoverySlaveDeviceId = deviceId
//        }
    }

    override fun onPause() {
        super.onPause()
        unBindService()
    }
    override fun onResume() {
        super.onResume()
        LogUtils.logE(mess = "页面生命周期:onResume")
    }

    //初始化分发柜和回收柜
    private fun initScene(){
        GlobalScope.launch(context = Dispatchers.IO) {
            val deviceId = DataStoreUtils.getInstance().getDeviceId()
            if (deviceId.isNotEmpty()){
                Contacts.testDeviceId = deviceId
                return@launch
            }
            if (AppUtils.isDistribute(packageName)) {//假如是分发柜
                SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_BROADCAST)
                Contacts.testDeviceId = Contacts.testDistributeDeviceId
            }else{//假如是回收柜的话，将场景改为主柜，然后判断主柜是否满了，如果满了就将场景改为辅柜
                SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_MASTER)
                Contacts.testDeviceId = Contacts.testRecoveryMasterDeviceId
            }
        }

    }

    private fun initView(){
        medical_workers_iv = findViewById<View>(R.id.medical_workers_iv)
        totalCount = findViewById(R.id.total_count)
        medical_workers_iv?.setOnClickListener(this::onClick)
        logistics_personnel_iv = findViewById<View>(R.id.logistics_personnel_iv)
        logistics_personnel_iv?.setOnClickListener(this::onClick)
        medical_workers_tv = findViewById<View>(R.id.medical_workers_tv)
        medical_workers_tv?.setOnClickListener(this::onClick)
        logistics_personnel_tv = findViewById<View>(R.id.logistics_personnel_tv)
        logistics_personnel_tv?.setOnClickListener(this::onClick)
        findViewById<View>(R.id.manager_tv).setOnClickListener(this::onClick)
    }

    private fun initData(){
        if (AppUtils.isDistribute(packageName)){//假如是分发柜
            medical_workers_iv?.visibility = View.GONE
            logistics_personnel_iv?.visibility = View.VISIBLE
            medical_workers_tv?.visibility = View.VISIBLE
            logistics_personnel_tv?.visibility = View.GONE
        }else{
            medical_workers_iv?.visibility = View.VISIBLE
            logistics_personnel_iv?.visibility = View.GONE
            medical_workers_tv?.visibility = View.GONE
            logistics_personnel_tv?.visibility = View.VISIBLE
        }
    }
    //通过网络访问获取现存的电子标签解析内容
    private fun parserInventoryLabelDescription(){
        GlobalScope.launch {
            val cacheData = DataStoreUtils.getInstance().getInventoryCache()
            Log.e(">>>>>>>>>>>>", "获取数据:$cacheData-->数据是否为空:${TextUtils.isEmpty(cacheData)}")
            if (TextUtils.isEmpty(cacheData)){
                return@launch
            }
            val type: Type = object : TypeToken<ArrayList<String>>() {}.type
            labelCache = Gson().fromJson(cacheData,type)
            runOnUiThread {
                totalCount?.text = "当前柜子布草总数量为${labelCache.size}件"
            }
        }
    }
    private fun initPermission(){

        XXPermissions.with(this)
            .permission(Permission.SYSTEM_ALERT_WINDOW)
//            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
//                        startFaceService()
                        SerialPortApplication.getInstance().startSmartFaceService()
                        ToastUtils.show(this@HomeActivity, "权限已授予")
                    } else {
                        ToastUtils.show(this@HomeActivity, "获取部分权限成功，但部分权限未正常授予")

                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    if (never) {
                        ToastUtils.show(this@HomeActivity, "被永久拒绝授权，请手动授予悬浮窗权限")
                        // 如果是被永久拒绝就跳转到应用权限系统设置页面
                        XXPermissions.startPermissionActivity(this@HomeActivity, permissions)
                    } else {
                        ToastUtils.show(this@HomeActivity, "获取悬浮窗权限失败")
                    }
                }
            })
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
                serviceSerialPort?.stopInventory()
            }
        }catch (e: Exception){

        }
    }

    //检测满桶状态
    private fun checkIsFill(){//检测是否是满桶
        if (null == serviceSerialPort){
            ToastUtils.show(this,"服务未启动,请稍后...")
            return
        }
        LogUtils.logE(mess = "检测柜子状态")
//        serviceSerialPort?.writeHex(ContactsCmd.UN_LOCK_BROADCAST)
        serviceSerialPort?.writeHex(SerialPortCmdUtil.getReadStatusCmd())
    }

    private fun startFaceService(){
        val service = Intent(this, ServiceSerialPort::class.java)
        startService(service)
    }

    private fun onClick(v: View){
        when(v.id){
            R.id.medical_workers_tv,R.id.logistics_personnel_tv -> {
                LogUtils.logE(mess = "物流人员登录")
                Contacts.isDoctor = false
                unBindService()
                startActivity(Intent(
                    v.context,
                    ScanCodeActivity::class.java
                ))
            }
            R.id.logistics_personnel_iv,R.id.medical_workers_iv -> {
                LogUtils.logE(mess = "医护人员登录")
                Contacts.isDoctor = true
                unBindService()
                startActivity(Intent(
                    v.context,
                    LoginByFaceActivity::class.java
                ))
            }
            R.id.manager_tv -> {
                LogUtils.logE(mess = "管理员登录")
                unBindService()
                startActivity(Intent(
                    v.context,
                    ManagerLoginActivity::class.java
                ))

            }
        }
    }

    override fun onDataReceived(buffer: ByteArray, size: Int) {
        val status = HexUtils.toHex(buffer)
//            LogUtils.logE(mess = "接收的内容$receiverContent")
//            addData(receiverContent)
        parserReceiverData(status)
    }

    private fun parserReceiverData(status: String){
        val tempporaryStoreData = TemporaryStorageParserUtils(status)
        val isLock = tempporaryStoreData.isLock()
        val isFull = tempporaryStoreData.isFill() //衣物是否满了
        LogUtils.logE(mess = "HomeActivity---->满桶状态:$isFull")
        LogUtils.logE(mess = "是否为分发柜:${AppUtils.isDistribute(packageName)}")
//        if (SerialPortCmdUtil.scene == SerialPortCmdUtil.SCENE_SLAVE){//如果是从机场景则进行返回
//            return
//        }
        if(isFull && !AppUtils.isDistribute(packageName)){//满桶且是回收柜,设置开启辅助柜子的门
            LogUtils.logE(mess = "设置场景为从机场景")
            setSlaveDeviceId()
        }
    }

    private fun setSlaveDeviceId(){
        GlobalScope.launch(context = Dispatchers.IO) {
            val deviceId = DataStoreUtils.getInstance().getSubDeviceId()
            if (deviceId.isNotEmpty()){
                Contacts.testDeviceId = deviceId
                return@launch
            }
            SerialPortCmdUtil.setScene(SerialPortCmdUtil.SCENE_SLAVE)
            Contacts.testDeviceId = Contacts.testRecoverySlaveDeviceId
        }

    }

    override fun onError(resourceId: Int) {
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroy = true
        unBindService()
    }
}