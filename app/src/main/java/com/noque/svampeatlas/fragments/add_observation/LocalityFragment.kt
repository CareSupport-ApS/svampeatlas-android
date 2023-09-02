package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.LocalityAdapter
import com.noque.svampeatlas.databinding.FragmentAddObservationLocalityBinding
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.fragments.AddObservationFragmentDirections
import com.noque.svampeatlas.fragments.MapFragment
import com.noque.svampeatlas.fragments.TermsFragment
import com.noque.svampeatlas.fragments.modals.LocationSettingsModal
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.models.Location
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.utilities.safeAutoCleared
import com.noque.svampeatlas.view_models.NewObservationViewModel
import java.util.Date

class LocalityFragment: Fragment(R.layout.fragment_add_observation_locality) {

    companion object {
        const val TAG = "LocalityFragment"
    }

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    // Views
    private val binding by autoClearedViewBinding(FragmentAddObservationLocalityBinding::bind) {
        it?.localityFragmentRecyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        it?.localityFragmentRecyclerView?.adapter = null
        onGlobalLayoutListener = null
    }

    private var mapFragment by safeAutoCleared<MapFragment> {
        it?.setListener(null)
    }

    // View models
    private val newObservationViewModel: NewObservationViewModel by navGraphViewModels(R.id.add_observation_nav)

    // Adapters
    private val localityAdapter by lazy {
        LocalityAdapter().apply {
            localitySelected = {
                newObservationViewModel.setLocality(it)
            }
        }
    }

    // Listeners
    private val retryButtonClicked by lazy {
        View.OnClickListener { newObservationViewModel.resetLocation() }
    }

    private val mapFragmentListener by lazy {
        object: MapFragment.Listener {
            override fun onClick() {}
            override fun observationSelected(observation: Observation) {}
            override fun localitySelected(locality: Locality) { newObservationViewModel.setLocality(locality) }
        }
    }

    private val markerOnTouchListener by lazy {
        object: View.OnTouchListener {

            var hasStarted = false
            var originX: Float = 0F
            var originY: Float = 0F

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                val x = motionEvent.rawX
                val y = motionEvent.rawY

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        originX = location.first().toFloat()
                        originY = location.last().toFloat()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if ((x - originX > 500 || y - originY > 500) || hasStarted) {
                            hasStarted = true
                            binding.localityFragmentMarkerImageView.translationX = x - (originX + ((binding.localityFragmentMarkerImageView.width.toFloat())))
                            binding.localityFragmentMarkerImageView.translationY = y - (originY + ((binding.localityFragmentMarkerImageView.height.toFloat()) * 2))
                        } else {
                            binding.localityFragmentMarkerImageView.translationX = x / 4 - originX
                            binding.localityFragmentMarkerImageView.translationY = (y - (originY + ((binding.localityFragmentMarkerImageView.height.toFloat())))) * 0.2.toFloat()
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (hasStarted) {
                            val location: IntArray = IntArray(2)
                            view.getLocationInWindow(location)
                            val finalX = location.first().toFloat()
                            val finalY = location.last().toFloat()

                            mapFragment?.getCoordinatesFor(finalX + (view.width / 2), finalY + view.height)?.let { newObservationViewModel.setCoordinateState(
                                State.Items(Location(Date(), it, 5F))
                            ) }
                        } else {
                            val bundle = Bundle()
                            bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.LOCALITYHELPER)
                            val dialog = TermsFragment()
                            dialog.arguments = bundle
                            dialog.show(childFragmentManager, null)
                        }

                        binding.localityFragmentMarkerImageView.translationX = 0F
                        binding.localityFragmentMarkerImageView.translationY = 0F
                        hasStarted = false
                    }
                }

                return true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.localityFragment_mapView) as MapFragment
        setupViews()
        setupViewModels()
    }

    private fun setupViews() {
        binding.localityFragmentRecyclerView.apply {
            onGlobalLayoutListener = object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapFragment?.setPadding(0, 0, 0, this@apply.height + this@apply.marginBottom)
                    mapFragment?.setRegionToShowMarkers()
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
            viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
            adapter = localityAdapter
            layoutManager = LinearLayoutManager(context).apply { orientation = RecyclerView.HORIZONTAL }
        }

        mapFragment?.setListener(mapFragmentListener)
        mapFragment?.setType(MapFragment.Category.REGULAR)
        binding.localityFragmentRetryButton.setOnClickListener(retryButtonClicked)
        binding.localityFragmentMarkerImageView.setOnTouchListener(markerOnTouchListener)

        binding.localityFragmentSettingsButton.setOnClickListener {
           val action = AddObservationFragmentDirections.actionAddObservationFragmentToLocationSettingsFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupViewModels() {
        newObservationViewModel.localitiesState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    localityAdapter.configure(it.items)
                    mapFragment?.addLocalities(it.items)
                }

                is State.Loading -> mapFragment?.setLoading()

                is State.Error -> {
                    mapFragment?.stopLoading()
                }

                is State.Empty -> {
                    localityAdapter.configure(emptyList())
                    mapFragment?.addLocalities(emptyList())
                }
            }
        })

        newObservationViewModel.locality.observe(viewLifecycleOwner) {

            it?.first?.let { locality ->
                binding.localityFragmentRecyclerView.scrollToPosition(
                    localityAdapter.setSelected(
                        locality,
                        it.second
                    )
                )
                mapFragment?.setSelectedLocalityAnnotation(locality.location)
            }
        }

        newObservationViewModel.coordinateState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    binding.localityFragmentLockedLocation.visibility =
                        if (it.items.second) View.VISIBLE else View.GONE
                    mapFragment?.addLocationMarker(
                        it.items.first.latLng,
                        resources.getString(R.string.locationAnnotation_title),
                        it.items.first.accuracy.toDouble()
                    )
                    mapFragment?.setRegion(it.items.first.latLng)
                    binding.localityFragmentPrecisionLabel.text = resources.getString(
                        R.string.precision,
                        it.items.first.accuracy
                    ) + ", lat: ${
                        String.format(
                            "%.2f",
                            it.items.first.latLng.latitude
                        )
                    }, lon: ${String.format("%.2f", it.items.first.latLng.longitude)}"
                }

                is State.Loading -> {
                    mapFragment?.setLoading()
                    binding.localityFragmentPrecisionLabel.text = "Finder placering..."
                }

                is State.Empty -> {
                    mapFragment?.removeAllMarkers()
                }

                is State.Error -> {
                    mapFragment?.setError(it.error) {
                        when (it) {
                            RecoveryAction.OPENSETTINGS -> openSettings()
                            RecoveryAction.TRYAGAIN -> newObservationViewModel.resetLocation()
                            else -> {}
                        }
                    }
                }
            }
        })
    }
}