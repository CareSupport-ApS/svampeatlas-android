package com.noque.svampeatlas.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentMapSettingsBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.NearbyObservationsViewModel

class MapSettingsFragment : DialogFragment(R.layout.fragment_map_settings) {
    // Views
    private val binding by autoClearedViewBinding(FragmentMapSettingsBinding::bind)

    private val viewModel by navGraphViewModels<NearbyObservationsViewModel>(R.id.nearby_fragment_nav)

    // Listeners
    private val onSeekBarChangeListener by lazy {
        object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (p0.id == R.id.mapSettingsFragment_radiusSlider) {
                    val radius = p1 + 1000
                    viewModel.setRadius(radius)
                } else if (p0.id == R.id.mapSettingsFragment_ageSlider) {
                    val age = p1 + 1
                    viewModel.setAgeInYears(age)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }
    }

    private val onExitButtonPressed by lazy {
        View.OnClickListener {
            dismiss()
        }
    }

    private val onSearchButtonPressed by lazy {
        View.OnClickListener {
            viewModel.reset(binding.mapSettingsFragmentSwitch.isChecked)
            dismiss()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModel()
    }


    private fun setupViews() {
        binding.mapSettingsFragmentCancelButton.setOnClickListener(onExitButtonPressed)
        binding.mapSettingsFragmentRadiusSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        binding.mapSettingsFragmentAgeSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        binding.mapSettingsFragmentSearchButton.setOnClickListener(onSearchButtonPressed)
    }

    private fun setupViewModel() {
        viewModel.radius.observe(viewLifecycleOwner) {
            binding.mapSettingsFragmentRadiusSlider.progress = it - 1000
            binding.mapSettingFragmentRadiusLabel.text = "${it.toDouble() / 1000} km."
        }

        viewModel.ageInYears.observe(viewLifecycleOwner) {
            binding.mapSettingFragmentAgeLabel.text =
                getString(R.string.mapViewSettingsView_year, it)
            binding.mapSettingsFragmentAgeSlider.progress = it - 1
        }
    }
}