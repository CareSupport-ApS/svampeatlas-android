package com.noque.svampeatlas.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoNames(
    @SerialName("geonames") val geoNames: List<GeoName>
)
