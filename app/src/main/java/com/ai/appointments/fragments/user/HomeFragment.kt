package com.ai.appointments.fragments.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.ai.appointments.R
import com.ai.appointments.activities.ServiceActivity
import com.ai.appointments.adapters.ServiceAdapter
import com.ai.appointments.databinding.FragmentHomeBinding
import com.ai.appointments.db.Repository.UserRepository
import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.ServiceCategory
import com.ai.appointments.model.ServiceItem
import com.bumptech.glide.Glide
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        setupRecycler()
    }

    private fun loadUserData() {
        // Load user data from Firebase
        UserRepository.getNormalUser { user ->
            if (user != null) {
                updateUserUI(user)
            } else {
                // User not found or error, show default
                showDefaultUserUI()
            }
        }
    }

    private fun updateUserUI(user: NormalUser) {
        // Update profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.demo_image)
                .into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.demo_image)
        }

        // Update name
        val fullName = "${user.firstName} ${user.lastName}".trim()
        if (fullName.isNotEmpty()) {
            binding.tvName.text = getString(R.string.hi, fullName)
        } else {
            binding.tvName.text = getString(R.string.hi_there)
        }

        // Update greeting based on time of day
        updateGreeting()
    }

    private fun showDefaultUserUI() {
        // Show default values if user data is not available
        binding.ivProfile.setImageResource(R.drawable.demo_image)
        binding.tvName.text = getString(R.string.hi_there)
        updateGreeting()
    }

    private fun updateGreeting() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when (hour) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            in 17..20 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }

        binding.tvGreeting.text = greeting
    }

    private fun setupRecycler() {
        // Get current language
        val currentLanguage = Locale.getDefault().language

        // Create list from ServiceCategory enum
        val serviceCategories = ServiceCategory.values()
        val list = serviceCategories.map { category ->
            ServiceItem(
                icon = getIconForCategory(category),
                title = if (currentLanguage == "el") category.displayName_el else category.displayName,
                subtitle = if (currentLanguage == "el") "Υπηρεσίες" else "Services"
            )
        }

        binding.rvServices.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ServiceAdapter(list) { item ->
                handleServiceClick(item, currentLanguage)
            }
        }
    }

    private fun getIconForCategory(category: ServiceCategory): Int {
        return when (category) {
            ServiceCategory.HEALTH -> R.drawable.ic_health
            ServiceCategory.WELLNESS -> R.drawable.ic_wellness
            ServiceCategory.TECHNICAL -> R.drawable.ic_technical
            ServiceCategory.EDUCATIONAL -> R.drawable.ic_education
            ServiceCategory.AUTO -> R.drawable.ic_auto
        }
    }

    private fun handleServiceClick(item: ServiceItem, language: String) {
        // Find the corresponding ServiceCategory
        val category = ServiceCategory.values().find {
            (language == "el" && it.displayName_el == item.title) ||
                    (language != "el" && it.displayName == item.title)
        } ?: ServiceCategory.HEALTH

        val intent = Intent(requireContext(), ServiceActivity::class.java).apply {
            putExtra("category", category.value)
            putExtra("category_name", item.title)
            putExtra("language", language)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when fragment resumes
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}