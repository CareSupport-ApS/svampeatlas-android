package com.noque.svampeatlas.fragments

import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.AddImagesAdapter
import com.noque.svampeatlas.adapters.add_observation.InformationAdapter
import com.noque.svampeatlas.constants.RELOAD_DATA
import com.noque.svampeatlas.databinding.FragmentAddObservationBinding
import com.noque.svampeatlas.extensions.handleError
import com.noque.svampeatlas.extensions.handleInfo
import com.noque.svampeatlas.extensions.handleSuccess
import com.noque.svampeatlas.extensions.hideSpinner
import com.noque.svampeatlas.extensions.showSpinner
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.UserObservation
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.SwipeToDeleteCallback
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.NewObservationViewModelFactory
import com.noque.svampeatlas.views.MainActivity
import www.sanju.motiontoast.MotionToastStyle
import java.io.File
import java.util.Date


class AddObservationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback,
    MenuProvider {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val KEY_ADDIMAGE_SHOWN = "KEY_ADDIMAGE_SHOWN"
        const val SAVED_STATE_FILE_PATH = "SAVED_STATE_FILE_PATH"
        const val SAVED_STATE_TAXON_ID = "SAVED_STATE_TAXON_ID"
    }

    enum class Context {
        New,
        FromRecognition,
        Edit,
        Note,
        EditNote,
        UploadNote;
    }

    enum class Category {
        LOCALITY,
        DETAILS,
        SPECIES;

        companion object {
            val values = values()
        }
    }

    // Objects
    private val args: AddObservationFragmentArgs by navArgs()
    private var addImageShown = false

    private val locationService by lazy {
        LocationService(requireContext())
    }

    // Views
    private val binding by autoClearedViewBinding(FragmentAddObservationBinding::bind) {
        it?.observationImagesRecyclerView?.adapter = null
        it?.addObservationFragmentViewPager?.adapter = null
        it?.addObservationFragmentTabLayout?.setupWithViewPager(null)
        deletedCallback.attachToRecyclerView(null)
    }

    // View models
    private val newObservationViewModel: NewObservationViewModel by navGraphViewModels(R.id.add_observation_nav) { NewObservationViewModelFactory(args.context, args.id, args.mushroomId, args.imageFilePath) }

    // Adapters
    private val addImagesAdapter by lazy {
        AddImagesAdapter().apply {
            addImageButtonClicked = {
                val action =
                    AddObservationFragmentDirections.actionAddObservationFragmentToCameraFragment().setContext(CameraFragment.Context.IMAGE_CAPTURE)
                findNavController().navigate(action)
            }
        }
    }

    private val informationAdapter by lazy {
        val categories = if (args.context == Context.Edit) {
            arrayOf(Category.DETAILS, Category.LOCALITY)
        } else {
            Category.values
        }
        InformationAdapter(
            context,
            categories,
            childFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
    }

    // Listeners


    private val viewPagerListener = object: ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            activity?.invalidateOptionsMenu()
            super.onPageSelected(position)
            when (Category.values()[position]) {
                Category.LOCALITY -> {
                    binding.observationImagesRecyclerView.apply {
                        foreground = null
                        isEnabled = true
                    }
                    SharedPreferences.decreasePositionReminderCounter()
                    if (SharedPreferences.shouldShowPositionReminder()) {
                        val bundle = Bundle()
                        bundle.putSerializable(
                            TermsFragment.KEY_TYPE,
                            TermsFragment.Type.LOCALITYHELPER
                        )
                        val dialog = TermsFragment()
                        dialog.arguments = bundle
                        dialog.show(childFragmentManager, null)
                    }
                }
                Category.DETAILS -> {
                    binding.observationImagesRecyclerView.apply {
                        foreground = null
                        isEnabled = true
                    }
                }
                Category.SPECIES -> {
                    binding.observationImagesRecyclerView.apply {
                        foreground = ColorDrawable(resources.getColor(R.color.colorPrimary_dimmed, null))
                        isEnabled = false
                    }
                    newObservationViewModel.getPredictions()
                }
            }
        }
    }

    private val locationServiceListener = object : LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) =
            requestPermissions(permissions, requestCode)
        override fun isLocating() = newObservationViewModel.setCoordinateState(State.Loading())
        override fun locationRetrievalError(error: LocationService.Error) =
            newObservationViewModel.setCoordinateState(State.Error(error))
        override fun locationRetrieved(location: Location) = newObservationViewModel.setCoordinateState(State.Items(com.noque.svampeatlas.models.Location(
                Date(), LatLng(location.latitude, location.longitude), location.accuracy)))
    }

    private val deletedCallback by lazy {
        ItemTouchHelper(
            SwipeToDeleteCallback(UP,
                { viewHolder ->
                    val position = viewHolder.adapterPosition
                    if (newObservationViewModel.images.value?.getOrNull(position) is UserObservation.Image.New) {
                        newObservationViewModel.removeImageAt(position)
                    } else if (!SharedPreferences.hasSeenImageDeletion) {
                        addImagesAdapter.notifyItemChanged(viewHolder.adapterPosition)
                        val dialog = TermsFragment()
                        dialog.arguments = Bundle().apply { putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IMAGEDELETIONS) }
                        dialog.show(childFragmentManager, null)
                    } else {
                        newObservationViewModel.removeImageAt(position)
                    }
                },
                requireContext(),
                resources
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addImageShown = savedInstanceState?.getBoolean(KEY_ADDIMAGE_SHOWN) ?: false
        newObservationViewModel.setDeterminationNotes(args.predictionNotes)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (!addImageShown && args.context == Context.New) {
            addImageShown = true
            val action =
                AddObservationFragmentDirections.actionAddObservationFragmentToCameraFragment().setContext(CameraFragment.Context.NEW_OBSERVATION)
            findNavController().navigate(action)
            null
        } else inflater.inflate(R.layout.fragment_add_observation, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupViewModels()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        when (args.context) {
            Context.New, Context.FromRecognition -> {
                if (binding.addObservationFragmentViewPager.currentItem == Category.SPECIES.ordinal) {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_new_upload, menu)
                } else {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_new_continue, menu)
                }
            }
            Context.UploadNote -> {
                if (binding.addObservationFragmentViewPager.currentItem == Category.SPECIES.ordinal) {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_note_upload, menu)
                } else {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_note_continue, menu)
                }
            }

            Context.Edit -> menuInflater.inflate(R.menu.add_observation_fragment_menu_observation_edit, menu)
            Context.Note -> menuInflater.inflate(R.menu.add_observation_fragment_menu_note_save, menu)
            Context.EditNote -> menuInflater.inflate(R.menu.add_observation_fragment_menu_note_edit, menu)
        }

        menu.findItem(R.id.menu_addObservationFragment_continueButton)?.let {
            it.actionView?.findViewById<Button>(R.id.actionView_continue)?.setOnClickListener {
                when (Category.values[binding.addObservationFragmentViewPager.currentItem]) {
                    Category.SPECIES -> newObservationViewModel.uploadNew()
                    Category.DETAILS -> if (newObservationViewModel.substrate.value != null && newObservationViewModel.vegetationType.value != null) binding.addObservationFragmentViewPager.currentItem =
                        Category.SPECIES.ordinal else newObservationViewModel.uploadNew()

                    Category.LOCALITY -> if (newObservationViewModel.coordinateState.value?.item?.first != null && newObservationViewModel.locality.value != null) binding.addObservationFragmentViewPager.currentItem =
                        Category.DETAILS.ordinal else newObservationViewModel.uploadNew()
                }
            }
        }
        menu.findItem(R.id.menu_addObservationFragment_note_save)?.let {
            it.actionView?.findViewById<Button>(R.id.actionView_saveNotebookEntry)?.setOnClickListener { newObservationViewModel.saveAsNote() }
        }

        menu.findItem(R.id.menu_addObservationFragment_uploadButton)?.let {
            it.actionView?.findViewById<Button>(R.id.actionView_upload)?.setOnClickListener { newObservationViewModel.uploadNew() }
        }

        menu.findItem(R.id.menu_addObservationFragment_uploadChanges)?.let {
            it.actionView?.findViewById<Button>(R.id.actionView_upload)?.setOnClickListener {  newObservationViewModel.uploadChanges() }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_addObservationFragment_uploadButton -> newObservationViewModel.uploadNew()
            R.id.menu_addObservationFragment_note_save -> newObservationViewModel.saveAsNote()
            R.id.menu_addObservationFragment_deleteButton -> newObservationViewModel.delete()
        }
        return false
    }


    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_ADDIMAGE_SHOWN, addImageShown)
        super.onSaveInstanceState(outState)
    }

    private fun setupView() {
        requireActivity().addMenuProvider(this, viewLifecycleOwner)

        fun setToolbar(@StringRes resId: Int, mSubtitle: String?, navIcon: Int? = null) {
            binding.addObservationFragmentToolbar.apply {
                setTitle(resId)
                subtitle = mSubtitle
                navIcon?.let { setNavigationIcon(it) }
            }
        }

        locationService.setListener(locationServiceListener)

        when (args.context) {
            Context.Edit -> setToolbar(R.string.action_editObservation, "ID: ${args.id}")
            Context.Note -> setToolbar(R.string.action_newNote, null)
            Context.EditNote -> setToolbar(R.string.action_editNote, Date(args.id).toReadableDate(false, ignoreTime = false))
            Context.UploadNote -> setToolbar(R.string.action_upload_note, Date(args.id).toReadableDate(false, ignoreTime = false))
            else -> setToolbar(R.string.addObservationVC_title, null, R.drawable.icon_menu_button)
        }
        (requireActivity() as MainActivity).setSupportActionBar(binding.addObservationFragmentToolbar)
        binding.addObservationFragmentTabLayout.setupWithViewPager(binding.addObservationFragmentViewPager)
        binding.observationImagesRecyclerView.apply {
            deletedCallback.attachToRecyclerView(this)

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.HORIZONTAL
            this.layoutManager = layoutManager
            this.adapter = addImagesAdapter

            addOnItemTouchListener(object: RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return !binding.observationImagesRecyclerView.isEnabled
                }
            })
        }

        binding.addObservationFragmentViewPager.apply {
            adapter = informationAdapter
            addOnPageChangeListener(viewPagerListener)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(SAVED_STATE_FILE_PATH)?.observe(viewLifecycleOwner
        ) {
            newObservationViewModel.appendImage(File(it))
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>(
                SAVED_STATE_FILE_PATH
            )
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>(SAVED_STATE_TAXON_ID)?.observe(viewLifecycleOwner
        ) {
            newObservationViewModel.setMushroom(it)
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>(
                SAVED_STATE_TAXON_ID
            )
        }
    }

    private fun setupViewModels() {
        newObservationViewModel.isLoading.observe(viewLifecycleOwner) {
            when (it) {
                true -> showSpinner()
                false -> hideSpinner()
            }
        }

        newObservationViewModel.images.observe(viewLifecycleOwner) {
            addImagesAdapter.configure(
                it ?: listOf()
            )
        }

        newObservationViewModel.coordinateState.observe(viewLifecycleOwner) {
            when (it) {
                is State.Items, is State.Error -> {
                    binding.addObservationFragmentTabLayout.getTabAt(Category.LOCALITY.ordinal)?.customView = null
                }
                is State.Loading -> {
                    binding.addObservationFragmentTabLayout.getTabAt(Category.LOCALITY.ordinal)
                        ?.setCustomView(R.layout.view_spinner_small)
                }
                is State.Empty -> {
                    // When coordinatestate is set to empty, we want to fetch new coordinates
                    locationService.start()
                }
            }
        }

        newObservationViewModel.localitiesState.observe(viewLifecycleOwner) {
            when (it) {
                is State.Items, is State.Empty, is State.Error -> binding.addObservationFragmentTabLayout.getTabAt(Category.LOCALITY.ordinal)?.customView =
                    null
                is State.Loading -> binding.addObservationFragmentTabLayout.getTabAt(Category.LOCALITY.ordinal)
                    ?.setCustomView(R.layout.view_spinner_small)
            }
        }

        newObservationViewModel.prompt.observe(viewLifecycleOwner) {
            PromptFragment().apply {
                                arguments = Bundle().apply {
                                    putString(PromptFragment.KEY_TITLE, getString(it.title))
                                    putString(PromptFragment.KEY_MESSAGE, getString(it.message))
                                    putString(PromptFragment.KEY_POSITIVE, getString(it.action.first))
                                    putString(PromptFragment.KEY_NEGATIVE, getString(it.action.second))
                                }
                                show(this@AddObservationFragment.parentFragmentManager, null)
                            }
        }

        parentFragmentManager.setFragmentResultListener(
            PromptFragment.REQUEST_KEY, viewLifecycleOwner
        ) { _, bundle ->
            val result = bundle.getString(PromptFragment.RESULT_KEY)
            if (result == PromptFragment.KEY_POSITIVE) {
                newObservationViewModel.promptPositive()
            } else if (result == PromptFragment.KEY_NEGATIVE) {
                newObservationViewModel.promptNegative()
            }
        }

        newObservationViewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is NewObservationViewModel.Event.Reset -> {
                    binding.addObservationFragmentViewPager.currentItem = 0
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(RELOAD_DATA, true)
                }
                is NewObservationViewModel.Event.GoBack -> {
                    if (it.reload) findNavController().previousBackStackEntry?.savedStateHandle?.set(RELOAD_DATA, true)
                    findNavController().popBackStack(R.id.myPageFragment, false)
                }

                is NewObservationViewModel.Event.GoBackToRoot -> {
                    findNavController().popBackStack(R.id.myPageFragment, false)
                }
            }
        }

        newObservationViewModel.notification.observe(viewLifecycleOwner) {
            when (it.type) {
                MotionToastStyle.SUCCESS -> handleSuccess(getString(it.title), if (it.args != null) getString(it.message, it.message, *it.args) else getString(it.message, it.message))
                MotionToastStyle.ERROR -> handleError(getString(it.title), getString(it.message))
                MotionToastStyle.WARNING,
                MotionToastStyle.INFO -> handleInfo(getString(it.title), getString(it.message))
                MotionToastStyle.DELETE -> TODO()
                MotionToastStyle.NO_INTERNET -> TODO()
            }

            when (it) {
                is NewObservationViewModel.Notification.NewObservationError -> {
                    when (it.error) {
                                UserObservation.Error.NoMushroomError -> binding.addObservationFragmentViewPager.currentItem =
                                    Category.SPECIES.ordinal
                                UserObservation.Error.NoSubstrateError, UserObservation.Error.NoVegetationTypeError -> binding.addObservationFragmentViewPager.currentItem =
                                    Category.DETAILS.ordinal
                                UserObservation.Error.NoLocationDataError, UserObservation.Error.NoLocalityDataError, UserObservation.Error.LowAccuracy -> binding.addObservationFragmentViewPager.currentItem =
                                    Category.LOCALITY.ordinal
                            }
                }
                else -> {}
            }
        }
    }

    override fun onDestroy() {
        locationService.setListener(null)
        locationService.stop()
        super.onDestroy()
    }
}