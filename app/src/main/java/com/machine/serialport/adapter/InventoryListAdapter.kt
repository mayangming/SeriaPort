package com.machine.serialport.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.machine.serialport.R
import com.machine.serialport.model.InventoryItemModel
import com.machine.serialport.util.LogUtils

//盘点列表
class InventoryListAdapter() : RecyclerView.Adapter<InventoryListAdapter.InventoryListViewHolder>(){

    private var context : Context ?= null
    private var layoutInflater : LayoutInflater ?= null
    private var inventoryList: MutableList<InventoryItemModel> = mutableListOf()

    inner class InventoryListViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val inventoryName:TextView = itemView.findViewById(R.id.inventory_item_name)
        val inventoryCount:TextView = itemView.findViewById(R.id.inventory_item_count)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryListViewHolder {
        if (null == context){
            context = parent.context
            layoutInflater = LayoutInflater.from(context)
        }
        val itemView = layoutInflater!!.inflate(R.layout.item_inventory,parent,false)
        return InventoryListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: InventoryListViewHolder, position: Int) {
        val model = inventoryList[position]
        holder.inventoryName.text = model.catidDicttext
        holder.inventoryCount.text = model.totalCount.toString()
    }

    override fun getItemCount(): Int {
        return inventoryList.size
    }

    fun setAdapterData(inventoryList: MutableList<InventoryItemModel>){
        this.inventoryList.clear()
        this.inventoryList.addAll(inventoryList)
        notifyDataSetChanged()
    }

    fun addAdapterData(inventoryItem: InventoryItemModel){
        this.inventoryList.add(inventoryItem)
        notifyDataSetChanged()
    }

    fun getData() = this.inventoryList

}