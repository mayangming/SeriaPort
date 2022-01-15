package com.machine.serialport.contact

//一些常用字段
object Contacts {
    //以下为本机可使用路径
    val ttyS0 = "/dev/ttyS0"
    val ttyS1 = "/dev/ttyS1"
    val ttyS2 = "/dev/ttyS2"
    val ttyS3 = "/dev/ttyS3"
    val ttyS4 = "/dev/ttyS4"
    val ttyACM0 = "/dev/ttyACM0"
//    val ttyACM0 = "/dev/ttyACM1"

    //以下为波特率
    val baudrate9600 = 9600
    val baudrate115200 = 115200

    //测试的设备ID
    var testDeviceId = "1"
    //分发柜的设备ID
    var testDistributeDeviceId = "1"
    //回收柜的主柜的设备ID
    var testRecoveryMasterDeviceId = "2"
    //回收柜的辅柜的设备ID
    var testRecoverySlaveDeviceId = "3"
        set(value) {
            field = value+"_1"
        }


    //人脸登录的code
    val faceCode = "B6M8FlVOcFrdJgXjhQxquXqES3bkUlx2"
    val qrCode = "313335313135323033393634343339373937"
    //人脸登录的code
//    val faceCode = "B6M8FlVOcFrdJgXjhQxquXqES3bkUlx2"

    //分发柜
    val distributeAppId = "com.machine.serialport.distribute"
    //回收柜
    val recoveryAppId = "com.machine.serialport.recovery"

    var isDoctor = false //是否是医生

}