package com.noque.svampeatlas.services

import com.noque.svampeatlas.utilities.api.API
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.put
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.contentType

object KtorService {
    private var bearerToken: String? = null
    val httpClient: HttpClient by lazy {
        HttpClient {
            install(JsonFeature) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                serializer = KotlinxSerializer(json)
            }
            install(DefaultRequest) {
                if (bearerToken != null) {
                    headers.append("Authorization", "Bearer $bearerToken")
                }
            }
        }
    }

    fun setBearerToken(token: String?) {
        bearerToken = token
    }

    suspend inline fun <reified T>put(api: API, putBody: Any): T {
        return httpClient.put(api.url()) {
            contentType(ContentType.Application.Json)
            body = putBody
        }
    }
}