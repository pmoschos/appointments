package com.ai.appointments.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.appointments.adapters.ServiceListAdapter
import com.ai.appointments.databinding.ActivityServiceBinding
import com.ai.appointments.db.models.Service
import com.ai.appointments.db.utils.DatabaseHelper

class ServiceActivity : AppCompatActivity() {

    private var _binding: ActivityServiceBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ServiceListAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private var allServices = mutableListOf<Service>()
    private var filteredServices = mutableListOf<Service>()
    private var category: String = ""
    private var language: String = "en"

    private val TAG = "ServiceActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityServiceBinding.inflate(layoutInflater)
        setContentView(_binding?.root)

        databaseHelper = DatabaseHelper.getInstance()
        category = intent.getStringExtra("category") ?: "health"
        language = intent.getStringExtra("language") ?: "en"

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        loadServices()
    }

    @SuppressLint("SetTextI18n")
    private fun setupToolbar() {
        val categoryName = intent.getStringExtra("category_name") ?: "Services"
        binding.serviceTitle.text = "$categoryName Services"
    }

    private fun setupRecyclerView() {
        adapter = ServiceListAdapter(filteredServices, language) { service ->
            navigateToServiceDetails(service)
        }
        binding.rvServices.layoutManager = LinearLayoutManager(this)
        binding.rvServices.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                filterServices(query)
            }
        })
    }

    private fun loadServices() {
        showLoading(true)

        Log.d(TAG, "Loading services for category: $category")

        // Method 1: Direct query on services node
        databaseHelper.servicesRef.get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)
                Log.d(TAG, "Snapshot children count: ${snapshot.childrenCount}")

                allServices.clear()
                for (child in snapshot.children) {
                    val service = child.getValue(Service::class.java)
                    if (service != null && service.isActive && service.category == category) {
                        Log.d(TAG, "Found service: ${service.name} (${service.category})")
                        allServices.add(service)
                    }
                }

                if (allServices.isEmpty()) {
                    // Try alternative loading method
                    loadServicesAlternative()
                } else {
                    filteredServices.clear()
                    filteredServices.addAll(allServices)
                    adapter.updateList(filteredServices)
                    Log.d(TAG, "Loaded ${allServices.size} services")

                    if (allServices.isEmpty()) {
                        Toast.makeText(this, "No services available in this category", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error loading services: ${e.message}")
                Toast.makeText(this, "Failed to load services: ${e.message}", Toast.LENGTH_SHORT).show()
                // Try alternative method
                loadServicesAlternative()
            }
    }

    private fun loadServicesAlternative() {
        Log.d(TAG, "Trying alternative loading method...")

        // Method 2: Load all services and filter locally
        databaseHelper.servicesRef.get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)

                allServices.clear()
                for (child in snapshot.children) {
                    val service = child.getValue(Service::class.java)
                    if (service != null && service.isActive) {
                        // Check if service matches category
                        if (service.category.equals(category, ignoreCase = true)) {
                            allServices.add(service)
                        }
                    }
                }

                filteredServices.clear()
                filteredServices.addAll(allServices)
                adapter.updateList(filteredServices)
                Log.d(TAG, "Alternative method loaded ${allServices.size} services")

                if (allServices.isEmpty()) {
                    Toast.makeText(this,
                        "No active services found for $category. Please check database.",
                        Toast.LENGTH_LONG
                    ).show()
                    showEmptyState()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Alternative method also failed: ${e.message}")
                Toast.makeText(this,
                    "Could not connect to database. Please check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
                showEmptyState()
            }
    }

    private fun filterServices(query: String) {
        if (query.isEmpty()) {
            filteredServices.clear()
            filteredServices.addAll(allServices)
        } else {
            filteredServices.clear()
            filteredServices.addAll(allServices.filter { service ->
                service.getDisplayName(language).lowercase().contains(query) ||
                        service.getDescription(language).lowercase().contains(query) ||
                        service.getCategoryDisplayName(language).lowercase().contains(query) ||
                        service.name.lowercase().contains(query) ||
                        service.name_el.lowercase().contains(query)
            })
        }
        adapter.updateList(filteredServices)

        // Show/hide empty search state
        if (filteredServices.isEmpty() && query.isNotEmpty()) {
            showEmptySearchState(query)
        } else {
            hideEmptyState()
        }
    }

    private fun navigateToServiceDetails(service: Service) {
        val intent = Intent(this, ServiceDetailsActivity::class.java).apply {
            putExtra("service_id", service.service_id) // Store service ID if available
            putExtra("service_provider_id", service.providerId)
            putExtra("service_name", service.getDisplayName(language))
            putExtra("service_description", service.getDescription(language))
            putExtra("service_price_min", service.priceMin)
            putExtra("service_price_max", service.priceMax)
            putExtra("service_currency", service.currency)
            putExtra("service_duration_min", service.durationMin)
            putExtra("service_duration_max", service.durationMax)
            putExtra("service_category", service.category)
            putExtra("service_image_url", service.imageUrl)
            putExtra("category", category)
            putExtra("language", language)
        }
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.progressBar.visibility = View.VISIBLE
            binding.rvServices.visibility = View.GONE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.rvServices.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.rvServices.visibility = View.GONE
        binding.tvEmptyState.text = "No services available in this category"
    }

    private fun showEmptySearchState(query: String) {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.rvServices.visibility = View.GONE
        binding.tvEmptyState.text = "No results for \"$query\""
    }

    private fun hideEmptyState() {
        binding.tvEmptyState.visibility = View.GONE
        binding.rvServices.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "ServiceActivity"
    }
}