package com.example.snapvault_mk1

data class Album(
    val album_id: Int,
    val user_id: Int,
    val album_name: String,
    val creation_date: String,
    val album_password: String?
)