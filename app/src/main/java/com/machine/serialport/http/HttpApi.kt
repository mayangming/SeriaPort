package com.machine.serialport.http

object HttpApi {
    //基础接口
    const val BASE_URL = "http://rfid.hw.jeegen.com"
    //用户信息
    const val USER_INFO = "/api/client/sys/user/info/"
    //用户人脸信息注册
    const val USER_FACE_REG = "/api/client/sys/user/face-reg"
    //用户登录
    const val USER_LOGIN = "/api/client/sys/user/login"
    //退出登录
    const val USER_LOGOUT = "/api/client/sys/user/logout"
    //二维码登录
    const val QRCODE_LOGIN = "/api/client/sys/user/qrcode-login"
    //人脸登录
    const val FACE_LOGIN = "/api/client/sys/user/face-login"

    //关门盘点
    const val GOODS_INVENTORY = "/api/client/goods/goods/stat-build"
    //查找物品概览
    const val GOODS_PREVIEW = "/api/client/goods/goods/stat-preview"

    //查找最近一次变化的数据
    const val STATE_CHANGE = "/api/client/goods/goods/stat-change"

    //根据标签ID获取标签内容
    const val LABELS_DES = "/api/client/goods/goods/list-by-ids"
    //修改设备的满桶状态
    const val UPDATE_STATE = "/api/client/base/device/state"

    //壁纸
    const val DEVICE_PREVIEW = "/api/client/base/device/image"

    //心跳
    const val DEVICE_BEAT = "/api/client/base/device/beat"

    //获取科室/柜子位置列表
    const val LIST_LOCATION = "/api/client/base/device/listLocation"

    //获取医院列表
    const val HOSPITAL_LIST = "/api/client/sys/depart/hospital-list"


    fun getURL(url: String) = BASE_URL + url

}