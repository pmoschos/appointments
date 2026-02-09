package com.ai.appointments.adapters


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ai.appointments.fragments.intro.OnboardingFragment1
import com.ai.appointments.fragments.intro.OnboardingFragment2

class OnboardingAdapter(
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingFragment1()
            1 -> OnboardingFragment2()

            else -> OnboardingFragment1()
        }
    }

    override fun getItemCount(): Int = 2
}
