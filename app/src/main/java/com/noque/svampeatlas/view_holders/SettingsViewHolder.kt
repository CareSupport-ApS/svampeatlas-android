package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemSettingBinding

class SettingsViewHolder(private val binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root) {
    fun configure(icon: Int, title: String, content: String?) {
        binding.settingItemIconImageView.setImageResource(icon)
        binding.settingItemTitleTextView.text = title
        binding.settingItemContentTextView.text = content
    }

}