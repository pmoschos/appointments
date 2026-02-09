package com.ai.appointments.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.ai.appointments.R
import com.ai.appointments.databinding.ActivityLoginBinding
import com.ai.appointments.db.models.BusinessInfo
import com.ai.appointments.db.models.NormalUser
import com.ai.appointments.db.models.ServiceProvider
import com.ai.appointments.db.models.WorkingHours
import com.ai.appointments.db.utils.DatabaseHelper
import com.ai.appointments.db.utils.RoleType

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var databaseHelper: DatabaseHelper

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)
        databaseHelper = DatabaseHelper.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.tvTermsCondition.setOnClickListener {
            startActivity(Intent(this, TermsAndConditionsActivity::class.java))
        }
        binding.btnContinue.setOnClickListener {
            lifecycleScope.launch {
                startGoogleLogin()
            }
        }

        // Setup Google Login
        binding.lvGoogle.setOnClickListener {
            lifecycleScope.launch {
                startGoogleLogin()
            }
        }

        // Setup Facebook Login (if you have Facebook SDK integrated)
        binding.lvFacebook.setOnClickListener {
            // TODO: Implement Facebook login flow
            Toast.makeText(this, "Facebook login coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
        }
    }

    // ==============================
    // Google Login with Credential Manager
    // ==============================
    private suspend fun startGoogleLogin() {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = this
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } else {
                showError("Google credential not received")
                Log.w(TAG, "Credential is not Google ID Token")
            }

        } catch (e: GetCredentialException) {
            if (e.message?.contains("cancelled") == true) {
                Log.i(TAG, "Google Sign-In cancelled by user")
            } else {
                showError("Google Sign-In failed")
                Log.e(TAG, "Google Sign-In error: ${e.message}", e)
            }
        } catch (e: Exception) {
            showError("Authentication error")
            Log.e(TAG, "Unexpected error during Google Sign-In", e)
        }
    }

    // ==============================
    // Firebase Authentication
    // ==============================
    private fun firebaseAuthWithGoogle(idToken: String) {
        showLoading(true)

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, getString(R.string.google_login_successful_for_user, user?.email))
                    Toast.makeText(this,
                        getString(R.string.welcome_back, user?.displayName ?: user?.email), Toast.LENGTH_SHORT).show()

                    // Check if user exists in our database
                    user?.uid?.let { uid ->
                        checkExistingUser(uid)
                    }
                } else {
                    showError(getString(R.string.authentication_failed, task.exception?.message))
                    Log.e(TAG, "Firebase signIn failed", task.exception)
                }
            }
    }

    // ==============================
    // Check Existing User in Database
    // ==============================
    private fun checkExistingUser(uid: String) {
        databaseHelper.getUserRole(uid) { roleType ->
            when (roleType) {
                RoleType.NORMAL_USER -> {
                    // Navigate directly to user dashboard
                    startActivity(Intent(this, UserDashboard::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                RoleType.SERVICE_PROVIDER -> {
                    // Navigate directly to provider dashboard
                    startActivity(Intent(this, ProviderDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                RoleType.UNKNOWN -> {
                    // New user - show account type selection
                    showAccountTypeSelection(uid)
                }
            }
        }
    }

    // ==============================
    // Account Type Selection Dialog
    // ==============================
    private fun showAccountTypeSelection(uid: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.popup_account_type_layout)
        dialog.setCancelable(false)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        val btnUser = dialog.findViewById<AppCompatButton>(R.id.btn_user)
        val btnProvider = dialog.findViewById<AppCompatButton>(R.id.btn_provider)
        val btnCancel = dialog.findViewById<View>(R.id.btn_cancel) // Optional cancel button

        btnUser.setOnClickListener {
            createUserProfile(uid, RoleType.NORMAL_USER)
            dialog.dismiss()
        }

        btnProvider.setOnClickListener {
            createUserProfile(uid, RoleType.SERVICE_PROVIDER)
            dialog.dismiss()
        }

        btnCancel?.setOnClickListener {
            auth.signOut()
            dialog.dismiss()
        }

        dialog.show()
    }

    // ==============================
    // Create User Profile in Database
    // ==============================
    private fun createUserProfile(uid: String, roleType: RoleType) {
        val currentUser = auth.currentUser ?: return

        val baseProfile = when (roleType) {
            RoleType.NORMAL_USER -> {
                val normalUser = NormalUser(
                    firstName = currentUser.displayName?.split(" ")?.getOrNull(0) ?: "",
                    lastName = currentUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                    email = currentUser.email ?: "",
                    dateOfBirth = 0, // To be completed in profile screen
                    language = getDefaultLanguage(),
                    profileImageUrl = currentUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    notificationsEnabled = true
                )
                databaseHelper.saveNormalUser(uid, normalUser)
                Log.d(TAG, "Created normal user profile for UID: $uid")
                Intent(this, UserDashboard::class.java)
            }
            RoleType.SERVICE_PROVIDER -> {
                val serviceProvider = ServiceProvider(
                    firstName = currentUser.displayName?.split(" ")?.getOrNull(0) ?: "",
                    lastName = currentUser.displayName?.split(" ")?.getOrNull(1) ?: "",
                    email = currentUser.email ?: "",
                    dateOfBirth = 0,
                    language = getDefaultLanguage(),
                    profileImageUrl = currentUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    notificationsEnabled = true,
                    businessInfo = BusinessInfo(),
                    workingHours = getDefaultWorkingHours(),
                    isActive = true
                )
                databaseHelper.saveServiceProvider(uid, serviceProvider)
                Log.d(TAG, "Created service provider profile for UID: $uid")
                Intent(this, ProviderDashboardActivity::class.java)
            }
            else -> return
        }

        // Navigate to respective dashboard
        startActivity(baseProfile.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }

    // ==============================
    // Helper Methods
    // ==============================
    private fun checkUserRoleAndNavigate(uid: String) {
        showLoading(true)
        databaseHelper.getUserRole(uid) { roleType ->
            showLoading(false)
            when (roleType) {
                RoleType.NORMAL_USER -> {
                    startActivity(Intent(this, UserDashboard::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                RoleType.SERVICE_PROVIDER -> {
                    startActivity(Intent(this, ProviderDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                RoleType.UNKNOWN -> {
                    // User authenticated but no profile in DB - show selection
                    showAccountTypeSelection(uid)
                }
            }
        }
    }

    private fun getDefaultLanguage(): String {
        val locale = Locale.getDefault().language
        return if (locale == "el") "el" else "en"
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
   //     binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
  //    binding.loginContainer.alpha = if (show) 0.5f else 1.0f
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // ==============================
    // Facebook Login (Skeleton - Implement with Facebook SDK)
    // ==============================
    /*
    private fun startFacebookLogin() {
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(this, listOf("email", "public_profile"))

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                val accessToken = loginResult.accessToken
                firebaseAuthWithFacebook(accessToken.token)
            }

            override fun onCancel() {
                Log.i(TAG, "Facebook login cancelled")
            }

            override fun onError(error: FacebookException) {
                showError("Facebook login failed")
                Log.e(TAG, "Facebook login error", error)
            }
        })
    }

    private fun firebaseAuthWithFacebook(accessToken: String) {
        val credential = FacebookAuthProvider.getCredential(accessToken)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.uid?.let { checkExistingUser(it) }
                } else {
                    showError("Facebook authentication failed")
                }
            }
    }
    */
}