package com.noque.svampeatlas.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializer(forClass = Date::class)
object DateSerializer : KSerializer<Date> {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Date) {
        val formattedDate = dateFormat.format(value)
        encoder.encodeString(formattedDate)
    }

    override fun deserialize(decoder: Decoder): Date {
        val dateString = decoder.decodeString()
        return dateFormat.parse(dateString) ?: throw IllegalArgumentException("Invalid date format")
    }
}

object LatLngSerializer : KSerializer<LatLng> {
    override val descriptor = buildClassSerialDescriptor("LatLng") {
        element<Double>("latitude")
        element<Double>("longitude")
    }

    override fun serialize(encoder: Encoder, value: LatLng) {
        encoder.encodeStructure(descriptor) {
            encodeDoubleElement(descriptor, 0, value.latitude)
            encodeDoubleElement(descriptor, 1, value.longitude)
        }
    }

    override fun deserialize(decoder: Decoder): LatLng {
        var latitude = 0.0
        var longitude = 0.0

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> latitude = decodeDoubleElement(descriptor, 0)
                    1 -> longitude = decodeDoubleElement(descriptor, 1)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw SerializationException("Unknown index $index")
                }
            }
        }

        return LatLng(latitude, longitude)
    }
}

@Serializable
data class Location(
    @Serializable(with = DateSerializer::class)
    val date: Date,
    @Serializable(with = LatLngSerializer::class)
    val latLng: LatLng,
    val accuracy: Float)