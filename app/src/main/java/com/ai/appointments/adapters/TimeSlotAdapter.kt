package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemTimeSlotBinding
import com.ai.appointments.model.TimeSlot


class TimeSlotAdapter(
    private val onItemClick: (TimeSlot) -> Unit
) : ListAdapter<TimeSlot, TimeSlotAdapter.ViewHolder>(TimeSlotDiffCallback()) {

    inner class ViewHolder(private val binding: ItemTimeSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TimeSlot) {
            binding.tvTimeSlot.text = item.time
            binding.tvTimeSlot.setBackgroundResource(
                if (item.isSelected) R.drawable.bg_time_slot_selected
                else R.drawable.bg_time_slot_normal
            )
            binding.tvTimeSlot.setTextColor(
                if (item.isSelected) binding.root.context.getColor(R.color.white)
                else binding.root.context.getColor(R.color.primary_black)
            )

            binding.root.setOnClickListener {
                onItemClick(item.copy(isSelected = true))
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

class TimeSlotDiffCallback : DiffUtil.ItemCallback<TimeSlot>() {
    override fun areItemsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
        return oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
        return oldItem == newItem
    }
}