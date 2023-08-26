package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemObservationBinding
import com.noque.svampeatlas.models.Observation

class ObservationViewHolder(binding: ItemObservationBinding) : RecyclerView.ViewHolder(binding.root) {

    private val observationView = binding.observationItemObservationView

    fun configure(observation: Observation, showValidationStatus: Boolean = false) {
        observationView.configure(observation, showValidationStatus)


    }
}