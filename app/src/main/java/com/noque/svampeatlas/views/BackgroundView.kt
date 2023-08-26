package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.databinding.ViewBackgroundBinding
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.RecoveryAction

class BackgroundView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {


    private val binding = ViewBackgroundBinding.inflate(LayoutInflater.from(context), this, false)

    fun setLoading() {
        binding.backgroundViewSpinnerView.startLoading()
    }

    fun setError(error: AppError) {
        binding.backgroundViewErrorViewTitleTextView.text = error.title
        binding.backgroundViewErrorViewMessageTextView.text = error.message
        binding.backgroundViewErrorViewLinearLayout.visibility = View.VISIBLE
    }

    fun setErrorWithHandler(error: AppError, recoveryAction: RecoveryAction?, handler: ((RecoveryAction?) -> Unit)) {
        binding.backgroundViewSpinnerView.stopLoading()
        binding.backgroundViewErrorViewLinearLayout.visibility = View.VISIBLE

        binding.backgroundViewErrorViewTitleTextView.text = error.title
        binding.backgroundViewErrorViewMessageTextView.text = error.message
        binding.backgroundViewHandlerButton.visibility = View.VISIBLE
        binding.backgroundViewHandlerButton.text = recoveryAction?.description(resources) ?: RecoveryAction.TRYAGAIN.description(resources)
        binding.backgroundViewHandlerButton.setOnClickListener {
            handler.invoke(recoveryAction)
        }
    }

    fun reset() {
        setBackgroundColor(Color.TRANSPARENT)
        binding.backgroundViewErrorViewLinearLayout.visibility = View.GONE
        binding.backgroundViewSpinnerView.stopLoading()
    }
}