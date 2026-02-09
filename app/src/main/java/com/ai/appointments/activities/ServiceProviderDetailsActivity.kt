package com.ai.appointments.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.adapters.CalendarAdapter
import com.ai.appointments.adapters.TimeSlotAdapter
import com.ai.appointments.databinding.ActivityServiceProviderDetailsBinding
import com.ai.appointments.db.Repository.AvailabilityRepository
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.models.ServiceProvider
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.model.CalendarDay
import com.ai.appointments.model.TimeSlot
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ServiceProviderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceProviderDetailsBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    private lateinit var databaseHelper: DatabaseHelper

    private var currentCalendarDays = listOf<CalendarDay>()
    private var currentTimeSlots = listOf<TimeSlot>()

    private lateinit var provider: ServiceProvider
    private lateinit var service: Service
    private var providerId: String = ""
    private var serviceId: String = ""
    private var language: String = "en"
    private var selectedDate: String = ""
    private var selectedTime: String = ""

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val TAG = "ProviderDetailsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityServiceProviderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper.getInstance()

        providerId = intent.getStringExtra("provider_id") ?: ""
        serviceId = intent.getStringExtra("service_id") ?: ""
        language = intent.getStringExtra("language") ?: "en"

        Log.d(TAG, "Provider ID: $providerId, Service ID: $serviceId")

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCalendar()
        loadProviderData()
        setupTimeSlots()
        setupClickListeners()
    }

    private fun loadProviderData() {
        Log.d(TAG, "Loading provider data for ID: $providerId")

        databaseHelper.getServiceProvider(providerId) { loadedProvider ->
            if (loadedProvider != null) {
                provider = loadedProvider
                loadServiceData()
                updateProviderUI()
            } else {
                Toast.makeText(this, "Provider not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadServiceData() {
        Log.d(TAG, "Loading service data for service ID: $serviceId")

        if (serviceId.isEmpty()) {
            Log.e(TAG, "Service ID is empty, cannot load service")
            Toast.makeText(this, "Service ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load specific service by its ID
        databaseHelper.servicesRef.child(serviceId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val loadedService = snapshot.getValue(Service::class.java)
                    if (loadedService != null) {
                        service = loadedService
                        Log.d(TAG, "Service loaded: ${service.name}, ID: $serviceId")

                        // Verify the service belongs to the correct provider
                        if (service.providerId != providerId) {
                            Log.w(TAG, "Service provider mismatch: ${service.providerId} != $providerId")
                            Toast.makeText(this, "Service does not belong to this provider", Toast.LENGTH_SHORT).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        updateServiceUI()
                        loadAvailableTimeSlots() // Load slots after service is loaded
                    } else {
                        Log.e(TAG, "Failed to parse service data")
                        Toast.makeText(this, "Failed to load service details", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Log.e(TAG, "Service not found with ID: $serviceId")
                    Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading service: ${e.message}")
                Toast.makeText(this, "Failed to load service: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    private fun updateProviderUI() {
        // Set provider name
        val providerName = if (provider.businessInfo.businessName.isNotEmpty()) {
            provider.businessInfo.businessName
        } else {
            "${provider.firstName} ${provider.lastName}"
        }
        binding.tvClinicName.text = providerName
        Log.d(TAG, "Provider name: $providerName")

        // Set provider image
        if (provider.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(provider.profileImageUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.ivClinic)
        }

        // Set specialty
        binding.tvDurationCategory.text = provider.businessInfo.specialty.ifEmpty { "General Service" }
    }

    private fun updateServiceUI() {
        // Update service-specific UI if needed
        val categoryName = service.getCategoryDisplayName(language)
        binding.tvDurationCategory.text = "${service.durationMax} min • $categoryName"
        Log.d(TAG, "Service: ${service.name}, Duration: ${service.durationMax} min, Category: $categoryName")
    }

    private fun setupCalendar() {
        // Generate next 7 days
        val todayCal = Calendar.getInstance()
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

            Log.d(TAG, "Generated day: $dateString ($dayName $dayNumber)")
        }

        currentCalendarDays = days
        selectedDate = days.first().dateString

        calendarAdapter = CalendarAdapter { selectedDay ->
            currentCalendarDays = currentCalendarDays.map { day ->
                day.copy(isSelected = day.dateString == selectedDay.dateString)
            }
            calendarAdapter.submitList(currentCalendarDays)

            // Update selected date and load available slots
            selectedDate = selectedDay.dateString
            Log.d(TAG, "Selected date: $selectedDate")
            loadAvailableTimeSlots()

            // Update displayed date
            try {
                val date = dateFormat.parse(selectedDate)
                date?.let {
                    binding.tvDate.text = displayDateFormat.format(it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing date: ${e.message}")
                binding.tvDate.text = selectedDate
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

        // Set initial date display
        try {
            val date = dateFormat.parse(selectedDate)
            date?.let {
                binding.tvDate.text = displayDateFormat.format(it)
            }
        } catch (e: Exception) {
            binding.tvDate.text = selectedDate
        }
    }

    private fun setupTimeSlots() {
        timeSlotAdapter = TimeSlotAdapter { selectedSlot ->
            currentTimeSlots = currentTimeSlots.map { slot ->
                slot.copy(isSelected = slot.time == selectedSlot.time && slot.isAvailable)
            }
            timeSlotAdapter.submitList(currentTimeSlots)

            selectedSlot?.let {
                if (it.isAvailable) {
                    selectedTime = it.originalTime
                    Log.d(TAG, "Selected time: $selectedTime")
                } else {
                    selectedTime = ""
                    Toast.makeText(this, "This time slot is not available", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = timeSlotAdapter
        }
    }

    private fun loadAvailableTimeSlots() {
        showLoading(true)

        if (!this::service.isInitialized) {
            Log.d(TAG, "Service not initialized yet, waiting...")
            return
        }

        Log.d(TAG, "Loading available slots for date: $selectedDate, service: $serviceId")

        AvailabilityRepository.getAvailableSlots(
            providerId = providerId,
            date = selectedDate,
            serviceId = serviceId
        ) { availabilitySlots ->
            showLoading(false)

            Log.d(TAG, "Received ${availabilitySlots.size} slots")

            if (availabilitySlots.isEmpty()) {
                currentTimeSlots = listOf()
                timeSlotAdapter.submitList(currentTimeSlots)
                binding.tvEmptySlots.visibility = View.VISIBLE
                binding.tvEmptySlots.text = "No available slots for $selectedDate"
                return@getAvailableSlots
            }

            // Convert AvailabilitySlot to TimeSlot
            val slots = availabilitySlots.map { availabilitySlot ->
                TimeSlot(
                    time = formatTimeForDisplay(availabilitySlot.time),
                    originalTime = availabilitySlot.time,
                    isAvailable = availabilitySlot.isAvailable,
                    isSelected = false
                )
            }

            Log.d(TAG, "Converted ${slots.size} time slots")

            currentTimeSlots = slots
            timeSlotAdapter.submitList(currentTimeSlots)

            binding.tvEmptySlots.visibility = View.GONE

            // Auto-select first available slot
            val firstAvailableSlot = slots.firstOrNull { it.isAvailable }
            if (firstAvailableSlot != null) {
                currentTimeSlots = currentTimeSlots.map { slot ->
                    slot.copy(isSelected = slot.originalTime == firstAvailableSlot.originalTime)
                }
                timeSlotAdapter.submitList(currentTimeSlots)
                selectedTime = firstAvailableSlot.originalTime
                Log.d(TAG, "Auto-selected time: $selectedTime")
            } else {
                selectedTime = ""
                Toast.makeText(this, "No available slots for this date", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatTimeForDisplay(time24h: String): String {
        return try {
            val date = timeFormat.parse(time24h)
            date?.let { displayTimeFormat.format(it) } ?: time24h
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting time: $time24h - ${e.message}")
            time24h
        }
    }

    private fun setupClickListeners() {
        binding.tvContinue.setOnClickListener {
            if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
                Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedSlot = currentTimeSlots.find { it.isSelected && it.isAvailable }
            if (selectedSlot == null) {
                Toast.makeText(this, "Please select an available time slot", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Continue with date: $selectedDate, time: $selectedTime")
            navigateToConfirmAppointment()
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun navigateToConfirmAppointment() {
        println("service_title= "+service.name)
        val intent = Intent(this, ConfirmAppointmentActivity::class.java).apply {
            putExtra("provider_id", providerId)
            putExtra("provider_name", if (provider.businessInfo.businessName.isNotEmpty())
                provider.businessInfo.businessName else "${provider.firstName} ${provider.lastName}")
            putExtra("provider_image_url", provider.profileImageUrl)
            putExtra("service_id", serviceId)
            putExtra("service_name", service.name)
            putExtra("service_description", service.getDescription(language))
            putExtra("service_duration", service.durationMax)
            putExtra("service_category", service.getCategoryDisplayName(language))
            putExtra("service_price", service.priceMax)
            putExtra("service_currency", service.currency)
            putExtra("selected_date", selectedDate)
            putExtra("selected_time", selectedTime)
            putExtra("display_date", binding.tvDate.text.toString())
            putExtra("display_time", formatTimeForDisplay(selectedTime))
            putExtra("language", language)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvTimeSlots.alpha = 0.5f
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvTimeSlots.alpha = 1.0f
        }
    }
}