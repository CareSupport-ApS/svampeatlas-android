package com.noque.svampeatlas.utilities

import android.app.Activity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

object ToastHelper {
    fun Fragment.handleError(error: AppError) {
       handleError(error.title, error.message)
    }

    fun Fragment.handleError(title: String, message: String) {
        MotionToast.createColorToast(
            requireActivity(), title, message, MotionToastStyle.ERROR, MotionToast.GRAVITY_BOTTOM,
            MotionToast.LONG_DURATION, ResourcesCompat.getFont(requireActivity(), R.font.avenir_next_medium)
        )
    }

    fun Fragment.handleSuccess(title: String, message: String) {
        MotionToast.createColorToast(
            requireActivity(), title, message, MotionToastStyle.SUCCESS, MotionToast.GRAVITY_BOTTOM,
            MotionToast.LONG_DURATION, ResourcesCompat.getFont(requireActivity(), R.font.avenir_next_medium)
        )
    }
}