package com.machine.serialport.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import com.machine.serialport.R
import com.machine.serialport.util.LogUtils
import com.machine.serialport.view.ProgressRing


class InventoryDialog: DialogFragment() {

    private var mProgressRing: ProgressRing? = null
    private var mProgressSecond = 5
    private val MESSAGE_PROGRESS = 0
    private var inventoryCountDownCallBack: (() -> Unit) ?= null
    private val mHandler = object : Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MESSAGE_PROGRESS -> {
                    mProgressSecond--
                    if (mProgressSecond <= 0) {
                        mProgressSecond = 5
                        inventoryCountDownCallBack?.invoke()
                        dismiss()
                        LogUtils.logE(mess = "关闭对话框")
                        return
                    }
                    mProgressRing?.setText(mProgressSecond.toString() + "S")
//                    val pro = (mProgressSecond * 10 / 6).toFloat()
                    val pro = (mProgressSecond * 20).toFloat()
                    mProgressRing?.progress = pro
                    sendEmptyMessageDelayed(MESSAGE_PROGRESS, 1000)
                }
                else -> {
                }
            }
        }
    }

    /**
     * Activity创建后才会有window
     */
    override fun onActivityCreated(@Nullable savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) //需要这一行来解决对话框背景有白色的问题(颜色随主题变动)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_inventory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mProgressRing = view.findViewById(R.id.pr_progress)
        mHandler.sendEmptyMessage(MESSAGE_PROGRESS)
        mProgressSecond = 5
    }

    fun setOnCountDownCallBack(callBack: () -> Unit){
        inventoryCountDownCallBack = callBack
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHandler.removeCallbacksAndMessages(null)
    }
}