package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemItemBinding

class ItemViewHolder(private val binding: ItemItemBinding) : RecyclerView.ViewHolder(binding.root) {

    fun configure(text: String) {
        binding.itemItemTextView.text = text
    }
}