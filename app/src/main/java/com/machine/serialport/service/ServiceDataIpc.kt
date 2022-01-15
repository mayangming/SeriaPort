package com.machine.serialport.service

//服务接口回调
interface ServiceDataIpc {
    fun onDataReceived(buffer: ByteArray, size: Int)
    fun onError(resourceId: Int)
}