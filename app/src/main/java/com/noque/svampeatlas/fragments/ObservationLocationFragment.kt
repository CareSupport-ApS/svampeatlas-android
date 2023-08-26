package com.noque.svampeatlas.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentObservationLocationBinding
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.views.MainActivity

class ObservationLocationFragment : Fragment() {

    // Objects
    private val args:  ObservationLocationFragmentArgs by navArgs()

    // Views
    private val binding by autoClearedViewBinding(FragmentObservationLocationBinding::bind)

    private var mapFragment by autoCleared<MapFragment>() {
        it?.setListener(null)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_observation_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()

        val latLng = LatLng(args.latitude.toDouble(), args.longitude.toDouble())
        mapFragment.addLocationMarker(latLng, getString(R.string.locationAnnotation_title))
        mapFragment.setRegion(latLng, 8000)
    }

    private fun initViews() {
        mapFragment = childFragmentManager.findFragmentById(R.id.observationLocationFragment_mapFragment) as MapFragment
    }

    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.observationLocationFragmentToolbar)
        mapFragment.showStyleSelector(true)
    }
}
