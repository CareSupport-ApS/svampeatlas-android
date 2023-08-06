package com.noque.svampeatlas.utilities

import android.app.Activity
import androidx.core.content.res.ResourcesCompat
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

object ToastHelper {
     fun handleError(activity: Activity, error: AppError) {
        MotionToast.createColorToast(activity, error.title, error.message, MotionToastStyle.ERROR,  MotionToast.GRAVITY_BOTTOM,
            MotionToast.LONG_DURATION,  ResourcesCompat.getFont(activity, R.font.avenir_next_medium))
    }
}