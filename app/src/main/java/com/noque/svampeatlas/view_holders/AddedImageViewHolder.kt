package com.noque.svampeatlas.view_holders

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemAddedImageBinding
import com.noque.svampeatlas.models.UserObservation


class AddedImageViewHolder(binding: ItemAddedImageBinding) : RecyclerView.ViewHolder(binding.root) {

    private var imageView: ImageView = binding.addedImageItemImageView
    private var lock: ImageView = binding.addedImageItemLock

    val isLocked: Boolean get() {
        return lock.visibility == View.VISIBLE
    }

    init {
        imageView.clipToOutline = true
        imageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                    val radius = itemView.resources.getDimension(R.dimen.app_rounded_corners)
                    outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }


    fun configure(image: UserObservation.Image) {
        lock.visibility = View.GONE
        imageView.alpha = 1F
        when (image) {
            is UserObservation.Image.LocallyStored -> {
            Glide.with(imageView)
                .load(image.file)
                .into(imageView)
            }

            is UserObservation.Image.New -> {
                Glide.with(imageView)
                    .load(image.file)
                    .into(imageView)
            }
            is UserObservation.Image.Hosted -> {
                Glide.with(imageView)
                    .load(image.url)
                    .into(imageView)
                if (!image.isDeletable) {
                    imageView.alpha = 0.3F
                    lock.visibility = View.VISIBLE
                }
            }
        }


    }
}