package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentPromptBinding
import com.noque.svampeatlas.utilities.autoClearedViewBinding

class PromptFragment: DialogFragment(R.layout.fragment_prompt) {

    companion object {
        const val REQUEST_KEY = "prompt_request"
        const val RESULT_KEY = "prompt_result"
        private const val KEY_TITLE = "key_title"
        private const val KEY_MESSAGE = "key_message"
         const val KEY_POSITIVE = "key_positive"
         const val KEY_NEGATIVE = "key_negative"

        fun newInstance(
            title: String,
            message: String,
            positive: String,
            negative: String
        ): PromptFragment {
            return PromptFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE, title)
                    putString(KEY_MESSAGE, message)
                    putString(KEY_POSITIVE, positive)
                    putString(KEY_NEGATIVE, negative)
                }
            }
        }
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