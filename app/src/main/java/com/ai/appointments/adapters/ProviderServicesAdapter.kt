package com.ai.appointments.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemServicesCardBinding
import com.ai.appointments.db.models.Service
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class ProviderServicesAdapter(
    private val language: String = "en",
    private val onItemClick: (Service) -> Unit,
    private val onEditClick: (Service) -> Unit,
    private val onDeleteClick: (Service) -> Unit
) : ListAdapter<ProviderServicesAdapter.ServiceItemWrapper, ProviderServicesAdapter.ViewHolder>(
    ServiceDiffCallback()
) {

    // Wrapper class to handle DiffUtil properly with Firebase keys
    data class ServiceItemWrapper(
        val id: String,
        val service: Service
    )

    inner class ViewHolder(private val binding: ItemServicesCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ServiceItemWrapper) {
            val service = item.service
            binding.apply {
                // Service name with language support
                tvServiceName.text = service.getDisplayName(language)

                // Category display
                tvClinicName.text = service.getCategoryDisplayName(language)

                // Price range
                tvPrice.text = "${service.currency}  ${String.format("%.2f", service.priceMax)}"

                // Duration range
                tvDurationvalue.text = "${service.durationMax} min"

                // Menu actions - UNCOMMENT THIS
                ivMore.setOnClickListener { view ->
                    showPopupMenu(view, item)
                }

                // Full card click
                root.setOnClickListener {
                    onItemClick(service)
                }
            }
        }

        private fun showPopupMenu(view: View, item: ServiceItemWrapper) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.service_menu, popup.menu)

            // Show/hide toggle action based on current status
            val isActive = item.service.isActive
            popup.menu.findItem(R.id.menu_toggle)?.title =
                if (isActive) "Deactivate" else "Activate"

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        onEditClick(item.service)
                        true
                    }
                    R.id.menu_delete -> {
                        showDeleteConfirmation(view, item)
                        true
                    }
                    R.id.menu_toggle -> {
                        // Toggle service status
                        val updatedService = item.service.copy(isActive = !item.service.isActive)
                        onEditClick(updatedService) // Reuse edit flow to update status
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showDeleteConfirmation(view: View, item: ServiceItemWrapper) {
            MaterialAlertDialogBuilder(view.context)
                .setTitle(view.context.getString(R.string.delete_appointment))
                .setMessage(
                    view.context.getString(
                        R.string.delete_service_message,
                        item.service.getDisplayName(language)
                    )
                )
                .setPositiveButton(view.context.getString(R.string.delete)) { _, _ ->
                    onDeleteClick(item.service)
                }
                .setNegativeButton(view.context.getString(R.string.cancel), null)
                .show()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServicesCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // Helper to submit list with proper wrappers
    fun submitServiceList(services: List<Service>) {
        val wrapped = services.map { ServiceItemWrapper(it.service_id ?: UUID.randomUUID().toString(), it) }
        submitList(wrapped)
    }
}

// DiffUtil for efficient updates
class ServiceDiffCallback : DiffUtil.ItemCallback<ProviderServicesAdapter.ServiceItemWrapper>() {
    override fun areItemsTheSame(
        oldItem: ProviderServicesAdapter.ServiceItemWrapper,
        newItem: ProviderServicesAdapter.ServiceItemWrapper
    ): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: ProviderServicesAdapter.ServiceItemWrapper,
        newItem: ProviderServicesAdapter.ServiceItemWrapper
    ): Boolean {
        return oldItem.service == newItem.service
    }
}