package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.databinding.ViewProfileImageBinding
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.highlighted
import com.noque.svampeatlas.services.DataService


class ProfileImageView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val binding = ViewProfileImageBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupView()
    }

    fun setupView() {
        binding.profileImageViewImageView.clipToOutline = true
        binding.profileImageViewImageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                        val radius = (view.width / 2).toFloat()
                        outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }

    fun configure(initials: String?, imageURL: String?, imageSize: DataService.ImageSize) {
        binding.profileImageViewImageView.setImageDrawable(null)

        if (imageURL != null) {
            binding.profileImageViewImageView.downloadImage(imageSize, imageURL, false)
        }

        binding.profileImageViewTextView.text = initials?.toUpperCase()?.highlighted()
    }

    fun configure(initials: String?, @DrawableRes drawableRes: Int) {
        binding.profileImageViewTextView.text = initials?.toUpperCase()?.highlighted()
        binding.profileImageViewImageView.setImageResource(drawableRes)
    }
}