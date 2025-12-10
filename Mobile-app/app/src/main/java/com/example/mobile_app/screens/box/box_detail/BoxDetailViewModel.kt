package com.example.mobile_app.screens.box.box_detail

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.model.Box
import com.example.mobile_app.model.service.StorageService
import com.example.mobile_app.screens.BoxAppViewModel

@HiltViewModel
class BoxDetailViewModel @Inject constructor(
    private val storageService: StorageService,
    savedStateHandle: SavedStateHandle // Here we get navigation arguments
) : BoxAppViewModel() {

    // "boxId" must match the name in the route definition
    private val boxId: String = savedStateHandle["boxId"] ?: ""

    var box by mutableStateOf(Box())
        private set

    init {
        launchCatching {
            // Load box data immediately
            val loadedBox = storageService.getBox(boxId)
            if (loadedBox != null) {
                box = loadedBox
            }
        }
    }

    // Function to download the QR Code using the Android DownloadManager.
    // We pass the Context here because DownloadManager is a system service.
    fun downloadQrCode(context: Context) {
        val url = box.qrCodeUrl
        if (url.isBlank()) return

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Box QR Code")
                .setDescription("Downloading QR Code for ${box.title}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                // Save to the standard "Downloads" folder
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "QR_${box.humanId}.png")

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            // Optional: You could trigger a small toast or snackbar here via a state
        } catch (e: Exception) {
            throw Exception("Error downloading QR code.")
        }
    }

}