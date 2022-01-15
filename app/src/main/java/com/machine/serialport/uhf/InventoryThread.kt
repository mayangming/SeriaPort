package com.machine.serialport.uhf

import android.os.Handler
import android.os.Message
import com.machine.serialport.util.LogUtils
import com.uhf.api.cls.Reader
import java.util.*
import kotlin.experimental.and

//盘点线程标签
//Reader: 读写器，用来进行读写操作
//ReaderParams: 读写参数，用来配置读写器
//轮询的原理是，当这里面处理完后，然后发消息给主线程开始进行下一个处理
class InventoryThread(val mReader: Reader,val readParams: ReaderParams,val handler: Handler) :Runnable{
    var labelSet:HashSet<String> = hashSetOf() //标签内容,里面的值不会重复
    var labelMap:MutableMap<String, String> = mutableMapOf() //存储了标签和天线绑定的数据
    var inventoryStatus = InventoryStatus.NORMAL //默认为正常
    override fun run() {
        labelSet = hashSetOf()
        var tag: ArrayList<String> = arrayListOf()
        val tagcnt = IntArray(1)
        tagcnt[0] = 0
        synchronized(this) {
            //Log.d("MYINFO", "read thread....");
            var er: Reader.READER_ERR
            er =  mReader.TagInventory_Raw(readParams.uants,
                    readParams.uants.size,
                    readParams.readtime.toShort(), tagcnt)
            // Log.d("MYINFO","read:" + er.toString() + " cnt:"+
            // String.valueOf(tagcnt[0]));
            if (er == Reader.READER_ERR.MT_OK_ERR) {
                if (tagcnt[0] > 0) {
//                    tvOnce?.setText("本次读到的标签数为:" + tagcnt[0].toString())
//                    tag = arrayOfNulls<String>(tagcnt[0])
                        tag = arrayListOf()
                    for (i in 0 until tagcnt[0]) {//将本次读到的所有内容发送出来
                        val tfs: Reader.TAGINFO = mReader.TAGINFO()
                        er = mReader.GetNextTag(tfs)
                        //	Log.d("MYINFO", "get tag cost time:"+ (edreadt2 - streadt2));
                        // Log.d("MYINFO","get tag index:" +
                        // String.valueOf(i)+ " er:" + er.toString());
                        if (er == Reader.READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) {
                            val error = "YM-------发生错误error:" + er.value().toString() + er.toString()
                            LogUtils.logE(mess = error)
                            inventoryStatus = InventoryStatus.ERROR
                            handler.removeCallbacksAndMessages(null)
                        }

                        if (er == Reader.READER_ERR.MT_OK_ERR) {
                            val EPC = Reader.bytes_Hexstr(tfs.EpcId)
                            tag.add(EPC)
                            tagsBufferResh(EPC, tfs)
                        } else break //一旦无法从缓冲区获取标签，就要重新调用读标签方法，不能继续获取标签缓冲
                    }
                }
            } else {
                LogUtils.logE(mess = "发生错误，${er}")
                inventoryStatus = InventoryStatus.ERROR
                return
            }
        }
        if (tag == null) {
            tag = arrayListOf()
        }
        if (labelSet.isNotEmpty()){
            val tempLabelSet = labelSet.clone() as HashSet<String>
//            val tempLabelMap = labelMap.clone() as MutableMap<String, String>
            var tempLabelMap:MutableMap<String, String> = mutableMapOf()
            tempLabelMap.putAll(labelMap)
//            sendMessage(tempLabelSet,labelMap,inventoryStatus)
            sendMessage(tempLabelSet,tempLabelMap,inventoryStatus)
        }
        labelSet.clear()//数据发送后，这里就将数据清空
        labelMap.clear()//数据发送后，这里就将数据清空
        handler.postDelayed(this, 400)
    }

    /**
     * 刷新标签缓冲，更新标签列表信息，根据是否u8标签，是否附加数据唯一，天线唯一来列表
     *
     * @param EPC
     * @param tfs
     */
    private fun tagsBufferResh(EPC: String, tfs: Reader.TAGINFO) {
        val key = EPC
        val epcstr = EPC
        val tid = ""
        val bid = ""
        val emdstr = ""
        var rfu = ""
        var antId = tfs.AntennaID //天线
        //rfu=String.valueOf(((tfs.Res[0]<<8|tfs.Res[1])&0x3f)*180/64);
        rfu = (tfs.Res[1] and 0x3f).toString()
//        if (!TagsMap.containsKey(key)) {
//
//            TagsMap[key] = tfs
//            totalcount++
//            // show
//            tvSum?.text = "共读到的标签个数为${totalcount}"
//        }
//        LogUtils.logE(mess = "标签内容rfu:${rfu}")
        LogUtils.logE(mess = "标签内容epcstr:${epcstr}")
//        sendMessage(label = rfu)
        labelMap[epcstr] = antId.toString()
        labelSet.add(epcstr)
    }



    private fun sendMessage(labelSet: HashSet<String>,labelMap: MutableMap<String, String>,status: Int = InventoryStatus.ERROR){
//        LogUtils.logE(mess = "标签的天线Map对象:${labelMap}")
        val model = InventoryModel()
        model.labelSet = labelSet
        model.status = status
        model.labelMap = labelMap
        val obt = Message.obtain()
        obt.what = 0
        obt.obj = model
        handler.sendMessage(obt)
    }

}