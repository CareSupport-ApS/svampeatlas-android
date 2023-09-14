package com.noque.svampeatlas.fragments.modals

import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R

class SpinnerFragment: DialogFragment(R.layout.fragment_spinner) {

    override fun onStart() {
        super.onStart()
        isCancelable = false
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

}