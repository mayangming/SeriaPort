package com.machine.serialport.model

//人脸用户数据
data class UserFaceModel(
    var faceId: Int = -1,
    var faceName: String = "",
    var facePhone: String = "",
    var faceJob: String = "",
)
