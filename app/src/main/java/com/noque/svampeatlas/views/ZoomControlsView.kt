package com.noque.svampeatlas.views

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.motion.widget.MotionLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewZoomControlsBinding


class ZoomControlsView(context: Context, attrs: AttributeSet?) : MotionLayout(context, attrs) {

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                binding.zoomControlsViewSeekbar.progress = binding.zoomControlsViewSeekbar.progress - (binding.zoomControlsViewSeekbar.max / 10)
                countDownTimer.start()
                if (binding.zoomControlsViewRoot.progress == 0F) expand()
                expand()
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                binding.zoomControlsViewSeekbar.progress = binding.zoomControlsViewSeekbar.progress - (binding.zoomControlsViewSeekbar.max / 10)
                countDownTimer.start()
                if (binding.zoomControlsViewRoot.progress == 0F) expand()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }


    interface Listener {
        fun zoomLevelSet(zoomRatio: Float)
        fun collapsed()
        fun expanded()
    }

    private val binding = ViewZoomControlsBinding.inflate(LayoutInflater.from(context), this, true)

    private var listener: Listener? = null
    private var countDownTimer = object: CountDownTimer(1500,2000) {
        override fun onTick(p0: Long) { }

        override fun onFinish() {
            collapse()
        }
    }
    private val minZoomRatio: Float = 0f

    private val seekbarChangeLister = object: SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            listener?.zoomLevelSet(getZoomRatio(p1))
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
            countDownTimer.cancel()
        }
        override fun onStopTrackingTouch(p0: SeekBar?) {
            countDownTimer.start()
        }
    }

    private val zoomOutButtonClickListener = View.OnClickListener {
        binding.zoomControlsViewSeekbar.progress = binding.zoomControlsViewSeekbar.progress - (binding.zoomControlsViewSeekbar.max / 10)
        listener?.zoomLevelSet(getZoomRatio(binding.zoomControlsViewSeekbar.progress))
        countDownTimer.start()
    }

    private val zoomInButtonClickListener = View.OnClickListener {
        if (binding.zoomControlsViewRoot.progress == 1F) {
            binding.zoomControlsViewSeekbar.progress =  (binding.zoomControlsViewSeekbar.max / 10) + binding.zoomControlsViewSeekbar.progress
            listener?.zoomLevelSet(getZoomRatio(binding.zoomControlsViewSeekbar.progress))
        } else if (binding.zoomControlsViewRoot.progress == 0F) {
            expand()
        }

        countDownTimer.start()
    }

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_zoom_controls, this)
        setupView()
    }

    private fun setupView() {
        binding.zoomControlsViewSeekbar.max = 100
        binding.zoomControlsViewSeekbar.setOnSeekBarChangeListener(seekbarChangeLister)
        binding.zoomControlsViewZoomOutButton.setOnClickListener(zoomOutButtonClickListener)
        binding.zoomControlsViewZoomInButton.setOnClickListener(zoomInButtonClickListener)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun setValue(zoomRatio: Float) {
        binding.zoomControlsViewSeekbar.progress = ((zoomRatio * 100) - (minZoomRatio * 100)).toInt()
    }

    private fun expand() {
        binding.zoomControlsViewRoot.transitionToEnd()
    }

    fun collapse() {
        binding.zoomControlsViewRoot.transitionToStart()
    }

    private fun getZoomRatio(newValue: Int): Float {
        return newValue.toFloat() / 100
    }

    fun rotate(transform: Float, animationDuration: Long) {
        binding.zoomControlsViewZoomInButton.animate().rotation(transform).setDuration(animationDuration).start()
        binding.zoomControlsViewZoomOutButton.animate().rotation(transform).setDuration(animationDuration).start()
    }
}