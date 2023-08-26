package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemMushroomBinding
import com.noque.svampeatlas.models.Mushroom

class MushroomViewHolder(private val itemClick: ((Mushroom) -> Unit)?, var binding: ItemMushroomBinding): RecyclerView.ViewHolder(binding.root) {

    private val mushroomView = binding.itemMushroomMushroomView

    init {
        mushroomView.round(false)
    }

    fun configure(mushroom: Mushroom) {
        mushroomView.configure(mushroom)
        mushroomView.setOnClickListener {
            itemClick?.invoke(mushroom)
        }
    }
}