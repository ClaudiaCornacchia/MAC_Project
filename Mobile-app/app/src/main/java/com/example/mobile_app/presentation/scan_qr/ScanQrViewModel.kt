package com.example.mobile_app.presentation.scan_qr


import com.example.mobile_app.R
import com.example.mobile_app.presentation.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.mobile_app.presentation.BoxAppViewModel
import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.data.repository.AccountRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class ScanQrViewModel @Inject constructor(
    private val application: Application,
    private val accountRepository: AccountRepository,
    private val firestore: FirebaseFirestore,
) : BoxAppViewModel() {

    // 2. Variable for Error Cooldown (Anti-Spam)
    private var lastErrorTime: Long = 0
    private val ERROR_COOLDOWN_MS = 2000L // 2 seconds pause between errors

    fun onQrCodeDetected(code: String, onNavigate: (String) -> Unit) {
        val prefix = "boxapp://box/"

        if (code.startsWith(prefix)) {
            val boxId = code.removePrefix(prefix)
            val currentUserId = accountRepository.currentUserId


            viewModelScope.launch {
                try {
                    val boxSnapshot = firestore.collection("boxes").document(boxId).get().await()

                    if (boxSnapshot.exists()) {
                        val sharedWith = boxSnapshot.get("sharedWith") as? List<*>

                        if (sharedWith?.contains(currentUserId) == true) {
                            onNavigate("BoxDetailScreen/$boxId")
                        } else {
                            val message = application.getString(R.string.invalid_qr_code)
                            SnackbarManager.showMessage(message)
                        }
                    } else {
                        val message = application.getString(R.string.invalid_qr_code)
                        SnackbarManager.showMessage(message)
                    }
                } catch (e: Exception) {
                    val message = application.getString(R.string.invalid_qr_code)
                    SnackbarManager.showMessage(message)
                }
            }
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



//    fun onQrCodeDetected(code: String, onNavigate: (String) -> Unit) {
//        val prefix = "boxapp://box/"
//
//        if (code.startsWith(prefix)) {
//            // Extract the ID: "boxapp://box/12345" -> "12345"
//            val boxId = code.removePrefix(prefix)
//
//            // Navigate to detail screen
//            // We use a prefix in the route to match your navigation graph
//            onNavigate("BoxDetailScreen/$boxId")
//        } else {
//            val currentTime = System.currentTimeMillis()
//
//            // Check if at least 2 seconds have passed since the last error
//            if (currentTime - lastErrorTime > ERROR_COOLDOWN_MS) {
//
//                // Show the Snackbar using the global Manager
//                // (Note: SnackbarManager.showMessage accepts the String Resource ID)
//                val message = application.getString(R.string.invalid_qr_code)
//                SnackbarManager.showMessage(message)
//
//                // Update the last error timestamp
//                lastErrorTime = currentTime
//            }
//        }
//    }


