package com.noque.svampeatlas.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentMapSettingsBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding

class MapSettingsFragment : DialogFragment() {
    companion object {
        const val KEY_RADIUS = "KEY_RADIUS"
        const val KEY_AGE = "KEY_AGE"
    }


    interface Listener {
        fun newSearch()
        fun radiusChanged(value: Int)
        fun ageChanged(value: Int)
        fun clearAllSet(value: Boolean)
    }

    // Objects
    private var radius: Int = 1000
    private var age = 1


    // Views
    private val binding by autoClearedViewBinding(FragmentMapSettingsBinding::bind)

    // Listeners
    private val onSeekBarChangeListener by lazy {
        object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (p0.id == R.id.mapSettingsFragment_radiusSlider) {
                    radius = p1 + 1000
                    (targetFragment as? Listener)?.radiusChanged(radius)
                    setRadiusLabel()
                } else if (p0.id == R.id.mapSettingsFragment_ageSlider) {
                    age = p1 + 1
                    (targetFragment as? Listener)?.ageChanged(age)
                    setAgeLabel()
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
            (targetFragment as? Listener)?.newSearch()
            dismiss()
        }
    }

    private val onSwitchValueChanged by lazy {
        CompoundButton.OnCheckedChangeListener { _, value ->
            (targetFragment as? Listener)?.clearAllSet(value)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        radius = requireArguments().getInt(KEY_RADIUS)
        age = requireArguments().getInt(KEY_AGE)
        return inflater.inflate(R.layout.fragment_map_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }


    private fun setupViews() {
        binding.mapSettingsFragmentCancelButton.setOnClickListener(onExitButtonPressed)
        binding.mapSettingsFragmentRadiusSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        binding.mapSettingsFragmentAgeSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        binding.mapSettingsFragmentSearchButton.setOnClickListener(onSearchButtonPressed)
        binding.mapSettingsFragmentSwitch.setOnCheckedChangeListener(onSwitchValueChanged)

        setRadiusLabel()
        setAgeLabel()

        binding.mapSettingsFragmentRadiusSlider.progress = radius - 1000
        binding.mapSettingsFragmentAgeSlider.progress = age - 1
    }

    private fun setRadiusLabel() {
        binding.mapSettingFragmentRadiusLabel.text = "${radius.toDouble() / 1000} km."
    }

    private fun setAgeLabel() {
        binding.mapSettingFragmentAgeLabel.text = getString(R.string.mapViewSettingsView_year, age)
    }
}