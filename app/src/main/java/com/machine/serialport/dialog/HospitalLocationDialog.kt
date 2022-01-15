package com.machine.serialport.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import cn.trinea.android.common.util.ToastUtils
import com.machine.serialport.adapter.DepartmentAdapter
import com.machine.serialport.adapter.HospitalAdapter
import com.machine.serialport.databinding.DialogHospitalLocationBinding
import com.machine.serialport.http.HttpApi
import com.machine.serialport.http.SerialPortHttpCallBack
import com.machine.serialport.model.DepartmentModel
import com.machine.serialport.model.HospitalModel
import com.machine.serialport.model.HttpBaseResponseMode
import com.machine.serialport.util.DataStoreUtils
import http.OkHttpUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import org.json.JSONObject


const val TYPE_NONE = "none" //根节点
const val TYPE_HOSPITAL = "hospital" //医院
const val TYPE_DEPT = "dept" //部门
const val TYPE_LOCATION = "location" //定位
const val TYPE_DISTRIBUTION = "distribution" //分发柜
const val TYPE_RECYCLE = "recycle" //回收柜
const val TYPE_CABINET = "cabinet" //柜子

//选择柜子位置的UI
class HospitalLocationDialog: DialogFragment() {
    private var binding: DialogHospitalLocationBinding ?= null
    private var hospitalAdapter: HospitalAdapter ?= null

    private var departmentAdapter: DepartmentAdapter?= null

    //选择结果返回
    var selectDeviceCallBack: ((DepartmentModel) -> Unit) ?= null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogHospitalLocationBinding.inflate(inflater,container,false)
        val root = binding?.root ?: super.onCreateView(inflater, container, savedInstanceState)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    private fun initView(){

        initRecycleView()

    }

    private fun initRecycleView(){
        binding?.hospitalList?.apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun initData(){
        getToken()
    }

    private fun updateHospitalRecycleView(hospitalList : List<HospitalModel>){
        binding?.hospitalList?.apply {
            hospitalAdapter = HospitalAdapter(hospitalList){
                queryDepartmentOrDeviceList(it.id, TYPE_HOSPITAL)
            }
            adapter = hospitalAdapter
        }
    }

    private fun updateDepartmentRecycleView(department : List<DepartmentModel>){
        binding?.hospitalList?.apply {
            departmentAdapter = DepartmentAdapter(department){
                route(it)
            }
            adapter = departmentAdapter
        }
    }

    //路由跳转区分
    private fun route(it: DepartmentModel){
        if (it.isBack){ //假如是返回的
            if (it.parentParentType == TYPE_DEPT
                || it.parentParentType == TYPE_HOSPITAL
                || it.parentParentType == TYPE_LOCATION
                || it.parentParentType == TYPE_CABINET
                || it.parentParentType == TYPE_RECYCLE
                || it.parentParentType == TYPE_DISTRIBUTION
            ){
                queryDepartmentOrDeviceList(it.parentParentId, it.parentParentType, it.parentParentId)
            }
            if (it.parentParentType == TYPE_NONE){
                getToken()
            }
            return
        }
        if (it.type == TYPE_DEPT || it.type == TYPE_LOCATION){//还有下一级
            queryDepartmentOrDeviceList(it.value, it.type, it.parentId)
        }else{//选择当前级别
//            val name = it.value
            selectDeviceCallBack?.invoke(it)
            dismiss()
        }
    }

    private fun getToken(){
        GlobalScope.launch(context = Dispatchers.IO) {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            Log.e("YM","--token:$token")
            queryHospitalList(token)
        }
    }

    //获取医院列表
    private fun queryHospitalList(token: String){
            OkHttpUtil.postJson()
                .url(HttpApi.getURL(HttpApi.HOSPITAL_LIST))
                .addHeader("token",token)
                .build().execute(object : SerialPortHttpCallBack<List<HospitalModel>>(){
                    override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                        e?.printStackTrace()
                        ToastUtils.show(context,"网络，请检查网络!")
                    }

                    override fun onResponse(
                        response: HttpBaseResponseMode<List<HospitalModel>>,
                        id: Int
                    ) {
                        if (!response.success){
                            ToastUtils.show(context,"服务出错，请联系运营商，错误信息:${response.msg}")
                            return
                        }
                        val data = response.data ?: return
                        updateHospitalRecycleView(data)
                    }
                })
    }

    //获取科室或者设备列表
    private fun queryDepartmentOrDeviceList(id: String, type: String, parentId: String = ""){
        GlobalScope.launch(context = Dispatchers.IO) {
            val token = DataStoreUtils.getInstance().getInventoryToken()
            Log.e("YM","--token:$token")
            val jsonObject = JSONObject()
            jsonObject.put("value", id)
            jsonObject.put("type", type)
            if (type == TYPE_LOCATION){
                jsonObject.put("parentId", parentId)
            }
            OkHttpUtil.postJson()
                .url(HttpApi.getURL(HttpApi.LIST_LOCATION))
                .addHeader("token",token)
                .content(jsonObject.toString())
                .build().execute(object : SerialPortHttpCallBack<List<DepartmentModel>>(){
                    override fun onError(call: Call?, e: java.lang.Exception?, id: Int) {
                        e?.printStackTrace()
                        ToastUtils.show(context,"网络，请检查网络!")
                    }

                    override fun onResponse(
                        response: HttpBaseResponseMode<List<DepartmentModel>>,
                        id: Int
                    ) {
                        if (!response.success){
                            ToastUtils.show(context,"服务出错，请联系运营商，错误信息:${response.msg}")
                            return
                        }
                        val data = response.data ?: return
                        if (data.isNullOrEmpty()){
                            ToastUtils.show(context,"下面没有科室，请选择别的科室吧!")
                            return
                        }
                        val first = data[0]
                        val list = arrayListOf<DepartmentModel>()

                        list.add(
                            DepartmentModel(
                                parentId = first.parentId,
                                parentType = first.parentType,
                                parentParentId = first.parentParentId,
                                parentParentType = first.parentParentType,
                                name = "点击返回上一级",
                                type = first.type,
                                isBack = true
                            )
                        )
                        list.addAll(data)
                        updateDepartmentRecycleView(list)
                    }
                })
        }

    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag)
        initData()
    }

}
