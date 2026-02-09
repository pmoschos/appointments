package com.ai.appointments.fragments.user

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.adapters.HistoryAdapter
import com.ai.appointments.bottomsheetdialouges.ApplyHistoryFilterBottomSheet
import com.ai.appointments.databinding.FragmentHistoryBinding
import com.ai.appointments.db.Repository.AppointmentRepository
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.db.models.AppointmentHistoricity
import com.ai.appointments.db.models.FilterSelection
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var historyAdapter: HistoryAdapter

    private var currentAppointments = listOf<Appointment>()
    private var filteredAppointments = listOf<Appointment>()
    private var currentFilter = "all" // "all", "upcoming", "past", "cancelled"
    private var language: String = "en"

    private val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        databaseHelper = DatabaseHelper.getInstance()
        language = Locale.getDefault().language

        // Set up RecyclerView
        binding.rvHistoryList.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = HistoryAdapter(emptyList())
        binding.rvHistoryList.adapter = historyAdapter

        // Load user appointments
        loadUserAppointments()

        // Set up filter button
        binding.btnFilter.setOnClickListener {
            showFilterBottomSheet()
        }

        // Set up refresh listener (pull to refresh if you implement it)
    }

    private fun loadUserAppointments() {
        showLoading(true)

        val userId = databaseHelper.getCurrentUserId()

        AppointmentRepository.getUserAppointments(
            userId = userId,
            callback = { appointments ->
                showLoading(false)

                if (appointments.isEmpty()) {
                    showEmptyState(true)
                    return@getUserAppointments
                }

                currentAppointments = appointments
                applyFilter(currentFilter)
                updateStats(appointments)
                showEmptyState(false)
            }
        )
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter

        filteredAppointments = when (filter) {
            "upcoming" -> currentAppointments.filter {
                it.status == AppointmentStatus.CONFIRMED.value &&
                        it.appointmentDate > System.currentTimeMillis()
            }
            "past" -> currentAppointments.filter {
                it.status == AppointmentStatus.COMPLETED.value ||
                        (it.status == AppointmentStatus.CONFIRMED.value &&
                                it.appointmentDate < System.currentTimeMillis())
            }
            "cancelled" -> currentAppointments.filter {
                it.status == AppointmentStatus.CANCELLED.value ||
                        it.status == AppointmentStatus.NO_SHOW.value
            }
            else -> currentAppointments // "all"
        }

        updateAppointmentsList()
    }

    private fun updateAppointmentsList() {
        val historyItems = filteredAppointments.map { appointment ->
            AppointmentHistoricity(
                serviceName = appointment.getServiceDisplayName(language),
                category = appointment.getCategoryDisplayName(language),
                duration = "${appointment.duration} min",
                date = dateFormat.format(Date(appointment.appointmentDate)),
                price = "${appointment.price} ${appointment.currency}",
                appointment = appointment // Store original appointment for click handling
            )
        }

        historyAdapter.submitList(historyItems)

        // Show/hide empty state
        if (filteredAppointments.isEmpty()) {
            showEmptyState(true, getEmptyMessageForFilter(currentFilter))
        } else {
            showEmptyState(false)
        }
    }

    private fun getEmptyMessageForFilter(filter: String): String {
        return when (filter) {
            "upcoming" -> getString(R.string.no_upcoming_appointments)
            "past" -> getString(R.string.no_past_appointments)
            "cancelled" -> getString(R.string.no_cancelled_appointments)
            else -> getString(R.string.no_appointments)
        }
    }

    private fun updateStats(appointments: List<Appointment>) {
        // Calculate stats
        val totalAppointments = appointments.size
        val totalDuration = appointments.sumOf { it.duration }
        val totalCost = appointments.sumOf { it.price }

        // Get current month's appointments for progress calculation
        val currentMonth = monthFormat.format(Date())
        val currentMonthAppointments = appointments.filter {
            monthFormat.format(Date(it.appointmentDate)) == currentMonth
        }
        val currentMonthDuration = currentMonthAppointments.sumOf { it.duration }

        // Calculate progress (percentage of monthly goal, assuming 20 hours = 1200 minutes goal)
        val monthlyGoalMinutes = 1200
        val progress = if (monthlyGoalMinutes > 0) {
            (currentMonthDuration * 100 / monthlyGoalMinutes).coerceAtMost(100)
        } else {
            0
        }

        // Update UI
        binding.tvTotalAppointments.text = totalAppointments.toString()
        binding.tvTotalTime.text = formatDuration(totalDuration)
        binding.tvTotalCost.text = String.format("%.2f %s", totalCost, appointments.firstOrNull()?.currency ?: "$")

        // Update progress bar
        binding.pbTotalTime.progress = progress
        binding.tvProgressPercent.text = "$progress%"

        // Calculate and set trends (simplified - you can make this smarter)
        calculateAndSetTrends(appointments)
    }

    private fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60

        return if (hours > 0) {
            if (mins > 0) {
                "$hours hours $mins min"
            } else {
                "$hours hours"
            }
        } else {
            "$mins min"
        }
    }

    private fun calculateAndSetTrends(appointments: List<Appointment>) {
        // Group appointments by month
        val monthlyTotals = appointments.groupBy {
            monthFormat.format(Date(it.appointmentDate))
        }.mapValues { (_, monthlyAppointments) ->
            Triple(
                monthlyAppointments.size,
                monthlyAppointments.sumOf { it.duration },
                monthlyAppointments.sumOf { it.price }
            )
        }

        // Sort by month
        val sortedMonths = monthlyTotals.toList().sortedBy { it.first }

        if (sortedMonths.size >= 2) {
            val currentMonth = sortedMonths.last()
            val previousMonth = sortedMonths[sortedMonths.size - 2]

            // Calculate percentage change
            val appointmentChange = calculatePercentageChange(
                currentMonth.second.first.toDouble(),
                previousMonth.second.first.toDouble()
            )
            val durationChange = calculatePercentageChange(
                currentMonth.second.second.toDouble(),
                previousMonth.second.second.toDouble()
            )
            val costChange = calculatePercentageChange(
                currentMonth.second.third,
                previousMonth.second.third
            )

            // Update trend indicators (you could add ImageViews for arrows)
            Log.d("HistoryFragment", "Trends: Appointments $appointmentChange%, Duration $durationChange%, Cost $costChange%")
        }
    }

    private fun calculatePercentageChange(current: Double, previous: Double): Double {
        return if (previous == 0.0) {
            100.0
        } else {
            ((current - previous) / previous * 100)
        }
    }

    private fun showFilterBottomSheet() {
        val bottomSheet = ApplyHistoryFilterBottomSheet().apply {
            setOnFilterAppliedListener { filterSelection ->
                applyAdvancedFilters(filterSelection)
            }
        }
        bottomSheet.show(childFragmentManager, "ApplyHistoryFilterBottomSheet")
    }
    private fun isMorningAppointment(appointment: Appointment): Boolean {
        // Parse the scheduledDateTime string (format: "YYYY-MM-DD HH:mm")
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = dateTimeFormat.parse(appointment.scheduledDateTime)
            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 6..11 // Morning: 6 AM to 11:59 AM
            } else {
                // Fallback to appointmentDate timestamp
                val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 6..11
            }
        } catch (e: Exception) {
            // Fallback to appointmentDate timestamp
            val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour in 6..11
        }
    }

    private fun isAfternoonAppointment(appointment: Appointment): Boolean {
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = dateTimeFormat.parse(appointment.scheduledDateTime)
            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 12..17 // Afternoon: 12 PM to 5:59 PM
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 12..17
            }
        } catch (e: Exception) {
            val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour in 12..17
        }
    }

    private fun isEveningAppointment(appointment: Appointment): Boolean {
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = dateTimeFormat.parse(appointment.scheduledDateTime)
            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour >= 18 || hour < 6 // Evening: 6 PM to 5:59 AM
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour >= 18 || hour < 6
            }
        } catch (e: Exception) {
            val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hour >= 18 || hour < 6
        }
    }
    private fun applyAdvancedFilters(filterSelection: FilterSelection) {
        filteredAppointments = currentAppointments.filter { appointment ->
            // Time range filter
            val timeMatches = filterSelection.timeRange?.let { timeRange ->
                when (timeRange) {
                    "any" -> true
                    "morning" -> isMorningAppointment(appointment)
                    "afternoon" -> isAfternoonAppointment(appointment)
                    "evening" -> isEveningAppointment(appointment)
                    else -> true
                }
            } ?: true

            // Category filter
            val categoryMatches = filterSelection.category?.let { category ->
                category == "all" || appointment.category == category
            } ?: true

            // Status filter
            val statusMatches = filterSelection.status?.let { status ->
                when (status) {
                    "all" -> true
                    "upcoming" -> appointment.status == "confirmed" &&
                            appointment.appointmentDate > System.currentTimeMillis()
                    "completed" -> appointment.status == "completed"
                    "cancelled" -> appointment.status == "cancelled"
                    "no_show" -> appointment.status == "no_show"
                    else -> true
                }
            } ?: true

            timeMatches && categoryMatches && statusMatches
        }

        updateAppointmentsList()
    }
    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvHistoryList.visibility = View.GONE
            binding.emptyState.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvHistoryList.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(show: Boolean, message: String = getString(R.string.no_appointments)) {
        if (show) {
            binding.emptyState.visibility = View.VISIBLE
            binding.tvEmptyMessage.text = message
            binding.rvHistoryList.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvHistoryList.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment resumes
        loadUserAppointments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}