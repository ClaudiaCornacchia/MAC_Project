package com.example.mobile_app.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.ERROR_TAG
import com.example.mobile_app.SnackbarManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

open class BoxAppViewModel : ViewModel() {
    fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                // Mostriamo l'errore all'utente tramite Snackbar
                SnackbarManager.showMessage(throwable.message ?: "An error occurred")
                // Logghiamo l'errore per il debug
                Log.d(ERROR_TAG, throwable.message.orEmpty())
            },
            block = block
        )
}