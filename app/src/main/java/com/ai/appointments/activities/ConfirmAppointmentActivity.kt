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
import com.ai.appointments.R
import com.ai.appointments.bottomsheetdialouges.BookingConfirmedBottomSheet
import com.ai.appointments.databinding.ActivityConfirmAppointmentBinding
import com.ai.appointments.db.Repository.AppointmentRepository
import com.ai.appointments.db.models.Appointment
import com.ai.appointments.db.models.AppointmentStatus
import com.ai.appointments.db.utils.DatabaseHelper
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class ConfirmAppointmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmAppointmentBinding
    private lateinit var databaseHelper: DatabaseHelper

    private var providerId: String = ""
    private var providerName: String = ""
    private var serviceId: String = ""
    private var serviceName: String = ""
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var displayDate: String = ""
    private var displayTime: String = ""
    private var price: Double = 0.0
    private var duration: Int = 0
    private var currency: String = "$"
    private var language: String = "en"

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityConfirmAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper.getInstance()

        // Get data from intent
        getIntentData()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupClickListeners()
    }

    private fun getIntentData() {
        println("service_title"+serviceName)
        providerId = intent.getStringExtra("provider_id") ?: ""
        providerName = intent.getStringExtra("provider_name") ?: ""
        serviceId = intent.getStringExtra("service_id") ?: ""
        serviceName = intent.getStringExtra("service_name") ?: ""
        selectedDate = intent.getStringExtra("selected_date") ?: ""
        selectedTime = intent.getStringExtra("selected_time") ?: ""
        displayDate = intent.getStringExtra("display_date") ?: ""
        displayTime = intent.getStringExtra("display_time") ?: ""
        price = intent.getDoubleExtra("service_price", 0.0)
        duration = intent.getIntExtra("service_duration", 30)
        currency = intent.getStringExtra("service_currency") ?: "EUR"
        language = intent.getStringExtra("language") ?: "en"
    }

    private fun setupUI() {
        // Set title
        binding.serviceTitle.text = serviceName

        // Set service image (using provider image as fallback)
        val imageUrl = intent.getStringExtra("provider_image_url")
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.ivService)
        }

        // Set service details
        binding.tvServiceName.text = serviceName
        binding.tvClinicName.text = providerName
        binding.tvDurationValue.text = getString(R.string.minutes, duration)
        binding.tvCategoryValue.text = intent.getStringExtra("service_category") ?: ""
        binding.tvDateValue.text = displayDate
        binding.tvTimeValue.text = displayTime
        binding.tvAmountValue.text = "$price $currency"

        // Set description if available
        val description = intent.getStringExtra("service_description")
        if (!description.isNullOrEmpty()) {
            // You could add a description TextView if needed
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener {
            confirmAppointment()
        }
    }

    private fun confirmAppointment() {
        val note = binding.etNote.text.toString().trim()

        // Validate input
        if (providerId.isEmpty() || serviceId.isEmpty() || selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "Missing required information", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        // Get current user info
        val userId = databaseHelper.getCurrentUserId()
        val userEmail = databaseHelper.getCurrentUserEmail()

        // Parse date and time to timestamp
        val appointmentTimestamp = parseDateTimeToTimestamp(selectedDate, selectedTime)
        if (appointmentTimestamp == 0L) {
            showLoading(false)
            Toast.makeText(this, "Invalid date/time selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Create appointment object
        val appointment = Appointment(
            userId = userId,
            userName = userEmail, // You might want to get actual user name from database
            providerId = providerId,
            providerName = providerName,
            serviceId = serviceId,
            serviceName = serviceName,
            serviceName_el = serviceName, // Set based on language if you have Greek version
            category = intent.getStringExtra("service_category") ?: "health",
            appointmentDate = appointmentTimestamp,
            scheduledDateTime = "$selectedDate $selectedTime",
            duration = duration,
            price = price,
            currency = currency,
            status = AppointmentStatus.CONFIRMED.value,
            notes = if (language == "el") note else "",
            notes_en = if (language == "en") note else "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Create appointment in database
        AppointmentRepository.createAppointment(
            appointment = appointment,
            onSuccess = { appointmentId ->
                showLoading(false)

                // Book the slot in availability
                bookAvailabilitySlot(appointmentId)

                // Show success bottom sheet
                val bottomSheet = BookingConfirmedBottomSheet().apply {
                    setOnBookingSuccessListener {
                        // Navigate to user dashboard or appointments list
                        navigateToDashboard()
                    }
                }
                bottomSheet.show(supportFragmentManager, "BookingConfirmed")
            },
            onError = { error ->
                showLoading(false)
                Toast.makeText(this, "Failed to create appointment: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("ConfirmAppointment", "Error creating appointment", error)
            }
        )
    }

    private fun bookAvailabilitySlot(appointmentId: String) {
        // Book the selected time slot
        com.ai.appointments.db.Repository.AvailabilityRepository.bookSlot(
            providerId = providerId,
            date = selectedDate,
            time = selectedTime,
            appointmentId = appointmentId,
            onSuccess = {
                Log.d("ConfirmAppointment", "Slot booked successfully")
            }
        )
    }

    private fun parseDateTimeToTimestamp(date: String, time: String): Long {
        return try {
            val dateTimeString = "$date $time"
            val parsedDate = dateTimeFormat.parse(dateTimeString)
            parsedDate?.time ?: 0L
        } catch (e: Exception) {
            Log.e("ConfirmAppointment", "Error parsing date/time: ${e.message}")
            0L
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, UserDashboard::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.btnConfirm.isEnabled = !show
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }
}