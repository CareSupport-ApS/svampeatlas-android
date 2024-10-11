package com.noque.svampeatlas.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.*

object UserRolesTypeConverters {

    // Convert from a JSON string to a List<Role>
    @TypeConverter
    @JvmStatic
    fun toRoles(data: String?): List<Role> {
        return data?.let {
            Json.decodeFromString(it)
        } ?: emptyList()
    }

    // Convert from a List<Role> to a JSON string
    @TypeConverter
    @JvmStatic
    fun toString(roles: List<Role>?): String {
        return roles?.let {
            Json.encodeToString(ListSerializer(Role.serializer()), it)
        } ?: "[]"
    }
}

@Serializable
@Entity(tableName = "user")
class User(
    @PrimaryKey
    @ColumnInfo(name = "id")
    @SerialName("_id") val id: Int,

    @ColumnInfo(name = "content")
    @SerialName("name") val name: String,

    @ColumnInfo(name = "initials")
    @SerialName("Initialer") val initials: String,

    @ColumnInfo(name = "email")
    @SerialName("email") val email: String,

    @ColumnInfo(name = "facebook_id")
    @SerialName("facebook") val facebookID: String? = null,

    @ColumnInfo(name = "roles")
    @SerialName("Roles") val roles: List<Role>? = null
) {

    val imageURL: String? get() {
        facebookID?.let {
            return "https://graph.facebook.com/${it}/picture?width=250&height=250"
        }
        return null
    }

    val isValidator: Boolean get() {
        roles?.forEach {
            if (it.name == "validator") return true
        }
        return false
    }
}

@Serializable
data class Role(
    @SerialName("name") val name: String)