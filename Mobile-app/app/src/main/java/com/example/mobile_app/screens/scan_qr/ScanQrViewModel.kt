package com.example.mobile_app.screens.scan_qr


import com.example.mobile_app.R
import com.example.mobile_app.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.screens.BoxAppViewModel
import android.app.Application

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val application: Application
) : BoxAppViewModel() {

    // 2. Variable for Error Cooldown (Anti-Spam)
    private var lastErrorTime: Long = 0
    private val ERROR_COOLDOWN_MS = 2000L // 2 seconds pause between errors


    fun onQrCodeDetected(code: String, onNavigate: (String) -> Unit) {
        val prefix = "boxapp://box/"

        if (code.startsWith(prefix)) {
            // Extract the ID: "boxapp://box/12345" -> "12345"
            val boxId = code.removePrefix(prefix)

            // Navigate to detail screen
            // We use a prefix in the route to match your navigation graph
            onNavigate("BoxDetailScreen/$boxId")
        } else {
            val currentTime = System.currentTimeMillis()

            // Check if at least 2 seconds have passed since the last error
            if (currentTime - lastErrorTime > ERROR_COOLDOWN_MS) {

                // Show the Snackbar using the global Manager
                // (Note: SnackbarManager.showMessage accepts the String Resource ID)
                val message = application.getString(R.string.invalid_qr_code)
                SnackbarManager.showMessage(message)

                // Update the last error timestamp
                lastErrorTime = currentTime
            }
        }
    }
}