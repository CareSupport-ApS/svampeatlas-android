package com.noque.svampeatlas.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.noque.svampeatlas.extensions.AppLanguage
import com.noque.svampeatlas.extensions.appLanguage
import com.noque.svampeatlas.extensions.capitalized
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey
    @SerialName("_id") val id: Int,
    @SerialName("DKname") val dkName: String?,
    @SerialName("LatinName") val latinName: String,
    @SerialName("probability") val probability: Int?,
    val isUserSelected: Boolean = false
) {
    val localizedName: String? get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dkName?.capitalized()
            else -> null
        }
    }
}