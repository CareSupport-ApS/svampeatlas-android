package com.noque.svampeatlas.view_models

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.noque.svampeatlas.R
import com.noque.svampeatlas.fragments.CameraFragment
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Prediction
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RecognitionService
import com.noque.svampeatlas.utilities.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream




class CameraViewModel(private val type: CameraFragment.Context, application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "CameraViewModel"
    }

    private val recognitionService = RecognitionService()

    private val _imageFileState by lazy { MutableLiveData<State<File>>(State.Empty()) }
    val imageFileState: LiveData<State<File>> get() = _imageFileState

    private val _predictionResultsState by lazy { MutableLiveData<State<Pair<List<Prediction>, Boolean>>>(State.Empty()) }
    val predictionResultsState: LiveData<State<Pair<List<Prediction>, Boolean>>> get() = _predictionResultsState

    fun setImageFile(imageFile: File) {
        _imageFileState.postValue(State.Items(imageFile))
        if (type == CameraFragment.Context.IDENTIFY) {
            viewModelScope.launch { getPredictions(imageFile) }
        }
    }

    fun setImageFile(imageUri: Uri, file: File) {
        _imageFileState.postValue(State.Loading())
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().contentResolver.openInputStream(imageUri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    setImageFile(file)
                }
            } catch (exception: FileNotFoundException) {
                val res = getApplication<Application>().resources
                _imageFileState.postValue(State.Error(AppError(res.getString(R.string.elPhotosError_unknownFetchError_title), res.getString(R.string.elPhotosError_unknownFetchError_message), null)))
            }
        }
    }

    fun setImageFileError(error: AppError) {
        _imageFileState.postValue(State.Error(error))
    }

    fun reset() {
        _imageFileState.value?.let {
            if (it is State.Items) it.items.delete()
        }

        _imageFileState.value = State.Empty()
        _predictionResultsState.value = State.Empty()
        recognitionService.reset()
        DataService.clearRequestsWithTag(TAG)
    }

    private suspend fun getPredictions(imageFile: File) = withContext(Dispatchers.Default) {
       try {
           _predictionResultsState.postValue(State.Loading())
           recognitionService.addPhotoToRequest(imageFile)
           delay(2000)
           when (val result = recognitionService.getResults()) {
               is Result.Error -> _predictionResultsState.postValue(
                   State.Error(
                       result.error.toAppError(
                           MyApplication.resources
                       )
                   )
               )
               is Result.Success -> {
                   val predictions =
                       DataService.mushroomsRepository.fetchMushrooms(
                           result.value
                       )
                   _predictionResultsState.postValue(
                       State.Items(
                           Pair(
                               predictions,
                               result.value.reliablePrediction
                           )
                       )
                   )
               }
           }
       } catch (error: Exception) {
           Log.d(TAG, error.toString())
       }
    }
}