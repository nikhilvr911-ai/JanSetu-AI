package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.JanSetuAiRepository
import com.example.data.ProfilePreferencesRepository
import com.example.model.AnalysisUiState
import com.example.model.AttachedDocument
import com.example.model.CitizenProfile
import com.example.model.FollowUpMessage
import com.example.model.OccupationCategory
import com.example.model.SampleNoticePreset
import com.example.model.SupportedLanguage
import com.example.voice.VoicePlaybackManager
import com.example.voice.VoicePlaybackState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "JanSetuViewModel"
private const val MAX_ATTACHMENT_BYTES = 18L * 1024L * 1024L // Stay safely under 20MB inline limit

enum class MobileTab {
    INPUT_PROFILE,
    AI_RESULTS
}

data class JanSetuUiState(
    val selectedLanguage: SupportedLanguage = SupportedLanguage.ENGLISH,
    val documentText: String = "",
    val attachedDocument: AttachedDocument? = null,
    val attachmentError: String? = null,
    val activeSampleId: String? = null,
    val profile: CitizenProfile = CitizenProfile(),
    val analysisState: AnalysisUiState = AnalysisUiState.Idle,
    val checkedDocumentIndices: Set<Int> = emptySet(),
    val followUpInput: String = "",
    val followUpMessages: List<FollowUpMessage> = emptyList(),
    val isAskingFollowUp: Boolean = false,
    val activeMobileTab: MobileTab = MobileTab.INPUT_PROFILE
) {
    val canAnalyze: Boolean
        get() = (documentText.isNotBlank() || attachedDocument != null) &&
            analysisState !is AnalysisUiState.Loading
}

class JanSetuViewModel(application: Application) : AndroidViewModel(application) {

    private val aiRepository = JanSetuAiRepository()
    private val prefsRepository = ProfilePreferencesRepository(application)
    private val voiceManager = VoicePlaybackManager(application)

    private val _uiState = MutableStateFlow(JanSetuUiState())
    val uiState: StateFlow<JanSetuUiState> = _uiState.asStateFlow()

    val voiceState: StateFlow<VoicePlaybackState> = voiceManager.state

    private var analysisJob: Job? = null
    private var followUpJob: Job? = null
    private var nextMessageId = 1L

    init {
        viewModelScope.launch {
            val saved = prefsRepository.preferencesFlow.first()
            _uiState.update {
                it.copy(
                    selectedLanguage = saved.language,
                    profile = saved.profile
                )
            }
        }
    }

    fun selectLanguage(language: SupportedLanguage) {
        voiceManager.stop()
        _uiState.update { it.copy(selectedLanguage = language) }
        viewModelScope.launch {
            prefsRepository.saveLanguage(language)
        }
    }

    fun selectMobileTab(tab: MobileTab) {
        _uiState.update { it.copy(activeMobileTab = tab) }
    }

    fun updateDocumentText(text: String) {
        _uiState.update {
            it.copy(
                documentText = text,
                activeSampleId = if (text.isBlank()) null else it.activeSampleId
            )
        }
    }

    fun clearDocumentText() {
        _uiState.update { it.copy(documentText = "", activeSampleId = null) }
    }

    fun clearAttachment() {
        _uiState.update { it.copy(attachedDocument = null, attachmentError = null) }
    }

