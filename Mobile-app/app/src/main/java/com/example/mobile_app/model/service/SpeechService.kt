package com.example.mobile_app.model.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject

// State of the recognition process
sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class Working(val partialText: String, val rmsDb: Float) : SpeechState() // rmsDb is volume for spikes
    data class Error(val message: String) : SpeechState()
    data class End(val finalText: String) : SpeechState()
}

class SpeechService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // We expose a Flow that emits the current state of speech recognition
    fun startRecognition(): Flow<SpeechState> = callbackFlow {
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Use phone language
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable real-time typing!
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechState.Listening)
            }

            override fun onBeginningOfSpeech() {}

            // This is called constantly while user speaks.
            // rmsdB is the loudness, we use it for spikes animation.
            override fun onRmsChanged(rmsdB: Float) {
                // We send the volume level to create the visual effect
                trySend(SpeechState.Working("", rmsdB))
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                // User stopped talking, waiting for final processing
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    else -> "Error occurred ($error)"
                }
                trySend(SpeechState.Error(message))
                close() // Close the channel
            }

            // Real-time results (text appearing while speaking)
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // Send partial text but keep loudness 0 for now (or keep last value)
                    trySend(SpeechState.Working(matches[0], 0f))
                }
            }

            // Final result when silence is detected
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    trySend(SpeechState.End(matches[0]))
                }
                close()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)

        awaitClose {
            speechRecognizer.destroy()
        }
    }
}