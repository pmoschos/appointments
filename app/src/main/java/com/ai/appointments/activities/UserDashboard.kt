package com.ai.appointments.activities

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ai.appointments.R
import com.ai.appointments.databinding.ActivityUserDashboardBinding
import com.ai.appointments.fragments.user.AppointmentFragment
import com.ai.appointments.fragments.user.HistoryFragment
import com.ai.appointments.fragments.user.HomeFragment
import com.ai.appointments.fragments.user.ProfileFragment

class UserDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupNav()
        selectNav(binding.navHome.root)
        loadFragment(HomeFragment())
    }

    private fun setupNav() {

        setupItem(binding.navHome, R.drawable.ic_home, getString(R.string.home)) {
            selectNav(it)
            loadFragment(HomeFragment())
        }

        setupItem(binding.navCalendar, R.drawable.ic_calendar, getString(R.string.appointments)) {
            selectNav(it)
            loadFragment(AppointmentFragment())
        }

        setupItem(binding.navHistory, R.drawable.ic_clock, getString(R.string.history)) {
            selectNav(it)
            loadFragment(HistoryFragment())
        }

        setupItem(binding.navProfile, R.drawable.ic_profile, getString(R.string.profile)) {
            selectNav(it)
            loadFragment(ProfileFragment())
        }
    }

    private fun setupItem(
        itemBinding: com.ai.appointments.databinding.ItemBottomNavBinding,
        iconRes: Int,
        title: String,
        onClick: (View) -> Unit
    ) {
        itemBinding.icon.setImageResource(iconRes)
        itemBinding.title.text = title
        itemBinding.root.setOnClickListener { onClick(itemBinding.root) }
    }

    private fun selectNav(selected: View) {

        val items = listOf(
            binding.navHome,
            binding.navCalendar,
            binding.navHistory,
            binding.navProfile
        )

        items.forEach {
            it.root.background = null
            it.title.visibility = View.GONE
            it.icon.setColorFilter(getColor(R.color.gray))
        }

        val selectedBinding = when (selected) {
            binding.navHome.root -> binding.navHome
            binding.navCalendar.root -> binding.navCalendar
            binding.navHistory.root -> binding.navHistory
            else -> binding.navProfile
        }

        selectedBinding.root.setBackgroundResource(R.drawable.bg_nav_selected)
        selectedBinding.title.visibility = View.VISIBLE
        selectedBinding.icon.setColorFilter(getColor(android.R.color.white))
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
