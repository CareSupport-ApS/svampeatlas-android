package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewObservationHeaderBinding
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Observation

class ObservationHeaderView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    interface Listener {
        fun menuButtonPressed(view: View)
    }

    private var listener: Listener? = null
    private val binding = ViewObservationHeaderBinding.inflate(LayoutInflater.from(context), this, false)


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_observation_header, this)
        binding.observationHeaderViewMoreButton.setOnClickListener {
            listener?.menuButtonPressed(it)
        }
    }

    fun configure(observation: Observation, listener: Listener) {
        this.listener = listener
        binding.observationHeaderViewIdLabel.text = "DMS: ${observation.id} | ${observation.observationBy} | ${observation.observationDate?.toReadableDate(true, true)}"
        binding.observationHeaderViewTitleLabel.text = observation.determination.localizedName ?: observation.determination.fullName

        when (observation.validationStatus) {
            Observation.ValidationStatus.APPROVED -> {
                binding.observationHeaderViewDeterminationIcon.setImageResource(R.drawable.glyph_checkmark)
                binding.observationHeaderViewDeterminationIcon.setBackgroundResource(R.drawable.circle_view_color_green)
                binding.observationHeaderViewDeterminationLabel.text = resources.getString(R.string.observationDetailsScrollView_validationStatus_approved)
            }
            Observation.ValidationStatus.VERIFYING -> {
                binding.observationHeaderViewDeterminationIcon.setImageResource(R.drawable.glyph_neutral)
                binding.observationHeaderViewDeterminationIcon.setBackgroundResource(R.drawable.circle_view_color_primary)
                binding.observationHeaderViewDeterminationLabel.text = resources.getString(R.string.observationDetailsScrollView_validationStatus_verifying)
            }
            Observation.ValidationStatus.REJECTED -> {
                binding.observationHeaderViewDeterminationIcon.setImageResource(R.drawable.glyph_denied)
                binding.observationHeaderViewDeterminationIcon.setBackgroundResource(R.drawable.circle_view_color_red)
                binding.observationHeaderViewDeterminationLabel.text = resources.getString(R.string.observationDetailsScrollView_validationStatus_declined)
            }
            Observation.ValidationStatus.UNKNOWN -> {
                binding.observationHeaderViewDeterminationIcon.visibility = View.GONE
                binding.observationHeaderViewDeterminationLabel.visibility = View.GONE
            }
        }
    }
}