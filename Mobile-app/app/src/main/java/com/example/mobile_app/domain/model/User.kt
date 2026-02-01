package com.example.mobile_app.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val provider: String = "",
    val displayName: String = "",
    val lastBoxNumber: Int = 0,
)