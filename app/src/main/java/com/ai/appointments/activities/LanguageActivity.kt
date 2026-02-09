package com.ai.appointments.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ai.appointments.R
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.children
import com.ai.appointments.databinding.ActivityLanguageBinding
import com.ai.appointments.utils.LanguageHelper

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding

    // Language codes mapping
    private val languageCodes = arrayOf("en", "el")
    private val radioButtonIds = intArrayOf(
        R.id.radio_english,
        R.id.radio_greek
    )

    private var currentLanguage: String = "en"
    private var isInitialLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupClickListeners()
        loadCurrentLanguage()
        setupSearch()

        // Set flag to false after initial setup
        isInitialLoad = false
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Set click listeners for radio buttons
        binding.radioEnglish.setOnClickListener { selectLanguage(0) }
        binding.radioGreek.setOnClickListener { selectLanguage(1) }

        // Make entire language rows clickable
        binding.languageList.children.forEachIndexed { index, view ->
            view.setOnClickListener { selectLanguage(index) }
        }
    }

    private fun setupSearch() {
        binding.searchLanguage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { filterLanguages(it.toString()) }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterLanguages(query: String) {
        val lowerQuery = query.lowercase().trim()
        binding.languageList.children.forEach { child ->
            if (child is LinearLayout) {
                val textView = child.getChildAt(1) as? TextView
                val isVisible = textView?.text?.toString()?.lowercase()?.contains(lowerQuery) == true
                child.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        }
    }

    private fun loadCurrentLanguage() {
        currentLanguage = LanguageHelper.getSavedLanguage(this)
        selectRadioButtonByLanguage(currentLanguage)
    }

    private fun selectRadioButtonByLanguage(languageCode: String) {
        val position = languageCodes.indexOf(languageCode).takeIf { it != -1 } ?: 0
        updateRadioButtonSelection(position, false) // false = don't show dialog
    }

    private fun selectLanguage(position: Int) {
        // Only show dialog if not initial load
        if (!isInitialLoad) {
            updateRadioButtonSelection(position, true) // true = show dialog
        } else {
            updateRadioButtonSelection(position, false) // false = don't show dialog
        }
    }

    private fun updateRadioButtonSelection(position: Int, showDialog: Boolean) {
        // Uncheck all radio buttons
        binding.radioEnglish.isChecked = false
        binding.radioGreek.isChecked = false

        // Check selected radio button
        val selectedRadio = findViewById<RadioButton>(radioButtonIds[position])
        selectedRadio.isChecked = true

        currentLanguage = languageCodes[position]

        // Only show dialog if requested (not during initial load)
        if (showDialog) {
            showConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.popup_start_now_layout)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
        }

        val btnStart = dialog.findViewById<AppCompatButton>(R.id.btn_start)
        btnStart.setOnClickListener {
            dialog.dismiss()
            saveLanguageAndContinue()
        }

        dialog.show()
    }

    private fun saveLanguageAndContinue() {
        // Save the selected language
        LanguageHelper.setAppLocale(this, currentLanguage)

        // Navigate to Dashboard
        startActivity(Intent(this, UserDashboard::class.java))
        finish()
    }
}