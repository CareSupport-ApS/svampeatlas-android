package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ItemLocalityBinding
import com.noque.svampeatlas.models.Locality

class LocalityViewHolder(binding: ItemLocalityBinding) : RecyclerView.ViewHolder(binding.root) {


    private var button: Button = binding.localityItemButton
    private var selected = false


    fun setListener(listener: View.OnClickListener) {
        button.tag = this
        button.setOnClickListener(listener)
    }

    fun configure(locality: Locality, selected: Boolean, isLocked: Boolean) {
        button.text = locality.name
        button.isSelected = selected
        if (isLocked) {
            button.setCompoundDrawablesWithIntrinsicBounds(button.resources.getDrawable(R.drawable.glyph_lock, null), null, null, null)
        } else {
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }
}