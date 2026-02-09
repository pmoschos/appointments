package com.ai.appointments.bottomsheetdialouges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ai.appointments.adapters.ServiceCategoryAdapter
import com.ai.appointments.adapters.StatusCategoryAdapter
import com.ai.appointments.adapters.TimeSlotAdapter
import com.ai.appointments.databinding.BottomsheetHistoyrApplyFilterBinding
import com.ai.appointments.db.models.FilterSelection
import com.ai.appointments.db.models.ServiceCategory
import com.ai.appointments.db.models.ServiceCategoryItem
import com.ai.appointments.db.models.StatusSlot


import com.ai.appointments.model.TimeSlot
import java.util.*

class ApplyHistoryFilterBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetHistoyrApplyFilterBinding? = null
    private val binding get() = _binding!!

    private lateinit var timeSlotAdapter: TimeSlotAdapter
    private lateinit var serviceCategoryAdapter: ServiceCategoryAdapter
    private lateinit var statusCategoryAdapter: StatusCategoryAdapter

    private var timeSlots = mutableListOf<TimeSlot>()
    private var serviceCategories = mutableListOf<ServiceCategoryItem>()
    private var statusItems = mutableListOf<StatusSlot>()

    // Current selections
    private var selectedTimeRange: String? = "any"
    private var selectedCategory: String? = "all"
    private var selectedStatus: String? = "all"

    private var filterAppliedListener: ((FilterSelection) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetHistoyrApplyFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup cancel button
        binding.icCancel.setOnClickListener { dismiss() }

        // Initialize data
        initTimeSlots()
        initServiceCategories()
        initStatusItems()

        // Setup adapters
        setupTimeSlotsAdapter()
        setupServiceCategoriesAdapter()
        setupStatusCategoriesAdapter()

        // Setup buttons
        setupApplyButton()
        setupClearButton()
    }

    private fun initTimeSlots() {
        timeSlots = mutableListOf(
            TimeSlot(
                time = "Any Time",
                originalTime = "any", // Using originalTime as value
                isSelected = true
            ),
            TimeSlot(
                time = "Morning",
                originalTime = "morning",
                isSelected = false
            ),
            TimeSlot(
                time = "Afternoon",
                originalTime = "afternoon",
                isSelected = false
            ),
            TimeSlot(
                time = "Evening",
                originalTime = "evening",
                isSelected = false
            )
        )
    }

    private fun initServiceCategories() {
        val language = Locale.getDefault().language
        val isGreek = language == "el"

        serviceCategories = mutableListOf(
            ServiceCategoryItem(
                name = "All Categories",
                value = "all",
                isSelected = true
            )
        )

        ServiceCategory.values().forEach { category ->
            serviceCategories.add(
                ServiceCategoryItem(
                    name = if (isGreek) category.displayName_el else category.displayName,
                    value = category.value,
                    isSelected = false
                )
            )
        }
    }

    private fun initStatusItems() {
        statusItems = mutableListOf(
            StatusSlot(
                status = "All Status",
                statusValue = "all",
                isSelected = true
            ),
            StatusSlot(
                status = "Upcoming",
                statusValue = "upcoming",
                isSelected = false
            ),
            StatusSlot(
                status = "Completed",
                statusValue = "completed",
                isSelected = false
            ),
            StatusSlot(
                status = "Cancelled",
                statusValue = "cancelled",
                isSelected = false
            ),
            StatusSlot(
                status = "No Show",
                statusValue = "no_show",
                isSelected = false
            )
        )
    }

    private fun setupTimeSlotsAdapter() {
        timeSlotAdapter = TimeSlotAdapter(
            onItemClick = { selectedTimeSlot ->
                // Update selection
                timeSlots = timeSlots.map { slot ->
                    slot.copy(isSelected = slot.originalTime == selectedTimeSlot.originalTime)
                }.toMutableList()
                timeSlotAdapter.submitList(timeSlots)
                selectedTimeRange = selectedTimeSlot.originalTime
            }
        )

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = timeSlotAdapter
        }
        timeSlotAdapter.submitList(timeSlots)
    }

    private fun setupServiceCategoriesAdapter() {
        serviceCategoryAdapter = ServiceCategoryAdapter(
            onItemClick = { selectedCategoryItem ->
                // Update selection
                serviceCategories = serviceCategories.map { category ->
                    category.copy(isSelected = category.value == selectedCategoryItem.value)
                }.toMutableList()
                serviceCategoryAdapter.submitList(serviceCategories)
                selectedCategory = selectedCategoryItem.value
            }
        )

        binding.rvCategorySlots.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = serviceCategoryAdapter
        }
        serviceCategoryAdapter.submitList(serviceCategories)
    }

    private fun setupStatusCategoriesAdapter() {
        statusCategoryAdapter = StatusCategoryAdapter(
            onItemClick = { selectedStatusSlot ->
                // Update selection
                statusItems = statusItems.map { status ->
                    status.copy(isSelected = status.statusValue == selectedStatusSlot.status)
                }.toMutableList()
                statusCategoryAdapter.submitList(statusItems)
                selectedStatus = selectedStatusSlot.status
            }
        )

        binding.rvStatusSlots.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = statusCategoryAdapter
        }
        statusCategoryAdapter.submitList(statusItems)
    }

    private fun setupApplyButton() {
        binding.btnAply.setOnClickListener {
            // Create filter selection object
            val filterSelection = FilterSelection(
                timeRange = selectedTimeRange,
                category = selectedCategory,
                status = selectedStatus
            )

            // Call the listener
            filterAppliedListener?.invoke(filterSelection)

            // Dismiss the bottom sheet
            dismiss()
        }
    }

    private fun setupClearButton() {
        binding.btnClear.setOnClickListener {
            clearAllFilters()
        }
    }

    private fun clearAllFilters() {
        // Reset time slots
        timeSlots = timeSlots.map { slot ->
            slot.copy(isSelected = slot.originalTime == "any")
        }.toMutableList()
        timeSlotAdapter.submitList(timeSlots)
        selectedTimeRange = "any"

        // Reset categories
        serviceCategories = serviceCategories.map { category ->
            category.copy(isSelected = category.value == "all")
        }.toMutableList()
        serviceCategoryAdapter.submitList(serviceCategories)
        selectedCategory = "all"

        // Reset statuses
        statusItems = statusItems.map { status ->
            status.copy(isSelected = status.statusValue == "all")
        }.toMutableList()
        statusCategoryAdapter.submitList(statusItems)
        selectedStatus = "all"
    }

    fun setOnFilterAppliedListener(listener: (FilterSelection) -> Unit) {
        filterAppliedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        filterAppliedListener = null
    }
}