    fun loadDocumentFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(attachmentError = null) }
            try {
                val loaded = withContext(Dispatchers.IO) {
                    readAttachmentFromUri(uri)
                }
                _uiState.update {
                    it.copy(
                        attachedDocument = loaded,
                        attachmentError = null,
                        activeSampleId = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read selected file", e)
                _uiState.update {
                    it.copy(
                        attachmentError = e.localizedMessage
                            ?: "Could not read selected file. Please choose a PDF or image under 18 MB."
                    )
                }
            }
        }
    }

    fun applySamplePreset(preset: SampleNoticePreset, applyProfile: Boolean = true) {
        voiceManager.stop()
        _uiState.update { current ->
            val updatedProfile = if (applyProfile) preset.suggestedProfile else current.profile
            current.copy(
                documentText = preset.noticeText,
                attachedDocument = null,
                attachmentError = null,
                activeSampleId = preset.id,
                profile = updatedProfile
            )
        }
        if (applyProfile) {
            viewModelScope.launch {
                prefsRepository.saveProfile(preset.suggestedProfile)
            }
        }
    }

    fun updateAge(age: String) {
        val digitsOnly = age.filter { it.isDigit() }.take(3)
        updateProfile { it.copy(age = digitsOnly) }
    }

    fun updateOccupation(occupation: OccupationCategory) {
        updateProfile { it.copy(occupation = occupation) }
    }

    fun updateAnnualIncome(income: String) {
        val digitsOnly = income.filter { it.isDigit() }.take(10)
        updateProfile { it.copy(annualIncomeInr = digitsOnly) }
    }

    fun updateStateName(state: String) {
        updateProfile { it.copy(state = state.take(40)) }
    }

    fun updateAdditionalContext(contextText: String) {
        updateProfile { it.copy(additionalContext = contextText.take(240)) }
    }

    private fun updateProfile(transform: (CitizenProfile) -> CitizenProfile) {
        val updated = transform(_uiState.value.profile)
        _uiState.update { it.copy(profile = updated) }
        viewModelScope.launch {
            prefsRepository.saveProfile(updated)
        }
    }

    fun toggleDocumentChecked(index: Int) {
        _uiState.update { state ->
            val updated = if (index in state.checkedDocumentIndices) {
                state.checkedDocumentIndices - index
            } else {
                state.checkedDocumentIndices + index
            }
            state.copy(checkedDocumentIndices = updated)
        }
    }

    fun analyzeDocument() {
        val snapshot = _uiState.value
        if (snapshot.documentText.isBlank() && snapshot.attachedDocument == null) {
            return
        }

        analysisJob?.cancel()
        followUpJob?.cancel()
        voiceManager.stop()

        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    analysisState = AnalysisUiState.Loading(stepIndex = 0),
                    activeMobileTab = MobileTab.AI_RESULTS,
                    checkedDocumentIndices = emptySet(),
                    followUpMessages = emptyList(),
                    followUpInput = "",
                    isAskingFollowUp = false
                )
            }

            // Progressively advance step labels while Gemini processes the document
            val stepTicker = launch {
                delay(1800L)
                _uiState.update { state ->
                    if (state.analysisState is AnalysisUiState.Loading) {
                        state.copy(analysisState = AnalysisUiState.Loading(stepIndex = 1))
                    } else state
                }
                delay(2400L)
                _uiState.update { state ->
                    if (state.analysisState is AnalysisUiState.Loading) {
                        state.copy(analysisState = AnalysisUiState.Loading(stepIndex = 2))
                    } else state
                }
            }

            try {
                val result = aiRepository.analyzeDocument(
                    documentText = snapshot.documentText,
                    attachedDocument = snapshot.attachedDocument,
                    profile = snapshot.profile,
                    language = snapshot.selectedLanguage
                )
                stepTicker.cancel()
                _uiState.update {
                    it.copy(analysisState = AnalysisUiState.Success(result))
                }
            } catch (e: TimeoutCancellationException) {
                stepTicker.cancel()
                Log.e(TAG, "Document analysis timed out", e)
                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Error(
                            "Analysis timed out after 60 seconds. Please check your connection and try again."
                        )
                    )
                }
            } catch (e: CancellationException) {
                stepTicker.cancel()
                throw e
            } catch (e: Exception) {
                stepTicker.cancel()
                Log.e(TAG, "Document analysis failed", e)
                _uiState.update {
                    it.copy(
                        analysisState = AnalysisUiState.Error(
                            e.localizedMessage ?: "Failed to analyze document. Please try again."
                        )
                    )
                }
            }
        }
    }

    fun updateFollowUpInput(text: String) {
        _uiState.update { it.copy(followUpInput = text) }
    }

    fun askFollowUpQuestion(overrideQuestion: String? = null) {
        val currentState = _uiState.value
        val successState = currentState.analysisState as? AnalysisUiState.Success ?: return
        val questionText = (overrideQuestion ?: currentState.followUpInput).trim()
        if (questionText.isEmpty() || currentState.isAskingFollowUp) return

        val messageId = nextMessageId++
        val priorHistory = currentState.followUpMessages
        val pendingMessage = FollowUpMessage(
            id = messageId,
            question = questionText,
            answer = "",
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                followUpInput = if (overrideQuestion == null) "" else it.followUpInput,
                followUpMessages = it.followUpMessages + pendingMessage,
                isAskingFollowUp = true
            )
        }

        followUpJob?.cancel()
        followUpJob = viewModelScope.launch {
            val builder = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiRepository.askFollowUpStream(
                        question = questionText,
                        documentText = currentState.documentText,
                        attachedDocument = currentState.attachedDocument,
                        previousResult = successState.result,
                        profile = currentState.profile,
                        language = currentState.selectedLanguage,
                        history = priorHistory
                    ).collect { chunk ->
                        builder.append(chunk)
                        val latestText = builder.toString()
                        _uiState.update { state ->
                            state.copy(
                                followUpMessages = state.followUpMessages.map { msg ->
                                    if (msg.id == messageId) {
                                        msg.copy(answer = latestText, isStreaming = true)
                                    } else msg
                                }
                            )
                        }
                    }
                }
                val finalAnswer = builder.toString().trim().ifEmpty {
                    "Could not generate an answer for this question. Please try rephrasing."
                }
                _uiState.update { state ->
                    state.copy(
                        isAskingFollowUp = false,
                        followUpMessages = state.followUpMessages.map { msg ->
                            if (msg.id == messageId) {
                                msg.copy(answer = finalAnswer, isStreaming = false)
                            } else msg
                        }
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Follow-up Q&A timed out", e)
                markFollowUpError(messageId, "Response timed out. Please ask again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Follow-up Q&A failed", e)
                markFollowUpError(
                    messageId,
                    e.localizedMessage ?: "Failed to answer your follow-up question."
                )
            }
        }
    }

    private fun markFollowUpError(messageId: Long, errorText: String) {
        _uiState.update { state ->
            state.copy(
                isAskingFollowUp = false,
                followUpMessages = state.followUpMessages.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(answer = errorText, isStreaming = false, isError = true)
                    } else msg
                }
            )
        }
    }

    fun playSummaryVoice() {
        val result = (_uiState.value.analysisState as? AnalysisUiState.Success)?.result ?: return
        voiceManager.play(
            text = result.voiceScript,
            language = result.analyzedLanguage,
            tag = "summary"
        )
    }

    fun playFollowUpVoice(message: FollowUpMessage) {
        if (message.answer.isBlank()) return
        voiceManager.play(
            text = message.answer,
            language = _uiState.value.selectedLanguage,
            tag = "qa_${message.id}"
        )
    }

    fun pauseVoice() = voiceManager.pause()

    fun resumeVoice() = voiceManager.resume()

    fun stopVoice() = voiceManager.stop()

    private fun readAttachmentFromUri(uri: Uri): AttachedDocument {
        val resolver = getApplication<Application>().contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val isSupported = mimeType == "application/pdf" || mimeType.startsWith("image/")
        if (!isSupported) {
            throw IllegalArgumentException("Please pick a PDF document or an image (JPG/PNG/WebP).")
        }

        var fileName = "official_notice"
        var reportedSize = -1L
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex) ?: fileName
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    reportedSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (reportedSize > MAX_ATTACHMENT_BYTES) {
            throw IllegalArgumentException("File is too large (max 18 MB). Please choose a smaller PDF or photo.")
        }

        val bytes = resolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalStateException("Unable to open the selected file.")

        if (bytes.size.toLong() > MAX_ATTACHMENT_BYTES) {
            throw IllegalArgumentException("File is too large (max 18 MB). Please choose a smaller PDF or photo.")
        }

        return AttachedDocument(
            fileName = fileName,
            mimeType = mimeType,
            bytes = bytes,
            sizeBytes = bytes.size.toLong(),
            isImage = mimeType.startsWith("image/")
        )
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
