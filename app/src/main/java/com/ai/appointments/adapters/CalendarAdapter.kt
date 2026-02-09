package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemCalendarBinding
import com.ai.appointments.model.CalendarDay


class CalendarAdapter(
    private val onItemClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.ViewHolder>(CalendarDayDiffCallback()) {

    inner class ViewHolder(private val binding: ItemCalendarBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalendarDay) {
            binding.apply {
                tvDayName.text = item.dayName
                tvDate.text = item.dayNumber

                // Background & text color logic
                when {
                    item.isSelected -> {
                        root.setBackgroundResource(R.drawable.bg_selected_day)
                        tvDayName.setTextColor(root.context.getColor(R.color.white))
                        tvDate.setTextColor(root.context.getColor(R.color.darkBrown))
                    }
                    item.isToday -> {
                        tvDayName.setTextColor(root.context.getColor(R.color.colorPrimary))
                        tvDate.setTextColor(root.context.getColor(R.color.colorPrimary))
                        root.setBackgroundResource(0) // no bg
                    }
                    else -> {
                        tvDayName.setTextColor(root.context.getColor(R.color.gray))
                        tvDate.setTextColor(root.context.getColor(R.color.black))
                        root.setBackgroundResource(0)
                    }
                }

                root.setOnClickListener {
                    onItemClick(item.copy(isSelected = true))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

// DiffUtil Callback
class CalendarDayDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
    override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
        return oldItem.dayNumber == newItem.dayNumber
    }

    override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
        return oldItem == newItem
    }
}