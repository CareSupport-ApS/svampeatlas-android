package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.noque.svampeatlas.databinding.ViewHeaderBinding

class HeaderView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    fun configure(title: String, extraMessage: String? = null) {
        binding.headerViewTextView .text = title
        binding.headerViewExtraTextView.text = extraMessage
    }
}