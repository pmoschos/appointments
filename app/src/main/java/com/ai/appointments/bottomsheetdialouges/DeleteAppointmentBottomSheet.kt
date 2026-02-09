package com.ai.appointments.bottomsheetdialouges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ai.appointments.databinding.BottomsheetDeleteAppointmentBinding

class DeleteAppointmentBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetDeleteAppointmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetDeleteAppointmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()

        }
        binding.btnConfirm.setOnClickListener {
            val bottomSheet = BookingConfirmedBottomSheet()
            bottomSheet.show(childFragmentManager, "BookingConfirmed")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}