package com.ai.appointments.fragments.provider

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.activities.AddServiceActivity
import com.ai.appointments.adapters.CalendarAdapter
import com.ai.appointments.adapters.MyAppointmentAdapter
import com.ai.appointments.bottomsheetdialouges.EditAppointmentBottomSheet
import com.ai.appointments.databinding.FragmentPAppointmentBinding
import com.ai.appointments.db.Repository.AppointmentRepository
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.model.CalendarDay
import com.ai.appointments.model.P_My_AppointmentItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class P_AppointmentFragment : Fragment() {

    private var _binding: FragmentPAppointmentBinding? = null
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var appointmentAdapter: MyAppointmentAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private val binding get() = _binding!!

    private var currentCalendarDays = listOf<CalendarDay>()
    private var appointments = mutableListOf<Appointment>()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var currentViewMode: ViewMode = ViewMode.TODAY
    private var isCalendarSelectionTriggered = false

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var language: String = "en"

    enum class ViewMode {
        TODAY, UPCOMING
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper.getInstance()
        language = Locale.getDefault().language

        setupTabs()
        setupCalendar()
        setupAppointmentsRecyclerView()
        updateDateDisplay()
        loadAppointments()

        binding.btnCreateAppointment.setOnClickListener {
            val intent = Intent(requireContext(), AddServiceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupTabs() {
        // Select Today's tab by default
        binding.tabLayout.getTabAt(0)?.select()

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isCalendarSelectionTriggered = false // Reset flag

                when (tab?.position) {
                    0 -> {
                        currentViewMode = ViewMode.TODAY
                        // When Today tab is selected, set date to today
                        selectedDate = Calendar.getInstance()
                        updateCalendarSelectionToToday()
                        TodayDayView()
                    }
                    1 -> {
                        currentViewMode = ViewMode.UPCOMING
                        upcomingView()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateCalendarSelectionToToday() {
        val todayString = todayFormat.format(Date())
        currentCalendarDays = currentCalendarDays.map { day ->
            day.copy(isSelected = day.dateString == todayString)
        }
        calendarAdapter.submitList(currentCalendarDays)
    }

    private fun setupCalendar() {
        // Start from today
        val todayCal = Calendar.getInstance()

        // Generate 7 days starting from today
        val days = mutableListOf<CalendarDay>()
        for (i in 0 until 7) {
            val cal = Calendar.getInstance().apply {
                time = todayCal.time
                add(Calendar.DATE, i)
            }

            val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                .take(1).uppercase()
            val dayNumber = cal.get(Calendar.DAY_OF_MONTH).toString()
            val dateString = dateFormat.format(cal.time)
            val isToday = i == 0

            days.add(CalendarDay(
                dayName = dayName,
                dayNumber = dayNumber,
                dateString = dateString,
                isToday = isToday,
                isSelected = i == 0 // Select today by default
            ))
        }

        currentCalendarDays = days
        selectedDate = Calendar.getInstance() // Today

        calendarAdapter = CalendarAdapter { selectedDay ->
            isCalendarSelectionTriggered = true

            // Update calendar selection
            currentCalendarDays = currentCalendarDays.map { day ->
                day.copy(isSelected = day.dateString == selectedDay.dateString)
            }
            calendarAdapter.submitList(currentCalendarDays)

            // Update selected date
            try {
                selectedDate.time = dateFormat.parse(selectedDay.dateString) ?: Date()
                updateDateDisplay()

                // Logic: If user selects a future date (not today), switch to Upcoming tab
                val selectedDateString = dateFormat.format(selectedDate.time)
                val todayString = todayFormat.format(Date())

                if (selectedDateString != todayString) {
                    // If not today and not already in Upcoming tab, switch to Upcoming
                    if (currentViewMode != ViewMode.UPCOMING) {
                        binding.tabLayout.getTabAt(1)?.select()
                        currentViewMode = ViewMode.UPCOMING
                    }
                } else {
                    // If selected today and not already in Today tab, switch to Today
                    if (currentViewMode != ViewMode.TODAY) {
                        binding.tabLayout.getTabAt(0)?.select()
                        currentViewMode = ViewMode.TODAY
                    }
                }

                loadAppointments()
            } catch (e: Exception) {
                selectedDate = Calendar.getInstance()
                loadAppointments()
            }
        }

        binding.rvCalendar.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = calendarAdapter
        }

        calendarAdapter.submitList(currentCalendarDays)
        updateDateDisplay()
    }

    private fun setupAppointmentsRecyclerView() {
        binding.rvAppointments.apply {
            layoutManager = LinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL,
                false
            )
            setHasFixedSize(true)
        }

        appointmentAdapter = MyAppointmentAdapter(
            onItemClick = { item ->
                showAppointmentDetails(item)
            },
            onEditClick = { item ->
                showEditAppointmentDialog(item)
            },
            onDeleteSuccess = {
                // Refresh appointments after deletion
                loadAppointments()
            }
        )

        binding.rvAppointments.adapter = appointmentAdapter
    }

    private fun loadAppointments() {
        showLoading(true)

        when (currentViewMode) {
            ViewMode.TODAY -> loadTodaysAppointments()
            ViewMode.UPCOMING -> loadUpcomingAppointments()
        }
    }

    private fun loadTodaysAppointments() {
        // Get today's date
        println("════════════════════════════════════════════════════")
        println("DEBUG: Loading TODAY's appointments")
        println("Selected date: ${Date(selectedDate.timeInMillis)}")

        // Set to start of selected day
        val startOfDay = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            println("Start of day: ${Date(timeInMillis)}")
        }

        val endOfDay = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            println("End of day: ${Date(timeInMillis)}")
        }
        println("════════════════════════════════════════════════════")

        loadProviderAppointments(startOfDay.timeInMillis, endOfDay.timeInMillis)
    }

    private fun loadUpcomingAppointments() {
        // For upcoming, get from start of tomorrow onwards (exclude today)
        val calendar = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If selected date is today, show from tomorrow onwards
        // If selected date is future, show from that date onwards
        val selectedDateString = dateFormat.format(calendar.time)
        val todayString = todayFormat.format(Date())

        if (selectedDateString == todayString) {
            // If today is selected, start from tomorrow
            calendar.add(Calendar.DATE, 1)
        }

        println("════════════════════════════════════════════════════")
        println("DEBUG: Loading UPCOMING appointments")
        println("From: ${Date(calendar.timeInMillis)} onwards")
        println("════════════════════════════════════════════════════")

        loadProviderAppointments(calendar.timeInMillis, null)
    }

    private fun loadProviderAppointments(startDate: Long?, endDate: Long?) {
        val providerId = databaseHelper.getCurrentUserId()

        println("════════════════════════════════════════════════════")
        println("DEBUG: Loading appointments")
        println("Provider ID: $providerId")
        println("Start Date: ${startDate?.let { Date(it) }}")
        println("End Date: ${endDate?.let { Date(it) }}")
        println("View Mode: $currentViewMode")
        println("════════════════════════════════════════════════════")

        databaseHelper.getProviderAppointments(
            providerId = providerId,
            startDate = startDate,
            endDate = endDate,
            statusFilter = AppointmentStatus.CONFIRMED.value,
            callback = { appointments ->
                println("════════════════════════════════════════════════════")
                println("DEBUG: Received ${appointments.size} appointments")

                // Filter out past appointments for Upcoming tab
                val filteredAppointments = when (currentViewMode) {
                    ViewMode.TODAY -> {
                        // For Today tab, show all appointments for the selected day
                        appointments
                    }
                    ViewMode.UPCOMING -> {
                        // For Upcoming tab, filter out past appointments
                        val now = System.currentTimeMillis()
                        appointments.filter { it.appointmentDate >= now }
                    }
                }

                println("DEBUG: Showing ${filteredAppointments.size} filtered appointments")
                println("════════════════════════════════════════════════════")

                showLoading(false)
                updateAppointmentsList(filteredAppointments)
            }
        )
    }

    private fun updateAppointmentsList(appointments: List<Appointment>) {
        this.appointments.clear()
        this.appointments.addAll(appointments)

        val appointmentItems = appointments.map { appointment ->
            convertToPAppointmentItem(appointment)
        }

        appointmentAdapter.submitList(appointmentItems)

        // Update UI text based on current view mode
        val titleText = when (currentViewMode) {
            ViewMode.TODAY -> getString(R.string.today)
            ViewMode.UPCOMING -> getString(R.string.upcoming)
        }
        binding.tvToday.text = titleText

        // Show/hide empty state with appropriate message
        if (appointments.isEmpty()) {
            val emptyMessage = when (currentViewMode) {
                ViewMode.TODAY -> "No appointments for today"
                ViewMode.UPCOMING -> "No upcoming appointments"
            }
            showEmptyState(true, emptyMessage)
        } else {
            showEmptyState(false)
        }
    }

    private fun convertToPAppointmentItem(appointment: Appointment): P_My_AppointmentItem {
        val date = displayDateFormat.format(Date(appointment.appointmentDate))
        val time = timeFormat.format(Date(appointment.appointmentDate))
        val duration = "${appointment.duration} minutes"
        val status = when (appointment.status) {
            AppointmentStatus.CONFIRMED.value -> "Confirmed"
            AppointmentStatus.CANCELLED.value -> "Cancelled"
            AppointmentStatus.COMPLETED.value -> "Completed"
            AppointmentStatus.NO_SHOW.value -> "No Show"
            else -> appointment.status
        }

        return P_My_AppointmentItem(
            serviceName = appointment.getServiceDisplayName(language),
            clinicName = appointment.getProviderDisplayName(language),
            duration = duration,
            date = date,
            time = time,
            status = status,
            appointment = appointment
        )
    }

    private fun showAppointmentDetails(item: P_My_AppointmentItem) {
        item.appointment?.let { appointment ->
            val dateStr = displayDateFormat.format(Date(appointment.appointmentDate))
            val timeStr = timeFormat.format(Date(appointment.appointmentDate))

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(appointment.getServiceDisplayName(language))
                .setMessage(
                    """
                    Client: ${appointment.userName}
                    Service: ${appointment.getServiceDisplayName(language)}
                    Date: $dateStr
                    Time: $timeStr
                    Duration: ${appointment.duration} minutes
                    Price: ${appointment.price} ${appointment.currency}
                    Status: ${appointment.status}
                    Category: ${appointment.getCategoryDisplayName(language)}
                    Notes: ${appointment.getNotes(language)}
                    """.trimIndent()
                )
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton("Update Status") { dialog, _ ->
                    showUpdateStatusDialog(appointment)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel Appointment") { dialog, _ ->
                    showCancelConfirmationDialog(appointment)
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun showUpdateStatusDialog(appointment: Appointment) {
        val statuses = arrayOf("Confirmed", "Completed", "Cancelled", "No Show")
        var selectedStatus = appointment.status

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Update Appointment Status")
            .setSingleChoiceItems(statuses, getStatusIndex(appointment.status)) { dialog, which ->
                selectedStatus = when (which) {
                    0 -> AppointmentStatus.CONFIRMED.value
                    1 -> AppointmentStatus.COMPLETED.value
                    2 -> AppointmentStatus.CANCELLED.value
                    3 -> AppointmentStatus.NO_SHOW.value
                    else -> AppointmentStatus.CONFIRMED.value
                }
            }
            .setPositiveButton("Update") { dialog, _ ->
                updateAppointmentStatus(appointment, selectedStatus)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun getStatusIndex(status: String): Int {
        return when (status) {
            AppointmentStatus.CONFIRMED.value -> 0
            AppointmentStatus.COMPLETED.value -> 1
            AppointmentStatus.CANCELLED.value -> 2
            AppointmentStatus.NO_SHOW.value -> 3
            else -> 0
        }
    }

    private fun updateAppointmentStatus(appointment: Appointment, newStatus: String) {
        databaseHelper.updateAppointmentStatus(
            appointmentId = appointment.id,
            newStatus = newStatus,
            onSuccess = {
                Toast.makeText(requireContext(), "Status updated successfully", Toast.LENGTH_SHORT).show()
                loadAppointments()
            }
        )
    }

    private fun showEditAppointmentDialog(item: P_My_AppointmentItem) {
        item.appointment?.let { appointment ->
            val bottomSheet = EditAppointmentBottomSheet().apply {
                setAppointment(appointment)
                setOnAppointmentUpdatedListener { updatedAppointment ->
                    // Refresh appointments after edit
                    loadAppointments()
                }
            }
            bottomSheet.show(childFragmentManager, "EditAppointmentBottomSheet")
        }
    }

    private fun showCancelConfirmationDialog(appointment: Appointment) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Appointment")
            .setMessage("Are you sure you want to cancel this appointment?")
            .setPositiveButton("Yes, Cancel") { dialog, _ ->
                cancelAppointment(appointment)
                dialog.dismiss()
            }
            .setNegativeButton("No, Keep") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun cancelAppointment(appointment: Appointment) {
        AppointmentRepository.cancelAppointment(
            appointmentId = appointment.id,
            cancelledBy = "provider",
            reason = "Provider cancelled",
            onSuccess = {
                Toast.makeText(
                    requireContext(),
                    "Appointment cancelled successfully",
                    Toast.LENGTH_SHORT
                ).show()
                loadAppointments()
            }
        )
    }

    private fun TodayDayView() {
        binding.rvCalendar.visibility = View.VISIBLE
        selectedDate = Calendar.getInstance()
        updateDateDisplay()
        loadAppointments()
    }

    private fun upcomingView() {
        binding.rvCalendar.visibility = View.VISIBLE
        selectedDate = Calendar.getInstance()
        updateDateDisplay()
        loadAppointments()
    }

    private fun updateDateDisplay() {
        binding.tvDate.text = displayDateFormat.format(selectedDate.time)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.rvAppointments.visibility = View.GONE
        } else {
            binding.rvAppointments.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(show: Boolean, message: String = "") {
        if (message.isNotEmpty()) {
            binding.tvEmptyAppointments.text = message
        }
        binding.tvEmptyAppointments.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvAppointments.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment resumes
        loadAppointments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}