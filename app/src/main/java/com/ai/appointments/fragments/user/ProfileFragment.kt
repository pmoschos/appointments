package com.ai.appointments.fragments.user

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ai.appointments.R
import com.ai.appointments.activities.EditProfileActivity
import com.ai.appointments.activities.LanguageActivity
import com.ai.appointments.activities.LoginActivity
import com.ai.appointments.activities.TermsAndConditionsActivity
import com.ai.appointments.databinding.FragmentProfileBinding
import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.ServiceProvider
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.db.utils.RoleType
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth

    // User data
    private var currentUserId: String = ""
    private var userRole: RoleType = RoleType.UNKNOWN
    private var normalUser: NormalUser? = null
    private var serviceProvider: ServiceProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize
        databaseHelper = DatabaseHelper.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = databaseHelper.getCurrentUserId()

        // Setup click listeners first
        setupClickListeners()

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        showLoading(true)

        // First, determine user role
        databaseHelper.getUserRole(currentUserId) { roleType ->
            userRole = roleType

            when (userRole) {
                RoleType.NORMAL_USER -> {
                    loadNormalUserData()
                }
                RoleType.SERVICE_PROVIDER -> {
                    loadServiceProviderData()
                }
                RoleType.UNKNOWN -> {
                    // Should not happen, but handle gracefully
                    showLoading(false)
                    binding.txtName.text = getString(R.string.user)
                    binding.txtRole.text = auth.currentUser?.email ?: ""
                    loadDefaultProfileImage()
                }
            }
        }
    }

    private fun loadNormalUserData() {
        databaseHelper.normalUsersRef.child(currentUserId).get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)

                normalUser = snapshot.getValue(NormalUser::class.java)
                normalUser?.let { user ->
                    updateUIWithUserData(user)
                } ?: run {
                    // User data not found, show default
                    updateUIWithAuthUser()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                updateUIWithAuthUser()
            }
    }

    private fun loadServiceProviderData() {
        databaseHelper.getServiceProvider(currentUserId) { provider ->
            showLoading(false)

            serviceProvider = provider
            provider?.let { serviceProvider ->
                updateUIWithProviderData(serviceProvider)
            } ?: run {
                // Provider data not found, show default
                updateUIWithAuthUser()
            }
        }
    }

    private fun updateUIWithUserData(user: NormalUser) {
        // Display name
        val fullName = if (user.firstName.isNotEmpty() || user.lastName.isNotEmpty()) {
            "${user.firstName} ${user.lastName}".trim()
        } else {
            auth.currentUser?.displayName ?: getString(R.string.user)
        }
        binding.txtName.text = fullName

        // Email
        binding.txtRole.text = user.email.ifEmpty {
            auth.currentUser?.email ?: ""
        }

        // Profile image
        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.demo_image2)
                .error(R.drawable.demo_image2)
                .into(binding.imgProfile)
        } else {
            loadDefaultProfileImage()
        }
    }

    private fun updateUIWithProviderData(provider: ServiceProvider) {
        // Display name
        val fullName = if (provider.firstName.isNotEmpty() || provider.lastName.isNotEmpty()) {
            "${provider.firstName} ${provider.lastName}".trim()
        } else {
            auth.currentUser?.displayName ?: getString(R.string.user)
        }
        binding.txtName.text = fullName

        // Email
        binding.txtRole.text = provider.email.ifEmpty {
            auth.currentUser?.email ?: ""
        }

        // Profile image
        if (provider.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(provider.profileImageUrl)
                .placeholder(R.drawable.demo_image2)
                .error(R.drawable.demo_image2)
                .into(binding.imgProfile)
        } else {
            loadDefaultProfileImage()
        }

        // Update role text to indicate provider
        val roleText = if (provider.businessInfo.businessName.isNotEmpty()) {
            "${provider.email} • ${provider.businessInfo.businessName}"
        } else {
            "${provider.email} • ${getString(R.string.service_provider)}"
        }
        binding.txtRole.text = roleText
    }

    private fun updateUIWithAuthUser() {
        val currentUser = auth.currentUser
        binding.txtName.text = currentUser?.displayName ?: getString(R.string.user)
        binding.txtRole.text = currentUser?.email ?: ""
        loadDefaultProfileImage()
    }

    private fun loadDefaultProfileImage() {
        val currentUser = auth.currentUser
        val photoUrl = currentUser?.photoUrl?.toString()

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.demo_image2)
                .error(R.drawable.demo_image2)
                .into(binding.imgProfile)
        } else {
            binding.imgProfile.setImageResource(R.drawable.demo_image2)
        }
    }

    private fun setupClickListeners() {
        // Edit profile click
        binding.btnEdit.setOnClickListener {
            navigateToEditProfile()
        }

        // Logout click
        binding.lvLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Other settings clicks
        binding.lvNotifications.setOnClickListener {
            navigateToTermsAndConditions(getString(R.string.notifications_settings))
        }

        binding.lvLinkedWorkshop.setOnClickListener {
            navigateToLanguageSettings()
        }

        binding.termsConditions.setOnClickListener {
            navigateToTermsAndConditions(getString(R.string.terms_and_conditions))
        }

        binding.privacyPolicy.setOnClickListener {
            navigateToTermsAndConditions(getString(R.string.privacy_policy))
        }

        // Profile image click (optional - could open image viewer)
        binding.imgProfile.setOnClickListener {
            // Optional: Open full screen image or allow changing profile picture
        }
    }


    private fun navigateToEditProfile() {
        val intent = Intent(requireContext(), EditProfileActivity::class.java).apply {
            // Pass user data to edit activity using Gson for serialization
            when (userRole) {
                RoleType.NORMAL_USER -> {
                    putExtra("user_role", "normal")
                    normalUser?.let { user ->
                        val userJson = Gson().toJson(user)
                        putExtra("user_data_json", userJson)
                    }
                }
                RoleType.SERVICE_PROVIDER -> {
                    putExtra("user_role", "provider")
                    serviceProvider?.let { provider ->
                        val providerJson = Gson().toJson(provider)
                        putExtra("provider_data_json", providerJson)
                    }
                }
                else -> {
                    putExtra("user_role", "unknown")
                }
            }
        }
        startActivity(intent)
    }

    private fun navigateToTermsAndConditions(title: String) {
        val intent = Intent(requireContext(), TermsAndConditionsActivity::class.java).apply {
            putExtra("document_type", title)
        }
        startActivity(intent)
    }

    private fun navigateToLanguageSettings() {
        val intent = Intent(requireContext(), LanguageActivity::class.java)
        startActivity(intent)
    }

    private fun showLogoutConfirmation() {
        performLogout()
//        val logoutSheet = LogoutBottomSheet().apply {
//            setOnLogoutSuccessListener {
//                // Handle logout success
//                performLogout()
//            }
//        }
//        logoutSheet.show(childFragmentManager, "LogoutConfirmation")
    }

    private fun performLogout() {
        showLoading(true)

        try {
            // Sign out from Firebase
            auth.signOut()

            // Navigate to login screen
            navigateToLogin()

        } catch (e: Exception) {
            showLoading(false)
            // Even if there's an error, still navigate to login
            navigateToLogin()
        }
    }

    // Called from LogoutSuccessBottomSheet after successful logout
    fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showLoading(show: Boolean) {
        if (show) {
         //   binding.progressBar.visibility = View.VISIBLE
            binding.cvProfile.alpha = 0.5f
        } else {
           // binding.progressBar.visibility = View.GONE
            binding.cvProfile.alpha = 1.0f
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh user data when fragment resumes (e.g., after editing profile)
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }
}