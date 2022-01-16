package com.machine.serialport.model

//网络请求的基础解析类
class HttpBaseResponseMode<T> {
    var code = 0
    var msg = ""
    var success = true //请求成功
    var data: T ?= null
}