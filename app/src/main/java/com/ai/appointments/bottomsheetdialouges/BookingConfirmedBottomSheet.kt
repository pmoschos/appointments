package com.ai.appointments.bottomsheetdialouges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ai.appointments.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ai.appointments.databinding.BottomsheetBookingConfirmedBinding

class BookingConfirmedBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetBookingConfirmedBinding? = null
    private val binding get() = _binding!!

    private var bookingSuccessListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetBookingConfirmedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the success message
        binding.tvTitle.text = getString(R.string.appointment_booked_successfully)
        binding.tvSubtitle.text =
            getString(R.string.your_appointment_has_been_scheduled_you_will_receive_a_confirmation_email_shortly)

        binding.btnDone.setOnClickListener {
            dismiss()
            bookingSuccessListener?.invoke()
        }
    }

    fun setOnBookingSuccessListener(listener: () -> Unit) {
        bookingSuccessListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        bookingSuccessListener = null
    }
}