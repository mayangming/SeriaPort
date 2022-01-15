package com.machine.serialport.util

/**
 * 寄存器内容解析处理
 * 格式类似于:
 * 00031A0000000000420362002B000000000000000200000000000000016B3E000000000000000000000000000000000000000000000000000000000000000000
 * 截取其中第15-18位,例如本例中的 0042 该段，然后将该段转换为二进制进行解码解码后内容参考文档
 */
class TemporaryStorageParserUtils {
    private var temporaryStorageCode = ""
    private var inputCode = "" //输入编码

    constructor(temporaryStorageCode: String){
        this.temporaryStorageCode = temporaryStorageCode
        getInputCode(temporaryStorageCode)
    }

    //数据长度是否满足寄存器内容格式
    public fun isOk() = temporaryStorageCode.length == 128

    private fun getInputCode(temporaryStorageCode: String){
        inputCode = temporaryStorageCode.substring(14,18)
        LogUtils.logE(mess = "获取的输入状态(原值):$inputCode")
        inputCode = HexUtil.hexString2binaryString(inputCode) //将结果翻转，然后获取相对应的内容
        inputCode = inputCode.reversed()
        inputCode = String.format("%-8s",inputCode).replace(" ","0") //不足8位补0
        LogUtils.logE(mess = "获取的输入状态(解析翻转值):$inputCode")
    }

    //门是否关闭
    //门信号， 0 表示关闭 1：表示打开
    public fun isLock():Boolean{
        val lockStatus = inputCode[3]
        return '0' == lockStatus
    }

    //是否满桶
    //满桶信号，  0 表示无效 1：表示满桶
    public fun isFill():Boolean{
        val lockStatus = inputCode[6]
        return '1' == lockStatus
    }

}