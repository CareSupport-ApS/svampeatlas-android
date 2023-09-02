package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewMushroomHeaderBinding
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.models.Mushroom

class MushroomDetailsHeaderView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewMushroomHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    fun configure(mushroom: Mushroom) {
        binding.mushroomHeaderViewTitleTextView.text =
            mushroom.localizedName ?: mushroom.fullName.italized()
        if (mushroom.localizedName != null) binding.mushroomHeaderViewSubtitleTextView.text =
            mushroom.fullName.italized() else binding.mushroomHeaderViewSubtitleTextView.visibility =
            View.GONE

        if (mushroom.redListStatus != null) {
            binding.mushroomHeaderViewRedlistLabel.text = mushroom.redListStatus
            when (mushroom.redListStatus) {
                "LC", "NT" -> {
                    binding.mushroomHeaderViewRedlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_green)
                    binding.mushroomHeaderViewRedlistLabel.text =
                        resources.getString(R.string.redlistView_lcnt)
                }
                "CR", "EN"-> {
                    binding.mushroomHeaderViewRedlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_red)
                    binding.mushroomHeaderViewRedlistLabel.text =
                        resources.getString(R.string.redlistView_lcnt)
                }
                "VU" -> {
                    binding.mushroomHeaderViewRedlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_yellow)
                    binding.mushroomHeaderViewRedlistLabel.text =
                        resources.getString(R.string.redlistView_vu)
                }
                "DD" -> {
                    binding.mushroomHeaderViewRedlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_secondary)
                    binding.mushroomHeaderViewRedlistLabel.text =
                        resources.getString(R.string.redlistView_dd)
                }
            }
        } else {
            binding.mushroomHeaderViewRedlistLabel.visibility = View.GONE
            binding.mushroomHeaderViewRedlistShortIcon.visibility = View.GONE
    }
    }
}