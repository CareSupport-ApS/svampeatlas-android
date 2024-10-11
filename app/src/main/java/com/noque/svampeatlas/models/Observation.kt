package com.noque.svampeatlas.models

import androidx.room.Embedded
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.services.RoomService
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


@Serializable
data class Observation(
    @SerialName("_id") private val _id: Int = 0,
    @SerialName("createdAt") private val _createdAt: String,
    @SerialName("observationDate") private val _observationDate: String?,
    @SerialName("ecologynote") private val _ecologyNote: String?,
    @SerialName("note") private val _note: String?,
    @SerialName("geom") private val _geom: Geom,
    @SerialName("DeterminationView") private val _determinationView: DeterminationView?,
    @SerialName("PrimaryDetermination") private val _primaryDetermination: PrimaryDeterminationView?,
    @SerialName("Images") private val _observationImages: List<ObservationImage>?,
    @SerialName("PrimaryUser") private val _primaryUser: PrimaryUser?,
    @SerialName("Locality") private val _locality: Locality?,
    @SerialName("GeoNames") private val _geoName: GeoName?,
    @SerialName("Forum") private val _forum: MutableList<Forum> = mutableListOf(),
    @SerialName("vegetationtype_id") private val vegetationTypeID: Int?,
    @SerialName("substrate_id") private val substrateID: Int?,
    @SerialName("accuracy") private val _accuracy: Int?,
    @SerialName("Substrate") private val _substrate: Substrate?,
    @SerialName("VegetationType") private val _vegetationType: VegetationType?,
    @SerialName("associatedTaxa") private val _associatedTaxa: List<AssociatedTaxa>?,
    @SerialName("users") private val _users: List<User>?

) {

    enum class ValidationStatus {
        APPROVED,
        VERIFYING,
        REJECTED,
        UNKNOWN
    }

    val id: Int
        get() {
            return _id
        }
    val createdAt: Date? get() {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime = OffsetDateTime.parse(_observationDate, formatter)
        return Date.from(dateTime.toInstant())
    }

    val observationDate: Date? get() {
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val dateTime = OffsetDateTime.parse(_observationDate, formatter)
        return Date.from(dateTime.toInstant())
    }

    val coordinate: LatLng
        get() {
            return LatLng(_geom.coordinates.last(), _geom.coordinates.first())
        }

    val users: List<User>?
        get() {return _users}

    val observationBy: String?
        get() {
            return _primaryUser?.profile?.name
        }
    val note: String?
        get() {
            return _note
        }
    val ecologyNote: String?
        get() {
            return _ecologyNote
        }

    val locationName: String? get() {
        if (_geoName != null) {
            return "${_geoName.countryName}, ${_geoName.name}"
        } else {
            return _locality?.name
        }
    }

    val location: Location?
        get() {
            return Location(createdAt ?: Date(), coordinate, _accuracy?.toFloat() ?: -1F)
        }

    val validationStatus: ValidationStatus
        get() {
            if (_determinationView?.determinationScore != null && _determinationView.determinationScore >= 80) {
                return ValidationStatus.APPROVED
            } else if (_determinationView?.determinationValidation != null) {
                return when (_determinationView.determinationValidation) {
                    "Afvist" -> ValidationStatus.REJECTED
                    "Godkendt" -> ValidationStatus.APPROVED
                    "Valideres" -> ValidationStatus.VERIFYING
                    else -> ValidationStatus.UNKNOWN
                }
            } else if (_primaryDetermination?.score != null && _primaryDetermination.score >= 80) {
                return ValidationStatus.APPROVED
            } else if (_primaryDetermination?.validation != null) {
                return when (_primaryDetermination.validation) {
                    "Afvist" -> ValidationStatus.REJECTED
                    "Godkendt" -> ValidationStatus.APPROVED
                    "Valideres" -> ValidationStatus.VERIFYING
                    else -> ValidationStatus.UNKNOWN
                }
            } else {
                return ValidationStatus.UNKNOWN
            }
        }

    val determination: Determination get() {
        return when {
            _primaryDetermination != null -> {
                Determination(
                    _primaryDetermination.taxon.acceptedTaxon.id,
                    _primaryDetermination.taxon.acceptedTaxon.fullName,
                    _primaryDetermination.taxon.acceptedTaxon.vernacularNameDK?.vernacularname_dk,
                    _primaryDetermination.confidence?.let { confidence -> DeterminationConfidence.values.first { it.databaseName == confidence } })
            }
            _determinationView != null -> {
                Determination(
                    _determinationView.taxonID,
                    _determinationView.fullName,
                    _determinationView.vernacularNameDK,
                    _determinationView.confidence?.let { confidence -> DeterminationConfidence.values.first { it.databaseName == confidence }  })
            }
            else -> throw InstantiationError()
        }
    }

    val images: List<Image>
        get() {
            return _observationImages?.map {
                Image(
                    it.id,
                    0,
                    "https://svampe.databasen.org/uploads/${it.name}.JPG",
                    null,
                    it.createdAt
                )
            } ?: listOf()
        }
    val comments: List<Comment>
        get() {
            return _forum.map {
                Comment(
                    it.id,
                    it.createdAt,
                    it.content,
                    it.user?.profile?.name ?: "Unknown",
                    it.user?.profile?.initials,
                    it.user?.profile?.facebook
                )
            }
        }

    val hosts: List<Host> get() {
        return _associatedTaxa?.map { Host(it.id, it.dkName, it.name, null)
        } ?: listOf()
    }

    val vegetationType: VegetationType? get() {
        return when {
            _vegetationType != null -> {
                _vegetationType
            }
            vegetationTypeID != null -> {
                when (val result = RoomService.vegetationTypes.getVegetationTypewithIDNow(vegetationTypeID)) {
                    is Result.Success -> result.value
                    is Result.Error -> null
                }
            }
            else -> {
                null
            }
        }
    }

    val substrate: Substrate? get() {
        return when {
            _substrate != null -> {
                _substrate
            }
            substrateID != null -> {
                when (val result = RoomService.substrates.getSubstrateWithIDNow(substrateID)) {
                    is Result.Success -> result.value
                    is Result.Error -> null
                }
            }
            else -> null
        }
    }

    val locality: Locality? get() {
        return when {
             _geoName != null -> {
                Locality(_geoName.geonameId, _geoName.name, null, _geoName.lat.toDouble(), _geoName.lng.toDouble(), _geoName)
            }
            _locality != null -> {
                _locality
            }
            else -> null
        }
    }

    fun addComment(comment: Comment) {
        _forum?.add(Forum(comment.id, comment.date?.toISO8601() ?: "", comment.content, PrimaryUser(Profile(comment.commenterName, comment.initials ?: "", comment.commenterProfileImageURL))))
    }

    fun isDeleteable(user: User): Boolean {
        if (user.isValidator) return true
        val createdAt = createdAt
        return user.name == observationBy && createdAt != null && createdAt.difDays() <= 2
    }

    fun isEditable(user: User): Boolean {
        if (user.isValidator) return true
        return user.name == observationBy
    }
}


