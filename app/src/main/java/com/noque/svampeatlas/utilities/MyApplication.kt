package com.noque.svampeatlas.utilities

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.downloader.PRDownloader
import com.noque.svampeatlas.services.FileManager
import com.onesignal.OneSignal


class MyApplication: Application() {

    companion object {
        lateinit var applicationContext: Context
        private set
            lateinit var resources: Resources
            private set
    }


    override fun onCreate() {
        MyApplication.applicationContext = applicationContext
        MyApplication.resources = resources
        SharedPreferences.init(applicationContext)
        PRDownloader.initialize(applicationContext);

        OneSignal.initWithContext(this, "0ad5c80b-f057-4651-bbcc-fe0a612d13ae")

        super.onCreate()
    }

    override fun onTerminate() {
        FileManager.clearTemporaryFiles()
        super.onTerminate()
    }
}