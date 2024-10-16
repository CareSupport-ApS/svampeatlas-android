package com.noque.svampeatlas.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.databinding.FragmentCameraBinding
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Prediction
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.utilities.DeviceOrientation
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.CameraViewModel
import com.noque.svampeatlas.view_models.factories.CameraViewModelFactory
import com.noque.svampeatlas.views.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class CameraFragment : Fragment(R.layout.fragment_camera), MenuProvider {
    enum class Context {
        NEW_OBSERVATION,
        IMAGE_CAPTURE,
        IDENTIFY
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction): AppError(
        title,
        message,
        recoveryAction
    ) {
        class PermissionsError(resources: Resources): Error(
            resources.getString(R.string.avViewError_permissionsError_title), resources.getString(
                R.string.avViewError_permissionsError_message
            ), RecoveryAction.OPENSETTINGS
        )
        class CaptureError(resources: Resources): Error(
            resources.getString(R.string.avViewError_cameraError_title), resources.getString(
                R.string.avViewError_unknown_message
            ), RecoveryAction.TRYAGAIN
        ) }

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if (it.key == Manifest.permission.CAMERA) binding.cameraFragmentCameraView.post { startSessionIfNeeded() }
            }
        }

    // Does not work currently if we want access to GPS exif data
   /* val pickMediaRequest = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            cameraViewModel.setImageFile(it, FileManager.createTempFile(requireContext()))
        }
    }*/

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && data != null) {
            data.data?.let {
                cameraViewModel.setImageFile(it, FileManager.createTempFile(requireContext()))
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    // Objects
    private val args: CameraFragmentArgs by navArgs()
    private var cameraControl: CameraControl? = null
    private var photoFile: File? = null

    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private val locationManager: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val deviceOrientation by lazy { DeviceOrientation() }
    private var currentOrientation = Surface.ROTATION_0
    private var sensorManager: SensorManager? = null

    // Views
    private val binding by autoClearedViewBinding(FragmentCameraBinding::bind)

    // View models

    private val cameraViewModel by viewModels<CameraViewModel> { CameraViewModelFactory(
        args.context,
        requireActivity().application
    ) }

    private val resultsAdapterListener by lazy {
        object : ResultsAdapter.Listener {
            override fun reloadSelected() {  cameraViewModel.reset(); }
            override fun predictionResultSelected(predictionResult: Prediction) {
                val state = cameraViewModel.predictionResultsState.value
                val action = CameraFragmentDirections.actionGlobalMushroomDetailsFragment(
                    predictionResult.mushroom.id,
                    DetailsFragment.TakesSelection.SELECT,
                    DetailsFragment.Context.SPECIES,
                    (cameraViewModel.imageFileState.value as? State.Items)?.items?.absolutePath,
                    if (state is State.Items) Prediction.getNotes(
                        predictionResult,
                        state.items.first
                    ) else null
                )

                findNavController().navigate(action)
            }
        }
    }

   private val sensorEventListener by lazy {
       object : SensorEventListener {
           override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
           override fun onSensorChanged(p0: SensorEvent?) {
               val surfaceRotation: Int = when (deviceOrientation.orientation) {
                   ExifInterface.ORIENTATION_ROTATE_90 -> Surface.ROTATION_0
                   ExifInterface.ORIENTATION_NORMAL -> Surface.ROTATION_90
                   ExifInterface.ORIENTATION_ROTATE_270 -> Surface.ROTATION_180
                   ExifInterface.ORIENTATION_ROTATE_180 -> Surface.ROTATION_270
                   else -> Surface.ROTATION_0
               }

               if (currentOrientation != surfaceRotation) {
                   currentOrientation = surfaceRotation
                   imageCaptureUseCase?.targetRotation = surfaceRotation
                   rotateViews(surfaceRotation)
               }
           }
       }
   }

    private val cameraControlsViewListener by lazy {
        object: CameraControlsView.Listener {
            override fun captureButtonPressed() {
                val metadata = ImageCapture.Metadata()

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.lastLocation.addOnCompleteListener {
                        metadata.location = it.result
                        takePicture(metadata)
                    }
                } else {
                    takePicture(metadata)
                }
            }

            override fun resetButtonPressed() {
                cameraViewModel.reset()
            }

            @RequiresApi(Build.VERSION_CODES.Q)
            override fun photoLibraryButtonPressed() {
               /* pickMediaRequest.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))*/
                permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION))
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                startActivityForResult(intent, 0)

            }
            override fun actionButtonPressed(state: CameraControlsView.State) {
                when (state) {
                    CameraControlsView.State.CONFIRM, CameraControlsView.State.CAPTURE_NEW -> {
                        val imageFileState = cameraViewModel.imageFileState.value
                        if (imageFileState is State.Items) findNavController().previousBackStackEntry?.savedStateHandle?.set(AddObservationFragment.SAVED_STATE_FILE_PATH, imageFileState.items.absolutePath)
                        findNavController().navigateUp()
                    }
                    else -> return
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val cameraViewTouchListener = View.OnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val point = binding.cameraFragmentCameraView.meteringPointFactory.createPoint(motionEvent.x, motionEvent.y)
                val action = FocusMeteringAction.Builder(point).build()
                cameraControl?.startFocusAndMetering(action)
                true
            } else {
                false
            }
        }

    private val zoomControlsViewListener = object: ZoomControlsView.Listener {
        override fun zoomLevelSet(zoomRatio: Float) {
            cameraControl?.setLinearZoom(zoomRatio)
        }
        override fun collapsed() {}
        override fun expanded() {}
    }

    private fun takePicture(metadata: ImageCapture.Metadata) {
        photoFile = FileManager.createTempFile(requireContext()).also {
            binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.LOADING)
            imageCaptureUseCase?.takePicture(
                ImageCapture.OutputFileOptions.Builder(it).setMetadata(metadata).build(),
                ContextCompat.getMainExecutor(requireContext()),
                onImageSavedCallback
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                binding.cameraFragmentRoot.postDelayed(
                    {
                        binding.cameraFragmentRoot.foreground = ColorDrawable(Color.WHITE)
                        binding.cameraFragmentRoot.postDelayed({ binding.cameraFragmentRoot.foreground = null }, 150)
                    }, 400
                )
            }
        }
    }

    private fun handleImageSaving(photoFile: File) {
        val saveImages = SharedPreferences.getSaveImages()
        if (saveImages == null) {
            PromptFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(PromptFragment.KEY_TITLE, getString(R.string.cameraVC_shouldSaveImagesPrompt_title))
                    bundle.putString(PromptFragment.KEY_MESSAGE, getString(R.string.cameraVC_shouldSaveImagesPrompt_message))
                    bundle.putString(PromptFragment.KEY_POSITIVE, getString(R.string.cameraVC_shouldSaveImagesPrompt_message_positive))
                    bundle.putString(PromptFragment.KEY_NEGATIVE, getString(R.string.cameraVC_shouldSaveImagesPrompt_message_negative))
                }
                it.show(parentFragmentManager, null)
            }
        } else {
            GlobalScope.launch(Dispatchers.IO) {
                if (saveImages) FileManager.saveTempImage(photoFile, FileManager.createFile(requireContext()), requireContext())
            }
        }
    }

    private val onImageSavedCallback by lazy {
        object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val photoFile = photoFile
                if (photoFile != null) {
                    cameraViewModel.setImageFile(photoFile)
                    handleImageSaving(photoFile)
                } else cameraViewModel.setImageFileError(Error.CaptureError(resources))
            }
            override fun onError(exception: ImageCaptureException) {
                activity?.let { cameraViewModel.setImageFileError(Error.CaptureError(resources)) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            this.sensorManager = (requireContext().getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager).also {
                it.registerListener(
                    sensorEventListener, it.getDefaultSensor(
                        Sensor.TYPE_ROTATION_VECTOR
                    ), SensorManager.SENSOR_DELAY_UI
                )
                it.registerListener(
                    deviceOrientation.eventListener, it.getDefaultSensor(
                        Sensor.TYPE_ACCELEROMETER
                    ), SensorManager.SENSOR_DELAY_UI
                )
                it.registerListener(
                    deviceOrientation.eventListener, it.getDefaultSensor(
                        Sensor.TYPE_MAGNETIC_FIELD
                    ), SensorManager.SENSOR_DELAY_UI
                )
            }

            setupViews()
            setupViewModels()
        }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) =
        inflater.inflate(R.menu.menu_camera_fragment, menu)

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.menu_cameraFragment_aboutButton -> {
                val bundle = Bundle()
                bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.CAMERAHELPER)

                val dialog = TermsFragment()
                dialog.arguments = bundle
                dialog.show(childFragmentManager, null)
            return true
            }
        }
        return false
    }

        override fun onResume() {
            super.onResume()
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as MainActivity).hideSystemBars()
            validateState()
        }

        override fun onStart() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as MainActivity).hideSystemBars()
            super.onStart()
        }

        override fun onStop() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            (requireActivity() as MainActivity).showSystemBars()
            super.onStop()
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupViews() {
            requireActivity().addMenuProvider(this, viewLifecycleOwner)

            when (args.context) {
                Context.IMAGE_CAPTURE -> {
                    binding.cameraFragmentToolbar.setNavigationIcon(R.drawable.glyph_cancel)
                    binding.cameraFragmentToolbar.setNavigationOnClickListener {
                        findNavController().navigateUp()
                    }
                }
                Context.IDENTIFY -> {
                    (requireActivity() as MainActivity).setSupportActionBar(binding.cameraFragmentToolbar)
                    binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.CAPTURE)
                }
                Context.NEW_OBSERVATION -> {
                    (requireActivity() as MainActivity).setSupportActionBar(binding.cameraFragmentToolbar)
                    binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.CAPTURE_NEW)
                }
            }

            binding.cameraFragmentCameraControlsView.setListener(cameraControlsViewListener)
            binding.cameraFragmentZoomControlsView.setListener(zoomControlsViewListener)
            binding.cameraFragmentResultsView.setListener(resultsAdapterListener)
            binding.cameraFragmentCameraView.setOnTouchListener(cameraViewTouchListener)
        }


        private fun setupViewModels() {
            cameraViewModel.imageFileState.observe(
                viewLifecycleOwner
            ) { state ->
                when (state) {
                    is State.Items -> setImageState(state.items)
                    is State.Error -> setError(state.error)
                    is State.Empty -> reset()
                    else -> {}
                }
            }

            cameraViewModel.predictionResultsState.observe(viewLifecycleOwner) { state ->
                when (state) {
                    is State.Loading -> {
                        binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.LOADING)
                    }
                    is State.Error -> {
                        binding.cameraFragmentRoot.transitionToEnd()
                        binding.cameraFragmentResultsView.showError(state.error)
                    }
                    is State.Items -> {
                        binding.cameraFragmentRoot.transitionToEnd()
                        binding.cameraFragmentResultsView.showResults(state.items.first, state.items.second)
                    }

                    is State.Empty -> {
                        binding.cameraFragmentResultsView.reset()
                        binding.cameraFragmentRoot.transitionToStart()
                    }
                }
            }

            parentFragmentManager.setFragmentResultListener(
                PromptFragment.REQUEST_KEY, viewLifecycleOwner
            ) { _, bundle ->
                val result = bundle.getString(PromptFragment.RESULT_KEY)
                if (result == PromptFragment.KEY_POSITIVE) {
                    SharedPreferences.setSaveImages(true)
                    photoFile?.let {
                        lifecycleScope.launch {
                            FileManager.saveTempImage(it, FileManager.createFile(requireContext()), requireContext())
                        }
                    }
                } else if (result == PromptFragment.KEY_NEGATIVE) {
                    SharedPreferences.setSaveImages(false)
                }
            }
        }

        private fun validateState() {
            if (args.context == Context.IDENTIFY && !SharedPreferences.hasAcceptedIdentificationTerms()) {
                TermsFragment().also {
                    it.arguments = Bundle().also { bundle -> bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IDENTIFICATION) }
                    it.listener = object: TermsFragment.Listener {
                        override fun onDismiss(termsAccepted: Boolean) = permissionRequest.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                    it.show(childFragmentManager, null)
                }
            } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                binding.cameraFragmentCameraView.post {
                    startSessionIfNeeded()
                }
            } else {
                permissionRequest.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }

        private fun reset() {
            binding.cameraFragmentBackgroundView.reset()
            binding.cameraFragmentImageView.setImageResource(android.R.color.transparent)
            binding.cameraFragmentImageView.setBackgroundResource(android.R.color.transparent)
            when (args.context) {
                Context.IMAGE_CAPTURE, Context.IDENTIFY -> binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.CAPTURE)
                Context.NEW_OBSERVATION -> binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.CAPTURE_NEW)
            }
        }

        private fun startSessionIfNeeded() {
            context?.let {
                if (ContextCompat.checkSelfPermission(it, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(it)
                    cameraProviderFuture.addListener({
                        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                        previewUseCase =
                            Preview.Builder()
                                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                                .setTargetRotation(Surface.ROTATION_0)

                                .build()
                        imageCaptureUseCase = ImageCapture.Builder().build()
                        cameraProvider.unbindAll()
                        try {

                            val camera = cameraProvider.bindToLifecycle(
                                this,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                imageCaptureUseCase
                            )
                            previewUseCase?.setSurfaceProvider(binding.cameraFragmentCameraView.surfaceProvider)
                            cameraControl = camera.cameraControl
                        } catch (error: Exception) {
                            cameraViewModel.setImageFileError(Error.CaptureError(resources))
                        }

                    },  ContextCompat.getMainExecutor(it))
            }
            }
        }

        private fun setImageState(file: File) {
            binding.cameraFragmentBackgroundView.reset()
            binding.cameraFragmentImageView.setBackgroundResource(android.R.color.black)
            binding.cameraFragmentImageView.apply {
                Glide.with(this)
                    .load(file)
                    .into(this)
            }

            if (args.context == Context.IMAGE_CAPTURE || args.context == Context.NEW_OBSERVATION) binding.cameraFragmentCameraControlsView.configureState(CameraControlsView.State.CONFIRM)
        }

        private fun setError(error: AppError) {
            binding.cameraFragmentBackgroundView.setErrorWithHandler(error, error.recoveryAction) {
                if (error.recoveryAction == RecoveryAction.OPENSETTINGS) openSettings()
                else if (error.recoveryAction == RecoveryAction.TRYAGAIN) cameraViewModel.reset()
            }
        }

        private fun rotateViews(rotation: Int) {
            val transform = when (rotation) {
                Surface.ROTATION_0 -> 0F
                Surface.ROTATION_90 -> 90F
                Surface.ROTATION_180 -> 180F
                Surface.ROTATION_270 -> -90F
                else -> 0F
            }
            binding.cameraFragmentCameraControlsView.rotate(transform, 350)
            binding.cameraFragmentZoomControlsView.rotate(transform, 350)
        }

        override fun onDestroyView() {
            sensorManager?.unregisterListener(deviceOrientation.eventListener)
            sensorManager?.unregisterListener(sensorEventListener)
            previewUseCase?.setSurfaceProvider(null)
            super.onDestroyView()
        }
}


