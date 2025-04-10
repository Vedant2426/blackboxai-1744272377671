package com.example.studentapp.data.models

data class User(
    val email: String,
    val password: String,
    val name: String = ""
)
