package com.machine.serialport.model

//医院的实体类
data class HospitalModel(
    val deptCode: String = "",//部门编码
    val deptName: String = "",//部门名称
    val deptType: Int = 1,//部门类型 1:部门 2:类型
    val id: String = "",//id
    val parentId: String = "",// 所属上级
    val remark: String = "",// 备注
    val sort: String = "",// 排序
)
