package com.noque.svampeatlas.services

import android.content.Context
import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.getBitmap
import com.noque.svampeatlas.extensions.rotate
import com.noque.svampeatlas.extensions.toAppError
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Comment
import com.noque.svampeatlas.models.GeoNames
import com.noque.svampeatlas.models.GsonNotifications
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Notification
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.models.User
import com.noque.svampeatlas.repositories.MushroomRepository
import com.noque.svampeatlas.repositories.ObservationsRepository
import com.noque.svampeatlas.repositories.SubstratesRepository
import com.noque.svampeatlas.repositories.VegetationTypeRepository
import com.noque.svampeatlas.utilities.MultipartFormImage
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.api.API
import com.noque.svampeatlas.utilities.api.APIType
import com.noque.svampeatlas.utilities.api.Geometry
import com.noque.svampeatlas.utilities.api.ObservationQueries
import com.noque.svampeatlas.utilities.api.SpeciesQueries
import com.noque.svampeatlas.utilities.volleyRequests.AppEmptyRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppJSONObjectRequest
import com.noque.svampeatlas.utilities.volleyRequests.AppMultipartPost
import com.noque.svampeatlas.utilities.volleyRequests.AppRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import org.json.JSONObject
import java.io.File

object DataService {
    const val TAG = "DATASERVICE"


    sealed class Error(title: String, message: String) : AppError(title, message, null) {
        class VolleyError(title: String, message: String) : Error(title, message)
        class SearchResponseEmpty(context: Context) : Error(
            context.getString(R.string.dataServiceError_searchResponseEmpty_title),
            context.getString(R.string.dataServiceError_searchResponseEmpty_message)
        )

        class LoginError(context: Context) : Error(
            context.getString(R.string.dataServiceError_loginError_title),
            context.getString(R.string.dataServiceError_loginError_message)
        )

        class NotFound(context: Context) : Error(
            context.getString(R.string.dataServiceError_empty_title),
            context.getString(R.string.dataServiceError_empty_message)
        )

        class UnknownError(context: Context) : Error(context.getString(R.string.dataServiceError_unknown_title), context.getString(R.string.dataServiceError_unknown_message))
        class ExtractionError() : Error("", "")
    }

