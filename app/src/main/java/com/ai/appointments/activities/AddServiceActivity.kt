package com.ai.appointments.activities

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ai.appointments.R
import com.ai.appointments.databinding.ActivityAddServiceBinding
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.models.ServiceCategory
import com.ai.appointments.db.utils.DatabaseHelper

import java.util.*

class AddServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddServiceBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var categoryAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseHelper = DatabaseHelper.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        // Setup category spinner
        val categories = ServiceCategory.values().map {
            if (Locale.getDefault().language == "el") it.displayName_el else it.displayName
        }
        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRotation.adapter = categoryAdapter

        // Set default duration hint based on category selection
        binding.spinnerRotation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = ServiceCategory.values()[position]

            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Back button
        binding.backButton.setOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            if (validateInputs()) {
                createService()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.etServiceName.text.toString().trim()
        val duration = binding.etServiceDuration.text.toString().trim()
        val price = binding.etServicePrice.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etServiceName.error = getString(R.string.service_name_is_required)
            return false
        }
        if (duration.isEmpty() || duration.toIntOrNull() == null) {
            binding.etServiceDuration.error = getString(R.string.valid_duration_required)
            return false
        }
        if (price.isEmpty() || price.toDoubleOrNull() == null) {
            binding.etServicePrice.error = getString(R.string.valid_price_required)
            return false
        }
        if (description.isEmpty()) {
            binding.etDescription.error = getString(R.string.description_is_required)
            return false
        }
        return true
    }

    private fun createService() {
        showLoading(true)

        val providerId = databaseHelper.getCurrentUserId()
        val categoryIndex = binding.spinnerRotation.selectedItemPosition
        val category = ServiceCategory.values()[categoryIndex]
        val language = if (Locale.getDefault().language == "el") "el" else "en"

        val service = Service(
            providerId = providerId,
            name = if (language == "en") binding.etServiceName.text.toString().trim() else "",
            description = if (language == "en") binding.etDescription.text.toString()
                .trim() else "",
            category = category.value,
            durationMin = binding.etServiceDuration.text.toString().toInt(),
            durationMax = binding.etServiceDuration.text.toString().toInt(),
            priceMin = binding.etServicePrice.text.toString().toDouble(),
            priceMax = binding.etServicePrice.text.toString().toDouble(),
            currency = "EUR",
            imageUrl = "", // TODO: Add image upload later
            isActive = true,
            createdAt = System.currentTimeMillis()
        )

        databaseHelper.createService(service, providerId) { serviceId ->
            showLoading(false)
            Toast.makeText(this,
                getString(R.string.service_created_successfully), Toast.LENGTH_SHORT).show()

            // Generate availability slots for next 7 days
            generateDefaultAvailability(providerId, serviceId)

            finish()
        }
    }

    private fun generateDefaultAvailability(providerId: String, serviceId: String) {
        val calendar = Calendar.getInstance()
        for (i in 0..6) {
            calendar.add(Calendar.DAY_OF_YEAR, if (i == 0) 0 else 1)
            val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

            // Generate slots for weekdays 9AM-6PM, weekends 10AM-2PM
            val (startTime, endTime) = if (calendar.get(Calendar.DAY_OF_WEEK) in listOf(Calendar.SATURDAY, Calendar.SUNDAY)) {
                "10:00" to "14:00"
            } else {
                "09:00" to "18:00"
            }

            databaseHelper.generateAvailabilitySlots(
                providerId = providerId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                slotDuration = 30,
                buffer = 15
            )
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnDone.isEnabled = !show
     //   binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}