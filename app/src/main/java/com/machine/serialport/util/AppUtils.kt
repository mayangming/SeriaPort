package com.machine.serialport.util

import com.machine.serialport.contact.Contacts

//对App的一些检测
object AppUtils {

    //是否是分发柜
    fun isDistribute(applicationId: String) = applicationId == Contacts.distributeAppId
}