package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewSpinnerBinding

class SpinnerView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val binding = ViewSpinnerBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_spinner, this)
    }

    fun startLoading() {
        isClickable = true
        isFocusable = true
        val color = ColorUtils.setAlphaComponent(Color.BLACK, 150)
        setBackgroundColor(color)
        binding.spinnerViewProgressBar.visibility = View.VISIBLE
    }

    fun stopLoading() {
        isClickable = false
        isFocusable = false

        setBackgroundColor(Color.TRANSPARENT)
        binding.spinnerViewProgressBar.visibility = View.GONE
    }
}