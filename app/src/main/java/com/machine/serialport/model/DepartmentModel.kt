package com.machine.serialport.model

//科室或者设备的节点
data class DepartmentModel(
    val name: String = "",
    val type: String = "",// dept-科室，location-位置
    val value: String = "", //科室ID/位置
    val parentId: String = "", //父级ID
    val parentType: String = "dept", //父级类型 科室: dept 医院: hospital
    val parentParentId: String = "", //祖父级ID
    val parentParentType: String = "dept", //祖父级类型 科室: dept 医院: hospital
    val isBack: Boolean = false, //是否可以返回上一级
    val subCabinetList: List<SubCabinetModel> = arrayListOf(), //副柜列表，当类型是回收柜时候需要取出副柜列表的id进行保存
)
