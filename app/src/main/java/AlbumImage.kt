package com.example.snapvault_mk1

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlbumImage(
    val id: Int,
    val image_path: String,
    val album_id: Int,
    val imagePaths: List<String>
) : Parcelable
