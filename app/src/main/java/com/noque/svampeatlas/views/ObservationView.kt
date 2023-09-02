package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewObservationBinding
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.services.DataService

class ObservationView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var _observation: Observation? = null
    val observation: Observation? get() = _observation

    private val binding = ViewObservationBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_observation, this)
        setupView()
    }

    private fun setupView() {
        binding.observationViewImageView.clipToOutline = true
        binding.observationViewImageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                    val radius = resources.getDimension(R.dimen.app_rounded_corners)
                    outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }

    fun configure(observation: Observation, showValidationStatus: Boolean = false) {
        this._observation = observation

        binding.observationViewImageView.visibility = View.GONE

        observation.images.firstOrNull()?.let {
            binding.observationViewImageView.downloadImage(DataService.ImageSize.MINI, observation.images.first().url)
            binding.observationViewImageView.visibility = View.VISIBLE
        }

        if (showValidationStatus) {
            binding.observationViewValidationImageView.visibility = View.VISIBLE

            when (observation.validationStatus) {
                Observation.ValidationStatus.APPROVED -> {
                    binding.observationViewValidationImageView.setImageResource(R.drawable.glyph_checkmark)
                    binding.observationViewValidationImageView.setBackgroundResource(R.drawable.circle_view_color_green)
                }
                Observation.ValidationStatus.VERIFYING -> {
                    binding.observationViewValidationImageView.setImageResource(R.drawable.glyph_neutral)
                    binding.observationViewValidationImageView.setBackgroundResource(R.drawable.circle_view_color_primary)
                }
                Observation.ValidationStatus.REJECTED -> {
                    binding.observationViewValidationImageView.setImageResource(R.drawable.glyph_denied)
                    binding.observationViewValidationImageView.setBackgroundResource(R.drawable.circle_view_color_red)
                }
                Observation.ValidationStatus.UNKNOWN -> {
                    binding.observationViewValidationImageView.visibility = View.GONE
                }
            }
        }

        binding.observationViewPrimaryTextView.text = observation.determination.localizedName ?: observation.determination.fullName

        val date = observation.observationDate
        if (date != null) {
            binding.observationViewSecondaryTextView.text = "${date.toReadableDate(true, true)}, ${observation.locality?.name}"
        } else {
            binding.observationViewSecondaryTextView.text = observation.locationName
        }

        binding.observationViewUserTextView.text = observation.observationBy
    }
}