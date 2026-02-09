package com.ai.appointments.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.appointments.R
import com.ai.appointments.databinding.ItemPMyAppointmentCardBinding
import com.ai.appointments.db.Repository.AppointmentRepository
import com.ai.appointments.model.P_My_AppointmentItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MyAppointmentAdapter(
    private val onItemClick: (P_My_AppointmentItem) -> Unit,
    private val onEditClick: (P_My_AppointmentItem) -> Unit,
    private val onDeleteSuccess: () -> Unit
) : ListAdapter<P_My_AppointmentItem, MyAppointmentAdapter.ViewHolder>(MyAppointmentDiffCallback()) {

    private lateinit var context: Context

    inner class ViewHolder(
        private val binding: ItemPMyAppointmentCardBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: P_My_AppointmentItem) {
            binding.apply {
                tvServiceName.text = item.serviceName
                tvClinicName.text = item.clinicName
                tvDateValue.text = item.date
                tvTimeValue.text = item.time
                tvDurationValue.text = item.duration
                tvValueStatus.text = item.status

                // Set status color
                val statusColor = when (item.status.lowercase()) {
                    "confirmed" -> R.color.green
                    "cancelled" -> R.color.red
                    "completed" -> R.color.blue
                    "no show" -> R.color.orange
                    else -> R.color.gray
                }
                tvValueStatus.setTextColor(context.getColor(statusColor))

                ivMore.setOnClickListener { view ->
                    showPopupMenu(view, item)
                }

                root.setOnClickListener {
                    onItemClick(item)
                }
            }
        }

        private fun showPopupMenu(view: View, item: P_My_AppointmentItem) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.appointment_menu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_edit -> {
                        onEditClick(item)
                        true
                    }
                    R.id.menu_delete -> {
                        showDeleteConfirmationDialog(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        private fun showDeleteConfirmationDialog(item: P_My_AppointmentItem) {
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.delete_appointment))
                .setMessage(context.getString(R.string.are_you_sure_you_want_to_delete_this_appointment_this_action_cannot_be_undone))
                .setPositiveButton(context.getString(R.string.delete)) { dialog, _ ->
                    item.appointment?.let { appointment ->
                        deleteAppointment(appointment)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        private fun deleteAppointment(appointment: com.ai.appointments.db.models.Appointment) {
            AppointmentRepository.deleteAppointment(
                appointmentId = appointment.id,
                providerId = appointment.providerId,
                userId = appointment.userId,
                onSuccess = {
                    Toast.makeText(context, context.getString(R.string.appointment_deleted_successfully), Toast.LENGTH_SHORT).show()
                    onDeleteSuccess()
                },
                onError = { error ->
                    Toast.makeText(context, "Failed to delete appointment: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding = ItemPMyAppointmentCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, parent.context)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MyAppointmentDiffCallback : DiffUtil.ItemCallback<P_My_AppointmentItem>() {
    override fun areItemsTheSame(
        oldItem: P_My_AppointmentItem,
        newItem: P_My_AppointmentItem
    ): Boolean {
        return oldItem.appointment?.id == newItem.appointment?.id
    }

    override fun areContentsTheSame(
        oldItem: P_My_AppointmentItem,
        newItem: P_My_AppointmentItem
    ): Boolean {
        return oldItem == newItem
    }
}