package com.example.mobile_app.data.remote

// What send to server
data class QrRequest(
    val boxId: String
)

// Received from server
data class QrResponse(
    val qrCodeUrl: String
)