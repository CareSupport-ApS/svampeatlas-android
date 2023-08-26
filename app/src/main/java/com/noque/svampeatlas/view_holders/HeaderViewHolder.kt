package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemHeaderBinding

class HeaderViewHolder(private val binding: ItemHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

    fun configure(title: String, extraMessage: String? = null) {
        binding.headerItemHeaderView.configure(title, extraMessage)
    }
}