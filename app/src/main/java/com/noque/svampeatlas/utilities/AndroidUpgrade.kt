package com.noque.svampeatlas.utilities

import android.app.Activity
import android.content.Context
import android.util.Log
import com.appupgrade.app_upgrade_android_sdk.AppUpgrade
import com.appupgrade.app_upgrade_android_sdk.models.AlertDialogConfig
import com.appupgrade.app_upgrade_android_sdk.models.AppInfo
import com.noque.svampeatlas.BuildConfig
import java.util.Locale

object AndroidUpgrade {
    private val appUpgrade = AppUpgrade()
    fun setup(activity: Activity) {
        val xApiKey = "YzA3MTM3OTYtNTRlMi00Mzg2LTg3NzAtMDc4ZTE3OGNjNDBh"
        val appInfo = AppInfo(
            appId = BuildConfig.APPLICATION_ID,
            appName = "Svampeatlas",
            appVersion = BuildConfig.VERSION_NAME,
            platform = "android",
            environment = "production",
            appLanguage = Locale.getDefault().language
        )

        val alertDialogConfig = AlertDialogConfig(
            title = "Update Required", //Default: Please Update
            updateButtonTitle = "Update Now", //Default: Update Now
            laterButtonTitle = "Not Now" //Default: Later
        )
        Log.d("App Upgrade", appInfo.toString())
        appUpgrade.checkForUpdates(activity, xApiKey, appInfo, alertDialogConfig)
    }
}