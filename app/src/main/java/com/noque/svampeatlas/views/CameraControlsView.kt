package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewCameraControlsBinding

class CameraControlsView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    interface Listener {
        fun captureButtonPressed()
        fun resetButtonPressed()
        fun photoLibraryButtonPressed()
        fun actionButtonPressed(state: State)
    }

    enum class State {
        CAPTURE_NEW,
        CAPTURE,
        LOADING,
        CONFIRM,
        HIDDEN
    }

    private var listener: Listener? = null
    private var state: State = State.HIDDEN
    private val binding = ViewCameraControlsBinding.inflate(LayoutInflater.from(context), this, true)


    init {
        setupView()
    }

    private fun setupView() {
        binding.cameraControlsViewCaptureButton.setOnClickListener {
            listener?.captureButtonPressed()
        }

        binding.cameraControlsViewLibraryButton.setOnClickListener {
            if (state == State.CONFIRM) listener?.resetButtonPressed() else listener?.photoLibraryButtonPressed()
        }

        binding.actionButton.setOnClickListener {
            listener?.actionButtonPressed(state)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun configureState(state: State) {
        this.state = state
        visibility = View.VISIBLE
        when (state) {
            State.CAPTURE_NEW -> {
                binding.cameraControlsViewLibraryButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_photo_library, null))
                binding.cameraControlsViewCaptureButtonSpinner.visibility = View.INVISIBLE
                binding.cameraControlsViewCaptureButton.visibility = View.VISIBLE
                binding.cameraControlsViewLibraryButton.visibility = View.VISIBLE
                binding.actionButton.visibility = View.VISIBLE
                binding.actionButton.setText(R.string.cameraControlTextButton_noPhoto)
            }
            State.LOADING -> {
                binding.cameraControlsViewCaptureButton.visibility = View.INVISIBLE
                binding.cameraControlsViewCaptureButtonSpinner.visibility = View.VISIBLE
                binding.cameraControlsViewLibraryButton.visibility = View.INVISIBLE
                binding.actionButton.visibility = View.INVISIBLE
            }

            State.CAPTURE -> {
                binding.cameraControlsViewLibraryButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_photo_library, null))
                binding.cameraControlsViewLibraryButton.visibility = View.VISIBLE
                binding.cameraControlsViewCaptureButton.visibility = View.VISIBLE
                binding.cameraControlsViewCaptureButtonSpinner.visibility = View.INVISIBLE
                binding.actionButton.visibility = View.INVISIBLE
            }

            State.CONFIRM -> {
                binding.cameraControlsViewCaptureButton.visibility = View.INVISIBLE
                binding.cameraControlsViewCaptureButtonSpinner.visibility = View.INVISIBLE
                binding.cameraControlsViewLibraryButton.visibility = View.VISIBLE
                binding.cameraControlsViewLibraryButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_back_button, null))
                binding.actionButton.setText(R.string.cameraControlTextButton_usePhoto)
                binding.actionButton.visibility = View.VISIBLE
            }
            State.HIDDEN -> {
                visibility = View.GONE
            }
        }
    }

    fun rotate(transform: Float, animationDuration: Long) {
        binding.cameraControlsViewLibraryButton.animate().rotation(transform).setDuration(animationDuration).start()
        binding.actionButton.animate().rotation(transform).setDuration(animationDuration).start()
    }
}