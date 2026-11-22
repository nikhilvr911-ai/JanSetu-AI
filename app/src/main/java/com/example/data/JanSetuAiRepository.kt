package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.model.AttachedDocument
import com.example.model.CitizenProfile
import com.example.model.EligibilityVerdict
import com.example.model.FollowUpMessage
import com.example.model.SchemeAnalysisResult
import com.example.model.SupportedLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "JanSetuAiRepository"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val MAX_IMAGE_DIMENSION_PX = 1600
private const val AI_TIMEOUT_MS = 60_000L

class JanSetuAiRepository {

    private val analysisSchema = Schema.obj(
        properties = mapOf(
            "documentTitle" to Schema.string(
                description = "Clear, citizen-friendly title of the scheme, policy, or notice strictly in the requested target language"
            ),
            "summaryPoints" to Schema.array(
                items = Schema.string(),
                description = "3 to 4 short, easy-to-understand bullet points with zero bureaucratic jargon, strictly in the requested target language"
            ),
            "eligibilityVerdict" to Schema.enumeration(
                values = listOf("ELIGIBLE", "PARTIALLY_ELIGIBLE", "NOT_ELIGIBLE"),
                description = "Machine verdict code based on the citizen profile compared against the document rules"
            ),
            "verdictDisplayBadge" to Schema.string(
                description = "Localized badge label in the target language corresponding to ELIGIBLE, PARTIALLY ELIGIBLE / CHECK NEEDED, or NOT ELIGIBLE"
            ),
            "eligibilityExplanation" to Schema.string(
                description = "A clear 2-sentence personalized explanation strictly in the target language directly referencing the user's age, occupation, income, state, and context"
            ),
            "keyBenefits" to Schema.array(
                items = Schema.string(),
                description = "Exact financial aid amounts in INR, subsidies, legal rights, protections, or helplines strictly in the target language"
            ),
            "importantDeadlines" to Schema.string(
                description = "Exact application deadlines, validity windows, or portal submission dates mentioned in the notice, strictly in the target language"
            ),
            "requiredDocuments" to Schema.array(
                items = Schema.string(),
                description = "Clear checklist of required documents or proofs needed, strictly in the target language"
            ),
            "applicationSteps" to Schema.array(
                items = Schema.string(),
                description = "Actionable step-by-step next steps (portals, offices, helplines) strictly in the target language"
            ),
            "voiceScript" to Schema.string(
                description = "A warm, clear, spoken-word summary (4 to 6 short natural sentences) strictly in the target language, optimized for Text-to-Speech narration covering the scheme purpose, eligibility verdict, main benefit, and immediate next step. Avoid markdown symbols, asterisks, or URLs."
            )
        )
    )

