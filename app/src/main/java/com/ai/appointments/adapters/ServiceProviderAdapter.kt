package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.databinding.ItemServiceProviderBinding
import com.ai.appointments.db.models.ServiceProvider
import com.bumptech.glide.Glide
import com.ai.appointments.R

class ServiceProviderAdapter(
    private var list: List<ServiceProvider> = emptyList(),
    private val onItemClick: (ServiceProvider) -> Unit
) : RecyclerView.Adapter<ServiceProviderAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemServiceProviderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceProviderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val provider = list[position]
        holder.binding.apply {
            // Load profile image
            if (provider.profileImageUrl.isNotEmpty()) {
                Glide.with(root.context)
                    .load(provider.profileImageUrl)
                    .placeholder(R.drawable.demo_image2)
                    .into(ivUserImage)
            } else {
                ivUserImage.setImageResource(R.drawable.demo_image2)
            }

            // Set provider name
            tvTitle.text = "${provider.firstName} ${provider.lastName}"

            // Set business name if available
            val businessName = provider.businessInfo.businessName
            if (businessName.isNotEmpty()) {
                tvTitle.text = businessName
            }

            // Set rating (you might want to calculate this from reviews)
            tvRating.text = "4.8" // Placeholder - implement actual rating

            // Set distance (placeholder - implement location-based distance)
            tvDistance.text = "2 km away"

            btnBook.setOnClickListener { onItemClick(provider) }
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<ServiceProvider>) {
        list = newList
        notifyDataSetChanged()
    }
}