package com.example.whoareyou.model

data class Employee(
    val id: Int,
    val name: String,
    val team: String,
    val position: String,
    val nickname: String,
    val jobTitle: String,
    val internalPhone: String,
    val mobilePhone: String,
    val fax: String,
    val email: String,
    val profileImageName: String? = null,
    val photoUrl: String? = null,        // 서버 API 연결 후 실제 사진 URL이 여기에 들어옴
    val isFavorite: Boolean = false
)
