package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemCreditationBinding

class CreditationViewHolder(private val binding: ItemCreditationBinding) : RecyclerView.ViewHolder(binding.root) {

    enum class Type {
        AI,
        AINEWOBSERVATION
    }

    fun configure(type: Type) {
        when (type) {
            Type.AI -> binding.itemCreditationTextView.setText(R.string.creditationCell_ai)
            Type.AINEWOBSERVATION ->  binding.itemCreditationTextView.setText(R.string.creditationCell_aiNewObservation)
        }
    }
}