@Serializable
data class Geom(val coordinates: List<Double>)

@Serializable
data class DeterminationView(
    @SerialName("taxon_id") val taxonID: Int,
    @SerialName("taxon_FullName") val fullName: String,
    @SerialName("taxon_vernacularname_dk") val vernacularNameDK: String? = null,
    @SerialName("redlistStatus") val redlistStatus: String? = null,
    @SerialName("determination_validation") val determinationValidation: String? = null,
    @SerialName("determination_score") val determinationScore: Int? = null,
    @SerialName("confidence") val confidence: String? = null
)

@Serializable
data class PrimaryDeterminationView(
    @SerialName("score") val score: Int?,
    @SerialName("validation") val validation: String?,
    @SerialName("Taxon") val taxon: Taxon,
    @SerialName("confidence") val confidence: String?
)

enum class DeterminationConfidence(val databaseName: String) {
    CONFIDENT("sikker"),
    LIKELY("sandsynlig"),
    POSSIBLE("mulig");

    companion object {
        val values = values()

        fun fromDatabaseName(name: String): DeterminationConfidence {
            return when (name) {
                "sikker" -> CONFIDENT
                "sandsynlig" -> LIKELY
                "mulig" -> POSSIBLE
                else -> CONFIDENT
            }
        }
    }
}

@Serializable
data class Determination(
    val id: Int,
    val fullName: String,
    private val danishName: String?,
    val confidence: DeterminationConfidence?
) {
    val localizedName: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> danishName?.capitalized()
            AppLanguage.English -> null
            AppLanguage.Czech -> null
        }
    }
}

@Serializable
data class Taxon(
    @SerialName("acceptedTaxon") val acceptedTaxon: AcceptedTaxon
)

@Serializable
data class AcceptedTaxon(
    @SerialName("_id") val id: Int,
    @SerialName("FullName") val fullName: String,

    @Embedded(prefix = "vernacularName_DK_")
    @SerialName("Vernacularname_DK") val vernacularNameDK: Vernacularname_DK? = null
)

@Serializable
data class Vernacularname_DK(
    val vernacularname_dk: String
)

@Serializable
data class ObservationImage(
    @SerialName("_id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("createdAt") val createdAt: String
)

@Serializable
data class PrimaryUser(val profile: Profile)
@Serializable
data class Profile(
    val name: String,
    @SerialName("Initialer") val initials: String,
    val facebook: String?
)

@Serializable
data class AssociatedTaxa(
    @SerialName("_id")  val id: Int,
    @SerialName("DKname")  val dkName: String?,
    @SerialName("LatinName")  val name: String
)




@Serializable
data class Forum(
    @SerialName("_id") val id: Int,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("content") val content: String,
    @SerialName("User") val user: PrimaryUser? = null
)
