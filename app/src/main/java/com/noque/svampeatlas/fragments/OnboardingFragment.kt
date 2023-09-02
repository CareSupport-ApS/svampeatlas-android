package com.noque.svampeatlas.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentOnboardingBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding

class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private val binding by autoClearedViewBinding(FragmentOnboardingBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.onboardingFragmentSpinnerView.startLoading()
    }
}
