package com.example.mobile_app.model.service

import com.example.mobile_app.model.QrRequest
import com.example.mobile_app.model.QrResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface QrApiService {
    // POST /generate-qr
    @POST("generate-qr")
    suspend fun generateQr(
        @Header("Authorization") token: String,
        @Body request: QrRequest
    ): QrResponse
}