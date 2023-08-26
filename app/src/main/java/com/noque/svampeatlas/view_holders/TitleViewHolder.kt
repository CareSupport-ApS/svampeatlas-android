package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemTitleBinding

class TitleViewHolder(private val binding: ItemTitleBinding) : RecyclerView.ViewHolder(binding.root) {

    fun configure(title: Int, message: Int) {
        binding.titleItemTitle.text = itemView.resources.getString(title)
        binding.titleItemMessage.text = itemView.resources.getString(message)
    }
}