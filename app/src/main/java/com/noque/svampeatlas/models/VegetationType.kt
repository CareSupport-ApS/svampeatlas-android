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
@Entity(tableName = "vegetationType")
data class VegetationType(
    @PrimaryKey
    @SerialName("_id") val id: Int,
    @SerialName("name") val dkName: String,
    @SerialName("name_uk") val enName: String,
    @SerialName("name_cz") val czName: String?) {

    val localizedName: String get() {
        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> dkName.capitalized()
            AppLanguage.English -> enName.capitalized()
            AppLanguage.Czech -> czName?.capitalized() ?: enName.capitalized()
        }
    }
}