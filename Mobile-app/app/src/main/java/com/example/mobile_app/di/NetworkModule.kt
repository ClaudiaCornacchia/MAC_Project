package com.example.mobile_app.di


import com.example.mobile_app.model.service.QrApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // With Emulator use "http://10.0.2.2:3000/"
    // With physical device: use local IP es. "http://192.168.1.X:3000/"
    private const val BASE_URL = "http://192.168.1.8:3000/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideQrApiService(retrofit: Retrofit): QrApiService {
        return retrofit.create(QrApiService::class.java)
    }
}