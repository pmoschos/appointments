package com.ai.appointments.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ai.appointments.utils.LanguageHelper


open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applySavedLanguage()
    }

    private fun applySavedLanguage() {
        val savedLanguage: String? = LanguageHelper.getSavedLanguage(this)
        LanguageHelper.setLocale(this, savedLanguage!!)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageHelper.updateBaseContextLocale(newBase!!))
    }

    override fun onResume() {
        super.onResume()

    }
}