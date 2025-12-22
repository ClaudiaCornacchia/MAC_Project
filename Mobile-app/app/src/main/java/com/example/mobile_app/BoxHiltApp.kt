package com.example.mobile_app


import android.app.Application
import android.content.pm.PackageManager
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BoxHiltApp : Application() {

    //Google Places for maps
    override fun onCreate() {
        super.onCreate()

        // Take the API key from the Manifest
        val apiKey = try {
            val appInfo = packageManager.getApplicationInfo(
                packageName,
                PackageManager.GET_META_DATA
            )
            appInfo.metaData.getString("com.google.android.geo.API_KEY") ?: ""
        } catch (e: Exception) {
            ""
        }
        if (apiKey.isNotBlank() && !Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
    }
}