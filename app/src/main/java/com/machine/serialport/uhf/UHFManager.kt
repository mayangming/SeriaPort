package com.machine.serialport.uhf

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.machine.serialport.util.DataStoreUtils
import com.machine.serialport.util.LogUtils
import com.uhf.api.cls.Reader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

//UHF的管理类，用来处理繁杂的链接逻辑
class UHFManager {
    private  var dv = Reader.deviceVersion()//设备版本
    private var mReader: Reader = Reader()//创建读写器,该读写器用来读写数据
    private var readParams: ReaderParams = ReaderParams()//读写器配置文件
    private var ipst: Reader.Inv_Potls_ST = mReader.Inv_Potls_ST()
    private val inventoryCache: HashSet<InventoryModel> = hashSetOf()
    private var uhfCallBack: ((InventoryModel)->Unit) ?= null//回调
    //获取保存的所有数据的回调,因为盘点的设备没有缓存功能，所以该功能是通过监听一段时间的数据来进行判断的，
    private var uhfAllCallBack: ((MutableList<InventoryModel>)->Unit) ?= null
    private var isStartInventory: Boolean = false //是否开始盘点
    private var inventoryDuration: Long = 4_000 // 盘点时间为十秒
    private var handler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what){
                0 -> {
                    val model: InventoryModel = msg.obj as InventoryModel
                    val mess = "本次读取的标签数量为:${model.labelSet.size},标签内容为:${model.labelSet}"
                    LogUtils.logE(mess = mess)
                    if(model.labelSet.size == 0){
                        return
                    }
                    uhfCallBack?.invoke(model)
                    if (isStartInventory){
                        inventoryCache.add(model)
                    }
                }
            }
        }
    }
    private val inventoryThread: InventoryThread = InventoryThread(mReader, readParams, handler)
    companion object{
        @JvmStatic
        private var managerInstance: UHFManager?= null
        fun getInstance(): UHFManager {
            if (managerInstance == null){
                managerInstance = UHFManager()
            }
            return managerInstance as UHFManager
        }
    }

    //建立链接
    fun connect(callBack: ()->Unit){
        GlobalScope.launch {
            Reader.GetDeviceVersion(UHFConfig.uhfAddress, dv)
            var error = mReader.InitReader_Notype(UHFConfig.uhfAddress, UHFConfig.portc)
            if (error == Reader.READER_ERR.MT_OK_ERR) {
                LogUtils.logE(mess = "链接成功")
                ipst.potlcnt = 1
                ipst.potls = arrayOfNulls(ipst.potlcnt)
                for (i in 0 until ipst.potlcnt) {
                    val ipl: Reader.Inv_Potl = mReader.Inv_Potl()
                    ipl.weight = 30
                    ipl.potl = Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2
                    ipst.potls[i] = ipl
                }
                error = mReader.ParamSet(
                    Reader.Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst
                )
                val av = IntArray(1)
                mReader.ParamGet(
                    Reader.Mtr_Param.MTR_PARAM_READER_AVAILABLE_ANTPORTS, av
                )
                initConfig()
                handler.post {
                    callBack.invoke()
                }
            }
        }
    }

    //断开链接
    fun disConnect(){
        mReader.CloseReader()
    }

    //链接成功后进行读写器配置
    private fun initConfig(){
        var er: Reader.READER_ERR
        val ltp: MutableList<Reader.SL_TagProtocol> = ArrayList()
        for (i in 0 until readParams.invpro.size) {
            if (readParams.invpro[i].equals("GEN2")) {
                ltp.add(Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2)
            }
        }
        val ipst: Reader.Inv_Potls_ST = mReader.Inv_Potls_ST()
        ipst.potlcnt = ltp.size
        ipst.potls = arrayOfNulls(ipst.potlcnt)
        val stp = ltp
            .toTypedArray()
        for (i in 0 until ipst.potlcnt) {
            val ipl: Reader.Inv_Potl = mReader.Inv_Potl()
            ipl.weight = 30
            ipl.potl = stp[i]
            ipst.potls[i] = ipl
        }
        er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst)
        Log.d("MYINFO", "Connected set pro:$er")

        er = mReader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_READER_IS_CHK_ANT,
            intArrayOf(readParams.checkant)
        )
        Log.d("MYINFO", "Connected set checkant:$er")

        val apcf: Reader.AntPowerConf = mReader.AntPowerConf()
        apcf.antcnt = UHFConfig.portc
        for (i in 0 until apcf.antcnt) {
            val jaap: Reader.AntPower = mReader.AntPower()
            jaap.antid = i + 1
            jaap.readPower = readParams.rpow[i].toShort()
            jaap.writePower = readParams.wpow[i].toShort()
            apcf.Powers[i] = jaap
        }
        mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPOWER, apcf);
        val rre: Reader.Region_Conf = when (readParams.region) {
            1 -> Reader.Region_Conf.RG_NA //除了1没别的
            else -> Reader.Region_Conf.RG_NONE
        }
        if (rre != Reader.Region_Conf.RG_NONE) {
            er = mReader.ParamSet(
                Reader.Mtr_Param.MTR_PARAM_FREQUENCY_REGION, rre)
        }

        if (readParams.frelen > 0) {
            val hdst: Reader.HoptableData_ST = mReader.HoptableData_ST()
            hdst.lenhtb = readParams.frelen
            hdst.htb = readParams.frecys
            er = mReader.ParamSet(
                Reader.Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst)
        }


        er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, intArrayOf(readParams.session))
        er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_Q, intArrayOf(readParams.qv))
        er = mReader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE, intArrayOf(readParams.wmode))
        er = mReader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN, intArrayOf(readParams.maxlen))
        er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, intArrayOf(readParams.target))

        if (readParams.filenable == 1) {
            val tfst: Reader.TagFilter_ST = mReader.TagFilter_ST()
            tfst.bank = readParams.filbank
            var len: Int = readParams.fildata.length
            len = if (len % 2 == 0) len else len + 1
            tfst.fdata = ByteArray(len / 2)
            mReader.Str2Hex(readParams.fildata,
                readParams.fildata.length, tfst.fdata)
            tfst.flen = readParams.fildata.length * 4
            tfst.startaddr = readParams.filadr
            tfst.isInvert = readParams.filisinver
            mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_FILTER, tfst)
        }

        if (readParams.emdenable == 1) {
            val edst: Reader.EmbededData_ST = mReader.EmbededData_ST()
            edst.accesspwd = null
            edst.bank = readParams.emdbank
            edst.startaddr = readParams.emdadr
            edst.bytecnt = readParams.emdbytec
            er = mReader.ParamSet(
                Reader.Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, edst)
        }

        er = mReader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYEMDDATA, intArrayOf(readParams.adataq))
        er = mReader.ParamSet(
            Reader.Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI, intArrayOf(readParams.rhssi))
        er = mReader.ParamSet(Reader.Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE, intArrayOf(readParams.invw))

        val apvr: Reader.AntPortsVSWR = mReader.AntPortsVSWR()
        apvr.andid = 1
        apvr.power = readParams.rpow[0].toShort()
        apvr.region = Reader.Region_Conf.RG_NA
        er = mReader.ParamGet(Reader.Mtr_Param.MTR_PARAM_RF_ANTPORTS_VSWR, apvr)

        val hardDetails: Reader.HardwareDetails = mReader.HardwareDetails()
        er = mReader.GetHardwareDetails(hardDetails)

    }

    //开始盘点
    fun startInventoryThread(){
        handler.postDelayed(inventoryThread, 0)
    }

    //停止盘点
    private fun stopInventoryThread(){
        handler.removeCallbacks(inventoryThread)
        handler.removeCallbacksAndMessages(null)
    }

    //获取现在所有的数据
    fun startGetCacheInventoryList(callBack : (MutableList<InventoryModel>)->Unit){
        isStartInventory = true
        inventoryCache.clear()
        handler.postDelayed({
            isStartInventory = false
            callBack(inventoryCache.toMutableList())
        },inventoryDuration)
    }

    fun destroyUhfManager(){
        stopInventoryThread()
        disConnect()
    }

    //添加回调
    fun addUhfListener(callBack : (InventoryModel)->Unit ){
        this.uhfCallBack = callBack
    }
    //添加回调
    fun addAllUhfListener(callBack : (MutableList<InventoryModel>)->Unit ){
        this.uhfAllCallBack = callBack
    }

}