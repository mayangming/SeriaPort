package com.machine.serialport.util

import android.util.Log

object LogUtils {
    fun logD(tag : String = "MYINFO", mess: String) {
        Log.d(tag, mess)
    }

    fun logE(tag : String = "YM", mess: String) {
        Log.e(tag, mess)
    }
}