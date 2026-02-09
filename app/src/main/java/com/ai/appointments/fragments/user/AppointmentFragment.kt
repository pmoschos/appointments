package com.ai.appointments.fragments.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.adapters.AppointmentAdapter
import com.ai.appointments.adapters.CalendarAdapter
import com.ai.appointments.bottomsheetdialouges.EditAppointmentBottomSheet
import com.ai.appointments.calender.ColoredDate
import com.ai.appointments.calender.KalendarView
import com.ai.appointments.databinding.FragmentAppointmentBinding
import com.ai.appointments.db.Repository.AppointmentRepository
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.model.AppointmentItem
import com.ai.appointments.model.CalendarDay
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AppointmentFragment : Fragment() {

    private var _binding: FragmentAppointmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var appointmentAdapter: AppointmentAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var mKalendarView: KalendarView

    private var currentCalendarDays = listOf<CalendarDay>()
    private var appointments = mutableListOf<Appointment>()
    private var selectedDate: Calendar = Calendar.getInstance()
    private var currentViewMode: ViewMode = ViewMode.DAY

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private var language: String = "en"

    enum class ViewMode {
        DAY, WEEK, MONTH
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper.getInstance()
        language = Locale.getDefault().language

        setupTabs()
        setupCalendar()
        setupKalendarView()
        setupAppointmentsRecyclerView()
        updateDateDisplay()
        loadAppointments()
    }

    private fun setupTabs() {
        binding.tabLayout.getTabAt(0)?.select()

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentViewMode = ViewMode.DAY
                        loadDayView()
                    }
                    1 -> {
                        currentViewMode = ViewMode.WEEK
                        loadWeekView()
                    }
                    2 -> {
                        currentViewMode = ViewMode.MONTH
                        loadMonthView()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupKalendarView() {
        mKalendarView = binding.kalendar

        // Set initial selected date = today
        mKalendarView.setInitialSelectedDate(Date())

        // Date click listener
        mKalendarView.setDateSelector(object : KalendarView.DateSelector {
            override fun onDateClicked(selectedDate: Date) {
                this@AppointmentFragment.selectedDate.time = selectedDate
                updateDateDisplay()
                loadAppointments()
            }
        })

        // Month change listener
        mKalendarView.setMonthSelector(object : KalendarView.MonthChanger {
            override fun onMonthChanged(changedMonth: Date) {
                // When user changes month in calendar, update selected date
                selectedDate.time = changedMonth
                updateDateDisplay()
                loadAppointments()
            }
        })
    }

    private fun updateCalendarColoredDates(appointments: List<Appointment>) {
        val coloredDates = mutableListOf<ColoredDate>()

        appointments.forEach { appt ->
            val date = Date(appt.appointmentDate)

            // Color code based on appointment status
            val colorRes = when (appt.status?.lowercase(Locale.getDefault())) {
                "confirmed" -> R.color.green
                "completed" -> R.color.blue
                "cancelled" -> R.color.red
                else -> R.color.gray
            }

            coloredDates.add(
                ColoredDate(date, resources.getColor(colorRes, requireContext().theme))
            )
        }

        mKalendarView.setColoredDates(coloredDates)
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
                isSelected = i == 0
            ))
        }

        currentCalendarDays = days
        selectedDate = Calendar.getInstance() // Today

        calendarAdapter = CalendarAdapter { selectedDay ->
            currentCalendarDays = currentCalendarDays.map { day ->
                day.copy(isSelected = day.dateString == selectedDay.dateString)
            }
            calendarAdapter.submitList(currentCalendarDays)

            // Update selected date
            try {
                selectedDate.time = dateFormat.parse(selectedDay.dateString) ?: Date()
                updateDateDisplay()
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

        appointmentAdapter = AppointmentAdapter(
            onItemClick = { appointment ->
                showAppointmentDetails(appointment)
            },
            onEditClick = { appointment ->
                showEditAppointmentDialog(appointment)
            },
            onDeleteSuccess = {
                // Refresh the appointments list after deletion
                loadAppointments()
            }
        )

        binding.rvAppointments.adapter = appointmentAdapter
    }

    private fun loadAppointments() {
        showLoading(true)

        when (currentViewMode) {
            ViewMode.DAY -> loadAppointmentsForDay()
            ViewMode.WEEK -> loadAppointmentsForWeek()
            ViewMode.MONTH -> loadAppointmentsForMonth()
        }
    }

    private fun loadAppointmentsForDay() {
        // Get start and end of the selected day
        val startOfDay = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endOfDay = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        AppointmentRepository.getUserAppointments(
            userId = databaseHelper.getCurrentUserId(),
            startDate = startOfDay.timeInMillis,
            endDate = endOfDay.timeInMillis,
            callback = { appointments ->
                showLoading(false)
                updateAppointmentsList(appointments)
                // Update colored dates on calendar
                updateCalendarColoredDates(appointments)
            }
        )
    }

    private fun loadAppointmentsForWeek() {
        // When in WEEK view, load appointments for the entire week
        val calendar = Calendar.getInstance().apply { time = selectedDate.time }
        calendar.firstDayOfWeek = Calendar.MONDAY

        // Get start of week (Monday)
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DATE, -1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis

        // Get end of week (Sunday)
        calendar.add(Calendar.DATE, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfWeek = calendar.timeInMillis

        AppointmentRepository.getUserAppointments(
            userId = databaseHelper.getCurrentUserId(),
            startDate = startOfWeek,
            endDate = endOfWeek,
            callback = { appointments ->
                showLoading(false)
                updateAppointmentsList(appointments)
                // Update colored dates on calendar
                updateCalendarColoredDates(appointments)
            }
        )
    }

    private fun loadAppointmentsForMonth() {
        // When in MONTH view, load appointments for the entire month
        val calendar = Calendar.getInstance().apply { time = selectedDate.time }

        // Get start of month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        // Get end of month
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DATE, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calendar.timeInMillis

        AppointmentRepository.getUserAppointments(
            userId = databaseHelper.getCurrentUserId(),
            startDate = startOfMonth,
            endDate = endOfMonth,
            callback = { appointments ->
                showLoading(false)
                updateAppointmentsList(appointments)
                // Update colored dates on calendar
                updateCalendarColoredDates(appointments)
            }
        )
    }

    private fun updateAppointmentsList(appointments: List<Appointment>) {
        this.appointments.clear()
        this.appointments.addAll(appointments)

        val appointmentItems = appointments.map { appointment ->
            convertToAppointmentItem(appointment)
        }

        appointmentAdapter.submitList(appointmentItems)

        // Show/hide empty state
        if (appointments.isEmpty()) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
        }
    }

    private fun convertToAppointmentItem(appointment: Appointment): AppointmentItem {
        val date = displayDateFormat.format(Date(appointment.appointmentDate))
        val time = timeFormat.format(Date(appointment.appointmentDate))

        return AppointmentItem(
            serviceName = appointment.getServiceDisplayName(language),
            clinicName = appointment.getProviderDisplayName(language),
            date = date,
            time = time,
            notes = appointment.getNotes(language),
            appointment = appointment
        )
    }

    private fun showAppointmentDetails(appointment: Appointment) {
        val dateStr = displayDateFormat.format(Date(appointment.appointmentDate))
        val timeStr = timeFormat.format(Date(appointment.appointmentDate))

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(appointment.getServiceDisplayName(language))
            .setMessage(
                """
                Provider: ${appointment.getProviderDisplayName(language)}
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
            .setNeutralButton("Cancel Appointment") { dialog, _ ->
                showCancelConfirmationDialog(appointment)
                dialog.dismiss()
            }
            .show()
    }

    private fun showEditAppointmentDialog(appointment: Appointment) {
        val bottomSheet = EditAppointmentBottomSheet().apply {
            setAppointment(appointment)
            setOnAppointmentUpdatedListener { updatedAppointment ->
                // Refresh appointments after edit
                loadAppointments()
            }
        }
        bottomSheet.show(childFragmentManager, "EditAppointmentBottomSheet")
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
            cancelledBy = "user",
            reason = "User cancelled",
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

    private fun loadDayView() {
        binding.rvCalendar.visibility = View.VISIBLE
        binding.cvKalendar.visibility = View.GONE
        selectedDate = Calendar.getInstance()
        updateDateDisplay()
        loadAppointments()
    }

    private fun loadWeekView() {
        binding.rvCalendar.visibility = View.GONE
        binding.cvKalendar.visibility = View.VISIBLE
        loadAppointments()
    }

    private fun loadMonthView() {
        binding.rvCalendar.visibility = View.GONE
        binding.cvKalendar.visibility = View.VISIBLE
        loadAppointments()
    }

    private fun updateDateDisplay() {
        binding.tvDate.text = displayDateFormat.format(selectedDate.time)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvAppointments.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvAppointments.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyAppointments.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvAppointments.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        loadAppointments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}