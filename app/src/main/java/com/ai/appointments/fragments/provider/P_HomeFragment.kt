package com.ai.appointments.fragments.provider

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.activities.AddServiceActivity
import com.ai.appointments.activities.EditServiceActivity
import com.ai.appointments.adapters.ProviderServicesAdapter
import com.ai.appointments.adapters.ServiceAdapter
import com.ai.appointments.databinding.FragmentPHomeBinding
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.model.ServiceItem
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.util.*

class P_HomeFragment : Fragment() {

    private var _binding: FragmentPHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var servicesAdapter: ProviderServicesAdapter
    private lateinit var statsAdapter: ServiceAdapter
    private var providerServices = listOf<Service>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPHomeBinding.inflate(inflater, container, false)
        databaseHelper = DatabaseHelper.getInstance()

        // Setup stats cards with placeholders
        setupStatsCards()

        // Setup services list
        setupServicesList()

        binding.btnAddService.setOnClickListener {
            val intent = Intent(requireContext(), AddServiceActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load all provider data
        loadProviderData()
    }

    private fun loadProviderData() {
        showLoading(true)

        try {
            val providerId = databaseHelper.getCurrentUserId()
            Log.d("P_HomeFragment", "Loading data for provider: $providerId")

            // 1. Load provider info (name, image)
            loadProviderInfo(providerId)

            // 2. Load provider services
            loadProviderServices(providerId)

            // 3. Load appointments for stats (today's appointments & monthly earnings)
            loadAppointmentsForStats(providerId)

        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                requireContext(),
                "Error loading data: ${e.message ?: "Unknown error"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadProviderInfo(providerId: String) {
        databaseHelper.getServiceProvider(providerId) { provider ->
            provider?.let {
                // Update provider name
                val providerName = if (it.businessInfo.businessName.isNotEmpty()) {
                    it.businessInfo.businessName
                } else {
                    "${it.firstName} ${it.lastName}"
                }
                binding.tvName.text =getString(R.string.hi, providerName)


                // Load profile image
                if (it.profileImageUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(it.profileImageUrl)
                        .placeholder(R.drawable.demo_image)
                        .into(binding.ivProfile)
                }
            } ?: run {
                binding.tvName.text = "Hi, Provider!"
            }
        }
    }

    private fun loadProviderServices(providerId: String) {
        databaseHelper.getProviderServices(
            providerId = providerId,
            callback = { services ->
                Log.d("P_HomeFragment", "Loaded ${services.size} services")
                providerServices = services
                updateServicesList(services)
            }
        )
    }

    private fun loadAppointmentsForStats(providerId: String) {
        Log.d("P_HomeFragment", "Loading appointments for stats...")

        // For testing: Your appointments are in Feb 2026
        // So we need to use 2026 dates for filtering
        val calendar = Calendar.getInstance()

        // Set to February 2026 (since your appointments are in 2026)
        // Comment these lines for real app, uncomment for testing
        calendar.set(2026, Calendar.FEBRUARY, 6, 0, 0, 0)

        // Calculate today's date range (Feb 6, 2026 for testing)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endOfToday = calendar.timeInMillis

        // Calculate February 2026 month range
        calendar.set(2026, Calendar.FEBRUARY, 1, 0, 0, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(2026, Calendar.FEBRUARY, 28, 23, 59, 59)
        val endOfMonth = calendar.timeInMillis

        Log.d("P_HomeFragment", "Today range: ${Date(startOfToday)} to ${Date(endOfToday)}")
        Log.d("P_HomeFragment", "Month range: ${Date(startOfMonth)} to ${Date(endOfMonth)}")

        // Query appointments directly
        databaseHelper.appointmentsRef
            .orderByChild("providerId")
            .equalTo(providerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val appointments = mutableListOf<Appointment>()
                    var todayCount = 0
                    var monthlyEarnings = 0.0

                    Log.d("P_HomeFragment", "Found ${snapshot.childrenCount} appointments")

                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)
                        appointment?.let {
                            appointments.add(it)

                            val appointmentDate = it.appointmentDate
                            val appointmentDateObj = Date(appointmentDate)

                            Log.d("P_HomeFragment", "Appointment: ${it.serviceName}")
                            Log.d("P_HomeFragment", "  Date: $appointmentDateObj")
                            Log.d("P_HomeFragment", "  Price: ${it.price}")
                            Log.d("P_HomeFragment", "  Status: ${it.status}")

                            // Check if appointment is today (Feb 6, 2026)
                            if (appointmentDate in startOfToday..endOfToday) {
                                if (it.status == "confirmed") {
                                    todayCount++
                                    Log.d("P_HomeFragment", "  → TODAY'S APPOINTMENT")
                                } else {
                                    Log.d("P_HomeFragment", "  → Today but status: ${it.status}")
                                }
                            } else {
                                Log.d("P_HomeFragment", "  → Not today")
                            }

                            // Check if appointment is in current month (Feb 2026)
                            if (appointmentDate in startOfMonth..endOfMonth) {
                                monthlyEarnings += it.price
                                Log.d("P_HomeFragment", "  → MONTHLY EARNING: $${it.price}")
                            }
                        }
                    }

                    Log.d("P_HomeFragment", "===== FINAL STATS =====")
                    Log.d("P_HomeFragment", "Today's appointments: $todayCount")
                    Log.d("P_HomeFragment", "Monthly earnings: $$monthlyEarnings")

                    // Update stats cards with real data
                    updateStatsCards(todayCount, monthlyEarnings)
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("P_HomeFragment", "Error loading appointments: ${error.message}")
                    // Show default stats
                    updateStatsCards(0, 0.0)
                    showLoading(false)
                }
            })
    }

    private fun setupStatsCards() {
        // Initialize with placeholder values
        val statsList = listOf(
            ServiceItem(R.drawable.ic_health, "0", "Today's Appointments"),
            ServiceItem(R.drawable.ic_wellness, "$0.00", "Monthly Earnings"),
        )

        statsAdapter = ServiceAdapter(statsList) { item ->
            handleServiceClick(item)
        }

        binding.rvServices.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = statsAdapter
        }
    }

    private fun updateStatsCards(todayCount: Int, monthlyEarnings: Double) {
        val statsList = listOf(
            ServiceItem(R.drawable.ic_health, todayCount.toString(), "Today's Appointments"),
            ServiceItem(R.drawable.ic_wellness, String.format("$%.2f", monthlyEarnings), "Monthly Earnings"),
        )

        // Update the adapter with real data
        statsAdapter.updateData(statsList)
    }

    private fun handleServiceClick(item: ServiceItem) {
        when (item.title) {
            "Today's Appointments" -> {
                Toast.makeText(
                    requireContext(),
                    "You have ${item.subtitle} appointments today",
                    Toast.LENGTH_SHORT
                ).show()
            }
            "Monthly Earnings" -> {
                Toast.makeText(
                    requireContext(),
                    "Monthly earnings: ${item.subtitle}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setupServicesList() {
        binding.rvServicesProvider.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            setHasFixedSize(true)
        }

        val language = getCurrentLanguage()
        servicesAdapter = ProviderServicesAdapter(
            language = language,
            onItemClick = { service ->
                Toast.makeText(
                    requireContext(),
                    "${service.getDisplayName(language)} - ${service.getPriceRange()}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEditClick = { service ->
                // Open edit service screen
                openEditServiceActivity(service)
            },
            onDeleteClick = { service ->
                deleteService(service)
            }
        )

        binding.rvServicesProvider.adapter = servicesAdapter
    }

    private fun openEditServiceActivity(service: Service) {
        val intent = Intent(requireContext(), EditServiceActivity::class.java).apply {
            putExtra("SERVICE_ID", service.service_id)
            putExtra("SERVICE_NAME", service.getDisplayName(getCurrentLanguage()))
            putExtra("SERVICE_DESCRIPTION", service.description)
            putExtra("SERVICE_CATEGORY", service.category)
            putExtra("SERVICE_DURATION", service.durationMax.toString())
            putExtra("SERVICE_PRICE", service.priceMax.toString())
            putExtra("SERVICE_CURRENCY", service.currency)
            putExtra("SERVICE_IS_ACTIVE", service.isActive)
        }
        startActivity(intent)
    }

    private fun updateServicesList(services: List<Service>) {
        if (services.isEmpty()) {
            binding.tvEmptyAppointments.apply {
                text = "No services added yet"
                visibility = View.VISIBLE
            }
            binding.rvServicesProvider.visibility = View.GONE
        } else {
            binding.tvEmptyAppointments.visibility = View.GONE
            binding.rvServicesProvider.visibility = View.VISIBLE

            // Fix: Use correct service ID (service_id)
            val wrappedServices = services.map { service ->
                ProviderServicesAdapter.ServiceItemWrapper(
                    id = service.service_id ?: UUID.randomUUID().toString(),
                    service = service
                )
            }
            servicesAdapter.submitList(wrappedServices)
        }
    }

    private fun deleteService(service: Service) {
        showLoading(true)
        try {
            val providerId = databaseHelper.getCurrentUserId()
            val serviceId = service.service_id ?: throw Exception("Service ID not found")

            databaseHelper.deleteService(
                serviceId = serviceId, // Fixed: Use service_id not providerId
                providerId = providerId,
                onSuccess = {
                    showLoading(false)
                    Toast.makeText(requireContext(), "Service deleted", Toast.LENGTH_SHORT).show()
                    loadProviderData() // Refresh all data
                }
            )
        } catch (e: Exception) {
            showLoading(false)
            Toast.makeText(
                requireContext(),
                "Error deleting service: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.rvServicesProvider.visibility = View.GONE
            binding.rvServices.visibility = View.GONE
        } else {
            binding.rvServicesProvider.visibility = View.VISIBLE
            binding.rvServices.visibility = View.VISIBLE
        }
    }

    private fun getCurrentLanguage(): String {
        val locale = Locale.getDefault().language
        return if (locale == "el") "el" else "en"
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to fragment
        loadProviderData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}