package com.ai.appointments.utils


import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

object LanguageHelper {

    private const val PREF_NAME = "AppPreferences"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"

    fun setLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode).also { Locale.setDefault(it) }

        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                configuration.setLocale(locale)
                context.createConfigurationContext(configuration)
            }
            else -> {
                configuration.locale = locale
            }
        }

        // Update configuration
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Save preference
        saveLanguagePreference(context, languageCode)
    }

    fun setAppLocale(context: Context, languageCode: String) {
        setLocale(context, languageCode)

        // Restart the current activity
        val refresh = Intent(context, context.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(refresh)

        if (context is android.app.Activity) {
            context.finish()
        }
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_LANGUAGE, languageCode)
            .apply()
    }

    fun getSavedLanguage(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun updateBaseContextLocale(context: Context): Context {
        val language = getSavedLanguage(context)
        val locale = Locale(language).also { Locale.setDefault(it) }

        return context.createConfigurationContext(
            context.resources.configuration.apply {
                setLocale(locale)
            }
        )
    }
}