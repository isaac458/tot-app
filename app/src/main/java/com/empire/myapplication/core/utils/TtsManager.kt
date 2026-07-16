package com.empire.myapplication.core.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var onFinished: (() -> Unit)? = null
    
    private var fullText: String = ""
    private var remainingText: String = ""
    private var isPaused = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ar"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isPaused = false
                    }
                    override fun onDone(utteranceId: String?) { 
                        if (!isPaused) {
                            remainingText = ""
                            onFinished?.invoke() 
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { onFinished?.invoke() }
                    override fun onError(utteranceId: String?, errorCode: Int) { onFinished?.invoke() }
                })
            }
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        this.onFinished = onFinished
        this.fullText = text
        this.remainingText = text
        this.isPaused = false
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "toot_utterance")
        }
    }

    fun pause() {
        if (isInitialized && tts?.isSpeaking == true) {
            isPaused = true
            tts?.stop()
            // Note: We don't really know where it stopped exactly, 
            // but we can toggle the UI state. 
            // For real "resume", we'd need more complex splitting.
            // For now, we'll just allow "restart" or "resume" from start if simple.
        }
    }

    fun resume(onFinished: (() -> Unit)? = null) {
        if (isInitialized && isPaused) {
            speak(remainingText, onFinished)
        }
    }

    fun stop() {
        isPaused = false
        remainingText = ""
        tts?.stop()
        onFinished?.invoke()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
