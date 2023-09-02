package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewResultBinding
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.services.DataService

class ResultView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewResultBinding.inflate(LayoutInflater.from(context), this, true)


    init {
        setupView()
    }

    private fun setupView() {
        binding.resultViewImageView.apply {
            clipToOutline = true
            outlineProvider = object: ViewOutlineProvider() {
                override fun getOutline(view: View?, outline: Outline?) {
                    view?.let {
                        val radius = resources.getDimension(R.dimen.app_rounded_corners)
                        outline?.setRoundRect(0,0,view.width, view.height, radius)
                    }
                }
        }
        }
    }

    fun configure(mushroom: Mushroom) {
        if (mushroom.isGenus) {
            binding.resultViewImageView.setImageResource(R.drawable.icon_genus)
            val primaryText = mushroom.localizedName?.upperCased() ?: mushroom.fullName.italized()
            binding.resultViewPrimaryLabel.text = resources.getString(R.string.containedResultCell_genus, primaryText)
            binding.resultViewSecondaryLabel.text = if (mushroom.localizedName != null) mushroom.fullName.italized() else null
        } else {
            if (mushroom.images?.firstOrNull() != null) {
                binding.resultViewImageView.visibility = View.VISIBLE
                binding.resultViewImageView.downloadImage(DataService.ImageSize.MINI, mushroom.images.first().url)
            } else {
                binding.resultViewImageView.visibility = View.GONE
            }

            binding.resultViewPrimaryLabel.text = mushroom.localizedName?.upperCased() ?: mushroom.fullName.italized()
            binding.resultViewSecondaryLabel.text = if (mushroom.localizedName != null) mushroom.fullName.italized() else null
        }

        if (mushroom.attributes?.isPoisonous == true) {
            binding.resultViewToxicityView.visibility = View.VISIBLE
        } else {
            binding.resultViewToxicityView.visibility = View.GONE
        }
    }

    fun configure(mushroom: Mushroom, score: Double?) {
        configure(mushroom)

        if (score != null) {
            binding.resultViewScoreLabel.visibility = View.VISIBLE
            binding.resultViewScoreLabel.text = "${String.format("%.1f", score * 100)}%"
        } else {
            binding.resultViewScoreLabel.visibility = View.GONE
        }
    }

    fun configure(drawable: Drawable, primaryText: String, secondaryText: String) {
        binding.resultViewImageView.visibility = View.VISIBLE
        binding.resultViewImageView.setImageDrawable(drawable)

        val params =  LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER_VERTICAL

        binding.resultViewImageView.layoutParams = params
        binding.resultViewPrimaryLabel.text = primaryText
        binding.resultViewSecondaryLabel.text = secondaryText
    }
}