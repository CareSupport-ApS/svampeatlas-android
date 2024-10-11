package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentPromptBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding

class PromptFragment: DialogFragment(R.layout.fragment_prompt) {
    companion object {
        const val REQUEST_KEY = "prompt_request"
        const val RESULT_KEY = "prompt_result"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_MESSAGE = "KEY_MESSAGE"
        const val KEY_POSITIVE = "KEY_POSITIVE"
        const val KEY_NEGATIVE = "KEY_NEGATIVE"
    }

// Views
private val binding by autoClearedViewBinding(FragmentPromptBinding::bind)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        binding.promptFragmentTitleTextView.text = arguments?.getString(KEY_TITLE)
        binding.promptFragmentMessageTextView.text = arguments?.getString(KEY_MESSAGE)
        binding.promptFragmentPositiveButton.text = arguments?.getString(KEY_POSITIVE)
        binding.promptFragmentNegativeButton.text = arguments?.getString(KEY_NEGATIVE)

        binding.promptFragmentPositiveButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(RESULT_KEY, KEY_POSITIVE)
            })
            dismiss()
        }

        binding.promptFragmentNegativeButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putString(RESULT_KEY, KEY_NEGATIVE)
            })
            dismiss()
        }
    }
}