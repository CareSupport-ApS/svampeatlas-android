package com.noque.svampeatlas.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.RecoveryAction
import kotlin.properties.Delegates


@SuppressLint("MissingPermission")
class LocationService(private val applicationContext: Context) {

    companion object {
        private const val TAG = "LocationService"
        private const val REQUEST_CODE = 210
        private const val DESIRED_ACCURACY = 60
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction): AppError(title, message,
        recoveryAction
    ) {
        class PermissionDenied(resources: Resources): Error(resources.getString(R.string.locationManagerError_permissionDenied_title), resources.getString(R.string.locationManagerError_permissionDenied_message), RecoveryAction.OPENSETTINGS)
        class PermissionsUndetermined(resources: Resources): Error(resources.getString(R.string.locationManagerError_permissionsUndetermined_title), resources.getString(R.string.locationManagerError_permissionsUndetermined_message), RecoveryAction.ACTIVATE)
        class BadAccuracy(resources: Resources): Error(resources.getString(R.string.locationManagerError_badAccuracy_title), resources.getString(R.string.locationManagerError_badAccuracy_message), RecoveryAction.TRYAGAIN)
    }

    enum class State {
        LOCATING,
        STOPPED
    }


    interface Listener {
        fun locationRetrieved(location: Location)
        fun locationRetrievalError(error: Error)
        fun isLocating()
        fun requestPermission(permissions: Array<out String>, requestCode: Int)
    }

    private var handler = Handler()
    private var locationClient: FusedLocationProviderClient? = null
    private var latestLocation: Location? = null
    private var listener: Listener? = null
    private var runnable: Runnable? = null

    private var state: State by Delegates.observable(State.STOPPED) { _, _, newValue ->
        when (newValue) {
            State.LOCATING -> {
                listener?.isLocating()
                locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
                val locationRequest = LocationRequest.create()
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                locationRequest.interval = 10
                locationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
            }

            State.STOPPED -> {
                latestLocation = null
                locationClient?.removeLocationUpdates(locationCallback)
                locationClient = null
                runnable?.let { handler.removeCallbacks(it) }
                runnable = null
            }
        }
    }


    private val permissionsNotDetermined: Boolean get() {
        return (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0.lastLocation?.let {
                if (it.accuracy > 0 && (SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos) < 5000000000) {
                    latestLocation = it
                    if (it.accuracy <= DESIRED_ACCURACY) stopServiceAndSendLocation()
                }
            }
        }
    }


    fun setListener(locationServiceDelegate: Listener?) {
        listener = locationServiceDelegate
    }

    fun start() {
        if (permissionsNotDetermined) {
            startUpdatingLocation()
        } else {
            requestPermissions()
        }
    }

    fun stop() {
        state = State.STOPPED
    }


    private fun startUpdatingLocation() {
        if (state == State.STOPPED) {
            state = State.LOCATING

            runnable = Runnable {
                if (state == State.LOCATING) {
                    stopServiceAndSendLocation()
                }
            }

            runnable?.let {
                handler.postDelayed(it, 10000)
            }
        }
    }

    private fun stopServiceAndSendLocation() {
        if (state == State.LOCATING) {
            val latestLocation = latestLocation
            state = State.STOPPED

            if (latestLocation != null) {
                listener?.locationRetrieved(latestLocation)
            } else {
                listener?.locationRetrievalError(Error.BadAccuracy(applicationContext.resources))
            }
        }
    }


    private fun requestPermissions() {
            listener?.requestPermission(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
                start()
            } else {
                listener?.locationRetrievalError(Error.PermissionDenied(applicationContext.resources))
            }
        }
    }
}