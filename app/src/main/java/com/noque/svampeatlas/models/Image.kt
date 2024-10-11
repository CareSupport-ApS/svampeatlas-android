package com.noque.svampeatlas.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Image(
    @PrimaryKey
        @SerialName("_id") val id: Int,
    @SerialName("taxon_id") val mushroomID: Int,
    @SerialName("uri") val uri: String?,
    @SerialName("photographer") val photographer: String?,
    @SerialName("createdAt") val createdAt: String?
    ) : Parcelable {

        val url: String
            get() {
                return uri ?: ""
            }

        constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString()
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(id)
            parcel.writeInt(mushroomID)
            parcel.writeString(uri)
            parcel.writeString(photographer)
            parcel.writeString(createdAt)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Image> {
            override fun createFromParcel(parcel: Parcel): Image {
                return Image(parcel)
            }

            override fun newArray(size: Int): Array<Image?> {
                return arrayOfNulls(size)
            }
        }

    }