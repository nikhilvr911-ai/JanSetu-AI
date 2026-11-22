package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.SupportedLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "VoicePlaybackManager"

enum class VoiceStatus {
    IDLE,
    SPEAKING,
    PAUSED,
    UNAVAILABLE
}

data class VoicePlaybackState(
    val status: VoiceStatus = VoiceStatus.IDLE,
    val activeTag: String? = null,
    val currentChunkIndex: Int = 0,
    val totalChunks: Int = 0,
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val noticeMessage: String? = null
)

class VoicePlaybackManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var chunks: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var activeLanguage: SupportedLanguage = SupportedLanguage.ENGLISH
    private var activeTag: String? = null

    private val _state = MutableStateFlow(VoicePlaybackState())
    val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupProgressListener()
                Log.d(TAG, "Android TextToSpeech initialized successfully.")
            } else {
                isInitialized = false
                _state.value = _state.value.copy(
                    status = VoiceStatus.UNAVAILABLE,
                    noticeMessage = "Text-to-Speech engine is unavailable on this device."
                )
                Log.e(TAG, "TextToSpeech initialization failed with code: $status")
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.substringAfter("chunk_")?.toIntOrNull() ?: currentIndex
                currentIndex = idx
                _state.value = _state.value.copy(
                    status = VoiceStatus.SPEAKING,
                    currentChunkIndex = idx,
                    totalChunks = chunks.size
                )
            }

            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.substringAfter("chunk_")?.toIntOrNull() ?: currentIndex
                val nextIdx = idx + 1
                if (_state.value.status == VoiceStatus.SPEAKING && nextIdx < chunks.size) {
                    currentIndex = nextIdx
                    speakCurrentChunk()
                } else if (_state.value.status == VoiceStatus.SPEAKING) {
                    currentIndex = 0
                    _state.value = _state.value.copy(
                        status = VoiceStatus.IDLE,
                        activeTag = null,
                        currentChunkIndex = 0
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _state.value = _state.value.copy(
                    status = VoiceStatus.IDLE,
                    activeTag = null,
                    noticeMessage = "Could not speak audio for this language on this device."
                )
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "TTS utterance error code=$errorCode")
                _state.value = _state.value.copy(
                    status = VoiceStatus.IDLE,
                    activeTag = null,
                    noticeMessage = "Voice playback error (code $errorCode). Ensure speech pack is installed."
                )
            }
        })
    }

    fun play(text: String, language: SupportedLanguage, tag: String = "summary") {
        val engine = tts
        if (!isInitialized || engine == null) {
            _state.value = _state.value.copy(
                status = VoiceStatus.UNAVAILABLE,
                noticeMessage = "Voice engine is still initializing or unavailable on this device."
            )
            return
        }

        val splitChunks = splitIntoSpeechChunks(text)
        if (splitChunks.isEmpty()) return

        engine.stop()
        activeLanguage = language
        activeTag = tag
        chunks = splitChunks
        currentIndex = 0

        val notice = configureLocale(engine, language)
        _state.value = VoicePlaybackState(
            status = VoiceStatus.SPEAKING,
            activeTag = tag,
            currentChunkIndex = 0,
            totalChunks = chunks.size,
            language = language,
            noticeMessage = notice
        )
        speakCurrentChunk()
    }

    fun pause() {
        if (_state.value.status != VoiceStatus.SPEAKING) return
        tts?.stop()
        _state.value = _state.value.copy(status = VoiceStatus.PAUSED)
    }

    fun resume() {
        val engine = tts ?: return
        if (!isInitialized || _state.value.status != VoiceStatus.PAUSED || chunks.isEmpty()) return
        configureLocale(engine, activeLanguage)
        _state.value = _state.value.copy(status = VoiceStatus.SPEAKING)
        speakCurrentChunk()
    }

    fun stop() {
        tts?.stop()
        currentIndex = 0
        chunks = emptyList()
        activeTag = null
        _state.value = _state.value.copy(
            status = VoiceStatus.IDLE,
            activeTag = null,
            currentChunkIndex = 0,
            totalChunks = 0,
            noticeMessage = null
        )
    }

    private fun speakCurrentChunk() {
        val engine = tts ?: return
        if (currentIndex !in chunks.indices) return
        val chunkText = chunks[currentIndex]
        engine.setSpeechRate(0.95f)
        engine.speak(chunkText, TextToSpeech.QUEUE_FLUSH, null, "chunk_$currentIndex")
    }

    private fun configureLocale(engine: TextToSpeech, language: SupportedLanguage): String? {
        val targetLocale = language.toLocale()
        val result = engine.setLanguage(targetLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Try base language without region
            val baseResult = engine.setLanguage(Locale(language.code))
            if (baseResult == TextToSpeech.LANG_MISSING_DATA || baseResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.forLanguageTag("en-IN"))
                return "${language.englishName} (${language.bcp47Tag}) voice pack is not installed on this device; falling back to default speech engine."
            }
        }
        return null
    }

    private fun splitIntoSpeechChunks(rawText: String): List<String> {
        val cleaned = rawText
            .replace(Regex("[*_#`]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (cleaned.isEmpty()) return emptyList()

        // Split cleanly on Latin punctuation and Indic danda (।)
        return cleaned
            .split(Regex("(?<=[.!?।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(cleaned) }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
