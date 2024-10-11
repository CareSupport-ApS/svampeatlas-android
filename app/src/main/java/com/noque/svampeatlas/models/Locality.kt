package com.noque.svampeatlas.models

import androidx.room.Embedded
import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Locality(
    @SerialName("_id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("kommune") val municipality: String?,
    @SerialName("decimalLatitude") val latitude: Double,
    @SerialName("decimalLongitude") val longitude: Double,

    @Embedded(prefix = "geoname_")
    val geoName: GeoName? = null)
{
//    companion object {
//        fun fromGson(value: String): Locality {
//            return Gson().fromJson(value, Locality::class.java)
//        }
//    }

    constructor(id: Int, name: String, municipality: String?, latitude: Double, longitude: Double): this(id, name, municipality, latitude, longitude, null)
    val location: LatLng get() {return LatLng(latitude, longitude)}


//    fun toGson(): String {
//        val gson = Gson()
//        return gson.toJson(this)
//    }
}