    private val analysisModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = analysisSchema
            }
        )
    }

    private val qaModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.3f
            }
        )
    }

    suspend fun analyzeDocument(
        documentText: String,
        attachedDocument: AttachedDocument?,
        profile: CitizenProfile,
        language: SupportedLanguage
    ): SchemeAnalysisResult {
        val promptContent = buildAnalysisContent(
            documentText = documentText,
            attachedDocument = attachedDocument,
            profile = profile,
            language = language
        )

        val response = withTimeout(AI_TIMEOUT_MS) {
            analysisModel.generateContent(promptContent)
        }

        val jsonString = response.text
        if (jsonString.isNullOrBlank()) {
            throw IllegalStateException("AI returned an empty response. Please try again.")
        }

        return parseAnalysisJson(jsonString, language)
    }

    fun askFollowUpStream(
        question: String,
        documentText: String,
        attachedDocument: AttachedDocument?,
        previousResult: SchemeAnalysisResult,
        profile: CitizenProfile,
        language: SupportedLanguage,
        history: List<FollowUpMessage>
    ): Flow<String> {
        val promptContent = buildFollowUpContent(
            question = question,
            documentText = documentText,
            attachedDocument = attachedDocument,
            previousResult = previousResult,
            profile = profile,
            language = language,
            history = history
        )
        return qaModel.generateContentStream(promptContent).mapNotNull { chunk ->
            chunk.text
        }
    }

    private suspend fun buildAnalysisContent(
        documentText: String,
        attachedDocument: AttachedDocument?,
        profile: CitizenProfile,
        language: SupportedLanguage
    ): Content {
        val decodedBitmap: Bitmap? = if (attachedDocument != null && attachedDocument.isImage) {
            withContext(Dispatchers.Default) {
                decodeScaledBitmap(attachedDocument.bytes)
            }
        } else {
            null
        }

        val prompt = buildString {
            appendLine("You are JanSetu AI, a trusted Indian civic-tech policy, government welfare scheme, and citizen rights explainer.")
            appendLine("TASK: Carefully read the attached official document (if provided) and/or pasted notice text, evaluate the citizen's eligibility against the actual rules in the document, and return a structured JSON analysis.")
            appendLine()
            appendLine("CRITICAL LANGUAGE RULE:")
            appendLine("- EVERY user-facing text field (documentTitle, summaryPoints, verdictDisplayBadge, eligibilityExplanation, keyBenefits, importantDeadlines, requiredDocuments, applicationSteps, voiceScript) MUST be written STRICTLY in: ${language.promptInstructionName}.")
            appendLine("- Only 'eligibilityVerdict' must remain one of the exact enum strings: ELIGIBLE, PARTIALLY_ELIGIBLE, or NOT_ELIGIBLE.")
            appendLine("- Use zero bureaucratic jargon. Explain like a helpful local civic officer speaking respectfully to a citizen.")
            appendLine("- Keep summaryPoints to 3 or 4 crisp, clear bullet points.")
            appendLine("- Keep eligibilityExplanation to exactly 2 clear sentences directly tied to the citizen profile below.")
            appendLine("- For voiceScript, write natural spoken prose in ${language.promptInstructionName} with short sentences, no bullets, no markdown symbols, and no raw web URLs.")
            appendLine()
            appendLine("CITIZEN PROFILE:")
            appendLine("- Age: ${profile.age.ifBlank { "Not specified" }}")
            appendLine("- Occupation / Category: ${profile.occupation.label}")
            appendLine("- Annual Family Income (INR): ${profile.annualIncomeInr.ifBlank { "Not specified" }}")
            appendLine("- State / UT: ${profile.state.ifBlank { "Karnataka" }}")
            appendLine("- Additional Context: ${profile.additionalContext.ifBlank { "None provided" }}")
            appendLine()
            if (documentText.isNotBlank()) {
                appendLine("OFFICIAL NOTICE / SCHEME TEXT:")
                appendLine(documentText.trim())
            } else {
                appendLine("Analyze the attached official notice document carefully.")
            }
        }

        return content {
            if (attachedDocument != null) {
                if (decodedBitmap != null) {
                    image(decodedBitmap)
                } else {
                    inlineData(attachedDocument.bytes, attachedDocument.mimeType)
                }
            }
            text(prompt)
        }
    }

    private fun buildFollowUpContent(
        question: String,
        documentText: String,
        attachedDocument: AttachedDocument?,
        previousResult: SchemeAnalysisResult,
        profile: CitizenProfile,
        language: SupportedLanguage,
        history: List<FollowUpMessage>
    ): Content {
        val prompt = buildString {
            appendLine("You are JanSetu AI, a helpful multilingual Indian citizen rights and government scheme assistant.")
            appendLine("Respond STRICTLY in ${language.promptInstructionName}. Keep your answer concise (2 to 5 short sentences), practical, accurate to the document, and free of bureaucratic jargon or raw markdown stars.")
            appendLine()
            appendLine("CITIZEN PROFILE:")
            appendLine("Age: ${profile.age}, Occupation: ${profile.occupation.label}, Annual Income: ₹${profile.annualIncomeInr}, State: ${profile.state}, Context: ${profile.additionalContext}")
            appendLine()
            appendLine("PREVIOUS SIMPLIFIED ANALYSIS SUMMARY:")
            appendLine("Scheme: ${previousResult.documentTitle}")
            appendLine("Verdict: ${previousResult.eligibilityVerdict} — ${previousResult.eligibilityExplanation}")
            appendLine("Deadlines: ${previousResult.importantDeadlines}")
            appendLine("Benefits: ${previousResult.keyBenefits.joinToString("; ")}")
            appendLine("Documents: ${previousResult.requiredDocuments.joinToString("; ")}")
            appendLine("Steps: ${previousResult.applicationSteps.joinToString("; ")}")
            if (documentText.isNotBlank()) {
                appendLine()
                appendLine("ORIGINAL NOTICE TEXT:")
                appendLine(documentText.take(6000))
            }
            if (history.isNotEmpty()) {
                appendLine()
                appendLine("RECENT FOLLOW-UP Q&A:")
                history.takeLast(4).forEach { msg ->
                    appendLine("Citizen: ${msg.question}")
                    if (msg.answer.isNotBlank() && !msg.isError) {
                        appendLine("JanSetu AI: ${msg.answer}")
                    }
                }
            }
            appendLine()
            appendLine("CITIZEN'S NEW QUESTION: $question")
        }

        return content {
            if (attachedDocument != null && !attachedDocument.isImage && documentText.isBlank()) {
                inlineData(attachedDocument.bytes, attachedDocument.mimeType)
            }
            text(prompt)
        }
    }

    private fun parseAnalysisJson(
        jsonString: String,
        language: SupportedLanguage
    ): SchemeAnalysisResult {
        try {
            val json = JSONObject(jsonString)
            val verdictWire = json.optString("eligibilityVerdict", "PARTIALLY_ELIGIBLE")
            val verdict = EligibilityVerdict.fromWire(verdictWire)
            val summaryPoints = json.optJSONArray("summaryPoints").toStringList()
            val keyBenefits = json.optJSONArray("keyBenefits").toStringList()
            val requiredDocuments = json.optJSONArray("requiredDocuments").toStringList()
            val applicationSteps = json.optJSONArray("applicationSteps").toStringList()
            val documentTitle = json.optString("documentTitle").ifBlank { "Official Notice Analysis" }
            val verdictBadge = json.optString("verdictDisplayBadge").ifBlank {
                when (verdict) {
                    EligibilityVerdict.ELIGIBLE -> "ELIGIBLE"
                    EligibilityVerdict.PARTIALLY_ELIGIBLE -> "PARTIALLY ELIGIBLE / CHECK NEEDED"
                    EligibilityVerdict.NOT_ELIGIBLE -> "NOT ELIGIBLE"
                }
            }
            val eligibilityExplanation = json.optString("eligibilityExplanation")
            val importantDeadlines = json.optString("importantDeadlines")
            val voiceScript = json.optString("voiceScript").ifBlank {
                buildString {
                    append(documentTitle).append(". ")
                    append(verdictBadge).append(". ")
                    append(eligibilityExplanation).append(" ")
                    append(summaryPoints.joinToString(". "))
                }
            }

            return SchemeAnalysisResult(
                documentTitle = documentTitle,
                summaryPoints = summaryPoints,
                eligibilityVerdict = verdict,
                verdictDisplayBadge = verdictBadge,
                eligibilityExplanation = eligibilityExplanation,
                keyBenefits = keyBenefits,
                importantDeadlines = importantDeadlines,
                requiredDocuments = requiredDocuments,
                applicationSteps = applicationSteps,
                voiceScript = voiceScript,
                analyzedLanguage = language
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse structured JSON response", e)
            throw IllegalStateException("Could not parse AI explanation. Please try again.", e)
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val item = optString(i).trim()
                if (item.isNotEmpty()) add(item)
            }
        }
    }

    private fun decodeScaledBitmap(bytes: ByteArray): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
        val width = boundsOptions.outWidth
        val height = boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / sampleSize > MAX_IMAGE_DIMENSION_PX || height / sampleSize > MAX_IMAGE_DIMENSION_PX) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
    }
}
