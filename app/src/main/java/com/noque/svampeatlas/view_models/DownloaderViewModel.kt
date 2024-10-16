package com.noque.svampeatlas.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.AppError2
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.api.SpeciesQueries
import com.noque.svampeatlas.utilities.volleyRequests.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.FileNotFoundException


class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    sealed class LoadingState(val resID: Int) {
        object DownloadingTaxon : LoadingState(R.string.downloader_message_taxon)
        object ReadingFile : LoadingState(R.string.downloader_readingFile)
        object DownloadingMetadata : LoadingState(R.string.downloader_message_data)
        object SavingFile : LoadingState(R.string.message_savingToStorage)
    }

    sealed class Error(title: Int, message: Int, recoveryAction: RecoveryAction?) :
        AppError2(title, message, recoveryAction) {
        object InternetError : Error(
            R.string.urlSessionError_noInternet_title,
            R.string.urlSessionError_noInternet_message,
            RecoveryAction.TRYAGAIN
        )
        object Unknown: Error(R.string.databaseError_saveError_title, R.string.dataServiceError_unknown_message, RecoveryAction.TRYAGAIN)
    }


    private val _loadingState by lazy { MutableLiveData<LoadingState>() }
    val loadingState: LiveData<LoadingState> get() = _loadingState

    private val _state by lazy { MutableLiveData<State<Void?>>(State.Empty()) }
    val state: LiveData<State<Void?>> get() = _state

    init {
        startDownload()
    }

    fun startDownload() {
        val file = FileManager.createDocumentFile("Taxon", getApplication())
        val api = API(APIType.Request.Mushrooms(null, listOf(SpeciesQueries.Attributes(null), SpeciesQueries.Images(false), SpeciesQueries.Statistics, SpeciesQueries.DanishNames), 0, null))

        _state.value = State.Loading()
        _loadingState.value = LoadingState.DownloadingTaxon
        PRDownloader.download(api.url(), file.parent, file.name).build().start(object: OnDownloadListener {
            override fun onDownloadComplete() {
                viewModelScope.launch {
                    readFile(file)
                    _loadingState.value = LoadingState.DownloadingMetadata
                    DataService.substratesRepository.getSubstrateGroups("Downloader", true)
                    DataService.vegetationTypeRepository.getVegetationTypes("Downloader", true)
                    _state.postValue(State.Items(null))
                }
            }

            override fun onError(error: com.downloader.Error?) {
                if (error != null && error.isConnectionError) {
                    _state.postValue(State.Error(Error.InternetError.toAppError(getApplication<MyApplication>().resources)))
                } else {
                    _state.postValue(State.Error(Error.Unknown.toAppError(getApplication<MyApplication>().resources)))
                }
            }
        })
    }

    suspend fun readFile(file: File) = withContext(Dispatchers.IO) {
        _loadingState.postValue(LoadingState.ReadingFile)
        try {
            file.inputStream().use { inputStream ->
                val mushrooms = Json.decodeFromStream<List<Mushroom>>(inputStream) // Stream the JSON data

                _loadingState.postValue(LoadingState.SavingFile)
                RoomService.mushrooms.save(mushrooms)
            }
            file.delete()
        } catch (error: FileNotFoundException) {
            _state.postValue(State.Error(AppError(getApplication<MyApplication>().resources.getString(R.string.dataServiceError_unknown_title), error.localizedMessage ?: "", RecoveryAction.TRYAGAIN)))
        }
    }

    override fun onCleared() {
        super.onCleared()
        PRDownloader.cancelAll()
    }
}