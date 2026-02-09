package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemServiceDetailBinding
import com.ai.appointments.db.models.Service
import com.bumptech.glide.Glide

class ServiceListAdapter(
    private var list: List<Service>,
    private val language: String,
    private val onItemClick: (Service) -> Unit
) : RecyclerView.Adapter<ServiceListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceDetailBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val service = list[position]
        holder.binding.apply {
            // Load image if available
            if (service.imageUrl.isNotEmpty()) {
                Glide.with(root.context)
                    .load(service.imageUrl)
                    .placeholder(R.drawable.demo_image2)
                    .into(ivServiceImage)
            } else {
                ivServiceImage.setImageResource(R.drawable.demo_image2)
            }

            tvTitle.text = service.getDisplayName(language)
            tvDescription.text = service.getDescription(language)
            tvDuration.text = root.context.getString(R.string.minutes, service.durationMax)

            tvCategory.text = service.getCategoryDisplayName(language)
            tvPrice.text = "${service.priceMin} ${service.currency}"
            btnBook.setOnClickListener { onItemClick(service) }
        }
    }

    override fun getItemCount() = list.size
    fun updateList(newList: List<Service>) {
        list = newList
        notifyDataSetChanged()
    }
}