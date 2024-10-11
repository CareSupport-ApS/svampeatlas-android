package com.noque.svampeatlas.repositories

import com.android.volley.RequestQueue
import com.android.volley.Response
import com.noque.svampeatlas.extensions.toAppError2
import com.noque.svampeatlas.models.AppError2
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.services.ImageService
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppEmptyRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppJSONObjectRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ObservationsRepository(private val requestQueue: RequestQueue) {

    suspend fun editObservation(id: Int, token: String, jsonObject: JSONObject, imageFiles: List<File>?): Result<Pair<Int, Int>, AppError2> {
        return when (val result = put(id, jsonObject, token)) {
            is Result.Success -> Result.Success(Pair(id, postImagesToObservation(id, imageFiles, token)))
            is Result.Error -> Result.Error(result.error)
        }
    }

    suspend fun uploadObservation(tag: String, token: String, jsonObject: JSONObject, imageFiles: List<File>?): Result<Pair<Int, Int>, AppError2> {
        return when (val result = post(jsonObject, token)) {
           is Result.Success -> Result.Success(Pair(result.value,postImagesToObservation(result.value, imageFiles, token)))
           is Result.Error -> {
               Result.Error(result.error)
           }
       }
    }

    suspend fun deleteObservation(id: Int, token: String): Result<Void?, AppError2> {
        return delete(id, token)
    }

    suspend fun getObservation(id: Int): Result<Observation, AppError2> {
    return get(id)
    }

    private suspend fun get(id: Int): Result<Observation, AppError2> = suspendCoroutine { cont ->
        val request = AppRequest(
           Observation.serializer(),
            API(APIType.Request.SingleObservation(id)),
            null,
            null,
            {
                cont.resume(Result.Success(it))
            },

            {
                cont.resume(Result.Error(it.toAppError2()))
            }
        )
        requestQueue.add(request)
    }

    private suspend fun post(json: JSONObject, token: String): Result<Int, AppError2> = suspendCoroutine { cont ->
        val request = AppJSONObjectRequest(
            API(APIType.Post.Observation),
            token,
            json,
            {
                val id = it.getInt("_id")
                cont.resume(Result.Success(id))
            },
            {
                cont.resume(Result.Error(it.toAppError2()))
            })

        requestQueue.add(request)
    }


    private suspend fun put(id: Int, json: JSONObject, token: String): Result<Void?, AppError2> = suspendCoroutine { cont ->
        val request = AppJSONObjectRequest(
            API(APIType.Put.Observation(id)),
            token,
            json,
            {
                cont.resume(Result.Success(null))
            },
            {
                cont.resume(Result.Error(it.toAppError2()))
            })

        requestQueue.add(request)
    }

    private suspend fun delete(id: Int, token: String): Result<Void?, AppError2> = suspendCoroutine { cont ->
        val request = AppEmptyRequest(
            API(APIType.Delete.Observation(id)),
            token,
            {
                cont.resume(Result.Success(null))
            },
            {
                cont.resume(Result.Error(it.toAppError2()))
            })

        requestQueue.add(request)
    }

    private suspend fun postImagesToObservation(id: Int, imageFiles: List<File>?, token: String): Int {
        if (imageFiles == null || imageFiles.isEmpty()) return 0
        var completedUploads = 0
        imageFiles.forEach {
            ImageService.uploadImage(requestQueue, id, it, token)
            completedUploads += 1
        }
        return completedUploads
    }
}