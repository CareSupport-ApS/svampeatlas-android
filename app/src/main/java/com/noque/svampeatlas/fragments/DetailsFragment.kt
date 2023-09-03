package com.noque.svampeatlas.fragments


import android.animation.TimeInterpolator
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout.TITLE_COLLAPSE_MODE_FADE
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.CommentsAdapter
import com.noque.svampeatlas.adapters.ObservationsAdapter
import com.noque.svampeatlas.databinding.FragmentDetailsBinding
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.ToastHelper.handleError
import com.noque.svampeatlas.utilities.api.Geometry
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.*
import com.noque.svampeatlas.view_models.factories.ObservationsViewModelFactory
import com.noque.svampeatlas.view_models.factories.SpeciesViewModelFactory
import com.noque.svampeatlas.views.*
import kotlin.math.abs


class DetailsFragment : Fragment(R.layout.fragment_details), AppBarLayout.OnOffsetChangedListener {

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        const val TAG = "DetailsFragment"
        const val KEY_HAS_EXPANDED = "KEY_HAS_EXPANDED"
    }

    enum class TakesSelection {
        NO,
        SELECT,
        DESELECT
    }

    enum class Context {
        SPECIES,
        OBSERVATION,
        OBSERVATION_WITH_SPECIES
    }

    // Objects
    private var title: CharSequence = ""
    private var hasExpanded = false

    private val args: DetailsFragmentArgs by navArgs()

    private val locationListener = object: LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            mapFragment.setError(LocationService.Error.PermissionsUndetermined(resources)) {
                if (it == RecoveryAction.ACTIVATE) requestPermissions(permissions, requestCode)
            }
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            mapFragment.setError(error) {
                if (it == RecoveryAction.OPENSETTINGS) openSettings()
                else if (it == RecoveryAction.TRYAGAIN) locationService.start()
            }
        }

        override fun isLocating() {
            mapFragment.setLoading()
        }

        override fun locationRetrieved(location: Location) {
            val geometry = Geometry(
                LatLng(location.latitude, location.longitude),
                35000,
                Geometry.Type.RECTANGLE
            )

            when (args.context) {
                Context.SPECIES -> {
                    speciesViewModel.getHeatMapObservations(geometry)
                    mapFragment.setRegion(geometry.coordinate, geometry.radius)
                }
                else -> {}
            }
        }
    }

    private var locationService by autoCleared<LocationService> {
        it?.setListener(null)
    }

    // Views
    private val binding by autoClearedViewBinding(FragmentDetailsBinding::bind) {
        it?.detailsFragmentRecyclerView?.adapter = null
        it?.detailsFragmentAppBarLayout?.removeOnOffsetChangedListener(this)
    }

    private var mapFragment by autoCleared<MapFragment> {
        it?.setListener(null)
    }

    // View models
    private val speciesViewModel by viewModels<SpeciesViewModel> { SpeciesViewModelFactory(args.id, requireActivity().application) }

    private val observationViewModel: ObservationViewModel by viewModels { ObservationsViewModelFactory(
            args.id,
            args.context == Context.OBSERVATION_WITH_SPECIES,
            requireActivity().application
        )}

    // Adapters
    private val commentsAdapter: CommentsAdapter by lazy {
        val adapter = CommentsAdapter()

        adapter.setListener(object: CommentsAdapter.Listener {
            override fun sendComment(comment: String) =
                Session.uploadComment(observationViewModel.id, comment)
        })
        adapter
    }

    private val mushroomViewListener by lazy {
        object : MushroomView.Listener {
            override fun onClicked(mushroom: Mushroom) {
                val action = DetailsFragmentDirections.actionGlobalMushroomDetailsFragment(
                    mushroom.id,
                    TakesSelection.NO, Context.SPECIES,
                    null,
                    null
                )
                findNavController().navigate(action)
            }
        }
    }

    private val observationsAdapter by lazy {
        val adapter = ObservationsAdapter()

        adapter.observationClicked = {
            val action = DetailsFragmentDirections.actionGlobalMushroomDetailsFragment(
                it.id,
                TakesSelection.NO,
                Context.OBSERVATION,
                null,
                null
            )

            findNavController().navigate(action)
        }

        adapter
    }


    // Listeners
    private val mapFragmentListener by lazy {
        object : MapFragment.Listener {
            override fun observationSelected(observation: Observation) {}
            override fun localitySelected(locality: Locality) {}

            override fun onClick() {
                when (args.context) {
                    Context.SPECIES -> {
                    }
                    Context.OBSERVATION, Context.OBSERVATION_WITH_SPECIES -> {
                        (observationViewModel.observationState.value as? State.Items)?.items?.let {
                            val action =
                                DetailsFragmentDirections.actionMushroomDetailsFragmentToObservationLocationFragment(
                                    it.coordinate.latitude.toFloat(),
                                    it.coordinate.longitude.toFloat()
                                )
                            findNavController().navigate(action)
                        }
                    }
                }
            }

        }
    }

    private val observationHeaderViewListener = object: ObservationHeaderView.Listener {
        override fun menuButtonPressed(view: View) {
            PopupMenu(requireContext(), view).apply {
                inflate(R.menu.details_fragment_menu)
                val user = Session.user.value
                if (user != null) {
                    if (observationViewModel.observationState.value?.item?.isEditable(user) == true) {
                        menu.findItem(R.id.menu_detailsFragment_report).isVisible = false
                        menu.findItem(R.id.menu_detailsFragment_edit).isEnabled = true
                        menu.findItem(R.id.menu_detailsFragment_delete).isEnabled = observationViewModel.observationState.value?.item?.isDeleteable(user) ?: false
                    } else {
                        menu.findItem(R.id.menu_detailsFragment_report).isVisible = true
                        menu.findItem(R.id.menu_detailsFragment_edit).isVisible = false
                        menu.findItem(R.id.menu_detailsFragment_delete).isVisible = false
                    }
                } else {
                    menu.findItem(R.id.menu_detailsFragment_delete).isVisible = false
                    menu.findItem(R.id.menu_detailsFragment_edit).isVisible = false
                }

                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_detailsFragment_report -> {
                            this@DetailsFragment.reportOffensiveContent()
                        }
                        R.id.menu_detailsFragment_edit -> {
                            observationViewModel.observationState.value?.item?.let { observation ->
                                val action =
                                    DetailsFragmentDirections.actionMushroomDetailsFragmentToAddObservationFragmentEdit()
                                action.context = AddObservationFragment.Context.Edit
                                action.id = observation.id.toLong()
                                findNavController().navigate(action)
                            }
                        }
                        R.id.menu_detailsFragment_delete -> {
                            observationViewModel.observationState.value?.item?.id?.let {
                                binding.detailsFragmentBackgroundView.setLoading()
                                Session.deleteObservation(it) {
                                    when (it) {
                                        is Result.Error -> {
                                            binding.detailsFragmentBackgroundView.setErrorWithHandler(it.error, RecoveryAction.TRYAGAIN) {
                                                binding.detailsFragmentBackgroundView.reset()
                                            }
                                        }
                                        is Result.Success -> {
                                            Session.reloadData(true)
                                            findNavController().navigateUp()
                                        }
                                    }
                                }
                            }

                        }
                    }
                    return@setOnMenuItemClickListener true
                }
            }.show()
        }

    }

    private val takesSelectionButtonPressed = View.OnClickListener {
        when (args.takesSelection) {
            TakesSelection.SELECT -> {
                if (args.context == Context.SPECIES) {
                    if (args.predictionResults != null && args.imageFilePath != null) {
                        val action =
                            DetailsFragmentDirections.actionMushroomDetailsFragmentToAddObservationFragment()
                        action.context = AddObservationFragment.Context.FromRecognition
                        action.imageFilePath = args.imageFilePath
                        action.mushroomId = args.id
                        action.predictionNotes = args.predictionResults
                        findNavController().navigate(action)
                    } else {
                        val savedStateHandle = findNavController().previousBackStackEntry?.savedStateHandle
                        savedStateHandle?.set(AddObservationFragment.SAVED_STATE_TAXON_ID, args.id)
                        findNavController().navigateUp()
                    }
                }
            }

            TakesSelection.DESELECT -> {
                findNavController().previousBackStackEntry?.savedStateHandle?.set(AddObservationFragment.SAVED_STATE_TAXON_ID, 0)
                findNavController().navigateUp()
            }
            TakesSelection.NO -> {
            }
        }
    }

    private val imagesViewOnClick = { index: Int ->
        val images = mutableListOf<Image>()

        when (args.context) {
            Context.SPECIES -> {
                (speciesViewModel.mushroomState.value as? State.Items)?.items?.images?.let {
                    images.addAll(it)
                }
            }
            Context.OBSERVATION, Context.OBSERVATION_WITH_SPECIES -> {
                (observationViewModel.observationState.value as? State.Items)?.items?.images?.let {
                    images.addAll(it)
                }
            }
        }

        val action = DetailsFragmentDirections.actionMushroomDetailsFragmentToImageFragment(
            index,
            images.toTypedArray()
        )
        if (findNavController().currentDestination?.id == R.id.mushroomDetailsFragment) {
            findNavController().navigate(action)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getBoolean(KEY_HAS_EXPANDED)?.let { hasExpanded = it }
        mapFragment = childFragmentManager.findFragmentById(binding.detailsFragmentMapFragment.id) as MapFragment

        setupViews()
        setupViewModels()

        locationService = LocationService(requireContext().applicationContext).also {
            it.setListener(locationListener)
        }
    }

    private fun reportOffensiveContent() = ReportFragment(args.id).show(childFragmentManager, null)

    override fun onStart() {
        super.onStart()
        fetchLocationIfNeeded()
    }

    override fun onResume() {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        super.onResume()
    }

    override fun onStop() {
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HAS_EXPANDED, binding.detailsFragmentCollapsingToolbarLayout.scrollY == 0)
        super.onSaveInstanceState(outState)
    }


    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.detailsFragmentToolbar)

        binding.detailsFragmentNestedScrollView.isNestedScrollingEnabled = false
        binding.detailsFragmentCollapsingToolbarLayout.setExpandedTitleColor(Color.alpha(0))
        binding.detailsFragmentCollapsingToolbarLayout.setCollapsedTitleTextColor(
            ResourcesCompat.getColor(
                resources,
                R.color.colorWhite,
                null
            )
        )


        binding.detailsFragmentAppBarLayout.setExpanded(false, false)
        binding.detailsFragmentImagesView.visibility = View.GONE
        binding.detailsFragmentImagesView.setOnClickedAtIndex(imagesViewOnClick)
        mapFragment.setListener(mapFragmentListener)
        mapFragment.disableGestures()
        binding.detailsFragmentNestedScrollView.visibility = View.GONE

        if (Session.isLoggedIn) {
            when (args.takesSelection) {
                TakesSelection.SELECT -> {
                    binding.detailsFragmentTakesSelectionButton.setOnClickListener(takesSelectionButtonPressed)
                    binding.detailsFragmentTakesSelectionButton.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorGreen
                        )
                    )
                }

                TakesSelection.DESELECT -> {
                    binding.detailsFragmentTakesSelectionButton.setOnClickListener(takesSelectionButtonPressed)
                    binding.detailsFragmentTakesSelectionButton.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.colorRed
                        )
                    )
                }

                TakesSelection.NO -> {
                    binding.detailsFragmentTakesSelectionButton.visibility = View.GONE
                }
            }
        } else {
            binding.detailsFragmentTakesSelectionButton.visibility = View.GONE
        }


        when (args.context) {
            Context.OBSERVATION -> {
                binding.detailsFragmentRecyclerView.apply {
                    adapter = commentsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
            }

            Context.OBSERVATION_WITH_SPECIES -> {
                binding.detailsFragmentMushroomViewHeader.visibility = View.VISIBLE
                binding.detailsFragmentMushroomView.setListener(mushroomViewListener)
                binding.detailsFragmentMushroomView.round(true)
                binding.detailsFragmentMushroomView.visibility = View.VISIBLE

                binding.detailsFragmentRecyclerView.apply {
                    adapter = commentsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }

            }

            Context.SPECIES -> {
                binding.detailsFragmentRecyclerView.apply {
                    adapter = observationsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
            }
        }
    }

    private fun setupViewModels() {
        Session.commentUploadState.observe(viewLifecycleOwner) {
            binding.detailsFragmentBackgroundView.reset()
            when (it) {
                is State.Items -> {
                    commentsAdapter.addComment(it.items)
                    observationViewModel.addComment(it.items)
                }
                is State.Error -> handleError(it.error)
                is State.Loading -> binding.detailsFragmentBackgroundView.setLoading()
                else -> {}
            }
        }

        when (args.context) {
            Context.SPECIES -> {
                speciesViewModel.mushroomState.observe(viewLifecycleOwner, Observer {
                    binding.detailsFragmentBackgroundView.reset()

                    when (it) {
                        is State.Items -> {
                            configureView(it.items)
                        }
                        is State.Loading -> {
                            binding.detailsFragmentAppBarLayout.setExpanded(false, false)
                            binding.detailsFragmentBackgroundView.setLoading()
                        }
                        is State.Error -> binding.detailsFragmentBackgroundView.setError(it.error)
                        else -> {}
                    }
                })

                speciesViewModel.heatMapObservationCoordinates.observe(
                    viewLifecycleOwner,
                    Observer {
                        when (it) {
                            is State.Items -> mapFragment.addHeatMap(it.items)
                            is State.Loading -> mapFragment.setLoading()
                            is State.Error -> mapFragment.setError(it.error, null)
                            else -> {}
                        }
                    })

                speciesViewModel.recentObservationsState.observe(viewLifecycleOwner, Observer {
                    when (it) {
                        is State.Items -> observationsAdapter.configure(it.items, false)
                        else -> {}
                    }
                })
            }

            Context.OBSERVATION, Context.OBSERVATION_WITH_SPECIES -> {
                observationViewModel.observationState.observe(viewLifecycleOwner, Observer {
                    binding.detailsFragmentBackgroundView.reset()

                    when (it) {
                        is State.Items -> {
                            configureView(it.items)
                        }

                        is State.Loading -> {
                            binding.detailsFragmentAppBarLayout.setExpanded(false, false)
                            binding.detailsFragmentBackgroundView.setLoading()
                        }

                        is State.Error -> {
                            binding.detailsFragmentBackgroundView.setError(it.error)
                        }
                        else -> {}
                    }
                })

                observationViewModel.mushroomState.observe(viewLifecycleOwner, Observer {
                    when (it) {
                        is State.Items -> {
                            binding.detailsFragmentMushroomView.configure(it.items)
                        }
                        else -> {}
                    }
                })
            }
        }
    }

    private fun prepareViewsForContent() {
        binding.detailsFragmentMushroomDetailsHeaderView.visibility = View.GONE
        binding.detailsFragmentObservationHeaderView.visibility = View.GONE
        binding.detailsFragmentNestedScrollView.visibility = View.VISIBLE
    }


    private fun configureView(observation: Observation) {
        prepareViewsForContent()
        configureUpperLayout("Fund af: ${observation.determination.localizedName ?: observation.determination.fullName.upperCased()}", observation.images)

        binding.detailsFragmentObservationHeaderView.visibility = View.VISIBLE
        binding.detailsFragmentObservationHeaderView.configure(observation, observationHeaderViewListener)

        addDescriptionView(
            resources.getString(R.string.observationDetailsScrollView_ecologyNotes),
            observation.ecologyNote
        )
        addDescriptionView(resources.getString(R.string.observationDetailsScrollView_notes), observation.note)


        val information = mutableListOf<Pair<String, String>>()
        observation.locality?.let {
            information.add(Pair(getString(R.string.observationDetailsScrollView_location), it.name))
        }

        observation.substrate?.let {
            information.add((Pair(getString(R.string.observationDetailsScrollView_substrate), it.localizedName)))
        }
        observation.vegetationType?.let {
            information.add(Pair(getString(R.string.observationDetailsScrollView_vegetationType), it.localizedName))
        }

        observation.hosts.let {
            var string = ""
            it.forEach { string += if (it.localizedName != null) "${it.localizedName}, " else "${it.latinName}, "}
            if (string.isNotBlank()) {
                string = string.dropLast(2)
                information.add(Pair(getString(R.string.observationDetailsCell_host), string ))
            }
        }

        binding.detailsFragmentInformationView.configure(information)

        binding.detailsFragmentRecyclerViewHeader.text = getString(R.string.observationDetailsScrollView_comments)
        commentsAdapter.configure(observation.comments, Session.isLoggedIn)
        binding.detailsFragmentMapFragmentHeader.setText(R.string.observationDetailsScrollView_location)
        mapFragment.addLocationMarker(observation.coordinate)
        mapFragment.setRegion(observation.coordinate, 80000)
    }

    private fun configureView(mushroom: Mushroom) {
        prepareViewsForContent()
        configureUpperLayout(mushroom.localizedName?.upperCased() ?: mushroom.fullName.italized(), mushroom.images)
        if (mushroom.isGenus) {
            when (args.takesSelection) {
                TakesSelection.NO -> {
                }
                TakesSelection.SELECT -> {
                    if (args.imageFilePath != null) {
                        binding.detailsFragmentTakesSelectionButton.setText(R.string.detailsVC_newSightingPrompt)
                    } else {
                        binding.detailsFragmentTakesSelectionButton.setText(R.string.observationSpeciesCell_chooseGenus)
                    }
                }
                TakesSelection.DESELECT -> {
                    binding.detailsFragmentTakesSelectionButton.setText(R.string.observationSpeciesCell_deselect)

                }
            }
        } else {
            when (args.takesSelection) {
                TakesSelection.NO -> {
                }
                TakesSelection.SELECT -> {
                    if (args.imageFilePath != null) {
                        binding.detailsFragmentTakesSelectionButton.setText(R.string.detailsVC_newSightingPrompt)
                    } else {
                        binding.detailsFragmentTakesSelectionButton.setText(R.string.observationSpeciesCell_chooseSpecies)
                    }
                }
                TakesSelection.DESELECT -> {
                    binding.detailsFragmentTakesSelectionButton.setText(R.string.observationSpeciesCell_deselect)
                }
            }
        }

        binding.detailsFragmentMushroomDetailsHeaderView.visibility = View.VISIBLE
        binding.detailsFragmentMushroomDetailsHeaderView.configure(mushroom)

        addDescriptionView(
            getString(R.string.mushroomDetailsScrollView_description),
            mushroom.attributes?.localizedDescription
        )

        addDescriptionView(
            getString(R.string.mushroomDetailsScrollView_ecology),
            mushroom.attributes?.localizedEcology
        )

        addDescriptionView(
            resources.getString(R.string.mushroomDetailsScrollView_similarities),
            mushroom.attributes?.localizedSimilarities
        )

        addDescriptionView(
            getString(R.string.mushroomDetailsScrollView_eatability),
            mushroom.attributes?.localizedEdibility
        )

       
        val information = mutableListOf<Pair<String, String>>()
        mushroom.statistics?.acceptedObservationsCount?.let {
            information.add(
                Pair(
                    getString(R.string.mushroomDetailsScrollView_acceptedRecords),
                    it.toString()
                )
            )
        }
        mushroom.statistics?.lastAcceptedObservationDate?.let {
            information.add(
                Pair(
                    getString(R.string.mushroomDetailsScrollView_latestAcceptedRecord),
                    it.toReadableDate(recentFormatting = true, ignoreTime = true)
                )
            )
        }
        mushroom.updatedAtDate?.let {
            information.add(
                Pair(
                    getString(R.string.mushroomDetailsScrollView_latestUpdated),
                    it.toReadableDate(recentFormatting = true, ignoreTime = true)
                )
            )
        }
        binding.detailsFragmentInformationView.configure(information)

        binding.detailsFragmentMapFragmentHeader.setText(R.string.mushroomDetailsScrollView_heatMap)
        binding.detailsFragmentRecyclerViewHeader.text = getString(R.string.mushroomDetailsScrollView_latestObservations)
    }

    private fun configureUpperLayout(title: CharSequence, images: List<Image>?) {
        if (images.isNullOrEmpty()) return

        this.title = title
        binding.detailsFragmentCollapsingToolbarLayout.layoutMode = TITLE_COLLAPSE_MODE_FADE
        binding.detailsFragmentCollapsingToolbarLayout.title = title
        binding.detailsFragmentNestedScrollView.isNestedScrollingEnabled = true
        binding.detailsFragmentImagesView.configure(images)
        binding.detailsFragmentAppBarLayout.addOnOffsetChangedListener(this)

        if (!hasExpanded) {
            binding.detailsFragmentImagesView.visibility = View.VISIBLE
            binding.detailsFragmentAppBarLayout.setExpanded(true, true)
        } else {
            binding.detailsFragmentCollapsingToolbarLayout.title = title
        }
    }

    private fun addDescriptionView(title: String?, content: String?) {
        if (content != null && content != "") {
            val descriptionView = DescriptionView(context, null)
            descriptionView.configure(title, content)
            binding.detailsFragmentDescriptionViewLinearLayout.addView(descriptionView)

            val space = Space(context)
            space.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (24F * (context?.getResources()?.displayMetrics?.density ?: 1F)).toInt()
            )
            binding.detailsFragmentDescriptionViewLinearLayout.addView(space)
        }
    }

    private fun fetchLocationIfNeeded() {
        when (args.context) {
            Context.SPECIES -> {
                if (speciesViewModel.heatMapObservationCoordinates.value == null) {
                    locationService.start()
                }
            }
            else -> {}
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        if (!hasExpanded) {
            if (verticalOffset == 0)  {
                hasExpanded = true
                binding.detailsFragmentCollapsingToolbarLayout.title = title
            }
        } else {
            if (abs(verticalOffset) < binding.detailsFragmentAppBarLayout.totalScrollRange && binding.detailsFragmentImagesView.visibility == View.GONE) {
                binding.detailsFragmentImagesView.visibility = View.VISIBLE
            }
        }
    }
}


