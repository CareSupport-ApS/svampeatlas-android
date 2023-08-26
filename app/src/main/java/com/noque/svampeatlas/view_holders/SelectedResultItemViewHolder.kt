package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemSelectedResultBinding
import com.noque.svampeatlas.models.DeterminationConfidence
import com.noque.svampeatlas.models.Mushroom

class SelectedResultItemViewHolder(private val binding: ItemSelectedResultBinding) : RecyclerView.ViewHolder(binding.root) {

    var confidenceSet: ((DeterminationConfidence) -> Unit)? = null
    var deselectClicked: (() -> Unit)? = null

    fun setOnClickListener(listener: View.OnClickListener) {
        binding.selectedResultItemResultView.tag = this
        binding.selectedResultItemResultView.setOnClickListener(listener)
    }

    fun configure(mushroom: Mushroom, confidence: DeterminationConfidence?) {
        binding.selectedResultItemResultView.configure(mushroom)

            binding.selectedResultItemDeSelectButton.setOnClickListener {
                deselectClicked?.invoke()
            }


            if (mushroom.isGenus) {
                binding.selectedResultItemRadioButtonDetermined.setText(R.string.selectedSpeciesCell_confident_genus)
                binding.selectedResultItemRadioButtonGuessing.setText(R.string.selectedSpeciesCell_likely_genus)
                binding.selectedResultItemRadioButtonUnsure.setText(R.string.selectedSpeciesCell_possible_genus)
            } else {
                binding.selectedResultItemRadioButtonDetermined.setText(R.string.selectedSpeciesCell_confident_species)
                binding.selectedResultItemRadioButtonUnsure.setText(R.string.selectedSpeciesCell_likely_species)
                binding.selectedResultItemRadioButtonGuessing.setText(R.string.selectedSpeciesCell_possible_species)
            }

            binding.selectedResultItemConfidenceRadioButtonGroup.setOnCheckedChangeListener(null)

            when (confidence) {
                DeterminationConfidence.CONFIDENT -> { binding.selectedResultItemConfidenceRadioButtonGroup.check(R.id.selectedResultItem_radioButton_determined) }
                DeterminationConfidence.LIKELY -> { binding.selectedResultItemConfidenceRadioButtonGroup.check(R.id.selectedResultItem_radioButton_unsure) }
                DeterminationConfidence.POSSIBLE -> { binding.selectedResultItemConfidenceRadioButtonGroup.check(R.id.selectedResultItem_radioButton_guessing) }
                else -> {}
            }

        binding.selectedResultItemConfidenceRadioButtonGroup.setOnCheckedChangeListener { _, i ->
                when (i) {
                    R.id.selectedResultItem_radioButton_guessing -> {confidenceSet?.invoke(DeterminationConfidence.POSSIBLE)}
                    R.id.selectedResultItem_radioButton_unsure -> {confidenceSet?.invoke(DeterminationConfidence.LIKELY)}
                    R.id.selectedResultItem_radioButton_determined -> {confidenceSet?.invoke(DeterminationConfidence.CONFIDENT)}
                }
            }
        }
}