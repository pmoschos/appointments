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
import com.ai.appointments.databinding.ActivityEditServiceBinding
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.models.ServiceCategory
import com.ai.appointments.db.utils.DatabaseHelper
import java.util.*

class EditServiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditServiceBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private var serviceId: String = ""
    private var isActive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        databaseHelper = DatabaseHelper.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get service data from intent
        serviceId = intent.getStringExtra("SERVICE_ID") ?: ""
        if (serviceId.isEmpty()) {
            Toast.makeText(this, "Service ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        loadServiceData()
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
                // You can add category-specific logic here if needed
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Back button
        binding.backButton.setOnClickListener { finish() }

        // Set title
        binding.serviceTitle.text = "Edit Service"
    }

    private fun loadServiceData() {
        showLoading(true)

        // Load service data from database
        databaseHelper.servicesRef.child(serviceId).get().addOnSuccessListener { snapshot ->
            val service = snapshot.getValue(Service::class.java)
            if (service != null) {
                populateForm(service)
            } else {
                Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show()
                finish()
            }
            showLoading(false)
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load service", Toast.LENGTH_SHORT).show()
            showLoading(false)
            finish()
        }
    }

    private fun populateForm(service: Service) {
        // Set service name
        val language = if (Locale.getDefault().language == "el") "el" else "en"
        binding.etServiceName.setText(service.getDisplayName(language))

        // Set category
        val category = ServiceCategory.values().find { it.value == service.category }
        category?.let {
            val position = ServiceCategory.values().indexOf(it)
            binding.spinnerRotation.setSelection(position)
        }

        // Set duration
        binding.etServiceDuration.setText(service.durationMax.toString())

        // Set price
        binding.etServicePrice.setText(service.priceMax.toString())

        // Set description
        binding.etDescription.setText(service.description)

        // Set active status
        isActive = service.isActive
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            if (validateInputs()) {
                updateService()
            }
        }

        // Add delete button listener
        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun validateInputs(): Boolean {
        val name = binding.etServiceName.text.toString().trim()
        val duration = binding.etServiceDuration.text.toString().trim()
        val price = binding.etServicePrice.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()

        if (name.isEmpty()) {
            binding.etServiceName.error = "Service name is required"
            return false
        }
        if (duration.isEmpty() || duration.toIntOrNull() == null) {
            binding.etServiceDuration.error = "Valid duration required"
            return false
        }
        if (price.isEmpty() || price.toDoubleOrNull() == null) {
            binding.etServicePrice.error = "Valid price required"
            return false
        }
        if (description.isEmpty()) {
            binding.etDescription.error = "Description is required"
            return false
        }
        return true
    }

    private fun updateService() {
        showLoading(true)

        val providerId = databaseHelper.getCurrentUserId()
        val categoryIndex = binding.spinnerRotation.selectedItemPosition
        val category = ServiceCategory.values()[categoryIndex]
        val language = if (Locale.getDefault().language == "el") "el" else "en"

        val updatedService = Service(
            service_id = serviceId,
            providerId = providerId,
            name = if (language == "en") binding.etServiceName.text.toString().trim() else "",
            description = if (language == "en") binding.etDescription.text.toString().trim() else "",
            category = category.value,
            durationMin = binding.etServiceDuration.text.toString().toInt(),
            durationMax = binding.etServiceDuration.text.toString().toInt(),
            priceMin = binding.etServicePrice.text.toString().toDouble(),
            priceMax = binding.etServicePrice.text.toString().toDouble(),
            currency = "EUR",
            imageUrl = "", // Keep existing image
            isActive = isActive,
            createdAt = System.currentTimeMillis() // Keep original creation time
        )

        databaseHelper.updateService(serviceId, updatedService) {
            showLoading(false)
            Toast.makeText(this, "Service updated successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Service")
            .setMessage("Are you sure you want to delete this service? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteService()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteService() {
        showLoading(true)
        val providerId = databaseHelper.getCurrentUserId()

        databaseHelper.deleteService(serviceId, providerId) {
            showLoading(false)
            Toast.makeText(this, "Service deleted successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.btnDone.isEnabled = !show
        binding.btnDelete.isEnabled = !show
        // Add progress bar if needed
    }
}