package com.noque.svampeatlas.view_holders

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.databinding.ItemCommentBinding
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Comment
import com.noque.svampeatlas.services.DataService

class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

    fun configure(comment: Comment) {
        binding.commentItemNameTextView.text = comment.commenterName
        binding.commentItemContentTextView.text = comment.content
        binding.commentItemDateTextView.text = comment.date?.toReadableDate()
        binding.commentItemProfileImageView.configure(comment.initials, comment.commenterProfileImageURL, DataService.ImageSize.FULL)
    }
}