package com.noque.svampeatlas.services

import android.util.Log
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.volleyRequests.Json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json

object NetworkService {
    private var bearerToken: String? = null

    val client by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json)
            }
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        Log.d("HttpLogging:", message)
                    }
                }
                level = LogLevel.BODY
            }

            install(ResponseObserver) {
                onResponse { response ->
                    Log.d("HTTP status:", "${response.status.value}")
                }
            }

            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                bearerToken?.let {
                    header(HttpHeaders.Authorization, "Bearer $it")
                }
            }


        }
    }

    fun setBearerToken(token: String?) {
        bearerToken = token
    }

    // Public PUT function (supports multipart, JSON, and string)
    suspend inline fun <reified T> get(api: API): T {
        return performRequest(api, HttpMethod.Get, null)
    }

    // Public PUT function (supports multipart, JSON, and string)
    suspend inline fun <reified T> put(api: API, body: Any): T {
        return performRequest(api, HttpMethod.Put, body)
    }

    // Public POST function (supports multipart, JSON, and string)
    suspend inline fun <reified T> post(api: API, body: Any): T {
        return performRequest(api, HttpMethod.Post, body)
    }


    // Function to determine content type and perform request
    suspend inline fun <reified T> performRequest(
        api: API,
        method: HttpMethod,
        body: Any?
    ): T {
        return client.request(api.url()) {
            this.method = method
            if (body != null) {
               setBody(body)
            }
        }.body()
    }


}