package com.noque.svampeatlas.fragments

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.AddImagesAdapter
import com.noque.svampeatlas.adapters.add_observation.InformationAdapter
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.pxToDp
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.models.UserObservation
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_holders.AddImageViewHolder
import com.noque.svampeatlas.view_holders.AddedImageViewHolder
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.NewObservationViewModelFactory
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.SpinnerView
import kotlinx.android.synthetic.main.action_view_continue.view.*
import kotlinx.android.synthetic.main.action_view_save_notebook_entry.view.*
import kotlinx.android.synthetic.main.action_view_upload_button.view.*
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_add_observation.*
import java.io.File
import java.util.*


class AddObservationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, PromptFragment.Listener,
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

    private var toast: Toast? = null

    // Views
    private var spinnerView by autoCleared<SpinnerView>()
    private var toolbar by autoCleared<Toolbar>()
    private var viewPager by autoCleared<ViewPager> { it?.adapter = null }
    private var addImagesRecyclerView by autoCleared<RecyclerView> { it?.adapter = null }
    private var tabLayout by autoCleared<TabLayout>() { it?.setupWithViewPager(null) }

    // View models
    private val newObservationViewModel by viewModels<NewObservationViewModel> { NewObservationViewModelFactory(args.context, args.id, args.mushroomId, args.imageFilePath,  requireActivity().application) }

    // Adapters
    private val addImagesAdapter by lazy {
        val adapter = AddImagesAdapter()
        adapter.addImageButtonClicked = {
            val action =
                AddObservationFragmentDirections.actionAddObservationFragmentToCameraFragment().setContext(CameraFragment.Context.IMAGE_CAPTURE)
            findNavController().navigate(action)
        }
        adapter
    }

    private val informationAdapter by lazy {
        if (args.context == Context.Edit) {
            InformationAdapter(
                context,
                arrayOf(Category.DETAILS, Category.LOCALITY),
                childFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            )
        } else {
            InformationAdapter(
                context,
                Category.values,
                childFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            )
        }
    }

    // Listeners
    override fun positiveButtonPressed() = newObservationViewModel.promptPositive()
    override fun negativeButtonPressed() = newObservationViewModel.promptNegative()

    private val viewPagerListener = object: ViewPager.SimpleOnPageChangeListener() {
        override fun onPageSelected(position: Int) {
            activity?.invalidateOptionsMenu()
            super.onPageSelected(position)
            when (Category.values()[position]) {
                Category.LOCALITY -> {
                    addImagesRecyclerView.foreground = null
                    addImagesRecyclerView.isEnabled = true
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
                    addImagesRecyclerView.foreground = null
                    addImagesRecyclerView.isEnabled = true
                }
                Category.SPECIES -> {
                    addImagesRecyclerView.isEnabled = false
                    addImagesRecyclerView.foreground = ColorDrawable(resources.getColor(R.color.colorPrimary)).apply { alpha = 150 }
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

    private val imageSwipedCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP) {
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return when (viewHolder) {
                    is AddImageViewHolder -> {
                        0
                    }
                    is AddedImageViewHolder -> {
                        if (!viewHolder.isLocked) super.getSwipeDirs(recyclerView, viewHolder) else 0
                    }
                    else -> {
                        super.getSwipeDirs(recyclerView, viewHolder)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                val trashIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_black_24dp, null)?.apply {
                    bounds = Rect(
                        viewHolder.itemView.left + (viewHolder.itemView.width / 2) - (intrinsicWidth / 2),
                        ((viewHolder.itemView.height) / 2) - (intrinsicHeight / 2) + recyclerView.paddingTop,
                        viewHolder.itemView.right - (viewHolder.itemView.width / 2) + (intrinsicWidth / 2),
                        (viewHolder.itemView.height / 2) + (intrinsicHeight / 2) + recyclerView.paddingTop
                    )
                }

                viewHolder.itemView.alpha = 1 - (-(dY) / viewHolder.itemView.height)

                trashIcon?.draw(c)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
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
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
        } else {
            val menuHost: MenuHost = requireActivity()
            menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
            inflater.inflate(R.layout.fragment_add_observation, container, false)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = addObservationFragment_toolbar
        viewPager = addObservationFragment_viewPager
        addImagesRecyclerView = addObservationFragment_addObservationImagesRecyclerView
        tabLayout = addObservationFragment_tabLayout
        spinnerView = addObservationFragment_spinner
        setupView()
        setupViewModels()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        when (args.context) {
            Context.New, Context.FromRecognition -> {
                if (viewPager.currentItem == Category.SPECIES.ordinal) {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_new_upload, menu)
                } else {
                    menuInflater.inflate(R.menu.add_observation_fragment_menu_new_continue, menu)
                }
            }
            Context.UploadNote -> {
                if (viewPager.currentItem == Category.SPECIES.ordinal) {
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
            (it.actionView as? LinearLayout)?.apply {
                actionView_continue.setOnClickListener {
                    when (Category.values[viewPager.currentItem]) {
                        Category.SPECIES -> newObservationViewModel.uploadNew()
                        Category.DETAILS -> if (newObservationViewModel.substrate.value != null && newObservationViewModel.vegetationType.value != null) viewPager.currentItem =
                            Category.SPECIES.ordinal else newObservationViewModel.uploadNew()
                        Category.LOCALITY -> if (newObservationViewModel.coordinateState.value?.item?.first != null && newObservationViewModel.locality.value != null) viewPager.currentItem = Category.DETAILS.ordinal else newObservationViewModel.uploadNew()
                    }
                }
            }
        }

        menu.findItem(R.id.menu_addObservationFragment_note_save)?.let {
            (it.actionView as? LinearLayout)?.apply {
                actionView_saveNotebookEntry.setOnClickListener {
                    newObservationViewModel.saveAsNote()
                }
            }
        }

        menu.findItem(R.id.menu_addObservationFragment_uploadButton)?.let {
            (it.actionView as? LinearLayout)?.apply {
                actionView_upload.setOnClickListener {
                    newObservationViewModel.uploadNew()
                }
            }
        }

        menu.findItem(R.id.menu_addObservationFragment_uploadChanges)?.let {
            (it.actionView as? LinearLayout)?.apply {
                actionView_upload.setOnClickListener {
                    newObservationViewModel.uploadChanges()
                }
            }
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
        locationService.setListener(locationServiceListener)

        when (args.context) {
            Context.Edit -> {
                toolbar.setTitle(R.string.action_editObservation)
                toolbar.subtitle = "ID: ${args.id}"
            }
            Context.Note -> {
                toolbar.setTitle(R.string.action_newNote)
            }
            Context.EditNote -> {
                toolbar.setTitle(R.string.action_editNote)
                toolbar.subtitle = Date(args.id).toReadableDate(false, ignoreTime = false)
            }
            Context.UploadNote -> {
                toolbar.setTitle(R.string.action_upload_note)
                toolbar.subtitle = Date(args.id).toReadableDate(false, ignoreTime = false)
            }
            else -> {
                toolbar.setNavigationIcon(R.drawable.icon_menu_button)
                toolbar.setTitle(R.string.addObservationVC_title)
            }
        }
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        tabLayout.setupWithViewPager(viewPager)
        addImagesRecyclerView.apply {
            val myHelper = ItemTouchHelper(imageSwipedCallback)
            myHelper.attachToRecyclerView(this)

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.HORIZONTAL
            this.layoutManager = layoutManager
            this.adapter = addImagesAdapter

            addOnItemTouchListener(object: RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                    return !addImagesRecyclerView.isEnabled
                }
            })
        }

        viewPager.apply {
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
        newObservationViewModel.resetEvent.observe(viewLifecycleOwner) {
            viewPager.currentItem = 0
        }

        newObservationViewModel.isLoading.observe(viewLifecycleOwner) {
            when (it) {
                true -> spinnerView.startLoading()
                false -> spinnerView.stopLoading()
            }
        }

        newObservationViewModel.images.observe(viewLifecycleOwner) {
            addImagesAdapter.configure(it ?: listOf())
        }

        newObservationViewModel.coordinateState.observe(viewLifecycleOwner) {
            when (it) {
                is State.Items, is State.Error -> {
                    tabLayout.getTabAt(Category.LOCALITY.ordinal)?.customView = null
                }
                is State.Loading -> {
                    tabLayout.getTabAt(Category.LOCALITY.ordinal)
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
                is State.Items, is State.Empty, is State.Error -> tabLayout.getTabAt(Category.LOCALITY.ordinal)?.customView =
                    null
                is State.Loading -> tabLayout.getTabAt(Category.LOCALITY.ordinal)
                    ?.setCustomView(R.layout.view_spinner_small)
            }
        }

        newObservationViewModel.showNotification.observe(viewLifecycleOwner) {
            // Get appropiate bitmap
            val bitmap = when (it) {
                is NewObservationViewModel.Notification.ObservationUploaded, is NewObservationViewModel.Notification.NoteSaved, is NewObservationViewModel.Notification.Deleted -> BitmapFactory.decodeResource(
                    resources,
                    R.drawable.icon_elmessageview_success
                ).changeColor(R.color.colorGreen)
                else -> BitmapFactory.decodeResource(
                    resources,
                    R.drawable.icon_elmessageview_failure
                ).changeColor(R.color.colorRed)
            }

            when (it) {
                is NewObservationViewModel.Notification.LocalityInaccessible -> createToast(
                    it.title,
                    it.message,
                    bitmap
                )
                is NewObservationViewModel.Notification.LocationInaccessible -> createToast(
                    it.title,
                    it.message,
                    bitmap
                )
                is NewObservationViewModel.Notification.ObservationUploaded -> {
                    // When a user has uploaded an observation, we always want to just reset, so they can prepare a new one
                    createToast(it.title, it.message, bitmap)
                    newObservationViewModel.delete()
                }
                is NewObservationViewModel.Notification.ObservationUpdated -> {
                    // When a user has updated an uploaded observation, we always want to take them back to the previous page.
                    val action = AddObservationFragmentDirections.actionGlobalMyPageFragment()
                    findNavController().navigate(action)
                }
                is NewObservationViewModel.Notification.NoteSaved -> {
                    // When a user has saved a note, we need to check which state the app is in, to determine if they should be taken back or stay, to create a new one.
                    when (args.context) {
                        Context.New, Context.FromRecognition -> {
                            viewPager.currentItem = Category.SPECIES.ordinal
                            createToast(it.title, it.message, bitmap)
                            newObservationViewModel.delete()
                            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                                NotesFragment.RELOAD_DATA_KEY,
                                true
                            )
                        }
                        Context.Note, Context.EditNote, Context.UploadNote -> {
                            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                                NotesFragment.RELOAD_DATA_KEY,
                                true
                            )
                            findNavController().navigateUp()
                        }
                        else -> {}
                    }
                }
                is NewObservationViewModel.Notification.Deleted -> {
                    when (args.context) {
                        Context.New, Context.Note, Context.FromRecognition -> {
                            viewPager.currentItem = Category.SPECIES.ordinal
                        }
                        Context.Edit -> {
                            val action =
                                AddObservationFragmentDirections.actionGlobalMyPageFragment()
                            findNavController().navigate(action)
                        }

                        Context.EditNote, Context.UploadNote -> {
                            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                                NotesFragment.RELOAD_DATA_KEY,
                                true
                            )
                            findNavController().navigateUp()
                        }
                    }
                }
                is NewObservationViewModel.Notification.NewObservationError -> {
                    when (it.error) {
                        UserObservation.Error.NoMushroomError -> viewPager.currentItem =
                            Category.SPECIES.ordinal
                        UserObservation.Error.NoSubstrateError, UserObservation.Error.NoVegetationTypeError -> viewPager.currentItem =
                            Category.DETAILS.ordinal
                        UserObservation.Error.NoLocationDataError, UserObservation.Error.NoLocalityDataError, UserObservation.Error.LowAccuracy -> viewPager.currentItem =
                            Category.LOCALITY.ordinal
                    }
                    createToast(it.title, it.message, bitmap)
                }
                is NewObservationViewModel.Notification.Error -> {
                    createToast(it.title, it.message, bitmap)
                }
                is NewObservationViewModel.Notification.UseImageMetadata -> {
                    PromptFragment().apply {
                        setTargetFragment(this@AddObservationFragment, 10)
                        arguments = Bundle().apply {
                            putString(PromptFragment.KEY_TITLE, it.title)
                            putString(PromptFragment.KEY_MESSAGE, it.message)
                            putString(PromptFragment.KEY_POSITIVE, it.action?.first ?: "")
                            putString(PromptFragment.KEY_NEGATIVE, it.action?.second ?: "")
                        }
                        show(this@AddObservationFragment.parentFragmentManager, null)
                    }
                }
            }
        }
    }

    private fun createToast(title: String, message: String, bitmap: Bitmap) {
       toast?.cancel()
        toast = null
            val container = custom_toast_container
            val layout = layoutInflater.inflate(R.layout.custom_toast, container)

            layout.customToast_titleTextView.text = title
            layout.customToast_messageTextView.text = message
            layout.customToast_imageView.setImageBitmap(bitmap)
            with (Toast(context)) {
                    setGravity(Gravity.BOTTOM, 0, 16.pxToDp(context))
                    duration = Toast.LENGTH_SHORT
                    view = layout
                    show()
                    toast = this
                }
            }

    override fun onDestroy() {
        locationService.setListener(null)
        locationService.stop()
        super.onDestroy()
    }
}