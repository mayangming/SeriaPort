package com.machine.serialport.model

//位置信息
data class LocationModel(
    val id: String = "", //Id
    val deptCode:String = "", //部门编码
    val deptName: String = "", //部门名称
    val deptType: Int = 1, //1公司2部门
    val parentId: String = "", //所属上级
    val remark: String = "", //备注
    val sort: Int = 0,//排序
    var check: Boolean = false,//是否选中
)