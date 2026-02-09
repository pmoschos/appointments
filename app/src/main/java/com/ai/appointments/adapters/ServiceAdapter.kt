package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.databinding.ItemServiceBinding
import com.ai.appointments.model.ServiceItem

class ServiceAdapter(
    private var list: List<ServiceItem> = emptyList(),
    private val onItemClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.apply {
            ivIcon.setImageResource(item.icon)
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun getItemCount() = list.size

    // Add this method to update data
    fun updateData(newList: List<ServiceItem>) {
        list = newList
        notifyDataSetChanged()
    }
}