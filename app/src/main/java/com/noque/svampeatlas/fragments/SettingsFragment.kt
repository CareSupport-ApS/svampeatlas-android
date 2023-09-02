package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentSettingsBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.views.MainActivity

class SettingsFragment: Fragment(R.layout.fragment_settings) {

    private val binding by autoClearedViewBinding(FragmentSettingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.settingsFragmentToolbar)
    }
}