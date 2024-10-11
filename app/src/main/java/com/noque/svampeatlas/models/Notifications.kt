package com.noque.svampeatlas.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
 class GsonNotifications(
    val endOfRecords: Boolean,
    val results: List<Notification>
)

@Serializable
data class Notification(
    @SerialName("observation_id") val observationID: Int,
    @SerialName("validation") val observationValidation: String,
    @SerialName("FullName") val observationFullName: String,
    @SerialName("eventType") val eventType: String,
    @SerialName("createdAt") val date: String,
    @SerialName("username") val triggerName: String,
    @SerialName("Initialer") val triggerInitials: String,
    @SerialName("user_facebook") private val triggerFacebookID: String?,
    @SerialName("img") private val observationImage: String?
) {

    val triggerImageURL: String? get() { return if (triggerFacebookID != null) "https://graph.facebook.com/${triggerFacebookID}/picture?width=70&height=70" else null }
    val imageURL: String? get() { return if (observationImage != null) "https://svampe.databasen.org/uploads/${observationImage}.JPG" else null }
}