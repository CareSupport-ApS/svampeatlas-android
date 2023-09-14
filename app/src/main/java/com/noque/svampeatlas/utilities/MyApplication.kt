package com.noque.svampeatlas.utilities

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.downloader.PRDownloader
import com.noque.svampeatlas.services.FileManager
import org.aviran.cookiebar2.CookieBar


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
        super.onCreate()
    }

    override fun onTerminate() {
        FileManager.clearTemporaryFiles()
        super.onTerminate()
    }
}