package com.machine.serialport.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.machine.serialport.R
import com.machine.serialport.databinding.ItemLocationBinding
import com.machine.serialport.model.DepartmentModel


//科室适配器
class DepartmentAdapter(
    private val list: List<DepartmentModel>,
    private val itemClickCallBack: (DepartmentModel) -> Unit,
) : RecyclerView.Adapter<DepartmentAdapter.VH>() {
    inner class VH(private val binding: ItemLocationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bindData(item: DepartmentModel) {
//            if (item.isBack){
//                // 使用代码设置drawableleft
//                val drawable: Drawable? = binding.root.context.getDrawable(R.drawable.ic_baseline_arrow_upward_24)
//                // 这一步必须要做，否则不会显示。
//                drawable?.setBounds(
//                    0, 0, drawable.minimumWidth ?: 0,
//                    drawable.minimumHeight ?: 0
//                )
//                binding.itemLocationName.setCompoundDrawables(drawable, null, null, null)
//            }else{
//                binding.itemLocationName.setCompoundDrawables(null, null, null, null)
//            }
            binding.itemLocationName.text = item.name
            binding.root.setOnClickListener {
                itemClickCallBack.invoke(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemLocationBinding.inflate(layoutInflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bindData(list[position])
    }

    override fun getItemCount() = list.size
}