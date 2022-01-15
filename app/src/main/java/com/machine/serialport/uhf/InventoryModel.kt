package com.machine.serialport.uhf

//盘点的数据类
class InventoryModel() {
    var status = InventoryStatus.NORMAL //盘点状态 0是正常
    var labelSet:HashSet<String> = hashSetOf() //标签内容,里面的值不会重复
    var labelMap:MutableMap<String, String> = mutableMapOf() //存储了标签和天线绑定的数据
}