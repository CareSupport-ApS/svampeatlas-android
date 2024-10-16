package com.noque.svampeatlas.utilities.volleyRequests

import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.noque.svampeatlas.utilities.api.API
import kotlinx.serialization.SerializationException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

class AppEmptyRequest(
    private val api: API,
    private val token: String?,
    private val listener: Response.Listener<Void>,
    errorListener: Response.ErrorListener
): Request<Void>(api.volleyMethod(), api.url(), errorListener) {
    override fun getHeaders(): MutableMap<String, String> {
        val mutableMap = mutableMapOf(Pair("Content-Type", "application/json"))

        token?.let {
            mutableMap.put("Authorization", "Bearer $it")
        }

        return mutableMap
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<Void> {
        return Response.success(null, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: Void?) {
        listener.onResponse(response)
    }
}

open class AppBaseRequest<T>(private val endpoint: API, private val token: String?, private val listener: Response.Listener<T>, errorListener: Response.ErrorListener): Request<Void>(endpoint.volleyMethod(), endpoint.url(), errorListener) {
    override fun getHeaders(): MutableMap<String, String> {
        val mutableMap = mutableMapOf(Pair("Content-Type", "application/json"))

        token?.let {
            mutableMap.put("Authorization", "Bearer $it")
        }

        return mutableMap
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<Void> {
        return Response.success(null, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: Void?) {
        TODO("Not yet implemented")
    }
}

class AppJSONObjectRequest(private val endpoint: API,
                           private val token: String?,
                           private val jsonObject: JSONObject,
                           private val listener: Response.Listener<JSONObject>,
                           errorListener: Response.ErrorListener): Request<JSONObject>(endpoint.volleyMethod(), endpoint.url(), errorListener) {

    override fun getBody(): ByteArray {
        return jsonObject.toString().toByteArray()
    }


    override fun getHeaders(): MutableMap<String, String> {
        val mutableMap = mutableMapOf(Pair("Content-Type", "application/json"))

        token?.let {
            mutableMap.put("Authorization", "Bearer $it")
        }

        return mutableMap
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
        return try {
            val json = String(response?.data ?: ByteArray(0),
                Charset.forName(HttpHeaderParser.parseCharset(response?.headers)))
            Response.success(JSONObject(json), HttpHeaderParser.parseCacheHeaders(response))
        } catch (error: UnsupportedEncodingException) {
            Response.error(ParseError(error))
        } catch (error: SerializationException) {
            Response.error(ParseError(error))
        }
    }

    override fun deliverResponse(response: JSONObject?) {
        listener.onResponse(response)
    }

}