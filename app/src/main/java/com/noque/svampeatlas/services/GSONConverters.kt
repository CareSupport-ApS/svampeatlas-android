package com.noque.svampeatlas.services

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.models.LatLngSerializer
import com.noque.svampeatlas.models.RedListData
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

object IDsConverter {

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<Int> {
        return data?.let {
            Json.decodeFromString(it)
        } ?: emptyList()
    }

    @TypeConverter
    @JvmStatic
    fun toString(ids: List<Int>?): String? {
        return ids?.let {
            Json.encodeToString(it)
        }
    }
}

object StringsConverter {

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<String> {
        return data?.let {
            Json.decodeFromString(it)
        } ?: emptyList()
    }

    @TypeConverter
    @JvmStatic
    fun toString(paths: List<String>?): String? {
        return paths?.let {
            Json.encodeToString(it)
        }
    }
}

object RedListDataConverter {

    @TypeConverter
    @JvmStatic
    fun toData(data: String?): List<RedListData> {
        return data?.let {
            Json.decodeFromString(it)
        } ?: emptyList()
    }

    @TypeConverter
    @JvmStatic
    fun toString(redListData: List<RedListData>?): String? {
        return redListData?.let {
            Json.encodeToString(it)
        }
    }
}



object ImagesConverter {
    @Serializable
    data class IndexedImage(val index: Int, val image: Image)

    @TypeConverter
    @JvmStatic
    fun toImages(data: String?): List<Image> {
        return data?.let {
            val indexedImages: List<IndexedImage> = Json.decodeFromString(it)
            indexedImages.sortedBy { it.index }.map { it.image }
        } ?: emptyList()
    }

    @TypeConverter
    @JvmStatic
    fun toString(images: List<Image>?): String? {
        return images?.let {
            val indexedImages = it.mapIndexed { index, image -> IndexedImage(index, image) }
            Json.encodeToString(indexedImages)
        }
    }
}

object DateConverter {
    @TypeConverter
    @JvmStatic
    fun toData(data: Long?): Date? {
        return data?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic
    fun toLong(date: Date): Long? {
        return date.time
    }
}

object LatLngConverter {
    @TypeConverter
    @JvmStatic
    fun toData(data: String?): LatLng? {
        return data?.let {
            Json.decodeFromString(LatLngSerializer, it)
        }
    }

    @TypeConverter
    @JvmStatic
    fun toString(latLng: LatLng?): String? {
        return latLng?.let {
            Json.encodeToString(LatLngSerializer, it)
        }
    }
}