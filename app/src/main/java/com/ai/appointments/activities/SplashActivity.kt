package com.ai.appointments.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ai.appointments.R

class SplashActivity : BaseActivity() {

    private  val SPLASH_DELAY: Int = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Handler().postDelayed(Runnable {
            val completed = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("onboarding_complete", false)
            val intent: Intent?

            if (completed) {
                // Go directly to main screen
                intent = Intent(this@SplashActivity, LoginActivity::class.java)
            } else {
                // Show onboarding only first time
                intent = Intent(this@SplashActivity, OnboardingActivity::class.java)
            }

            startActivity(intent)
            finish()
        }, SPLASH_DELAY.toLong())
    }
}