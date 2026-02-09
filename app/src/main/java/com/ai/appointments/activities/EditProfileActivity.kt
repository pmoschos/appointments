package com.ai.appointments.activities

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ai.appointments.R
import com.ai.appointments.databinding.ActivityEditProfileBinding
import com.ai.appointments.db.models.BusinessInfo
import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.ServiceProvider
import com.ai.appointments.db.models.WorkingHours
import com.ai.appointments.db.utils.DatabaseHelper
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage

    // User data
    private var userRole: String = ""
    private var normalUser: NormalUser? = null
    private var serviceProvider: ServiceProvider? = null
    private var selectedImageUri: Uri? = null
    private var dateOfBirthTimestamp: Long = 0

    // Date formatters
    private val displayDateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
    private val databaseDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Image picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            selectedImageUri = it
            // Show preview
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.demo_image2)
                .into(binding.profileImage)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseHelper = DatabaseHelper.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        loadUserData()
        setupListeners()
    }

    private fun setupUI() {
        // Setup date picker
        binding.etDateOBirth.setOnClickListener {
            showDatePicker()
        }

        // Setup profile image click
        binding.profileContainer.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.plusIcon.setOnClickListener {
            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        // Get user role and data from intent
        userRole = intent.getStringExtra("user_role") ?: "unknown"

        when (userRole) {
            "normal" -> {
                val userJson = intent.getStringExtra("user_data_json")
                if (!userJson.isNullOrEmpty()) {
                    normalUser = Gson().fromJson(userJson, NormalUser::class.java)
                    normalUser?.let { populateNormalUserData(it) }
                } else {
                    loadUserFromDatabase()
                }
            }
            "provider" -> {
                val providerJson = intent.getStringExtra("provider_data_json")
                if (!providerJson.isNullOrEmpty()) {
                    serviceProvider = Gson().fromJson(providerJson, ServiceProvider::class.java)
                    serviceProvider?.let { populateProviderData(it) }
                } else {
                    loadUserFromDatabase()
                }
            }
            else -> {
                // Load from database if intent data is missing
                loadUserFromDatabase()
            }
        }
    }
    private fun loadUserFromDatabase() {
        val userId = databaseHelper.getCurrentUserId()

        databaseHelper.getUserRole(userId) { roleType ->
            when (roleType) {
                com.ai.appointments.db.utils.RoleType.NORMAL_USER -> {
                    databaseHelper.normalUsersRef.child(userId).get()
                        .addOnSuccessListener { snapshot ->
                            normalUser = snapshot.getValue(NormalUser::class.java)
                            normalUser?.let { populateNormalUserData(it) }
                        }
                }
                com.ai.appointments.db.utils.RoleType.SERVICE_PROVIDER -> {
                    databaseHelper.getServiceProvider(userId) { provider ->
                        serviceProvider = provider
                        provider?.let { populateProviderData(it) }
                    }
                }
                else -> {
                    // Show default data from Firebase Auth
                    populateAuthUserData()
                }
            }
        }
    }

    private fun populateNormalUserData(user: NormalUser) {
        userRole = "normal"

        // First name
        binding.etName.setText(user.firstName)

        // Last name
        binding.etLName.setText(user.lastName)

        // Email
        binding.etEmail.setText(user.email)

        // Date of birth
        if (user.dateOfBirth > 0) {
            dateOfBirthTimestamp = user.dateOfBirth
            binding.etDateOBirth.setText(displayDateFormat.format(Date(user.dateOfBirth)))
        }

        // Profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.profileImage)
        } else {
            loadDefaultProfileImage()
        }
    }

    private fun populateProviderData(provider: ServiceProvider) {
        userRole = "provider"

        // First name
        binding.etName.setText(provider.firstName)

        // Last name
        binding.etLName.setText(provider.lastName)

        // Email
        binding.etEmail.setText(provider.email)

        // Date of birth
        if (provider.dateOfBirth > 0) {
            dateOfBirthTimestamp = provider.dateOfBirth
            binding.etDateOBirth.setText(displayDateFormat.format(Date(provider.dateOfBirth)))
        }

        // Profile image
        if (provider.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(provider.profileImageUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.profileImage)
        } else {
            loadDefaultProfileImage()
        }
    }

    private fun populateAuthUserData() {
        val currentUser = auth.currentUser

        // Name
        currentUser?.displayName?.let { displayName ->
            val parts = displayName.split(" ")
            if (parts.size >= 1) binding.etName.setText(parts[0])
            if (parts.size >= 2) binding.etLName.setText(parts[1])
        }

        // Email
        binding.etEmail.setText(currentUser?.email ?: "")

        // Profile image
        loadDefaultProfileImage()
    }

    private fun loadDefaultProfileImage() {
        val currentUser = auth.currentUser
        val photoUrl = currentUser?.photoUrl?.toString()

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.demo_image2)
                .into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(R.drawable.demo_image2)
        }
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            if (validateInputs()) {
                if (selectedImageUri != null) {
                    uploadImageAndSaveProfile()
                } else {
                    saveProfile()
                }
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Set default date to current date of birth if available
        if (dateOfBirthTimestamp > 0) {
            calendar.timeInMillis = dateOfBirthTimestamp
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                dateOfBirthTimestamp = selectedCalendar.timeInMillis
                binding.etDateOBirth.setText(displayDateFormat.format(Date(dateOfBirthTimestamp)))
            },
            year,
            month,
            day
        )

        // Set max date to today
        datePicker.datePicker.maxDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun validateInputs(): Boolean {
        val firstName = binding.etName.text.toString().trim()
        val lastName = binding.etLName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (firstName.isEmpty()) {
            binding.etName.error = getString(R.string.first_name_is_required)
            return false
        }

        if (lastName.isEmpty()) {
            binding.etLName.error = getString(R.string.last_name_is_required)
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = getString(R.string.valid_email_is_required)
            return false
        }

        return true
    }

    private fun uploadImageAndSaveProfile() {
        val userId = databaseHelper.getCurrentUserId()
        selectedImageUri?.let { uri ->
            showLoading(true)

            // Create reference to profile image in storage
            val storageRef = storage.reference
            val profileImageRef = storageRef.child("profile_images/$userId.jpg")

            // Upload image
            profileImageRef.putFile(uri)
                .addOnSuccessListener { taskSnapshot ->
                    // Get download URL
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveProfile(downloadUri.toString())
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(this, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                        saveProfile() // Save without image URL
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    saveProfile() // Save without image URL
                }
        }
    }

    private fun saveProfile(imageUrl: String? = null) {
        showLoading(true)

        val userId = databaseHelper.getCurrentUserId()
        val firstName = binding.etName.text.toString().trim()
        val lastName = binding.etLName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        // Update Firebase Auth profile
        updateFirebaseAuthProfile(firstName, lastName, imageUrl)

        // Update database based on user role
        when (userRole) {
            "normal" -> {
                updateNormalUserProfile(userId, firstName, lastName, email, imageUrl)
            }
            "provider" -> {
                updateServiceProviderProfile(userId, firstName, lastName, email, imageUrl)
            }
            else -> {
                // Determine role and create appropriate profile
                determineAndCreateProfile(userId, firstName, lastName, email, imageUrl)
            }
        }
    }

    private fun updateFirebaseAuthProfile(firstName: String, lastName: String, imageUrl: String?) {
        val currentUser = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName("$firstName $lastName")
            .apply {
                imageUrl?.let { setPhotoUri(Uri.parse(it)) }
            }
            .build()

        currentUser?.updateProfile(profileUpdates)
    }

    private fun updateNormalUserProfile(userId: String, firstName: String, lastName: String,
                                        email: String, imageUrl: String?) {
        val updatedUser = NormalUser(
            firstName = firstName,
            lastName = lastName,
            email = email,
            dateOfBirth = dateOfBirthTimestamp,
            language = normalUser?.language ?: "en",
            profileImageUrl = imageUrl ?: normalUser?.profileImageUrl ?: "",
            createdAt = normalUser?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            notificationsEnabled = normalUser?.notificationsEnabled ?: true
        )

        databaseHelper.normalUsersRef.child(userId).setValue(updatedUser.toMap())
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this,
                    getString(R.string.profile_updated_successfully), Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateServiceProviderProfile(userId: String, firstName: String, lastName: String,
                                             email: String, imageUrl: String?) {
        val updatedProvider = ServiceProvider(
            firstName = firstName,
            lastName = lastName,
            email = email,
            dateOfBirth = dateOfBirthTimestamp,
            language = serviceProvider?.language ?: "en",
            profileImageUrl = imageUrl ?: serviceProvider?.profileImageUrl ?: "",
            createdAt = serviceProvider?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            notificationsEnabled = serviceProvider?.notificationsEnabled ?: true,
            businessInfo = serviceProvider?.businessInfo ?: BusinessInfo(),
            workingHours = serviceProvider?.workingHours ?: getDefaultWorkingHours(),
            isActive = serviceProvider?.isActive ?: true
        )

        databaseHelper.providersRef.child(userId).setValue(updatedProvider.toMap())
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun determineAndCreateProfile(userId: String, firstName: String, lastName: String,
                                          email: String, imageUrl: String?) {
        databaseHelper.getUserRole(userId) { roleType ->
            when (roleType) {
                com.ai.appointments.db.utils.RoleType.NORMAL_USER -> {
                    userRole = "normal"
                    updateNormalUserProfile(userId, firstName, lastName, email, imageUrl)
                }
                com.ai.appointments.db.utils.RoleType.SERVICE_PROVIDER -> {
                    userRole = "provider"
                    updateServiceProviderProfile(userId, firstName, lastName, email, imageUrl)
                }
                else -> {
                    // User doesn't exist in database yet - create normal user by default
                    userRole = "normal"
                    createNewNormalUserProfile(userId, firstName, lastName, email, imageUrl)
                }
            }
        }
    }

    private fun createNewNormalUserProfile(userId: String, firstName: String, lastName: String,
                                           email: String, imageUrl: String?) {
        val newUser = NormalUser(
            firstName = firstName,
            lastName = lastName,
            email = email,
            dateOfBirth = dateOfBirthTimestamp,
            language = "en",
            profileImageUrl = imageUrl ?: "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            notificationsEnabled = true
        )

        databaseHelper.normalUsersRef.child(userId).setValue(newUser.toMap())
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Profile created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Failed to create profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getDefaultWorkingHours(): Map<String, WorkingHours> {
        val defaultHours = WorkingHours("09:00", "17:00")
        return mapOf(
            "monday" to defaultHours,
            "tuesday" to defaultHours,
            "wednesday" to defaultHours,
            "thursday" to defaultHours,
            "friday" to defaultHours
        )
    }

    private fun showLoading(show: Boolean) {
        binding.btnDone.isEnabled = !show
        if (show) {
       //     binding.progressBar.visibility = View.VISIBLE
        } else {
         //   binding.progressBar.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "EditProfileActivity"
    }
}