package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.databinding.ItemHistoryAppointmentBinding
import com.ai.appointments.db.models.AppointmentHistoricity

class HistoryAdapter(
    private var appointments: List<AppointmentHistoricity> = emptyList()
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    var onItemClick: ((AppointmentHistoricity) -> Unit)? = null

    inner class ViewHolder(
        private val binding: ItemHistoryAppointmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppointmentHistoricity) {
            with(binding) {
                // Service name
                tvTitle.text = item.serviceName

                // Duration
                tvDuration.text = item.duration

                // Category
                tvCategory.text = item.category

                // Date
                tvDate.text = item.date

                // Price
                tvCost.text = item.price

                // Load service/provider image if available
                // You would need to pass image URL in AppointmentHistoricity
                // For now, using placeholder
                // Glide.with(root.context).load(item.imageUrl).into(ivUserImage)

                // Set status indicator based on appointment status
               /// setStatusIndicator(item)

                // Handle click
                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }

                // Optional: Add menu button for actions (cancel, reschedule, etc.)
                setupMenuButton(item)
            }
        }

//        private fun setStatusIndicator(item: AppointmentHistoricity) {
//            val appointment = item.appointment
//            if (appointment != null) {
//                when (appointment.status) {
//                    "confirmed" -> {
//                        // Show upcoming indicator if appointment is in future
//                        if (appointment.appointmentDate > System.currentTimeMillis()) {
//                            binding.ivStatusIndicator.setBackgroundResource(R.drawable.bg_status_upcoming)
//                        } else {
//                            binding.ivStatusIndicator.setBackgroundResource(R.drawable.bg_status_completed)
//                        }
//                    }
//                    "completed" -> {
//                        binding.ivStatusIndicator.setBackgroundResource(R.drawable.bg_status_completed)
//                    }
//                    "cancelled" -> {
//                        binding.ivStatusIndicator.setBackgroundResource(R.drawable.bg_status_cancelled)
//                    }
//                    "no_show" -> {
//                        binding.ivStatusIndicator.setBackgroundResource(R.drawable.bg_status_no_show)
//                    }
//                }
//            }
//        }

        private fun setupMenuButton(item: AppointmentHistoricity) {
            // You can add a menu button for actions like:
            // - Cancel upcoming appointment
            // - Reschedule
            // - View details
            // - Add review for completed appointments
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryAppointmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount(): Int = appointments.size

    fun submitList(newList: List<AppointmentHistoricity>) {
        appointments = newList
        notifyDataSetChanged()
    }
}