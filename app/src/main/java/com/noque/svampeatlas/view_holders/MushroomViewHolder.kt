package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemMushroomBinding
import com.noque.svampeatlas.models.Mushroom

class MushroomViewHolder(private val itemClick: ((Mushroom) -> Unit)?, var binding: ItemMushroomBinding): RecyclerView.ViewHolder(binding.root) {

    init {
        binding.mushroomView.round(false)
    }

    fun configure(mushroom: Mushroom) {
        binding.mushroomView.apply {
            configure(mushroom)
            setOnClickListener {
                itemClick?.invoke(mushroom)
            }
        }
    }
}