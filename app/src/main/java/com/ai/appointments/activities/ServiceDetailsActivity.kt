package com.ai.appointments.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.R
import com.ai.appointments.adapters.ServiceProviderAdapter
import com.ai.appointments.databinding.ActivityServiceDetailsBinding
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.models.ServiceProvider
import com.ai.appointments.db.utils.DatabaseHelper
import com.bumptech.glide.Glide

class ServiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailsBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var service: Service
    private  var provider_id: String=""
    private var language: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityServiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper.getInstance()
        language = intent.getStringExtra("language") ?: "en"

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        loadServiceDetails()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupUI() {
        // Set service name from intent
        val serviceName = intent.getStringExtra("service_name") ?: "Service"
        binding.serviceTitle.text = serviceName
    }

    private fun loadServiceDetails() {
        val serviceId = intent.getStringExtra("service_id") ?: ""

        // Load service details from database
        databaseHelper.servicesRef.child(serviceId).get()
            .addOnSuccessListener { snapshot ->
                val loadedService = snapshot.getValue(Service::class.java)
                if (loadedService != null) {
                    service = loadedService
                    updateServiceUI()
                    loadServiceProviders()
                } else {
                    Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load service details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateServiceUI() {
        // Set service image
        if (service.imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(service.imageUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.ivServiceImage)
        }

        // Set service details
        binding.tvPrice.text = "${service.priceMax} ${service.currency}"
        binding.txtAboutDescription.text = service.getDescription(language)

        // Update duration and category
        val durationText = "${service.durationMax} min"
        val categoryText = service.getCategoryDisplayName(language)

        // You might need to update these text views if they exist in your layout
        binding.tvDuration.text = durationText
        binding.tvCategory.text = categoryText
        binding.tvServiceName.text = service.name
    }

    private fun setupRecyclerView() {
        val adapter = ServiceProviderAdapter(listOf()) { provider ->
            navigateToProviderDetails(provider)
        }

        binding.rvServicesProvider.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }

    private fun loadServiceProviders() {
        // Load providers for this service
        databaseHelper.getServiceProvider(service.providerId) { provider ->
            provider?.let {
                updateProvidersList(listOf(it))
            } ?: run {
                Toast.makeText(this, "Provider not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProvidersList(providers: List<ServiceProvider>) {
        val adapter = binding.rvServicesProvider.adapter as? ServiceProviderAdapter
        adapter?.updateList(providers)
        provider_id=providers.get(0).userId
        if (providers.isEmpty()) {
            binding.txtProviders.text = "No Providers Available"
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        binding.btnBookAppointment.setOnClickListener {
            navigateToBooking()
        }
    }

    private fun navigateToProviderDetails(provider: ServiceProvider) {
        val intent = Intent(this, ServiceProviderDetailsActivity::class.java).apply {
            putExtra("provider_id", provider.userId) // Using email as ID for now
            putExtra("service_id", service.service_id)
            putExtra("language", language)
        }
        startActivity(intent)
    }

    private fun navigateToBooking() {
        val intent = Intent(this, ServiceProviderDetailsActivity::class.java).apply {
            putExtra("provider_id",provider_id) // Using email as ID for now
            putExtra("service_id", service.service_id)
            putExtra("language", language)
        }
        startActivity(intent)
    }
}