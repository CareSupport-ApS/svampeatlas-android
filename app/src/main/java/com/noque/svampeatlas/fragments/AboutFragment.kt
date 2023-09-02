package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentAboutBinding
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.views.HeaderView
import com.noque.svampeatlas.views.MainActivity

class AboutFragment: Fragment() {

    private val binding by autoClearedViewBinding(FragmentAboutBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    private fun setupView() {
        (requireActivity() as MainActivity).setSupportActionBar(binding.aboutFragmentToolbar)
        createText(getString(R.string.aboutVC_recognition_title), getString(R.string.aboutVC_recognition_message))
        createText(getString(R.string.aboutVC_general_title), getString(R.string.aboutVC_general_message))
        createText(getString(R.string.aboutVC_generalTerms_title), getString(R.string.aboutVC_generalTerms_message))
        createText(getString(R.string.aboutVC_qualityAssurance_title), getString(R.string.aboutVC_qualityAssurance_message))
        createText(getString(R.string.aboutVC_guidelines_title), getString(R.string.aboutVC_guidelines_message))
    }

    private fun createText(title: String, message: String) {
        val headerView = HeaderView(context, null)
        headerView.configure(title)

        val textView = TextView(context, null, 0, R.style.AppPrimary)
        textView.text = message

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = 8.dpToPx(context)
        params.marginEnd = 8.dpToPx(context)
        params.bottomMargin = 16.dpToPx(context)
        textView.layoutParams = params

        binding.aboutFragmentLinearLayout.addView(headerView)
        binding.aboutFragmentLinearLayout.addView(textView)
    }
}