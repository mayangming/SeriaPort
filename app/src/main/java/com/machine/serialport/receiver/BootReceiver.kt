package com.machine.serialport.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.machine.serialport.activity.SplashActivity

//开机自启动
class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        if (intent!!.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            val i = Intent(context, SplashActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context!!.startActivity(i)
        }
    }
}