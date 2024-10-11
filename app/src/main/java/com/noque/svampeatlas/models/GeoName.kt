package com.noque.svampeatlas.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double

object DoubleOrStringAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DoubleOrStringAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return when (val jsonValue = decoder as? JsonDecoder) {
            null -> ""
            else -> {
                val jsonElement = jsonValue.decodeJsonElement()
                when (jsonElement) {
                    is JsonPrimitive -> {
                        // Check if it's a string
                        if (jsonElement.isString) {
                            jsonElement.content
                        } else {
                            // Handle the case where it's a number (as double)
                            jsonElement.double.toString() // Convert Double to String
                        }
                    }
                    else -> throw IllegalArgumentException("Unexpected type for latitude/longitude")
                }
            }
        }
    }
}

@Serializable
data class GeoName(
    val geonameId: Int,
    val name: String,
    val countryName: String,
    @Serializable(with = DoubleOrStringAsStringSerializer::class)
    val lat: String,
    @Serializable(with = DoubleOrStringAsStringSerializer::class)
    val lng: String,
    val countryCode: String,
    val fcodeName: String,
    val fclName: String,
    val adminName1: String
)