package com.noque.svampeatlas.services

import android.graphics.Bitmap
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.utilities.MultipartFormImage
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppJSONObjectRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppMultipartPost
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.lang.reflect.Type
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class RecognitionService {

    sealed class Error(title: Int, message: Int, recoveryAction: RecoveryAction?)  :
        AppError2(title, message, recoveryAction) {
            object NotInitialized: Error(R.string.recognitionServiceError_title, 0, null)
            object ErrorAddingData: Error(R.string.recognitionServiceError_title, R.string.recognitionServiceError_addingDataError_message, null)
            object ErrorFetchingResults: Error(R.string.recognitionServiceError_title, R.string.recognitionServiceError_addingDataError_message, null)
        }

    companion object {
        private val TAG = "RecognitionService"
    }

    private val requestQueue = RequestQueue(NoCache(), BasicNetwork(HurlStack())).apply {
        start()
    }

    private var currentRequest: Deferred<String>? = null


    suspend fun addPhotoToRequest(image: File) = withContext(Dispatchers.IO) {
           val result = image.getBitmap()
           result.onError { /*completion(Result.Error(it))*/ }
           result.onSuccess {
               val byteArray = it.rotate(image).toJPEG(0.6)
               currentRequest = if (currentRequest == null) {
                   async { performAddPhotoRequest(null, byteArray) }
               } else {
                   val id = currentRequest?.await() ?: "Empty ID0"
                   async { performAddPhotoRequest(id, byteArray) }
               }
           }
    }

    fun reset() {
        currentRequest?.cancel(null)
        currentRequest = null
    }

    data class AddPhotoRequestResult(@SerializedName("observation_id")  val observationId: String)

    private suspend fun performAddPhotoRequest(id: String?, byteArray: ByteArray): String = suspendCoroutine { cont ->
            val request = AppMultipartPost(
                API(APIType.Post.ImagePredictionAddPhoto(id)),
                null,
                MultipartFormImage(byteArray, "image"),
                {
                    val result = Gson().fromJson(String(it.data), AddPhotoRequestResult::class.java)
                    cont.resume(result.observationId)
                },
                {
                    cont.resumeWithException(it)
                })

            requestQueue.add(request)
    }


    suspend fun addMetadataToRequest(vegetationType: VegetationType, substrate: Substrate, date: Date) = withContext(Dispatchers.Default) {
        val id = currentRequest?.await()
        // If no ID, then we do not want to add metadata
        if (id != null) {
            val json = JSONObject().apply {
                put("habitat", vegetationType.id)
                put("substrate", substrate.id)
                put("month", date.month)
            }

            currentRequest = async { performAddMetadataToRequest(id, json) }
        }
    }

    private suspend fun performAddMetadataToRequest(id: String, json: JSONObject) = suspendCoroutine {  cont ->
        val request = AppRequest<AddPhotoRequestResult>( object : TypeToken<AddPhotoRequestResult>() {}.type, API(APIType.Post.ImagePredictionAddMetaData(id)), null, json,
            {
                cont.resume(it.observationId)
            },
            {
                cont.resumeWithException(it)
            })
        requestQueue.add(request)
    }

   data class GetResultsRequestResult(@SerializedName("taxon_ids") val taxonIds: List<Int> = listOf(), val conf: List<Double> = listOf(), @SerializedName("reliable_preds") val reliablePrediction: Boolean)

   suspend fun getResults(): Result<GetResultsRequestResult, Error> {
        val id = currentRequest?.await() ?: return Result.Error(Error.NotInitialized)
       var result: GetResultsRequestResult
       do {
            result = performGetResults(id)
            if (result.taxonIds.isEmpty()) delay(2000)
        } while (result.taxonIds.isNullOrEmpty())
return Result.Success(result)
    }

    private suspend fun performGetResults(id: String): GetResultsRequestResult = suspendCoroutine { conf ->
        val request = AppRequest<GetResultsRequestResult>( object : TypeToken<GetResultsRequestResult>() {}.type, API(APIType.Request.ImagePredictionGetResults(id)), null, null, {
            conf.resume(it)
        }, {
    conf.resumeWithException(it)
        })
        requestQueue.add(request)
    }
}