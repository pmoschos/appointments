package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemTimeSlotBinding
import com.ai.appointments.db.models.ServiceCategoryItem


class ServiceCategoryAdapter(
    private val onItemClick: (ServiceCategoryItem) -> Unit
) : ListAdapter<ServiceCategoryItem, ServiceCategoryAdapter.ViewHolder>(ServiceCategoryDiffCallback()) {

    inner class ViewHolder(private val binding: ItemTimeSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServiceCategoryItem) {
            binding.tvTimeSlot.text = item.name
            binding.tvTimeSlot.setBackgroundResource(
                if (item.isSelected) R.drawable.bg_time_slot_selected
                else R.drawable.bg_time_slot_normal
            )
            binding.tvTimeSlot.setTextColor(
                if (item.isSelected) binding.root.context.getColor(R.color.white)
                else binding.root.context.getColor(R.color.primary_black)
            )

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ServiceCategoryDiffCallback : DiffUtil.ItemCallback<ServiceCategoryItem>() {
    override fun areItemsTheSame(oldItem: ServiceCategoryItem, newItem: ServiceCategoryItem): Boolean {
        return oldItem.value == newItem.value
    }

    override fun areContentsTheSame(oldItem: ServiceCategoryItem, newItem: ServiceCategoryItem): Boolean {
        return oldItem == newItem
    }
}