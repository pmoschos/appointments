package com.ai.appointments.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.ai.appointments.R
import com.ai.appointments.adapters.OnboardingAdapter
import com.ai.appointments.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var onboardingAdapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Adapter
        onboardingAdapter = OnboardingAdapter(this)
        binding.viewPager.adapter = onboardingAdapter
        binding.dotsIndicator.attachTo(binding.viewPager)

        // Initial button text
        updateButtonState(0)

        // Page change listener
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateButtonState(position)
                }
            }
        )

        // ✅ Continue / Get Started button logic
        binding.btnNext.setOnClickListener {
            val currentPage = binding.viewPager.currentItem

            if (currentPage < onboardingAdapter.itemCount - 1) {
                // 👉 Continue → next page
                binding.viewPager.currentItem = currentPage + 1
            } else {
                // 👉 Get Started → MainActivity
                finishOnboarding()
            }
        }
    }

    private fun updateButtonState(position: Int) {
        if (position == onboardingAdapter.itemCount - 1) {
            binding.btnNext.text = getString(R.string.getstarted)
        } else {
            binding.btnNext.text = getString(R.string.continues)
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_complete", true)
            .apply()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        finish()
    }
}
