package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.noque.svampeatlas.adapters.add_observation.AddImagesAdapter
import com.noque.svampeatlas.databinding.ViewAddObservationImagesBinding

class AddObservationImagesView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewAddObservationImagesBinding.inflate(LayoutInflater.from(context), this, false)

    private val adapter by lazy {
        AddImagesAdapter()
    }

    init {
        setupView()
    }

    private fun setupView() {
        binding.addObservationImagesViewRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.addObservationImagesViewRecyclerView.adapter = adapter
    }
}