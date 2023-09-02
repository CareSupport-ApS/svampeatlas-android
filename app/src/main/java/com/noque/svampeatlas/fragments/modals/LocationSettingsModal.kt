package com.noque.svampeatlas.fragments.modals

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.navGraphViewModels
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentModalLocalitySettingsBinding
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.NewObservationViewModel


class LocationSettingsModal(
) : DialogFragment(R.layout.fragment_modal_locality_settings) {

    private val binding by autoClearedViewBinding(FragmentModalLocalitySettingsBinding::bind)
    private val viewModel: NewObservationViewModel by navGraphViewModels(R.id.add_observation_nav)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupViews()
        setupViewModels()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupViews() {
        val localityLockPossible = when (viewModel.context) {
            AddObservationFragment.Context.Note, AddObservationFragment.Context.EditNote, AddObservationFragment.Context.Edit -> false
            else -> true
        }

        if (!localityLockPossible)  binding.localitySettingsFragmentLocalitySwitch.visibility = View.GONE

        binding.localitySettingsFragmentLocationSwitch.setOnCheckedChangeListener { _, newValue ->
            if (newValue && binding.localitySettingsFragmentLocalitySwitch.visibility == View.VISIBLE) binding.localitySettingsFragmentLocalitySwitch.isChecked = true
        }

        binding.localitySettingsFragmentSaveButton.setOnClickListener {
            viewModel.setLocalityLock(binding.localitySettingsFragmentLocalitySwitch.isChecked)
            viewModel.setLocationLock(binding.localitySettingsFragmentLocationSwitch.isChecked)
            dismiss()
        }

        binding.cancelButton.apply { setOnClickListener { dismiss() } }
    }

    private fun setupViewModels() {
        viewModel.locality.observe(viewLifecycleOwner) {
            binding.localitySettingsFragmentLocalitySwitch.isChecked = it?.second ?: false
        }

        viewModel.coordinateState.observe(viewLifecycleOwner) {
            binding.localitySettingsFragmentLocationSwitch.isChecked = it.item?.second ?: false
        }
    }
}