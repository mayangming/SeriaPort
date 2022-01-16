package com.machine.serialport.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.machine.serialport.R

class SimpleDialog: BaseDialog() {
    var title = ""
    var content = ""
    var negativeTitle = ""
    var positiveTitle = ""
    var titleTv: TextView ?= null
    var contentTv: TextView ?= null
    var negativeTv: TextView ?= null
    var positiveTv: TextView ?= null
    private var onNegativeClick: (() -> Unit)? = null
    private var onPositiveClick: (() -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        isCancelable = false
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_simple,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTv = view.findViewById(R.id.dialog_title)
        contentTv = view.findViewById(R.id.dialog_content)
        negativeTv = view.findViewById(R.id.dialog_negative)
        positiveTv = view.findViewById(R.id.dialog_positive)
        negativeTv?.setOnClickListener(this::onClick)
        positiveTv?.setOnClickListener(this::onClick)
        setView()
    }

    private fun onClick(view: View){
        when(view.id){
            R.id.dialog_negative -> {
                this.onNegativeClick?.invoke()
                dismiss()
            }
            R.id.dialog_positive -> {
                this.onPositiveClick?.invoke()
                dismiss()
            }
        }
    }
    private fun setView(){
        val tempTitle = arguments?.getString("title")
        val tempContent = arguments?.getString("content")
        val tempNegativeTitle = arguments?.getString("negativeTitle")
        val tempPositiveTitle = arguments?.getString("positiveTitle")
        val tempIsCancelable = arguments?.getBoolean("isCancelable",false)
        val tempIsShowCancelBtn = arguments?.getBoolean("isShowCancelBtn",false)
        titleTv?.text = tempTitle
        contentTv?.text = tempContent
        negativeTv?.text = tempNegativeTitle
        positiveTv?.text = tempPositiveTitle
        isCancelable = tempIsCancelable ?: false
        negativeTv?.visibility = if(tempIsShowCancelBtn != false) View.VISIBLE else View.GONE
    }


    //取消监听
    fun setOnNegativeClick(callBack: () -> Unit){
        this.onNegativeClick = callBack
    }
    //确定监听
    fun setOnPositiveClick(callBack: () -> Unit){
        this.onPositiveClick = callBack
    }

}