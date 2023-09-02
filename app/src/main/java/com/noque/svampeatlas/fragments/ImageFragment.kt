package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentImageBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.views.MainActivity

class ImageFragment: Fragment(R.layout.fragment_image) {

    // Objects
    private val args: ImageFragmentArgs by navArgs()

    // Views
    private val binding by autoClearedViewBinding(FragmentImageBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as MainActivity).hideSystemBars()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as MainActivity).showSystemBars()
    }


    private fun setupViews() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.imageFragmentToolbar)
        binding.imageFragmentImagesView.configure(args.images.toList(), args.selectedIndex, ImageView.ScaleType.FIT_CENTER)

    }
}