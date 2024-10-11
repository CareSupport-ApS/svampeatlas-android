package com.noque.svampeatlas.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class Prediction(val mushroom: Mushroom, val score: Double) {
    companion object {
        fun getNotes(
            selectedPrediction: Prediction?,
            predictionsResults: List<Prediction>
        ): String {
            var string = ""

            if (selectedPrediction != null) {
                string += "#imagevision_score: ${String.format(
                    "%.2f",
                    selectedPrediction.score * 100
                ).replace(",", ".")}; "
            }

            string += "#imagevision_list: "

            predictionsResults.forEach {
                string += "${it.mushroom.fullName} (${String.format("%.2f", it.score * 100).replace(",", ".")}), "
            }

            string = string.dropLast(2)
            return string
        }
    }
}

@Serializable
class PredictionResult(
    @SerialName("_id") private val id: Int,
    @SerialName("score") val score: Double,
    @SerialName("acceptedTaxon") private val acceptedTaxon: AcceptedTaxon,
    @SerialName("Vernacularname_DK") private val vernacularNameDK: VernacularNameDK?,
    @SerialName("attributes") private val attributes: Attributes?,
    @SerialName("Images") private val images: List<Image>?
) {

    companion object {
        fun getNotes(
            selectedPrediction: PredictionResult?,
            predictionsResults: List<PredictionResult>
        ): String {
            var string = ""

            if (selectedPrediction != null) {
                string += "#imagevision_score: ${String.format(
                    "%.2f",
                    selectedPrediction.score * 100
                ).replace(",", ".")}; "
            }

            string += "#imagevision_list: "

            predictionsResults.forEach {
                string += "${it.mushroom.fullName} (${String.format("%.2f", it.score * 100).replace(",", ".")}), "
            }

            string = string.dropLast(2)
            return string
        }
    }

    val mushroom: Mushroom get() {
        return Mushroom(id,
            acceptedTaxon.fullName,
            null,
            null,
            null,
            null,
            null,
            null,
            vernacularNameDK,
            attributes,
            null,
            null,
            images)
    }
}