    enum class ImageSize(val value: String) {
        FULL(""), MINI("https://svampe.databasen.org/unsafe/175x175/")
    }

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(MyApplication.applicationContext)
    }

    val mushroomsRepository by lazy {MushroomRepository(requestQueue)}
    val substratesRepository by lazy { SubstratesRepository(requestQueue) }
    val vegetationTypeRepository by lazy { VegetationTypeRepository(requestQueue) }
    val observationsRepository by lazy { ObservationsRepository(requestQueue) }

    private fun <RequestType> addToRequestQueue(request: Request<RequestType>) {
        requestQueue.add(request)
    }


    private val applicationContext = MyApplication.applicationContext

    fun getMushrooms(
        tag: String,
        searchString: String?,
        queries: List<SpeciesQueries>,
        offset: Int = 0,
        limit: Int = 100,
        completion: (Result<List<Mushroom>, Error>) -> Unit
    ) {
        val request = AppRequest(
            ListSerializer(Mushroom.serializer()),
            API(
                APIType.Request.Mushrooms(
                    searchString,
                    queries,
                    offset,
                    limit
                )
            ),
            null,
            null,
            {
                if (it.isEmpty()) {
                    completion(Result.Error(Error.SearchResponseEmpty(applicationContext)))
                } else {
                    completion(Result.Success(it))
                }
            },
            {
                completion(Result.Error(it.toAppError()))
            })
        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservationsWithin(
        tag: String,
        geometry: Geometry,
        taxonID: Int? = null,
        ageInYear: Int? = null,
        completion: (Result<List<Observation>, AppError>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                geometry,
                listOf(
                    ObservationQueries.Locality,
                    ObservationQueries.DeterminationView(taxonID),
                    ObservationQueries.Images,
                    ObservationQueries.GeomNames,
                    ObservationQueries.User(null)
                ),
                ageInYear,
                null,
                null
            )
        )
        val request = AppRequest(
            ListSerializer(Observation.serializer()),
            api,
            null,
            null,
            {
                completion(Result.Success(it))
            },
            {
                completion(Result.Error(it.toAppError()))
            })

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getRecentObservations(
        tag: String,
        offset: Int,
        taxonID: Int,
        completion: (Result<List<Observation>, Error>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                null,
                listOf(
                    ObservationQueries.Images,
                    ObservationQueries.DeterminationView(taxonID),
                    ObservationQueries.GeomNames,
                    ObservationQueries.Locality,
                    ObservationQueries.User(null)
                ),
                null,
                10,
                offset
            )
        )

        val request = AppRequest(
            ListSerializer(Observation.serializer()),
            api,
            null,
            null,
            {
                completion(Result.Success(it))
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getLocalities(
        tag: String,
        coordinate: LatLng,
        radius: API.Radius = API.Radius.SMALLEST,
        completion: (Result<List<Locality>, Error>) -> Unit
    ) {

        if (radius == API.Radius.COUNTRY) {
            val api = API(APIType.Request.GeomNames(coordinate))
            val request = AppRequest(
             GeoNames.serializer(),
                api,
                null,
                null,
                {
                    val localities = it.geoNames.map {
                        Locality(
                            it.geonameId,
                            "${it.name}, ${it.countryName}",
                            null,
                            it.lat.toDouble(),
                            it.lng.toDouble(),
                            it
                        )
                    }
                    completion(Result.Success(localities))
                },
                {
                    completion(Result.Error(it.toAppError()))
                }
            )

            request.tag = tag
            addToRequestQueue(request)

        } else {
            val api = API(
                APIType.Request.Locality(
                    Geometry(
                        coordinate,
                        radius.radius,
                        Geometry.Type.RECTANGLE
                    )
                )
            )


            val request = AppRequest(
               ListSerializer(Locality.serializer()),
                api,
                null,
                null,
                Response.Listener {
                    if (it.count() <= 3) {
                        val newRadius: API.Radius

                        when (radius) {
                            API.Radius.SMALLEST -> newRadius = API.Radius.SMALLER
                            API.Radius.SMALLER -> newRadius = API.Radius.SMALL
                            API.Radius.SMALL -> newRadius = API.Radius.MEDIUM
                            API.Radius.MEDIUM -> newRadius = API.Radius.LARGE
                            API.Radius.LARGE -> newRadius = API.Radius.LARGER
                            API.Radius.LARGER -> newRadius = API.Radius.LARGEST
                            API.Radius.LARGEST -> newRadius = API.Radius.HUGE
                            API.Radius.HUGE -> newRadius = API.Radius.HUGER
                            API.Radius.HUGER -> newRadius = API.Radius.HUGEST
                            API.Radius.HUGEST -> {
                                if (it.count() != 0) {
                                    (completion(Result.Success(it)))
                                    return@Listener
                                } else {
                                    newRadius = API.Radius.COUNTRY
                                }
                            }
                            API.Radius.COUNTRY -> return@Listener
                        }

                        getLocalities(tag, coordinate, newRadius, completion)
                    } else {
                        completion(Result.Success(it))
                    }
                },
                Response.ErrorListener {
                    completion(Result.Error(it.toAppError()))
                }
            )

            request.tag = tag
            addToRequestQueue(request)
        }
    }

    fun getHosts(
        tag: String,
        searchString: String?,
        completion: (Result<List<Host>, Error>) -> Unit
    ) {
        val api = API(APIType.Request.Host(searchString))
        val request = AppRequest(
            ListSerializer(Host.serializer()),
            api,
            null,
            null,
            {
                if (searchString == null) {
                    GlobalScope.launch {
                        RoomService.hosts.saveHosts(it)
                    }
                    completion(Result.Success(it))
                } else {
                    completion(Result.Success(it.map { Host(it.id, it.dkName, it.latinName, it.probability, true) }))
                }
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    suspend fun deleteObservation(
        tag: String,
        id: Int,
        token: String,
        completion: (Result<Void?, Error>) -> Unit) = withContext(Dispatchers.IO) {
        val api = API(APIType.Delete.Observation(id))
        val request = AppEmptyRequest(
            api,
            token,
            {
                completion(Result.Success(null))
            },
            {
                completion(Result.Error(it.toAppError()))
            }
        )
        request.tag = tag
        addToRequestQueue(request)
    }

    suspend fun deleteImage(
        tag: String,
        id: Int,
        token: String,
        completion: (Result<Void?, Error>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val api = API(APIType.Delete.Image(id))
        val request = AppEmptyRequest(
            api,
            token,
            {
                completion(Result.Success(null))
            },
            {
                completion(Result.Error(it.toAppError()))
            }
        )
        request.tag = tag
        addToRequestQueue(request)
    }

    private suspend fun uploadImage(
        tag: String,
        observationID: Int,
        image: File,
        token: String,
        completion: (Result<Boolean, AppError>) -> Unit
    ) = withContext(Dispatchers.IO) {
        val result = image.getBitmap()
        result.onError { completion(Result.Error(it)) }
        result.onSuccess {

            val api = API(APIType.Post.Image(observationID))
            val multipartFormImage = MultipartFormImage(it.rotate(image).toJPEG(0.6), "file")
            val request = AppMultipartPost(api,
                token,
                multipartFormImage,
                {
                    image.delete()
                    completion(Result.Success(true))
                },
                {
                    image.delete()
                    completion(Result.Error(it.toAppError()))
                }
            )

            request.tag = tag
            request.retryPolicy = DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            )
            addToRequestQueue(request)
        }
    }

    fun login(initials: String, password: String, completion: (Result<String, Error>) -> Unit) {
        val api = API(APIType.Post.Login)
        val jsonObject = JSONObject()
        jsonObject.put("Initialer", initials)
        jsonObject.put("password", password)

        val request = AppJSONObjectRequest(api,
            null,
            jsonObject,

            {
                val token = it.getString("token")
                completion(Result.Success(token))
            },

            {
                if (it is AuthFailureError) completion(
                    Result.Error(
                        Error.LoginError(
                            applicationContext
                        )
                    )
                ) else completion(Result.Error(it.toAppError()))
            })

        addToRequestQueue(request)
    }

    fun getUser(tag: String, token: String, completion: (Result<User, Error>) -> Unit) {
        val api = API(APIType.Request.User())
        val request = AppRequest(
           User.serializer(),
            api,
            token,
            null,
            {
                //                saveUser(it)
                completion(Result.Success(it))
            },

            {
                completion(Result.Error(it.toAppError()))
            })

        request.tag = tag
        addToRequestQueue(request)
    }


    fun getUserNotificationCount(
        tag: String,
        token: String,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val api = API(APIType.Request.UserNotificationCount())
        val request = object : JsonObjectRequest(api.volleyMethod(),
            api.url(),
            null,
            Response.Listener {
                val count = it.getInt("count")
                completion(Result.Success(count))
            },
            Response.ErrorListener {
                completion(Result.Error(it.toAppError()))
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf(Pair("Authorization", "Bearer $token"))
            }
        }

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getNotifications(
        tag: String,
        token: String,
        limit: Int,
        offset: Int,
        completion: (Result<List<Notification>, Error>) -> Unit
    ) {
        val api = API(APIType.Request.UserNotifications(limit, offset))
        val request = AppRequest(
            GsonNotifications.serializer(),
            api,
            token,
            null,
            {
                Log.d(TAG, it.toString())
                completion(Result.Success(it.results))
            },
            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservation(tag: String, id: Int, completion: (Result<Observation, Error>) -> Unit) {
        val api = API(APIType.Request.SingleObservation(id))

        val request = AppRequest(
            Observation.serializer(),
            api,
            null,
            null,
            {
                completion(Result.Success(it))
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun getObservationCountForUser(
        tag: String,
        userId: Int,
        completion: (Result<Int, Error>) -> Unit
    ) {
        val api = API(APIType.Request.ObservationCountForUser(userId))

        val request = JsonArrayRequest(
            api.volleyMethod(),
            api.url(),
            null,
            {
                completion(Result.Success(it.getJSONObject(0).getInt("count")))
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }


    fun getObservationsForUser(
        tag: String,
        userId: Int,
        offset: Int,
        limit: Int,
        completion: (Result<List<Observation>, Error>) -> Unit
    ) {
        val api = API(
            APIType.Request.Observation(
                null,
                listOf(
                    ObservationQueries.User(userId),
                    ObservationQueries.Images,
                    ObservationQueries.GeomNames,
                    ObservationQueries.Locality,
                    ObservationQueries.DeterminationView(null),
                    ObservationQueries.Comments
                )
                , null,
                limit, offset
            )
        )

        val request = AppRequest(
            ListSerializer(Observation.serializer()),
            api,
            null,
            null,
            {
                completion(Result.Success(it))
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun postComment(
        observationID: Int,
        comment: String,
        token: String,
        completion: (Result<Comment, Error>) -> Unit
    ) {
        val api = API(APIType.Post.Comment(observationID))

        val jsonObject = JSONObject().put("content", comment)

        val request = AppJSONObjectRequest(
            api,
            token,
            jsonObject,

            {
                val id = it.optInt("_id")
                val date = it.optString("createdAt")
                val content = it.optString("content")
                val user = it.optJSONObject("User")
                val name = user?.optString("name")
                val facebook = it.optString("facebook")
                val initials = it.optString("Initialer")

                if (!name.isNullOrEmpty() && !date.isNullOrEmpty() && !content.isNullOrEmpty() && name.isNotEmpty()) {
                    completion(Result.Success(Comment(id, date, content, name, initials, facebook)))
                } else {
                    completion(Result.Error(Error.ExtractionError()))
                }
            },

            {
                completion(Result.Error(it.toAppError()))
            }
        )

        addToRequestQueue(request)
    }

    fun markNotificationAsRead(tag: String, notificationID: Int, token: String) {
        val api = API(APIType.Put.NotificationLastRead(notificationID))
        val request = AppJSONObjectRequest(
            api,
            token,
            JSONObject(),
            {},
            {}
        )

        request.tag = tag
        addToRequestQueue(request)
    }

    fun clearRequestsWithTag(tag: String) {
        requestQueue.cancelAll(tag)
    }

    fun postOffensiveComment(tag: String, observationID: Int, json: JSONObject, token: String) {
        val api = API(APIType.Post.OffensiveContentComment(observationID))
        val request = AppJSONObjectRequest(
            api,
            token,
            json,
            {  },
            {  }
        )

        request.tag = tag
        addToRequestQueue(request)
    }
}