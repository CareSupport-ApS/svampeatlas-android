package com.noque.svampeatlas.repositories

import com.android.volley.RequestQueue
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.serialization.builtins.ListSerializer
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SubstratesRepository(private val requestQueue: RequestQueue) {

    suspend fun getSubstrateGroups(tag: String, freshDownload: Boolean = false): Result<List<SubstrateGroup>, DataService.Error> {
        if (!freshDownload) RoomService.substrates.getSubstrates().onSuccess {
            return Result.Success(SubstrateGroup.createFromSubstrates(it))
        }



        return fetchSubstrateGroups("SubstratesRepository").apply {
            onSuccess {
                RoomService.substrates.save(it.flatMap { it.substrates })
            }
        }
     }

    private suspend fun fetchSubstrateGroups(tag: String): Result<List<SubstrateGroup>, DataService.Error> = suspendCoroutine { cont ->
        val api = API(APIType.Request.Substrate())

        val request = AppRequest(
            ListSerializer(Substrate.serializer()),
            api,
            null,
            null,
            {
                if (it.firstOrNull() != null) {
                    cont.resume(Result.Success(SubstrateGroup.createFromSubstrates(it)))
                } else {
                    cont.resume(Result.Error(DataService.Error.NotFound(
                        MyApplication.applicationContext
                    )))
                }
            },
            {
                cont.resume(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        requestQueue.add(request)
    }
}