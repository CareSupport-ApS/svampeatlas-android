package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemUnknownSpeciesBinding

class UnknownSpeciesViewHolder(private val binding: ItemUnknownSpeciesBinding) : RecyclerView.ViewHolder(binding.root) {


    fun configure(isSelected: Boolean) {
//        if (isSelected) resultsView.setBackgroundColor(Color.TRANSPARENT) else resultsView.setBackgroundColor(itemView.resources.getColor(R.color.colorGreen))
    }
}