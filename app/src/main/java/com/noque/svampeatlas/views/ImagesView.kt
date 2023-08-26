package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.ImagesAdapter
import com.noque.svampeatlas.databinding.ViewImagesBinding
import com.noque.svampeatlas.models.Image

class ImagesView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val binding = ViewImagesBinding.inflate(LayoutInflater.from(context), this, false)


    private val imagesAdapter by lazy {
        val adapter = ImagesAdapter()
        adapter
    }

    fun setOnClickedAtIndex(listener: ((Int) -> Unit)?) {
        imagesAdapter.onClickedAtIndex = listener
    }

    init {
        setupView()
    }

    private fun setupView() {
        binding.imagesViewRecyclerView.apply {
            val gridLayout = GridLayoutManager(context, 1)
            gridLayout.orientation = RecyclerView.HORIZONTAL
            layoutManager = gridLayout
            adapter = imagesAdapter
            addItemDecoration(
                PaginatorDecoration(
                    ContextCompat.getColor(
                        context,
                        R.color.colorThird
                    ), ContextCompat.getColor(context, R.color.colorWhite)
                )
            )

        }

        val pageSnap = PagerSnapHelper()
        pageSnap.attachToRecyclerView(binding.imagesViewRecyclerView)
    }

    fun configure(images: List<Image>, beginningPosition: Int = 0,  scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP) {
        imagesAdapter.configure(images, scaleType)
        binding.imagesViewRecyclerView.scrollToPosition(beginningPosition)
    }
}