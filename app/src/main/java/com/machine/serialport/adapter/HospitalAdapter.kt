package com.machine.serialport.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.machine.serialport.databinding.ItemLocationBinding
import com.machine.serialport.model.HospitalModel

//医院选择适配器
class HospitalAdapter(
    private val hospitalList: List<HospitalModel>,
    private val itemClickCallBack: (HospitalModel) -> Unit
): RecyclerView.Adapter<HospitalAdapter.VH>() {

    inner class VH(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bindData(item: HospitalModel) {
            binding.root.setOnClickListener {
                itemClickCallBack.invoke(item)
            }
            Log.e("YM--->","适配器数据:${item.deptName}")
            binding.itemLocationName.text = item.deptName
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflate = LayoutInflater.from(parent.context)
        val binding = ItemLocationBinding.inflate(inflate,parent,false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bindData(hospitalList[position])
    }

    override fun getItemCount() = hospitalList.size
}