package com.machine.serialport.data

import com.machine.serialport.model.InventoryItemModel

//测试数据
object TestData {
    const val SPLASH_SRC = "https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fb-ssl.duitang.com%2Fuploads%2Fitem%2F201408%2F05%2F20140805000309_aYaLF.png&refer=http%3A%2F%2Fb-ssl.duitang.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1623471707&t=9cd10c26667b4133669c0184cdaa7a73"
    fun getInventoryListData(): MutableList<InventoryItemModel>{
        var data = mutableListOf<InventoryItemModel>()
        data.add(InventoryItemModel("大褂",10))
        data.add(InventoryItemModel("裤子",30))
        data.add(InventoryItemModel("大褂",40))
        data.add(InventoryItemModel("上衣",50))
        data.add(InventoryItemModel("裙子",20))
        data.add(InventoryItemModel("鞋子",10))
        data.add(InventoryItemModel("裙子",30))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        data.add(InventoryItemModel("手套",10))
        return data
    }
}