package com.ai.appointments.bottomsheetdialouges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ai.appointments.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ai.appointments.databinding.BottomsheetLogoutBinding
import com.ai.appointments.databinding.BottomsheetLogoutSuccessBinding

class LogoutBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetLogoutBinding? = null
    private val binding get() = _binding!!

    // Callback for logout success
    private var logoutSuccessListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetLogoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            // Hide current bottom sheet
            dismiss()

            // Show success message
            showLogoutSuccessBottomSheet()
        }
    }

    private fun showLogoutSuccessBottomSheet() {
        val successSheet = LogoutSuccessBottomSheet().apply {
            setOnLogoutSuccessListener {
                // Call the callback when user clicks "Done" on success sheet
                logoutSuccessListener?.invoke()
            }
        }
        successSheet.show(parentFragmentManager, "LogoutSuccess")
    }

    // Method to set logout success listener
    fun setOnLogoutSuccessListener(listener: () -> Unit) {
        logoutSuccessListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        logoutSuccessListener = null
    }
}
class LogoutSuccessBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetLogoutSuccessBinding? = null
    private val binding get() = _binding!!

    private var logoutSuccessListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetLogoutSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the success message
        binding.tvTitle.text = getString(R.string.log_out_successfully)
        binding.tvSubtitle.text = getString(R.string.you_have_logged_out_from_app_successfully)

        binding.btnDone.setOnClickListener {
            dismiss()

            // Call the success listener
            logoutSuccessListener?.invoke()
        }
    }

    // Method to set logout success listener
    fun setOnLogoutSuccessListener(listener: () -> Unit) {
        logoutSuccessListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        logoutSuccessListener = null
    }
}