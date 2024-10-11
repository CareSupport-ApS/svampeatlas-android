package com.noque.svampeatlas.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Location
import com.noque.svampeatlas.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

suspend fun File.getBitmap(): Result<Bitmap, AppError> = withContext(Dispatchers.IO) {
    try {
        if (!this@getBitmap.exists()) Result.Error<Bitmap, AppError>(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
        val bitmap = BitmapFactory.decodeFile(this@getBitmap.absolutePath)
        if (bitmap != null) {
            Result.Success(bitmap)
        } else {
            Result.Error(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
        }
    } catch (exception: IllegalArgumentException) {
        Result.Error(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
    }
}

fun File.getExifLocation(): Location? {
ExifInterface(this.inputStream()).apply {
    val lat = latLong?.first()
    val lng = latLong?.last()
    val date = getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it)
    }

    if (lat != null && lng != null) {
        return Location(date ?: Date(), LatLng(lat, lng), 65f)
    } else {
        return null
    }
}
}

suspend fun File.copyTo(file: File): Result<File, AppError> = withContext(Dispatchers.IO) {

    try {
        return@withContext Result.Success<File, AppError>(value = this@copyTo.copyTo(file, false))
    } catch (exception: NoSuchFileException) {
        Result.Error<File, AppError>(AppError("", "", null))
    } catch (exception: IOException) {
        Result.Error<File, AppError>(AppError("", "", null))
    }
    }


