package com.noque.svampeatlas.fragments

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.noque.svampeatlas.extensions.toRectanglePolygon
import com.noque.svampeatlas.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.noque.svampeatlas.databinding.FragmentMapBinding
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.*
import com.noque.svampeatlas.views.BackgroundView

data class ObservationItem(val observation: Observation) : ClusterItem {
    override fun getSnippet(): String? {
        return null
    }

    override fun getTitle(): String? {
        return null
    }

    override fun getPosition(): LatLng {
        return observation.coordinate
    }

    override fun hashCode(): Int {
        var result = observation.id
        return result
    }
}

class MapFragment : Fragment(R.layout.fragment_map), ViewTreeObserver.OnGlobalLayoutListener {

    companion object {
        const val TAG = "MapFragment"

        const val LOCALITY_TAG = "locality"
        const val LOCATION_TAG = "location"
    }

    enum class Category {
        REGULAR,
        SATELLITE,
        TOPOGRAPHY
    }


    interface Listener {
        fun onClick()
        fun observationSelected(observation: Observation)
        fun localitySelected(locality: Locality)
    }

    // Objects
    private var dispatchGroup by safeAutoCleared<DispatchGroup> {
        it?.clear()
    }
    private var listener: Listener? = null
    private var clusterManager: ClusterManager<ObservationItem>? = null

    private var markers = mutableListOf<Marker>()
    private var localities = mutableMapOf<String, Locality>()

    private var locationMarker: Marker? = null
    private var selectedMarker: Marker? = null
    private var tileOverlay: TileOverlay? = null
    private var accuracyOverlay: Circle? = null
    private var circleOverlays = mutableListOf<Circle>()

    var showStyleSelector = false

    // Views
    private val binding by autoClearedViewBinding(FragmentMapBinding::bind) {
       it?.fragmentMapMapView?.viewTreeObserver?.removeOnGlobalLayoutListener { this }
       it?.fragmentMapMapView?.onDestroy()
    }

    private var googleMap by autoCleared<GoogleMap> {
        it?.setOnCameraIdleListener(null)
        it?.setOnMarkerClickListener(null)
        it?.setOnMapClickListener(null)
        it?.clear()
    }

    // Listeners
    private val onClickListener by lazy {
        GoogleMap.OnMapClickListener {
            listener?.onClick()
        }
    }

    private val onMarkerClickListener by lazy {
        GoogleMap.OnMarkerClickListener {
            when (it.tag) {
                LOCALITY_TAG -> {
                    localities[it.id]?.let { listener?.localitySelected(it) }
                }
                LOCATION_TAG -> {
                    if (locationMarker?.title != null) it.showInfoWindow()
                }
            }

            true
        }
    }

