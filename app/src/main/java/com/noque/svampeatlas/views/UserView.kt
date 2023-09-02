package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.ViewUserBinding
import com.noque.svampeatlas.models.User
import com.noque.svampeatlas.services.DataService

class UserView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val binding = ViewUserBinding.inflate(LayoutInflater.from(context), this, true)

    fun configure(user: User) {
        binding.userViewProfileImageView.configure(user.initials, user.imageURL, DataService.ImageSize.FULL)
        binding.userViewSecondaryTextView.visibility = View.GONE
        binding.userViewPrimaryTextView.text = user.name
    }

    fun configureAsGuest() {
        binding.userViewPrimaryTextView.text = resources.getText(R.string.userView_guest_title)
        binding.userViewSecondaryTextView.text = resources.getText(R.string.userView_guest_message)
        binding.userViewSecondaryTextView.visibility = View.VISIBLE
        binding.userViewProfileImageView.configure(null, R.mipmap.android_app)
    }
}