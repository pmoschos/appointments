package com.ai.appointments.bottomsheetdialouges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ai.appointments.R
import com.ai.appointments.adapters.CalendarAdapter
import com.ai.appointments.adapters.TimeSlotAdapter
import com.ai.appointments.databinding.BottomsheetEditAppointmentLayoutBinding
import com.ai.appointments.db.Repository.AvailabilityRepository
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.model.CalendarDay
import com.ai.appointments.model.TimeSlot
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class EditAppointmentBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetEditAppointmentLayoutBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var timeSlotAdapter: TimeSlotAdapter

    private var currentCalendarDays = listOf<CalendarDay>()
    private var currentTimeSlots = listOf<TimeSlot>()

    private var appointment: Appointment? = null
    private var originalDate: String = ""
    private var originalTime: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private var onAppointmentUpdatedListener: ((Appointment) -> Unit)? = null

    private val database = FirebaseDatabase.getInstance()
    private val appointmentsRef = database.getReference("appointments")
    private val availabilityRef = database.getReference("availability")
    private val indexesRef = database.getReference("indexes")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetEditAppointmentLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.icCancel.setOnClickListener { dismiss() }

        if (appointment == null) {
            Toast.makeText(requireContext(), "No appointment data provided", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        setupCalendar()
        setupTimeSlots()
        setupContinueButton()
        updateUIWithAppointmentData()
    }

    fun setAppointment(appointment: Appointment) {
        this.appointment = appointment

        val scheduledDateTime = appointment.scheduledDateTime
        println("DEBUG: Scheduled date time from appointment: $scheduledDateTime")

        if (scheduledDateTime.isNotEmpty() && scheduledDateTime.contains(" ")) {
            val parts = scheduledDateTime.split(" ")
            if (parts.size >= 2) {
                originalDate = parts[0]
                originalTime = parts[1]
                selectedDate = originalDate
                selectedTime = originalTime
                println("DEBUG: Parsed original date: $originalDate, time: $originalTime")
            }
        } else {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = appointment.appointmentDate
            }
            originalDate = dateFormat.format(calendar.time)
            originalTime = String.format("%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE))
            selectedDate = originalDate
            selectedTime = originalTime
            println("DEBUG: Fallback original date: $originalDate, time: $originalTime")
        }
    }

    fun setOnAppointmentUpdatedListener(listener: (Appointment) -> Unit) {
        this.onAppointmentUpdatedListener = listener
    }

    private fun updateUIWithAppointmentData() {
        appointment?.let {
            try {
                val date = dateFormat.parse(selectedDate)
                date?.let {
                    binding.tvDate.text = displayDateFormat.format(it)
                }
            } catch (e: Exception) {
                binding.tvDate.text = selectedDate
            }

         //   binding.tvEditAppointmentLabel.text = "Edit ${it.getServiceDisplayName("en")} Appointment"
        }
    }

    private fun setupCalendar() {
        val todayCal = Calendar.getInstance()
        val days = mutableListOf<CalendarDay>()

        for (i in 0 until 14) {
            val cal = Calendar.getInstance().apply {
                time = todayCal.time
                add(Calendar.DATE, i)
            }

            val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                .take(1).uppercase()
            val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
            val dateString = dateFormat.format(cal.time)
            val isOriginalDate = dateString == originalDate
            val isToday = i == 0

            days.add(CalendarDay(
                dayName = dayName,
                dayNumber = dayNumber,
                dateString = dateString,
                isToday = isToday,
                isSelected = isOriginalDate
            ))
        }

        currentCalendarDays = days

        calendarAdapter = CalendarAdapter { selectedDay ->
            currentCalendarDays = currentCalendarDays.map { day ->
                day.copy(isSelected = day.dateString == selectedDay.dateString)
            }
            calendarAdapter.submitList(currentCalendarDays)

            selectedDate = selectedDay.dateString
            loadAvailableTimeSlots(selectedDate)

            try {
                val date = dateFormat.parse(selectedDate)
                date?.let {
                    binding.tvDate.text = displayDateFormat.format(it)
                }
            } catch (e: Exception) {
                binding.tvDate.text = selectedDate
            }
        }

        binding.rvCalendar.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
        }

        calendarAdapter.submitList(currentCalendarDays)
    }

    private fun setupTimeSlots() {
        timeSlotAdapter = TimeSlotAdapter { selectedSlot ->
            if (selectedSlot.isAvailable) {
                currentTimeSlots = currentTimeSlots.map { slot ->
                    slot.copy(isSelected = slot.time == selectedSlot.time)
                }
                timeSlotAdapter.submitList(currentTimeSlots)
                selectedTime = selectedSlot.originalTime
                println("DEBUG: Selected time: $selectedTime")
            } else {
                Toast.makeText(requireContext(), "This time slot is not available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = timeSlotAdapter
        }

        loadAvailableTimeSlots(selectedDate)
    }

    private fun loadAvailableTimeSlots(date: String) {
        showLoading(true)

        appointment?.let { appt ->
            AvailabilityRepository.getAvailableSlots(
                providerId = appt.providerId,
                date = date,
                serviceId = appt.serviceId
            ) { allSlots ->
                showLoading(false)

                if (allSlots.isEmpty()) {
                    currentTimeSlots = listOf()
                    timeSlotAdapter.submitList(currentTimeSlots)
                    Toast.makeText(requireContext(), "No slots found for $date", Toast.LENGTH_SHORT).show()
                    return@getAvailableSlots
                }

                val slots = allSlots.map { availabilitySlot ->
                    val isOriginalSlot = date == originalDate && availabilitySlot.time == originalTime
                    val isCurrentAppointmentSlot = availabilitySlot.appointmentId == appt.id

                    val isAvailable = availabilitySlot.isAvailable || isOriginalSlot || isCurrentAppointmentSlot
                    val isSelected = isOriginalSlot

                    TimeSlot(
                        time = formatTimeForDisplay(availabilitySlot.time),
                        originalTime = availabilitySlot.time,
                        isAvailable = isAvailable,
                        isSelected = isSelected
                    )
                }

                currentTimeSlots = slots
                timeSlotAdapter.submitList(currentTimeSlots)

                // Ensure original time is selected
                if (date == originalDate) {
                    val originalSlot = slots.find { it.originalTime == originalTime }
                    if (originalSlot != null) {
                        selectedTime = originalSlot.originalTime
                    }
                }
            }
        }
    }

    private fun formatTimeForDisplay(time24h: String): String {
        return try {
            val date = timeFormat.parse(time24h)
            date?.let { displayTimeFormat.format(it) } ?: time24h
        } catch (e: Exception) {
            time24h
        }
    }

    private fun setupContinueButton() {
        binding.btnUpdate.setOnClickListener {
            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(requireContext(), "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSlot = currentTimeSlots.find { it.isSelected && (it.isAvailable) }
            if (selectedSlot == null) {
                Toast.makeText(requireContext(), "Please select a time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedDate == originalDate && selectedTime == originalTime) {
                Toast.makeText(requireContext(), "No changes made", Toast.LENGTH_SHORT).show()
                dismiss()
                return@setOnClickListener
            }

            updateAppointment()
        }
    }

    private fun updateAppointment() {
        appointment?.let { originalAppointment ->
            showLoading(true)

            // Calculate new timestamp for the appointment
            val newTimestamp = parseDateTimeToTimestamp(selectedDate, selectedTime)
            if (newTimestamp == 0L) {
                showLoading(false)
                Toast.makeText(requireContext(), "Invalid date/time selected", Toast.LENGTH_SHORT).show()
                return
            }

            // Create updated appointment object
            val updatedAppointment = originalAppointment.copy(
                appointmentDate = newTimestamp,
                scheduledDateTime = "$selectedDate $selectedTime",
                updatedAt = System.currentTimeMillis()
            )

            // Perform the update transaction
            updateAppointmentInDatabase(updatedAppointment)
        }
    }

    private fun updateAppointmentInDatabase(updatedAppointment: Appointment) {
        appointment?.let { originalAppointment ->
            val appointmentId = originalAppointment.id

            // 1. Update the appointment record
            val appointmentUpdates = mapOf(
                "appointmentDate" to updatedAppointment.appointmentDate,
                "scheduledDateTime" to updatedAppointment.scheduledDateTime,
                "updatedAt" to updatedAppointment.updatedAt
            )

            appointmentsRef.child(appointmentId).updateChildren(appointmentUpdates)
                .addOnSuccessListener {
                    println("DEBUG: Appointment updated successfully")

                    // 2. Handle slot changes
                    updateAvailabilitySlots(originalAppointment, appointmentId, updatedAppointment)
                }
                .addOnFailureListener { error ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to update appointment: ${error.message}", Toast.LENGTH_SHORT).show()
                    println("ERROR: Failed to update appointment: ${error.message}")
                }
        }
    }

    private fun updateAvailabilitySlots(
        originalAppointment: Appointment,
        appointmentId: String,
        updatedAppointment: Appointment
    ) {
        // Find and release ALL slots that have this appointmentId
        val providerRef = availabilityRef.child(originalAppointment.providerId)

        providerRef.get().addOnSuccessListener { snapshot ->
            val updates = mutableMapOf<String, Any?>()

            // Find all slots with this appointmentId
            for (dateSnapshot in snapshot.children) {
                val date = dateSnapshot.key ?: continue

                for (timeSnapshot in dateSnapshot.children) {
                    val time = timeSnapshot.key ?: continue
                    val slotData = timeSnapshot.value as? Map<*, *>

                    val slotAppointmentId = slotData?.get("appointmentId") as? String
                    if (slotAppointmentId == appointmentId) {
                        // This slot has our appointmentId, release it
                        val updatedSlotData = mutableMapOf<String, Any?>()

                        // Copy all existing fields except appointmentId
                        slotData.forEach { (key, value) ->
                            if (key != "appointmentId") {
                                updatedSlotData[key.toString()] = value
                            }
                        }

                        updatedSlotData["isAvailable"] = true

                        updates["${originalAppointment.providerId}/$date/$time"] = updatedSlotData
                        println("DEBUG: Will release slot with appointmentId: $date $time")
                    }
                }
            }

            // Now book the new slot
            val newSlotRef = availabilityRef.child(originalAppointment.providerId)
                .child(selectedDate)
                .child(selectedTime)

            newSlotRef.get().addOnSuccessListener { newSnapshot ->
                val newSlotData = mutableMapOf<String, Any?>()

                if (newSnapshot.exists()) {
                    val value = newSnapshot.value
                    if (value is Map<*, *>) {
                        // Copy all existing fields
                        for ((key, v) in value) {
                            if (key is String) {
                                newSlotData[key] = v
                            }
                        }
                    }
                } else {
                    // If slot doesn't exist, create with default values
                    newSlotData["duration"] = 30
                    newSlotData["serviceId"] = originalAppointment.serviceId
                }

                // Mark as unavailable and add appointmentId
                newSlotData["isAvailable"] = false
                newSlotData["appointmentId"] = appointmentId

                updates["${originalAppointment.providerId}/$selectedDate/$selectedTime"] = newSlotData

                // Perform batch update
                if (updates.isNotEmpty()) {
                    availabilityRef.updateChildren(updates)
                        .addOnSuccessListener {
                            println("DEBUG: Successfully updated ${updates.size} slots")
                            updateIndexes(originalAppointment, appointmentId, updatedAppointment)
                        }
                        .addOnFailureListener { error ->
                            showLoading(false)
                            Toast.makeText(requireContext(), "Failed to update slots: ${error.message}", Toast.LENGTH_SHORT).show()
                            println("ERROR: Failed to update availability: ${error.message}")
                        }
                } else {
                    // No updates needed (shouldn't happen but just in case)
                    updateIndexes(originalAppointment, appointmentId, updatedAppointment)
                }
            }.addOnFailureListener { error ->
                showLoading(false)
                Toast.makeText(requireContext(), "Failed to get new slot data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateIndexes(
        originalAppointment: Appointment,
        appointmentId: String,
        updatedAppointment: Appointment
    ) {
        val newTimestamp = updatedAppointment.appointmentDate

        val providerIndexUpdate = mapOf(
            "appointments_by_provider/${originalAppointment.providerId}/$appointmentId" to newTimestamp
        )
        val userIndexUpdate = mapOf(
            "appointments_by_user/${originalAppointment.userId}/$appointmentId" to newTimestamp
        )

        val allIndexUpdates = providerIndexUpdate + userIndexUpdate

        indexesRef.updateChildren(allIndexUpdates)
            .addOnSuccessListener {
                println("DEBUG: Indexes updated successfully")

                // Notify listener and dismiss
                onAppointmentUpdatedListener?.invoke(updatedAppointment)

                showLoading(false)
                Toast.makeText(requireContext(), "Appointment updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { error ->
                showLoading(false)
                Toast.makeText(requireContext(), "Failed to update indexes: ${error.message}", Toast.LENGTH_SHORT).show()
                println("ERROR: Failed to update indexes: ${error.message}")

                // Still notify listener even if indexes failed
                onAppointmentUpdatedListener?.invoke(updatedAppointment)
                dismiss()
            }
    }

    private fun parseDateTimeToTimestamp(date: String, time: String): Long {
        return try {
            val dateTimeString = "$date $time"
            val parsedDate = dateTimeFormat.parse(dateTimeString)
            parsedDate?.time ?: 0L
        } catch (e: Exception) {
            println("ERROR: Failed to parse date/time: $date $time - ${e.message}")
            0L
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnUpdate.isEnabled = !show
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvTimeSlots.alpha = 0.5f
            binding.btnUpdate.text = getString(R.string.updating)
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvTimeSlots.alpha = 1.0f
            binding.btnUpdate.text = getString(R.string.update)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        onAppointmentUpdatedListener = null
    }
}