    private val onTabChangeListener by lazy {
        object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> setType(Category.REGULAR)
                    1 -> setType(Category.SATELLITE)
                    2 -> setType(Category.TOPOGRAPHY)
                }
            }
        }
    }

    private val onClusterClickListener by lazy {ClusterManager.OnClusterClickListener<ObservationItem> {
        val builder = LatLngBounds.builder()
        it.items.forEach {
            builder.include(it.position)
        }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))
        true
    }}

    private val onClusterItemListener by lazy {
        ClusterManager.OnClusterItemClickListener<ObservationItem> {
            listener?.observationSelected(it.observation)
            false
        }
    }

    override fun onGlobalLayout() {
        if (viewLifecycleOwnerLiveData.value?.lifecycle?.currentState != Lifecycle.State.DESTROYED && binding.fragmentMapMapView?.height != 0 && binding.fragmentMapMapView?.width != 0) {
            binding.fragmentMapMapView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            dispatchGroup?.leave()
        }
    }

    private val onMapReadyCallBack by lazy {
        OnMapReadyCallback {
            googleMap = it.apply {
                uiSettings.isIndoorLevelPickerEnabled = false
                uiSettings.isMapToolbarEnabled = false
                uiSettings.isCompassEnabled = false
                setOnMarkerClickListener(onMarkerClickListener)
                setOnMapClickListener(onClickListener)
            }
            setType(Category.REGULAR)
            dispatchGroup?.leave()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dispatchGroup = DispatchGroup("MapFragment")
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        dispatchGroup?.notify { binding.fragmentMapMapView?.onStart() }
    }

    override fun onResume() {
        super.onResume()
        dispatchGroup?.notify { binding.fragmentMapMapView?.onResume() }
    }

    override fun onPause() {
        dispatchGroup?.notify { binding.fragmentMapMapView.onPause() }
        super.onPause()
    }

    override fun onStop() {
        dispatchGroup?.notify { binding.fragmentMapMapView.onStop() }
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        dispatchGroup?.notify { binding.fragmentMapMapView.onLowMemory() }
    }
    private fun setupViews() {
        dispatchGroup?.enter()
        dispatchGroup?.enter()

        if (showStyleSelector) {
            binding.mapFragmentTabLayout.visibility = View.VISIBLE
            binding.mapFragmentTabLayout.addOnTabSelectedListener(onTabChangeListener)
        }

        binding.fragmentMapBackgroundView.setLoading()
        binding.fragmentMapMapView.viewTreeObserver?.addOnGlobalLayoutListener(this)
        binding.fragmentMapMapView.onCreate(null)
        binding.fragmentMapMapView.postDelayed({
            binding.fragmentMapMapView.getMapAsync(onMapReadyCallBack)
        }, 10)
    }

    fun setType(category: Category) {
        dispatchGroup?.notify {
            tileOverlay?.remove()
            tileOverlay = null
            when (category) {
                Category.REGULAR -> googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                Category.SATELLITE -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                Category.TOPOGRAPHY -> {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NONE
                    tileOverlay = googleMap.addTileOverlay(
                        TileOverlayOptions().tileProvider(OpenStreetMapTileProvider())
                    )
                }
            }
        }
    }

    fun disableGestures() {
        dispatchGroup?.notify {
            googleMap.apply {
                uiSettings.isRotateGesturesEnabled = false
                uiSettings.isScrollGesturesEnabled = false
                uiSettings.isZoomControlsEnabled = false
                uiSettings.isMapToolbarEnabled = false
                uiSettings.setAllGesturesEnabled(false)
                setOnMarkerClickListener(null)
            }
        }
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun setLoading() {
//         Putting this in notify, as the fragment itself shows a spinner before Google map is ready.
//         By putting it in notify, this function called by the developer is not stopped in OnMapReadyCallback.
        dispatchGroup?.notify {
            binding.fragmentMapBackgroundView.reset()
            binding.fragmentMapBackgroundView.setLoading()
        }
    }

    fun stopLoading() {
        dispatchGroup?.notify {
            binding.fragmentMapBackgroundView.reset()
        }
    }

    fun setError(error: AppError, handler: ((RecoveryAction?) -> Unit)?) {
        dispatchGroup?.notify {
            binding.fragmentMapBackgroundView.reset()
            binding.fragmentMapMapView?.alpha = 0.3f
            if (error.recoveryAction != null && handler != null) {
                binding.fragmentMapBackgroundView.setErrorWithHandler(
                    error,
                    error.recoveryAction,
                    handler
                )
            } else {
                binding.fragmentMapBackgroundView.setError(error)
            }
        }
    }

    fun setPadding(leftPx: Int, topPx: Int, rightPx: Int, bottomPx: Int) {
        dispatchGroup?.notify {
            googleMap.setPadding(leftPx, topPx, rightPx, bottomPx)
        }
    }


    fun setRegion(coordinate: LatLng, radius: Int) {
        dispatchGroup?.notify {
            reset()
            val bounds = LatLngBounds.Builder().apply {
                coordinate.toRectanglePolygon(radius).forEach { include(it) }
            }.build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        }
    }

    fun setRegion(coordinate: LatLng) {
        dispatchGroup?.notify {
            reset()
            if ((googleMap.cameraPosition?.zoom ?: 0f) <= 13) {
                googleMap.moveCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.fromLatLngZoom(
                            coordinate,
                            13f
                        )
                    )
                )
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(coordinate))
            }
        }
    }

    fun setRegionToShowMarkers() {
        dispatchGroup?.notify {
            reset()
            if (markers.isNotEmpty()) {
                view?.let { view ->
                    val bounds = LatLngBounds.Builder().apply {
                        markers.forEach { include(it.position) }
                    }.build()

                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            view.width,
                            view.height,
                            80.dpToPx(context)
                        )
                    )
                }
            }
        }
    }

    fun setSelectedLocalityAnnotation(latLng: LatLng) {
        dispatchGroup?.notify {
            reset()
            when (selectedMarker?.tag) {
                LOCALITY_TAG -> {
                    val bitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                    selectedMarker?.setIcon(
                        BitmapDescriptorFactory.fromBitmap(
                            bitmap.changeColor(
                                ResourcesCompat.getColor(resources, R.color.colorSecondary, null)
                            )
                        )
                    )
                    selectedMarker = null
                }
            }

            markers.find { it.position == latLng }?.let {
                when (it.tag) {
                    LOCALITY_TAG -> {
                        selectedMarker = it
                        val bitmap =
                            BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        it.setIcon(
                            BitmapDescriptorFactory.fromBitmap(
                                bitmap.changeColor(
                                    ResourcesCompat.getColor(resources, R.color.colorGreen, null)
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun setShowMyLocation(show: Boolean) {
        dispatchGroup?.notify { googleMap.isMyLocationEnabled = show }
    }

    fun addHeatMap(coordinates: List<LatLng>) {
        dispatchGroup?.notify {
            reset()

            if (coordinates.isNotEmpty()) {
                HeatmapTileProvider.Builder()


                val provider = HeatmapTileProvider.Builder()
                    .data(coordinates)
                    .radius(25)
                    .opacity(0.8)
                    .build()

                googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireActivity(),
                        R.raw.raw_googlemap_heatmap_style
                    )
                )
                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(provider))
            }
        }
    }

    fun addLocalities(localities: List<Locality>) {
        dispatchGroup?.notify {
            reset()

            localities.forEach { locality ->
                val bitmap =
                    BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)

                val markerOptions = MarkerOptions()
                    .title(locality.name)
                    .position(locality.location)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            bitmap.changeColor(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.colorSecondary,
                                    null
                                )
                            )
                        )
                    )
                googleMap.addMarker(markerOptions)?.let {
                    it.tag = LOCALITY_TAG
                    markers.add(it)
                    this.localities[it.id] = locality
                }
            }
        }
    }


    fun addLocationMarker(location: LatLng, title: String? = null, accuracy: Double? = null) {
        dispatchGroup?.notify {
            reset()

            locationMarker?.remove()
            locationMarker = null
            accuracyOverlay?.remove()
            accuracyOverlay = null

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_location)

            val markerOptions = MarkerOptions()
                .position(location)
                .title(title)
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        bitmap.changeColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorPrimary,
                                null
                            )
                        )
                    )
                )

            googleMap.addMarker(markerOptions)?.let {
                it.tag = LOCATION_TAG
                markers.add(it)
                locationMarker = it
            }
            accuracy?.let {
                googleMap.addCircle(
                    CircleOptions()
                        .center(location)
                        .strokeWidth(1F)
                        .radius(it)
                        .zIndex(10F)
                        .fillColor(
                            ColorUtils.setAlphaComponent(
                                resources.getColor(R.color.colorGreen),
                                40
                            )
                        )
                )?.let {
                    accuracyOverlay = it
                }
            }
        }
    }

    fun addObservationMarkers(observations: List<Observation>) {
        dispatchGroup?.notify(Runnable {
            reset()

            if (clusterManager != null) {
                clusterManager?.clearItems()

            } else {
                clusterManager = ClusterManager(requireContext(), googleMap)
            }

            googleMap.setOnCameraIdleListener(clusterManager)
            googleMap.setOnMarkerClickListener(clusterManager)
            clusterManager?.setOnClusterClickListener(onClusterClickListener)
            clusterManager?.setOnClusterItemClickListener(onClusterItemListener)

            observations.forEach { observation ->
              clusterManager?.addItem(ObservationItem(observation))
            }
        })
    }

    fun addCircleOverlay(center: LatLng, radius: Int) {
        dispatchGroup?.notify {
            reset()
            googleMap.addCircle(
                CircleOptions()
                    .center(center)
                    .strokeWidth(2F)
                    .radius(radius.toDouble())
                    .zIndex(10F)
                    .fillColor(
                        ColorUtils.setAlphaComponent(
                            resources.getColor(R.color.colorPrimary),
                            30
                        )
                    )
            )?.let {
                circleOverlays.add(it)
            }
        }
    }

    fun getCoordinatesFor(x: Float, y: Float): LatLng? {
            val location = IntArray(2)
            binding.fragmentMapMapView.getLocationInWindow(location)
            return googleMap.projection?.fromScreenLocation(Point(x.toInt() - location.first(), y.toInt() - location.last()))
    }

    fun clearCircleOverlays() {
        circleOverlays.forEach {
            it.remove()
        }
    }


    fun removeAllMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }


    private fun reset() {
        binding.fragmentMapBackgroundView.reset()
        binding.fragmentMapMapView.alpha = 1f
    }

    override fun onDestroyView() {
        clusterManager?.clearItems()
        clusterManager = null

        listener = null
        locationMarker = null
        selectedMarker = null
        tileOverlay?.remove()
        tileOverlay?.clearTileCache()
        accuracyOverlay?.remove()
        accuracyOverlay = null
        tileOverlay = null

        markers.forEach { it.remove() }
        markers.clear()
        localities.clear()
        circleOverlays.forEach { it.remove() }
        circleOverlays.clear()
        super.onDestroyView()
    }
}