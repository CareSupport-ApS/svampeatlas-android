package com.noque.svampeatlas.view_holders

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemAddCommentBinding

class AddCommentViewHolder(val binding: ItemAddCommentBinding) : RecyclerView.ViewHolder(binding.root) {

    fun setOnClickListener(listener: View.OnClickListener) {
        binding.addCommentSendButton.setOnClickListener {
            listener.onClick(itemView)
        }
    }

    fun getComment(): String? {
            val system = ContextCompat.getSystemService(itemView.context, InputMethodManager::class.java)
            system?.hideSoftInputFromWindow(binding.addCommentEditText.windowToken, 0)

        return if (binding.addCommentEditText.text.isNullOrEmpty()) null else binding.addCommentEditText.text.toString()
    }
}