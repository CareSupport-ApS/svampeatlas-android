package com.noque.svampeatlas.fragments.modals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.noque.svampeatlas.R
import com.noque.svampeatlas.databinding.FragmentModalDownloadBinding
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoClearedViewBinding
import com.noque.svampeatlas.view_models.DownloaderViewModel


class DownloaderFragment: DialogFragment(R.layout.fragment_modal_download) {

    private val binding by autoClearedViewBinding(FragmentModalDownloadBinding::bind)
    private val viewModel by viewModels<DownloaderViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupViewModels()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupViews() {
        binding.downloaderFragmentTitleTextView.text = getString(R.string.downloader_message_data)
    }

    private fun setupViewModels() {
        viewModel.state.observe(viewLifecycleOwner) {
            isCancelable = true
            when (it) {
                is State.Items -> {
                    SharedPreferences.databaseWasUpdated()
                    dismiss()
                }

                is State.Empty -> {}
                is State.Loading -> {
                    isCancelable = false
                    binding.downloaderFragmentErrorView.setLoading()
                }

                is State.Error -> {
                    binding.downloaderFragmentMessageTextView.text = null
                    binding.downloaderFragmentErrorView.setErrorWithHandler(
                        it.error,
                        it.error.recoveryAction
                    ) {
                        viewModel.startDownload()
                    }
                }
            }
        }

        viewModel.loadingState.observe(viewLifecycleOwner) {
            binding.downloaderFragmentMessageTextView.text = getString(it.resID)
        }
    }
}