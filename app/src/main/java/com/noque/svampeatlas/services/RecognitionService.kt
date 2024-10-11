package com.noque.svampeatlas.services

import android.util.Log
import com.android.volley.toolbox.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toDatabaseName
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

class RecognitionService {

    sealed class Error(title: Int, message: Int, recoveryAction: RecoveryAction?)  :
        AppError2(title, message, recoveryAction) {
            object NotInitialized: Error(R.string.recognitionServiceError_title, 0, null)
            object ErrorAddingData: Error(R.string.recognitionServiceError_title, R.string.recognitionServiceError_addingDataError_message, null)
            object ErrorFetchingResults: Error(R.string.recognitionServiceError_title, R.string.recognitionServiceError_addingDataError_message, null)
        }

    private var currentRequest: Deferred<String>? = null

    // This method
    suspend fun addPhotoToRequest(image: File) = withContext(Dispatchers.IO) {
        try {
            val result = image.getBitmap()
            result.onError { /* Handle error silently */ }
            result.onSuccess {
                val byteArray = it.rotate(image).toJPEG(0.6)
                val id = currentRequest?.await()

                currentRequest = async {
                    performAddPhotoRequest(id, byteArray)?.observationId ?: "Empty ID"
                }
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    fun reset() {
        currentRequest?.cancel(null)
        currentRequest = null
    }

    @Serializable
    data class AddPhotoRequestResult(@SerialName("observation_id")  val observationId: String)

    private suspend fun performAddPhotoRequest(id: String?, byteArray: ByteArray): AddPhotoRequestResult? = withContext(Dispatchers.Default) {
        try {
            val filename = "photo-${Calendar.getInstance().time.toDatabaseName()}.jpg"
            val data = MultiPartFormDataContent(
                formData {
                    append(
                        "image", // This should match what the server expects
                        byteArray,
                        Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"image\"; filename=\"$filename\"")
                            append(HttpHeaders.ContentType, ContentType.Image.JPEG.toString()) // Use the correct content type
                        }
                    )
                }
            )
            NetworkService.post(API(APIType.Post.ImagePredictionAddPhoto(id)), data)
        } catch (e: Exception) {
            Log.d("RECOGNITIONSERVICE", e.message ?: "ERROR")
            // Handle error silently
            null
        }
    }

    @Serializable
    data class MetadataRequest(
        val habitat: Int,
        val substrate: Int,
        val month: Int
    )
    suspend fun addMetadataToRequest(vegetationType: VegetationType, substrate: Substrate, date: Date) = withContext(Dispatchers.Default) {
        try {
            val id = currentRequest?.await() ?: return@withContext
            val metadataRequest = MetadataRequest(
                habitat = vegetationType.id,
                substrate = substrate.id,
                month = date.month
            )

            currentRequest = async { performAddMetadataToRequest(id, metadataRequest)?.observationId ?: "Empty ID" }
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    private suspend fun performAddMetadataToRequest(id: String, metadataRequest: MetadataRequest): AddPhotoRequestResult? = withContext(Dispatchers.Default) {
        try {
            NetworkService.post(API(APIType.Post.ImagePredictionAddMetaData(id)), metadataRequest)
        } catch (e: Exception) {
            null // Return null in case of failure
        }
    }

   @Serializable
    data class GetResultsRequestResult(@SerialName("taxon_ids") val taxonIds: List<Int> = listOf(), val conf: List<Double> = listOf(), @SerialName("reliable_preds") val reliablePrediction: Boolean)

   suspend fun getResults(): Result<GetResultsRequestResult, Error> = withContext(Dispatchers.IO) {
       try {
           val id = currentRequest?.await() ?: return@withContext Result.Error(Error.NotInitialized)
           if (id == "Empty ID") return@withContext Result.Error(Error.ErrorAddingData)
           var result: GetResultsRequestResult
           do {
               result = performGetResults(id)
               if (result.taxonIds.isEmpty()) delay(2000)
           } while (result.taxonIds.isNullOrEmpty())
           Result.Success(result)
       } catch (e: Exception) {
           Result.Error(Error.ErrorFetchingResults)
       }
    }

    private suspend fun performGetResults(id: String): GetResultsRequestResult = withContext(Dispatchers.IO) {
       NetworkService.get(API(APIType.Request.ImagePredictionGetResults(id)))
    }
}