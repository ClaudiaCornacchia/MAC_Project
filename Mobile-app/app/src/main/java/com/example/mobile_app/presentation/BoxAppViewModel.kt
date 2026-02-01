package com.example.mobile_app.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobile_app.presentation.SnackbarManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

const val ERROR_TAG = "BoxAppError"
open class BoxAppViewModel : ViewModel() {
    fun launchCatching(block: suspend CoroutineScope.() -> Unit) =
        // 1. Launches a new Coroutine tied to the ViewModel's lifecycle.
        viewModelScope.launch(
           // 2. If ANY exception is thrown inside 'block' and not caught,
            // this handler catches it instantly to prevent the app from crashing.
            CoroutineExceptionHandler { _, throwable ->
                // 3. Call SnackbarManager that will show the error
                SnackbarManager.showMessage(throwable.message ?: "An error occurred")
                Log.d(ERROR_TAG, throwable.message.orEmpty())
            },
            // 4. The block of code passed when called launchCatching
            block = block
        )
}