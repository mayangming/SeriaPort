package com.machine.serialport.activity

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.postDelayed
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.trinea.android.common.util.ToastUtils
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.machine.serialport.R
import com.machine.serialport.SerialPortApplication
import com.machine.serialport.adapter.InventoryListAdapter
import com.machine.serialport.contact.Contacts
import com.machine.serialport.dialog.InventoryDialog
import com.machine.serialport.dialog.SimpleDialog
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.HttpApi.getURL
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.EmptyObjctModel
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.model.InventoryItemModel
import com.machine.serialport.service.ServiceDataIpc
import com.machine.serialport.service.ServiceSerialPort
import com.machine.serialport.uhf.UHFManager
import com.machine.serialport.util.*
import com.smdt.deviceauth.HexUtils
import http.OkHttpUtil
import http.callback.StringCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.Type

//进入的主页
class MainActivity : BaseActivity(), ServiceDataIpc {
    private val delayedCloseType = 1
    private val delayedDuration: Long = 40_000 //延迟时间
    private var serviceSerialPort: ServiceSerialPort? = null
    private var mBound = false
    private var scroller: NestedScrollView ?= null
    private var inventoryTotalTv: TextView ?= null
    private var inventoryRecycleView: RecyclerView ?= null
    private var submitBtn:View ?= null
    private var inventoryAdapter: InventoryListAdapter ?= null
    private var inventoryData = mutableListOf<InventoryItemModel>()
    private var labelCache: MutableList<String> = mutableListOf()//标签缓存，如果同一个标签读取多次的话，则不再进行处理
    private var labelMap:MutableMap<String, String> = mutableMapOf() //存储了标签和天线绑定的数据
    private var inventoryTotal = 0 // 物品总数量
    private var uhfManager: UHFManager ?= null
    private var isDestroy = false
    private val dialog = SimpleDialog()
    private val closeDoorHandle: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
//                heartType -> sendHeartData()
                delayedCloseType -> {//延迟关门
                    autoClosePutter()
                }
            }
        }
    }
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: ServiceSerialPort.LocalBinder = service as ServiceSerialPort.LocalBinder
            serviceSerialPort = binder.getService()
            serviceSerialPort?.setSerialPortCallBack(this@MainActivity)
            mBound = true
            serviceSerialPort?.startInventory()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        initData()
        initDialog()
        initConnectInventoryThread()
        scroller?.postDelayed({
            bindSerialPortService()
        },1000)
        startPreviewData()
        closeDoorHandle.sendEmptyMessageDelayed(delayedCloseType,delayedDuration)
    }
    val argument = Bundle()
    private fun initDialog(){
        argument.putString("title","系统提示")

        argument.putString("negativeTitle","取消")
        argument.putString("positiveTitle","确定")
        argument.putBoolean("isCancelable",false)
        argument.putBoolean("isShowCancelBtn",false)
        dialog.arguments = argument
    }

    private fun initConnectInventoryThread(){
        uhfManager = UHFManager.getInstance()
        uhfManager?.connect {//防止主线程阻塞，内部使用子线程
            uhfManager?.startInventoryThread()
        }
//        uhfManager?.addUhfListener {
//            //接收到盘点的数据
//            it.labelSet.toArray().forEach { value ->
//                val label = value.toString()
//                LogUtils.logE("YM","本次读取的标签为:${label}")
//                addData(label)
//            }
//        }
    }


    private fun initView(){
        findViewById<View>(R.id.inventory_back).setOnClickListener(this::onClick)
        submitBtn = findViewById<View>(R.id.btn_submit)
        submitBtn?.setOnClickListener(this::onClick)
        inventoryTotalTv = findViewById(R.id.inventory_total)
        inventoryRecycleView = findViewById(R.id.inventory_rv)
        scroller = findViewById(R.id.scroller)
        initRecycleView(inventoryRecycleView!!)
//        submitBtn?.visibility = View.VISIBLE
        if (!AppUtils.isDistribute(packageName)) {
           if(Contacts.isDoctor){//提交按钮是否隐藏显示
                submitBtn?.visibility = View.VISIBLE
            }else{
                submitBtn?.visibility = View.GONE
            }
        }else{
            submitBtn?.visibility = View.GONE
        }
//        if(!Contacts.isDoctor){//提交按钮是否隐藏显示
//            submitBtn?.visibility = View.VISIBLE
//        }else{
//            submitBtn?.visibility = View.GONE
//        }
    }

    private fun initRecycleView(recyclerView: RecyclerView){
        inventoryAdapter = InventoryListAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = inventoryAdapter
    }


    private fun initData(){
//        inventoryData = TestData.getInventoryListData()
        inventoryAdapter?.setAdapterData(inventoryData)
        updateTotal()
        parserInventoryLabelDescription()
        scroller?.postDelayed(500){
            scroller?.fullScroll(NestedScrollView.FOCUS_UP)//滚动到顶部
        }
//        showInventoryDialog()
    }

    private fun startPreviewData(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            inventoryPreview(token)
        }
    }
    //结束程序时候进行预览
    private fun endPreviewData(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            inventoryPreview(token,true)
        }
    }
    //设备内容预览
    private fun inventoryPreview(token: String,isGoHome: Boolean = false){
        val jsonObject = JSONObject()
        jsonObject.put("deviceId",Contacts.testDeviceId)
        jsonObject.put("code","")
        jsonObject.put("catId","")
        jsonObject.put("id","")
        jsonObject.put("title","")
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.GOODS_PREVIEW))
            .content(jsonObject.toString())
            .addHeader("token",token)
            .build().execute(object : SerialPortHttpCallBack<List<InventoryItemModel>>(){
                override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                    ToastUtils.show(this@MainActivity,"网络，请检查网络!")
                    isInventoryIng = false
                }

                override fun onResponse(
                    response: HttpBaseResponseMode<List<InventoryItemModel>>,
                    id: Int
                ) {
                    if (!response.success){
                        ToastUtils.show(this@MainActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                        return
                    }
                    val data = response.data ?: return
                    inventoryData.clear()
                    inventoryData.addAll(data)
                    inventoryAdapter?.setAdapterData(inventoryData)
                    updateTotal()
                    if (isGoHome){
                        scroller?.postDelayed({//延迟3秒后返回首页，用来给用户看更新后的数据
                            goLogin()
                        },3000)
                    }
                }
            })
    }

    //获取当前缓存的电子标签，该函数需要在盘点线程完全启动后执行
    private fun getCacheLabel(){
//        val cacheJson = Gson().toJson(labelCache)
//        GlobalScope.launch {
//            Log.e(">>>>>>>>>>>>", "保存数据:$cacheJson")
//            DataStoreUtils.getInstance().putInventoryCache(cacheJson)
//            startUploadData()
//        }

        uhfManager?.startGetCacheInventoryList {
            LogUtils.logE("YM","关门盘点程序--11111111111111111111111")
            labelCache.clear()
            if (it.isEmpty()){
                LogUtils.logE("YM","关门盘点程序--读取数据为空")
//                labelCache.clear()
            }

            val flatMap = it.flatMap { flat ->
                flat.labelSet

            }.toHashSet()
            it.forEach { item ->
               labelMap.putAll(item.labelMap)
                LogUtils.logE("YM","标签的天线信息:${item.labelMap}")
            }
            Log.e("YM", "缓存标签---保存的缓存标签内容000000:$labelCache")
            labelCache.clear()
            flatMap.forEach { flatResult ->
                LogUtils.logE("YM","关门盘点程序--本次读取的缓存标签为:${flatResult}")
                addData(flatResult)
            }
            Log.e("YM", "缓存标签---保存的缓存标签内容:$labelCache")
            Log.e("YM", "缓存标签---获取的标签数据:$labelMap")
            val cacheJson = Gson().toJson(labelCache)
            GlobalScope.launch {
                Log.e(">>>>>>>>>>>>", "保存数据:$cacheJson")
                DataStoreUtils.getInstance().putInventoryCache(cacheJson)
                startUploadData()
            }
        }
    }
    //根据id列表获取标签内容
    private fun parserLabels(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            val jsonArray = JSONArray()
            labelCache.map {
                it.replace(" ","")
            }.forEach {
                jsonArray.put(it)
            }
            val jsonObject = JSONObject()
            jsonObject.put("ids", jsonArray)
            OkHttpUtil.postJson()
                .url(HttpApi.getURL(HttpApi.LABELS_DES))
                .content(jsonObject.toString())
                .addHeader("token",token)
                .build().execute(object : SerialPortHttpCallBack<List<InventoryItemModel>>(){
                    override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                        ToastUtils.show(this@MainActivity,"网络，请检查网络!")
                    }

                    override fun onResponse(
                        response: HttpBaseResponseMode<List<InventoryItemModel>>,
                        id: Int
                    ) {
                        if (!response.success){
                            ToastUtils.show(this@MainActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                            return
                        }
                        val data = response.data ?: return
                        data.forEach { newItem ->
                            var isHas = false //是否已经包含了该标签
                            inventoryData.forEach { oldItem ->
                                if (newItem.catId == oldItem.catId){
                                    oldItem.totalCount++
                                    isHas = true
                                    return@forEach
                                }
                            }
                            if (!isHas){
                                inventoryData.add(newItem)
                            }
                        }
                        inventoryAdapter?.setAdapterData(inventoryData)
                        updateTotal()
                    }
                })
        }
    }
    private fun onClick(v: View){
        when(v.id){
            R.id.btn_submit -> {
                if (!AppUtils.isDistribute(packageName)) {//假如是回收柜 且是物流人员的话
                    closeDoorHandle.removeCallbacksAndMessages(null)
                    closePutter()
//                    labelCache.add("10001")
//                    labelCache.add("10002")
                    showInventoryDialog()
                }
            }
//            R.id.inventory_back -> {
////                goLogin()
////                labelCache.add("010070000000000000767567")
//                showInventoryDialog()
//            }
        }
    }
    private fun autoClosePutter(){
        if (!AppUtils.isDistribute(packageName) && Contacts.isDoctor) {//假如是回收柜 且是物流人员的话
            closeDoorHandle.removeCallbacksAndMessages(null)
            closePutter()
//                    labelCache.add("10001")
//                    labelCache.add("10002")
            showInventoryDialog()
        }
    }
    private fun closePutter(){
        if (null == serviceSerialPort){
            ToastUtils.show(this,"机器服务未启动,请稍后...")
            return
        }
//                    serviceSerialPort?.writeHex(ContactsCmd.CLOSE_Putter)
//                    serviceSerialPort?.writeHex(ContactsCmd.Close_Putter_BROADCAST)
        serviceSerialPort?.writeHex(SerialPortCmdUtil.getClosePutterCmd())
    }

    override fun onBackPressed() {
//        super.onBackPressed()
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
        }
    }
    private fun startUploadData(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            uploadInventoryData(token)
        }
    }
    //上传盘点的数据

    private fun uploadInventoryData(token: String){
//        if(labelCache.isEmpty()){
//            try {
//                ToastUtils.show(this@MainActivity,"该次操作没有投放皮草!")
//            }catch (e: Exception){
//            }
//
//            return
//        }
        val jsonArray = JSONArray()
        labelCache.forEach {
            jsonArray.put(it)
        }
        val jsonObject = JSONObject()
        jsonObject.put("deviceId",Contacts.testDeviceId)
        jsonObject.put("codes", jsonArray)
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.GOODS_INVENTORY))
            .content(jsonObject.toString())
            .addHeader("token",token)
            .build().execute(object : StringCallback(){
                override fun onError(call: okhttp3.Call?, e: Exception?, id: Int) {
                    LogUtils.logE(mess = "${e?.message}")
                    isInventoryIng = false
                }

                override fun onResponse(response: String?, id: Int) {
                    LogUtils.logE(mess = "$response")
//                    isInventoryIng = false
//                    goLogin()
//                    finish()
//                    startPreviewData()
                }

            })
    }

    private var isInventoryIng = false
    private fun showInventoryDialog(){
        if (isInventoryIng){
            return
        }
        isInventoryIng = true
        val dialog = InventoryDialog()
        dialog.isCancelable = false
        dialog.setOnCountDownCallBack {
//            startUploadData()
//            closeDoor()
            endPreviewData()
//            isInventoryIng = false
        }
        dialog.show(supportFragmentManager,"inventoryDialog")
        getCacheLabel()
    }

    //关门逻辑处理
    private fun closeDoor(){
//        if (AppUtils.isDistribute(packageName)) {//假如是分发柜
//            goLogin()
//        }else{//回收柜的话需要弹出对话框列表
//            closeDoorInventoryData()
//        }
        closeDoorInventoryData()
    }

    //退出账号
    private fun logOut() {
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            Log.e("YM", "退出登录")
            OkHttpUtil.postJson()
                .url(getURL(HttpApi.USER_LOGOUT))
                .addHeader("token",token)
                .build()
                .execute(object : StringCallback() {
                    override fun onError(call: Call, e: java.lang.Exception, id: Int) {}
                    override fun onResponse(response: String, id: Int) {}
                })
        }

    }
    private fun closeDoorInventoryData(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            closeDoorInventory(token)
        }
    }

    //查找最近一次变化的数据
    private fun closeDoorInventory(token: String){
        val jsonObject = JSONObject()
        jsonObject.put("deviceId",Contacts.testDeviceId)
        jsonObject.put("code","")
        jsonObject.put("catId","")
        jsonObject.put("id","")
        jsonObject.put("title","")
        OkHttpUtil.postJson()
            .url(HttpApi.getURL(HttpApi.STATE_CHANGE))
            .content(jsonObject.toString())
            .addHeader("token",token)
            .build().execute(object : SerialPortHttpCallBack<List<InventoryItemModel>>(){
                override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                    ToastUtils.show(this@MainActivity,"网络错误，请检查网络!")
                }

                override fun onResponse(
                    response: HttpBaseResponseMode<List<InventoryItemModel>>,
                    id: Int
                ) {
                    if (!response.success){
                        ToastUtils.show(this@MainActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                        return
                    }
                    LogUtils.logE(mess = "差异数据是否为null：${response.data == null}")
                    val data = response.data ?: return

                    showDiffDialog(data)

//                    inventoryData.clear()
//                    inventoryData.addAll(data)
//                    inventoryAdapter?.setAdapterData(inventoryData)
//                    updateTotal()
                }
            })
    }

    //显示差异的对话框
    private fun showDiffDialog(data: List<InventoryItemModel>) {
//        val diffData = data.filter {
//            it.totalCount > 0
//        }.map {
//           "${it.totalCount}.${it.catidDicttext}"
//        }
        val diffData = data.filter {
            it.deviceId == Contacts.testDeviceId
        }.map {
            var title = ""
            if (it.totalCount > 0){
                title = "增加${Math.abs(it.totalCount)}件"
            }else{
                title = "取出${Math.abs(it.totalCount)}件"
            }
           "名字:${it.catidDicttext} 数量变化:${title}"
        }
        var title2 = "投放"
        if (AppUtils.isDistribute(packageName)){//分发柜
            if (Contacts.isDoctor){
                title2 = "取走"
            }else{
                title2 = "投放"
            }
        }else{//回收柜
            if (Contacts.isDoctor){
                title2 = "投放"

            }else{
                title2 = "取走"
            }
        }

        if (diffData.isEmpty()){
//            dialog.content = "本次没有${title2}衣物!"
            argument.putString("content","本次没有${title2}衣物!")
            dialog.arguments = argument
//            MaterialDialog(this).show {
//                title(text = "系统提示")
//                message(text = "本次没有${title2}衣物!")
//                negativeButton(text = "取消")
//                positiveButton(text = "确定"){
//                    goLogin()
//                }
//            }
            dialog.setOnPositiveClick {
                goLogin()
            }
            dialog.show(supportFragmentManager,"TAG")
            return
        }
//        val title = if (Contacts.isDoctor) "新增皮草" else "取出皮草"
        val title = "皮草数量变化"

        MaterialDialog(this).show {
            cancelOnTouchOutside(false)
            cancelable(false)
            title(text = title)
            listItems(items = diffData){ dialog, index, text ->
                // Invoked when the user selects an item
            }
            positiveButton(text = "确定"){
                LogUtils.logE(mess = "点击确认按钮")
                goLogin()
            }
        }
    }

    override fun onDataReceived(buffer: ByteArray, size: Int) {
        if (isDestroy){
            return
        }
        runOnUiThread {
//            Log.e(TAG, "接收的值mReceptionS是否为null: " + (null == mReception))
//            if (mReception != null) {
            val receiverContent = String(buffer, 0, size)
            val status = HexUtils.toHex(buffer)
//            LogUtils.logE(mess = "接收的内容$receiverContent")
//            addData(receiverContent)
            parserReceiverData(status)
        }
    }

    private fun parserReceiverData(status: String){
        val tempporaryStoreData = TemporaryStorageParserUtils(status)
        val isLock = tempporaryStoreData.isLock()
        val isFull = tempporaryStoreData.isFill() //衣物是否满了
        LogUtils.logE(mess = "是否锁门了:$isLock:--->$status")
        LogUtils.logE(mess = "是否是分发柜:${AppUtils.isDistribute(packageName)}")
        LogUtils.logE(mess = "医生状态:${Contacts.isDoctor}")
//        if (!Contacts.isDoctor && isLock){
        if (!AppUtils.isDistribute(packageName)) {//回收柜
            if (!Contacts.isDoctor && isLock){//假如不是医生切锁门了就进行上传
                //关门了
                //开始上传数据
//            goLogin()
                showInventoryDialog()
            }
        }else{//分发柜就进行上传数据关门盘点
            if (isLock){//假如不是医生切锁门了就进行上传
                //关门了
                //开始上传数据
//            goLogin()
                showInventoryDialog()
            }
        }

        LogUtils.logE(mess = "满桶状态:$isFull")
        if(isFull){//满桶状态
            uploadStatusWhenFull()
        }
    }

    //当衣物满的话，将该状态通知服务器
    private fun uploadStatusWhenFull(){
        GlobalScope.launch {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            val jsonObject = JSONObject()
            jsonObject.put("id",Contacts.testDeviceId)
            jsonObject.put("state","1")
            OkHttpUtil.postJson()
                .url(HttpApi.getURL(HttpApi.UPDATE_STATE))
                .content(jsonObject.toString())
                .addHeader("token",token)
                .build().execute(object : SerialPortHttpCallBack<EmptyObjctModel>(){
                    override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                        ToastUtils.show(this@MainActivity,"网络错误，请检查网络!")
                    }

                    override fun onResponse(
                        response: HttpBaseResponseMode<EmptyObjctModel>,
                        id: Int
                    ) {
                        if (!response.success){
                            ToastUtils.show(this@MainActivity,"服务出错，请联系运营商，错误信息:${response.msg}")
                            return
                        }
                    }
                })
        }
    }

    private fun goLogin(){
        logOut()
        startActivity(
            Intent(
                this,
                SplashActivity::class.java
            )
        )
        finish()
    }
    //添加标签
    private fun addData(label: String){
        if (labelCache.contains(label)){
            return
        }
        labelCache.add(label)
        //接收到盘点的数据
//        val model = InventoryItemModel("仪器",3)
//        inventoryAdapter?.addAdapterData(model)
//        updateTotal()
    }

    private fun updateTotal(){
        val data = inventoryAdapter?.getData()
        inventoryTotal = data!!.sumBy {
            LogUtils.logE(mess = "标签数量为:"+it.totalCount)
            it.totalCount
        }
        LogUtils.logE(mess = "标签总数量为:"+inventoryTotal)
        inventoryTotalTv?.text = "$inventoryTotal"
    }

    override fun onError(resourceId: Int) {
        DisplayError(resourceId)
    }

//    override fun onBackPressed() {
////        super.onBackPressed()
//
//    }

    override fun onDestroy() {
        super.onDestroy()
        closeDoorHandle.removeCallbacksAndMessages(null)
        isDestroy = true
        closePutter()
        unBindService()
        UHFManager.getInstance().destroyUhfManager()
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

}