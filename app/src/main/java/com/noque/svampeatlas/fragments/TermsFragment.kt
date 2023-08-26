package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentTermsBinding
import com.noque.svampeatlas.extensions.loadGif
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoClearedViewBinding

class TermsFragment: DialogFragment() {

    companion object {
        const val KEY_TYPE = "KEY_TERMFRAGMENT_TYPE"
    }

    enum class Type {
        IDENTIFICATION,
        LOCALITYHELPER,
        CAMERAHELPER,
        WHATSNEW,
        IMAGEDELETIONS
    }

    interface Listener {
        fun onDismiss(termsAccepted: Boolean)
    }


    private lateinit var type: Type
    var listener: Listener? = null


    // Views
    private val binding by autoClearedViewBinding(FragmentTermsBinding::bind)

    // Listeners

    private val acceptButtonPressed by lazy {
        View.OnClickListener {
            when (type) {
                Type.IDENTIFICATION -> {
                    SharedPreferences.setHasAcceptedIdentificationTerms(true)
                }
                Type.IMAGEDELETIONS -> {
                    SharedPreferences.hasSeenImageDeletion = true
                }
                Type.WHATSNEW -> {
                    SharedPreferences.hasSeenWhatsNew = true
                }
                Type.LOCALITYHELPER -> {
                    SharedPreferences.setHasShownPositionReminder()
                }
                else -> {}
            }

            listener?.onDismiss(termsAccepted = true)
            dismiss()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        type = arguments?.getSerializable(KEY_TYPE) as Type
        setupViews()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }


    private fun setupViews() {
        binding.termsFragmentAcceptButton.setOnClickListener(acceptButtonPressed)

        when (type) {
            Type.IDENTIFICATION -> {
                binding.termsFragmentHeaderView.configure(getString(R.string.termsVC_mlPredict_title))
                binding.termsFragmentContentTextView.text = getString(R.string.termsVC_mlPredict_message)
            }
            Type.CAMERAHELPER -> {
                binding.termsFragmentHeaderView.configure(getString(R.string.termsVC_cameraHelper_title))
                binding.termsFragmentContentTextView.text = getString(R.string.termsVC_cameraHelper_message)
            }
            Type.WHATSNEW -> {
                binding.termsFragmentHeaderView.configure(getString(R.string.whats_new_title))
                binding.termsFragmentContentTextView.text = getString(R.string.whats_new_3_0)
            }
            Type.IMAGEDELETIONS -> {
                binding.termsFragmentHeaderView.configure(getString(R.string.message_deletionsAreFinal))
                binding.termsFragmentContentTextView.text = getString(R.string.message_imageDeletetionsPermanent)
            }
            Type.LOCALITYHELPER -> {
                binding.termsFragmentImageView.loadGif(R.drawable.locality_helper)
                binding.termsFragmentHeaderView.configure(resources.getString(R.string.modal_localityHelper_title))
                binding.termsFragmentContentTextView.text = resources.getString(R.string.modal_localityHelper_message)
            }
        }
    }
}