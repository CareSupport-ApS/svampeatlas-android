package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemReloaderBinding

class ReloaderViewHolder(binding: ItemReloaderBinding) : RecyclerView.ViewHolder(binding.root) {

    enum class Type {
        RELOAD,
        LOAD
    }

    private val textView = binding.reloaderItemTextView

    fun configure(type: Type) {
        when (type) {
            Type.RELOAD -> { textView.setText(R.string.reloadCell_tryAgain) }
            Type.LOAD -> { textView.setText(R.string.reloadCell_showMore) }
        }
    }
}