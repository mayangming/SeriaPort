package com.machine.serialport.service

import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Toast
import com.machine.serialport.contact.Contacts
import com.machine.serialport.contact.ContactsCmd
import com.machine.serialport.util.LogUtils
import com.machine.serialport.util.SerialPortCmdUtil
import com.machine.serialport.util.SerialPortWorkThread
import java.io.IOException

/**
 * 串口通信后台服务
 * 同一个路径只能使用一个波特率
 */
class ServiceSerialPort :BaseService(), SerialPortWorkThread.SerialPortDataReceiverIpc {
    private val TAG = "YM"
    private val SERIAL_PORT_STATUS_TAG = 1 //读取寄存器状态的标记
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null
    // Binder given to clients
    private val binder = LocalBinder()
    private var serialPortWorkThreadS3: SerialPortWorkThread? = null
    private var serialPortWorkThreadS1: SerialPortWorkThread? = null
    private var serialPortCallBack: ServiceDataIpc ?= null
    //线程缓存
    private var serialWorkThreadPool: MutableMap<String, SerialPortWorkThread> = mutableMapOf()
    private var isStartStatusListener = false // 是否开启了状态监听
    private var isStop = false
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
//            stopSelf(msg.arg1)
            when(msg.what){
                SERIAL_PORT_STATUS_TAG -> {
                    startStatusListener()
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): ServiceSerialPort = this@ServiceSerialPort
    }

    override fun onCreate() {
        Log.e(TAG, "服务启动---->onCreate")
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }
        initSerialPortWorkThread();

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "服务启动---->onStartCommand")
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        serviceHandler?.obtainMessage()?.also { msg ->
            msg.arg1 = startId
            serviceHandler?.sendMessage(msg)
        }

        // If we get killed, after returning from here, restart
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e(TAG, "服务启动---->onBind")
        // We don't provide binding, so return null
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e(TAG, "服务解绑---->onUnbind")
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        Log.e(TAG, "服务启动---->onRebind")
        super.onRebind(intent)
    }

    override fun onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show()
        isStartStatusListener = false
        serviceHandler?.removeCallbacksAndMessages(null)
        stopAllSerialPortByPath()
    }



    //初始化工作线程
    //这里应该把所有的端口都启动起来
    private fun initSerialPortWorkThread(){
        serialPortWorkThreadS3 = SerialPortWorkThread()
        serialPortWorkThreadS3?.create(Contacts.ttyS3, Contacts.baudrate9600)
        serialPortWorkThreadS3?.setOnSerialPortDataReceiverIpc(this)

        serialPortWorkThreadS1 = SerialPortWorkThread()
        serialPortWorkThreadS1?.create(Contacts.ttyACM0, Contacts.baudrate115200)
        serialPortWorkThreadS1?.setOnSerialPortDataReceiverIpc(this)
        serialWorkThreadPool[Contacts.ttyS3] = serialPortWorkThreadS3!!
        serialWorkThreadPool[Contacts.ttyACM0] = serialPortWorkThreadS1!!
    }

    /**
     * 写入文本
     */
    @Throws(IOException::class)
    fun writeText(text: String) {
        serialWorkThreadPool[Contacts.ttyS3]?.writeText(text)
    }

    /**
     * 写入十六进制
     */
    fun writeHex(text: String?) {
        LogUtils.logE(mess = "发送的指令为:$text")
        val serialPort = serialWorkThreadPool[Contacts.ttyS3]
        if(serialPort == null){
//            LogUtils.logE("YM","串口为null")
        }else{
//            LogUtils.logE("YM","串口不为null")
            serialPort.writeHex(text)
        }

    }
    //停止端口
    private fun stopSerialPortByPath(path: String){
        serialWorkThreadPool[Contacts.ttyS3]?.clear()
    }

    //停止所有端口
    private fun stopAllSerialPortByPath(){
        serialWorkThreadPool.forEach { (_, thread) ->
            thread.clear()
        }
    }

    fun setSerialPortCallBack(serialPortCallBack: ServiceDataIpc){
        this.serialPortCallBack = serialPortCallBack
    }

    //开始盘点
    fun startInventory(){
        isStop = false
        startStatusListener()
    }

    //停止盘点
    fun stopInventory(){
        isStop = true
    }

    //开启寄存器状态监听
    private fun startStatusListener(){
//        LogUtils.logE(mess = "发送获取状态的命令0000000000-->${isStartStatusListener}")
//        if (isStartStatusListener){
//            return
//        }
        if (isStop){
            return
        }
        serviceHandler?.removeMessages(SERIAL_PORT_STATUS_TAG)
//        LogUtils.logE(mess = "发送获取状态的命令111111111111-->${isStartStatusListener}")
        isStartStatusListener = true
//        writeHex(ContactsCmd.READ_STATUS_BROADCAST)
        writeHex(SerialPortCmdUtil.getReadStatusCmd())
        serviceHandler?.sendEmptyMessageDelayed(SERIAL_PORT_STATUS_TAG,500)
    }

    override fun onDataReceived(buffer: ByteArray, size: Int) {
        serialPortCallBack?.onDataReceived(buffer, size)
    }

    override fun onError(resourceId: Int) {
        serialPortCallBack?.onError(resourceId)
    }
}