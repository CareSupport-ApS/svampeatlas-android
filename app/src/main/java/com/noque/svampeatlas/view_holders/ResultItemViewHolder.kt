package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemResultBinding
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.views.ResultView

class ResultItemViewHolder(binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root) {

    private val resultView: ResultView = binding.resultItemResultView

    fun configure(mushroom: Mushroom) {
       resultView.configure(mushroom)
    }

    fun configure(mushroom: Mushroom, score: Double?) {
        resultView.configure(mushroom, score)
    }
}