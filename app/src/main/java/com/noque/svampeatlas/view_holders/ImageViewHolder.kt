package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemImageBinding
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.services.DataService

class ImageViewHolder(val scaleType: ImageView.ScaleType, val binding: ItemImageBinding): RecyclerView.ViewHolder(binding.root) {


    init {
        binding.imageItemPhotoView.scaleType = scaleType
    }

    fun setOnClickListener(listener: View.OnClickListener) {
        binding.imageItemPhotoView.setOnPhotoTapListener { _, _, _ ->
            listener.onClick(itemView)
        }
    }

    fun configure(image: Image) {
        binding.imageItemPhotoView.downloadImage(DataService.ImageSize.FULL, image.url)
    }
}