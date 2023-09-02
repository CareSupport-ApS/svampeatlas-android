package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.noque.svampeatlas.databinding.ViewDescriptionBinding

class DescriptionView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewDescriptionBinding.inflate(LayoutInflater.from(context), this, true)

    fun configure(title: String?, content: String) {
        if (title == null) {
            binding.descriptionViewDividerTextView.visibility = GONE
        } else {
            binding.descriptionViewDividerTextView.text = title
        }

        binding.descriptionViewContentTextView.text = content
    }

}