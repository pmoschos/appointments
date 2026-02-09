package com.ai.appointments.fragments.provider

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.adapters.HistoryAdapter
import com.ai.appointments.bottomsheetdialouges.ApplyHistoryFilterBottomSheet
import com.ai.appointments.databinding.FragmentPHistoryBinding
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.db.models.AppointmentHistoricity
import com.ai.appointments.db.models.FilterSelection
import java.text.SimpleDateFormat
import java.util.*

class P_HistoryFragment : Fragment() {

    private var _binding: FragmentPHistoryBinding? = null
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
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPHistoryBinding.inflate(inflater, container, false)
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

        // Set click listener for appointments
        historyAdapter.onItemClick = { appointmentHistory ->
            appointmentHistory.appointment?.let {
                showAppointmentDetails(it)
            }
        }

        // Load provider appointments
        loadProviderAppointments()

        // Set up filter button
        binding.btnFilter.setOnClickListener {
            showFilterBottomSheet()
        }

        // Add debug button temporarily
        setupDebugButton()
    }

    private fun setupDebugButton() {
        // Add a debug button to check what's happening
        binding.btnFilter.setOnLongClickListener {
            debugCheckAppointments()
            true
        }
    }

    private fun debugCheckAppointments() {
        val providerId = databaseHelper.getCurrentUserId()
        Log.d("DEBUG", "=== Checking Appointments ===")
        Log.d("DEBUG", "Provider ID: $providerId")

        databaseHelper.appointmentsRef
            .orderByChild("providerId")
            .equalTo(providerId)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    Log.d("DEBUG", "Total found in DB: ${snapshot.childrenCount}")

                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)
                        appointment?.let {
                            Log.d("DEBUG", "Appointment ID: ${child.key}")
                            Log.d("DEBUG", "  Service: ${it.serviceName}")
                            Log.d("DEBUG", "  Date: ${Date(it.appointmentDate)}")
                            Log.d("DEBUG", "  Provider ID: ${it.providerId}")
                            Log.d("DEBUG", "  Status: ${it.status}")
                            Log.d("DEBUG", "  Price: ${it.price}")
                            Log.d("DEBUG", "  Duration: ${it.duration}")
                        }
                    }

                    // Show toast with results
                    Toast.makeText(requireContext(),
                        "Found ${snapshot.childrenCount} appointments",
                        Toast.LENGTH_LONG).show()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Log.e("DEBUG", "Error: ${error.message}")
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun loadProviderAppointments() {
        showLoading(true)

        val providerId = databaseHelper.getCurrentUserId()
        Log.d("P_HistoryFragment", "Loading appointments for provider: $providerId")

        // Use the DatabaseHelper method to get appointments
        databaseHelper.getProviderAppointments(
            providerId = providerId,
            startDate = null, // Get all appointments
            endDate = null,
            statusFilter = null, // Get all statuses
            callback = { appointments ->
                Log.d("P_HistoryFragment", "Received ${appointments.size} appointments from DatabaseHelper")

                showLoading(false)

                if (appointments.isEmpty()) {
                    showEmptyState(true)
                    updateStats(emptyList())
                    return@getProviderAppointments
                }

                // Store all appointments
                currentAppointments = appointments

                // Log all appointments for debugging
                appointments.forEachIndexed { index, appointment ->
                    Log.d("P_HistoryFragment", "Appointment $index: ${appointment.serviceName}")
                    Log.d("P_HistoryFragment", "  Date: ${Date(appointment.appointmentDate)}")
                    Log.d("P_HistoryFragment", "  Price: ${appointment.price}")
                    Log.d("P_HistoryFragment", "  Duration: ${appointment.duration}")
                    Log.d("P_HistoryFragment", "  Status: ${appointment.status}")
                }

                // Apply default filter
                applyFilter(currentFilter)

                // Update stats with ALL appointments
                updateStats(currentAppointments)

                showEmptyState(false)
            }
        )
    }

    private fun applyFilter(filter: String) {
        currentFilter = filter
        val now = System.currentTimeMillis()

        filteredAppointments = when (filter) {
            "upcoming" -> currentAppointments.filter {
                it.status == AppointmentStatus.CONFIRMED.value &&
                        it.appointmentDate > now
            }
            "past" -> currentAppointments.filter {
                it.status == AppointmentStatus.COMPLETED.value ||
                        (it.status == AppointmentStatus.CONFIRMED.value &&
                                it.appointmentDate < now)
            }
            "cancelled" -> currentAppointments.filter {
                it.status == AppointmentStatus.CANCELLED.value ||
                        it.status == AppointmentStatus.NO_SHOW.value
            }
            else -> currentAppointments // "all"
        }

        Log.d("P_HistoryFragment", "Filter '$filter' applied: ${filteredAppointments.size} appointments")
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
                appointment = appointment
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

    private fun showAppointmentDetails(appointment: Appointment) {
        val dateStr = displayDateFormat.format(Date(appointment.appointmentDate))
        val timeStr = timeFormat.format(Date(appointment.appointmentDate))

        val message = """
            Service: ${appointment.getServiceDisplayName(language)}
            Client: ${appointment.userName}
            Date: $dateStr
            Time: $timeStr
            Duration: ${appointment.duration} minutes
            Price: ${appointment.price} ${appointment.currency}
            Status: ${appointment.status}
            Category: ${appointment.getCategoryDisplayName(language)}
            Notes: ${appointment.getNotes(language)}
        """.trimIndent()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Appointment Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateStats(appointments: List<Appointment>) {
        Log.d("P_HistoryFragment", "Updating stats for ${appointments.size} appointments")

        if (appointments.isEmpty()) {
            // Show 0 for all stats
            binding.tvTotalAppointments.text = "0"
            binding.tvTotalTime.text = "0 min"
            binding.tvTotalCost.text = "$0.00 EUR"
            binding.pbTotalTime.progress = 0
            binding.tvProgressPercent.text = "0%"
            return
        }

        // Calculate stats from ALL appointments
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

        // Update UI with real data
        binding.tvTotalAppointments.text = totalAppointments.toString()
        binding.tvTotalTime.text = formatDuration(totalDuration)

        // Get currency from first appointment or default to EUR
        val currency = appointments.firstOrNull()?.currency ?: "EUR"
        binding.tvTotalCost.text = String.format("%.2f %s", totalCost, currency)

        // Update progress bar
        binding.pbTotalTime.progress = progress
        binding.tvProgressPercent.text = "$progress%"

        // Log stats for debugging
        Log.d("P_HistoryFragment", "===== STATS =====")
        Log.d("P_HistoryFragment", "Total appointments: $totalAppointments")
        Log.d("P_HistoryFragment", "Total duration: $totalDuration minutes")
        Log.d("P_HistoryFragment", "Total cost: $totalCost $currency")
        Log.d("P_HistoryFragment", "Current month appointments: ${currentMonthAppointments.size}")
        Log.d("P_HistoryFragment", "Current month duration: $currentMonthDuration minutes")
        Log.d("P_HistoryFragment", "Progress: $progress%")

        // Calculate and set trends
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

            Log.d("P_HistoryFragment", "Trends: Appointments $appointmentChange%, Duration $durationChange%, Cost $costChange%")
        } else {
            Log.d("P_HistoryFragment", "Not enough months data for trends")
        }
    }

    private fun calculatePercentageChange(current: Double, previous: Double): Double {
        return if (previous == 0.0) {
            if (current == 0.0) 0.0 else 100.0
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
        return try {
            val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = dateTimeFormat.parse(appointment.scheduledDateTime)
            if (date != null) {
                val calendar = Calendar.getInstance().apply { time = date }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 6..11
            } else {
                val calendar = Calendar.getInstance().apply { timeInMillis = appointment.appointmentDate }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour in 6..11
            }
        } catch (e: Exception) {
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
                hour in 12..17
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
                hour >= 18 || hour < 6
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
            val timeMatches = filterSelection.timeRange?.let { timeRange ->
                when (timeRange) {
                    "any" -> true
                    "morning" -> isMorningAppointment(appointment)
                    "afternoon" -> isAfternoonAppointment(appointment)
                    "evening" -> isEveningAppointment(appointment)
                    else -> true
                }
            } ?: true

            val categoryMatches = filterSelection.category?.let { category ->
                category == "all" || appointment.category == category
            } ?: true

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
        loadProviderAppointments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}