package com.noque.svampeatlas.fragments.modals

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentModalLocalitySettingsBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding


class LocationSettingsModal(
    private val lockedLocality: Boolean,
    private val lockedLocation: Boolean,
    private val allowLockingLocality: Boolean
) : DialogFragment() {

    interface Listener {
        fun lockLocalitySet(value: Boolean)
        fun lockLocationSet(value: Boolean)
    }

    private val binding by autoClearedViewBinding(FragmentModalLocalitySettingsBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_modal_locality_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViews()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupViews() {
        binding.localitySettingsFragmentLocationSwitch.isChecked = lockedLocation
        binding.localitySettingsFragmentLocalitySwitch.isChecked = lockedLocality

        if (!allowLockingLocality) binding.localitySettingsFragmentLocalitySwitch.visibility = View.GONE

        binding.localitySettingsFragmentLocationSwitch.setOnCheckedChangeListener { _, newValue ->
            if (newValue &&    binding.localitySettingsFragmentLocalitySwitch.visibility == View.VISIBLE)    binding.localitySettingsFragmentLocalitySwitch.isChecked = true
        }

        binding.localitySettingsFragmentSaveButton.setOnClickListener {
            (targetFragment as? Listener)?.lockLocalitySet(   binding.localitySettingsFragmentLocalitySwitch.isChecked)
            (targetFragment as? Listener)?.lockLocationSet( binding.localitySettingsFragmentLocationSwitch.isChecked)
            dismiss()
        }

        binding.cancelButton.apply {
            setOnClickListener {
                dismiss()
            }
        }
    }
}