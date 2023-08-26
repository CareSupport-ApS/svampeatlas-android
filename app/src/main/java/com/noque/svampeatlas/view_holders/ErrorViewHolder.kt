package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemErrorBinding
import com.noque.svampeatlas.models.AppError

class ErrorViewHolder(private val binding: ItemErrorBinding) : RecyclerView.ViewHolder(binding.root) {
    fun configure(error: AppError) {
        binding.errorItemTitle.text = error.title
        binding.errorItemSecondary.text = error.message
    }
}