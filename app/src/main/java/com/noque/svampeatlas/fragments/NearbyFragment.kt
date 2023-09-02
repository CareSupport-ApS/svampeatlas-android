package com.noque.svampeatlas.fragments

import android.location.Location
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentNearbyBinding
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.NearbyObservationsViewModel
import com.noque.svampeatlas.views.MainActivity

class NearbyFragment : Fragment(R.layout.fragment_nearby) {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val TAG = "NearbyFragment"
    }

    // Objects
    private var locationService by autoCleared<LocationService> {
        it?.setListener(null)
    }

    private val rootConstraintSet by lazy {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_nearby)
        constraintSet
    }
    private val observationConstraintSet by lazy {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_nearby_observation)
        constraintSet
    }


    // Views
    private val binding by autoClearedViewBinding(FragmentNearbyBinding::bind)
    private var mapFragment by autoCleared<MapFragment> {
        it?.setListener(null)
    }
    // View models
    private val nearbyObservationsViewModel by navGraphViewModels<NearbyObservationsViewModel>(R.id.nearby_fragment_nav)

    private val locationServiceListener = object: LocationService.Listener {
        override fun locationRetrieved(location: Location) {
            mapFragment?.setShowMyLocation(true)
            nearbyObservationsViewModel.getObservationsNearby(LatLng(location.latitude, location.longitude))
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            mapFragment?.setError(error) {
                if (error.recoveryAction == RecoveryAction.OPENSETTINGS) {
                    openSettings()
                }
            }
        }

        override fun isLocating() {}

        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            requestPermissions(permissions, requestCode)
        }
    }

    private val mapFragmentListener by lazy {
        object: MapFragment.Listener {
            override fun onClick() { hideObservationView() }
            override fun observationSelected(observation: Observation) { showObservationView(observation) }
            override fun localitySelected(locality: Locality) {}
        }
    }

    private val observationViewOnClick by lazy {
        View.OnClickListener {
            binding.nearbyFragmentObservationView.observation?.let {
                val action = NearbyFragmentDirections.actionGlobalMushroomDetailsFragment(
                    it.id,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Context.OBSERVATION_WITH_SPECIES,
                    null,
                    null
                )

                findNavController().navigate(action)
            }
        }
    }

    private val settingsButtonOnClick  by lazy {
        View.OnClickListener {
            val action = NearbyFragmentDirections.actionNearbyFragmentToMapSettingsFragment()
            findNavController().navigate(action)
        }
    }


    private val markerOnTouchListener by lazy {
        object: View.OnTouchListener {

            var originX: Float = 0F
            var originY: Float = 0F

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                val x = motionEvent.rawX
                val y = motionEvent.rawY
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location: IntArray = IntArray(2)
                        view.getLocationInWindow(location)
                        originX = location.first().toFloat()
                        originY = location.last().toFloat() + 250

                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX =  x -originX
                        val deltaY = y - originY

                        binding.nearbyFragmentMarkerImageView.translationX = deltaX
                        binding.nearbyFragmentMarkerImageView.translationY = deltaY
                    }

                    MotionEvent.ACTION_UP -> {

                        mapFragment?.getCoordinatesFor(x, y)?.let {
                           nearbyObservationsViewModel.getObservationsNearby(it)
                       }

                        binding.nearbyFragmentMarkerImageView.translationX = 0F
                        binding.nearbyFragmentMarkerImageView.translationY = 0F
                    }
                }

                return true
            }

        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(binding.nearbyFragmentMapFragment.id) as MapFragment
        setupViews()
        setupViewModels()
        locationService = LocationService(requireContext().applicationContext).apply { setListener(locationServiceListener) }
    }


    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.nearbyFragmentToolbar)
        mapFragment.showStyleSelector = true
        mapFragment.setListener(mapFragmentListener)
        binding.nearbyFragmentObservationView?.apply {
            setOnClickListener(observationViewOnClick)
        }

        binding.nearbyFragmentSettingsButton.setOnClickListener(settingsButtonOnClick)
        binding.nearbyFragmentMarkerImageView.setOnTouchListener(markerOnTouchListener)
    }

    private fun setupViewModels() {
        nearbyObservationsViewModel.observationsState.observe(viewLifecycleOwner) {
            when (it) {
                is State.Loading -> {
                    mapFragment.setLoading()
                }

                is State.Error -> {
                    mapFragment.setError(it.error, null)
                }

                is State.Items -> {
                    mapFragment.clearCircleOverlays()
                    mapFragment.addObservationMarkers(it.items.first)
                    mapFragment.setRegion(
                        it.items.second.last().coordinate,
                        (it.items.second.last().radius * 1.1).toInt()
                    )
                    it.items.second.forEach {
                        mapFragment?.addCircleOverlay(
                            it.coordinate,
                            it.radius
                        )
                    }
                }

                is State.Empty -> {
                    locationService.start()
                }
            }
        }

        nearbyObservationsViewModel.radius.observe(viewLifecycleOwner) {
            binding.nearbyFragmentDistanceLabel.text = "${String.format("%.1f", it.toDouble() / 1000)} km."
        }

        nearbyObservationsViewModel.ageInYears.observe(viewLifecycleOwner) {
            binding.nearbyFragmentAgeLabel.text = resources.getString(R.string.mapViewSettingsView_year, it)
        }
    }


    private fun showObservationView(observation: Observation) {
        binding.nearbyFragmentObservationView?.configure(observation, true)
        TransitionManager.beginDelayedTransition(binding.nearbyFragmentRoot)
        observationConstraintSet.applyTo(binding.nearbyFragmentRoot)
    }

   private fun hideObservationView() {
        TransitionManager.beginDelayedTransition(binding.nearbyFragmentRoot)
        rootConstraintSet.applyTo(binding.nearbyFragmentRoot)
    }
}
