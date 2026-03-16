package com.example.whoareyou.model

data class CustomContact(
    val id: String,
    val name: String,
    val phone: String,
    val note: String = ""
)
