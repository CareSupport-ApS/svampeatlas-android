package com.noque.svampeatlas.extensions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.utilities.MyApplication
import org.aviran.cookiebar2.CookieBar
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle

fun Fragment.openSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
    intent.data = uri
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}

fun Fragment.showSpinner() {
    findNavController().navigate(R.id.spinnerFragment)
}

fun Fragment.hideSpinner() {
    findNavController().popBackStack(R.id.spinnerFragment, true)
}

fun Fragment.handleError(error: AppError) {
    hideSpinner()
    handleError(error.title, error.message)
}

fun Fragment.handleError(title: String, message: String) {
    val activity = requireActivity()
    CookieBar.build(activity)
        .setTitle(title)
        .setIcon(R.drawable.icon_elmessageview_failure)
        .setMessage(message)
        .setBackgroundColor(R.color.colorRed)
        .setDuration(7000) // 5 seconds
        .setEnableAutoDismiss(false)
        .setSwipeToDismiss(false)
        .setAction("OK") { CookieBar.dismiss(activity) }
        .setCookiePosition(Gravity.BOTTOM)
        .show()
}

fun Fragment.handleSuccess(title: String, message: String) {
    val activity = requireActivity()
    CookieBar.build(activity)
        .setTitle(title)
        .setIcon(R.drawable.icon_elmessageview_success)
        .setMessage(message)
        .setBackgroundColor(R.color.colorGreen)
        .setDuration(5000) // 5 seconds
        .setEnableAutoDismiss(true)
        .setSwipeToDismiss(true)
        .setAction("OK") { CookieBar.dismiss(activity) }
        .setCookiePosition(Gravity.BOTTOM)
        .show()
}

fun Fragment.handleInfo(title: String, message: String) {
    val activity = requireActivity()
    CookieBar.build(activity)
        .setTitle(title)
        .setBackgroundColor(R.color.colorPrimary)
        .setDuration(3000)
        .setMessage(message)// 5 seconds
        .setEnableAutoDismiss(true)
        .setSwipeToDismiss(true)
        .setAction("OK") { CookieBar.dismiss(activity) }
        .setCookiePosition(Gravity.BOTTOM)
        .show()
}