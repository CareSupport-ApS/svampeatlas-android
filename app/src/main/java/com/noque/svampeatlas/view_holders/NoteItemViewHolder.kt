package com.noque.svampeatlas.view_holders

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemNewObservationBinding
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.NewObservation

class NoteItemViewHolder(private val binding: ItemNewObservationBinding) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun configure(newObservation: NewObservation, onUploadButtonClick: (() -> Unit)) {
        binding.newObservationItemUploadButton.setOnClickListener {
            onUploadButtonClick()
        }

        if (!newObservation.images.isEmpty()) {
            binding.newObservationItemImageLayout.visibility = View.VISIBLE

            Glide.with(binding.newObservationItemImageView)
                .load(newObservation.images.first())

                .into(binding.newObservationItemImageView)

            if (newObservation.images.count() > 1) {
                binding.newObservationItemImageLabel.text = "+ ${newObservation.images.count() - 1}"
                binding.newObservationItemImageLabel.visibility = View.VISIBLE
            } else {
                binding.newObservationItemImageLabel.visibility = View.GONE
            }
        } else {
            binding.newObservationItemImageLayout.visibility = View.GONE
        }

        binding.newObservationItemSmallLabel.text = itemView.resources.getString(R.string.observationDetailsScrollView_observationDate) + " " + newObservation.observationDate.toReadableDate(
            recentFormatting = false,
            ignoreTime = true
        ) + ", " +  (newObservation.locality?.name ?: itemView.resources.getString(R.string.common_localityNotSelected))
        binding.newObservationItemPrimaryLabel.text = newObservation.species?.localizedName ?: newObservation.species?.fullName ?: "-"
    }
}