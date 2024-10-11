package com.noque.svampeatlas.models

import androidx.room.*
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@SuppressWarnings(RoomWarnings.PRIMARY_KEY_FROM_EMBEDDED_IS_DROPPED)
@Entity(tableName = "mushrooms")
class Mushroom(
    @PrimaryKey
    @SerialName("_id") val id: Int,
    @SerialName("FullName") val fullName: String,
    @SerialName("TaxonName") val taxonName: String? = null,
    @SerialName("Author") val fullNameAuthor: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("probability") val probability: Int? = null,
    @SerialName("RankName") val _rankName: String? = null,

    @Embedded(prefix = "acceptedTaxon_")
    @SerialName("acceptedTaxon") val acceptedTaxon: AcceptedTaxon? = null,

    @Embedded
    @SerialName("Vernacularname_DK") val _vernacularNameDK: VernacularNameDK? = null,

    @Embedded
    @SerialName("attributes") val attributes: Attributes? = null,

    @Embedded
    @SerialName("Statistics") val statistics: Statistics? = null,

    @SerialName("redlistdata") val _redListData: List<RedListData>? = null,
    @SerialName("Images") val images: List<Image>? = null,
    var isUserFavorite: Boolean = false
) {

    constructor(id: Int, fullName: String, vernacularNameDK: VernacularNameDK?) : this(id, fullName, null, null, null, null, null, null, vernacularNameDK, null, null, null, null)

    val localizedName: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> _vernacularNameDK?._vernacularNameDK?.ifBlank { null }?.capitalized()
            AppLanguage.English -> attributes?.vernacularNameEn?.ifBlank { null }?.capitalized()
            AppLanguage.Czech -> attributes?.vernacularNameCz?.ifBlank { null }?.capitalized()
        }
    }

    val redListStatus: String? get() {
        return _redListData?.firstOrNull()?.status
    }


    val updatedAtDate: Date?
        get() {
            return Date(updatedAt)
        }


    val isGenus: Boolean get() { return (_rankName == "gen.") }
}

@Serializable
data class VernacularNameDK(
    @SerialName("vernacularname_dk") val _vernacularNameDK: String? = null,
    @SerialName("source") val _source: String? = null
)

@Serializable
data class RedListData(
    @SerialName("status") val status: String? = null
)

@Serializable
data class Attributes(
    @SerialName("diagnose") val diagnosis: String? = null,
    @SerialName("bogtekst_gyldendal_en") val diagnosisEn: String? = null,
    @SerialName("spiselighedsrapport") val edibility: String? = null,
    @SerialName("forvekslingsmuligheder") val similarities: String? = null,
    @SerialName("oekologi") val ecology: String? = null,
    @SerialName("valideringsrapport") val validationTips: String? = null,
    @SerialName("vernacular_name_GB") val vernacularNameEn: String? = null,
    @SerialName("vernacular_name_CZ") val vernacularNameCz: String? = null,
    @SerialName("PresentInDK") val presentInDenmark: Boolean? = null
) {

    val localizedDescription: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> diagnosis?.capitalized()
            AppLanguage.English -> diagnosisEn?.capitalized()
            AppLanguage.Czech -> diagnosisEn?.capitalized()
        }
    }

    val localizedEdibility: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> edibility?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val localizedSimilarities: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> similarities?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val localizedEcology: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> ecology?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }

    val isPoisonous: Boolean get() {
            return edibility != null && edibility.contains("giftig", true) && !edibility.contains("ikke giftig", true)
    }
}

@Serializable
data class Statistics(
    @SerialName("accepted_count") val acceptedCount: Int? = null,
    @SerialName("last_accepted_record") val lastAcceptedRecord: String? = null,
    @SerialName("first_accepted_record") val firstAcceptedRecord: String? = null
) {

    val acceptedObservationsCount: Int?
        get() {
            return acceptedCount
        }

    val lastAcceptedObservationDate: Date?
        get() {
            return Date(lastAcceptedRecord)
        }

    val firstAcceptedObservationDate: Date? get() { return Date(firstAcceptedRecord) }
}
