package com.example.model

import java.util.Locale

enum class SupportedLanguage(
    val code: String,
    val bcp47Tag: String,
    val englishName: String,
    val nativeLabel: String,
    val promptInstructionName: String,
    val quickQuestions: List<String>
) {
    ENGLISH(
        code = "en",
        bcp47Tag = "en-IN",
        englishName = "English",
        nativeLabel = "English",
        promptInstructionName = "Simple Indian English",
        quickQuestions = listOf(
            "What is the last date?",
            "Where do I submit this?",
            "What if a document is missing?"
        )
    ),
    KANNADA(
        code = "kn",
        bcp47Tag = "kn-IN",
        englishName = "Kannada",
        nativeLabel = "ಕನ್ನಡ",
        promptInstructionName = "Simple, everyday conversational Kannada (ಕನ್ನಡ script)",
        quickQuestions = listOf(
            "ಅರ್ಜಿ ಸಲ್ಲಿಸಲು ಕೊನೆಯ ದಿನಾಂಕ ಯಾವುದು?",
            "ಇದನ್ನು ಎಲ್ಲಿ ಸಲ್ಲಿಸಬೇಕು?",
            "ದಾಖಲೆ ಇಲ್ಲದಿದ್ದರೆ ಏನು ಮಾಡಬೇಕು?"
        )
    ),
    HINDI(
        code = "hi",
        bcp47Tag = "hi-IN",
        englishName = "Hindi",
        nativeLabel = "हिन्दी",
        promptInstructionName = "Simple, everyday conversational Hindi (Devanagari हिन्दी script)",
        quickQuestions = listOf(
            "आवेदन की अंतिम तारीख क्या है?",
            "यह फॉर्म कहाँ जमा करना है?",
            "यदि कोई दस्तावेज़ न हो तो क्या करें?"
        )
    ),
    TELUGU(
        code = "te",
        bcp47Tag = "te-IN",
        englishName = "Telugu",
        nativeLabel = "తెలుగు",
        promptInstructionName = "Simple, everyday conversational Telugu (తెలుగు script)",
        quickQuestions = listOf(
            "దరఖాస్తుకు చివరి తేదీ ఎప్పుడు?",
            "దీనిని ఎక్కడ సమర్పించాలి?",
            "పత్రం లేకపోతే ఏమి చేయాలి?"
        )
    ),
    TAMIL(
        code = "ta",
        bcp47Tag = "ta-IN",
        englishName = "Tamil",
        nativeLabel = "தமிழ்",
        promptInstructionName = "Simple, everyday conversational Tamil (தமிழ் script)",
        quickQuestions = listOf(
            "விண்ணப்பிக்க கடைசி தேதி எது?",
            "இதை எங்கே சமர்ப்பிக்க வேண்டும்?",
            "ஆவணம் இல்லாவிட்டால் என்ன செய்வது?"
        )
    );

    fun toLocale(): Locale = Locale.forLanguageTag(bcp47Tag)

    companion object {
        fun fromCode(code: String?): SupportedLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class OccupationCategory(val label: String) {
    STUDENT("Student"),
    FARMER("Farmer"),
    UNORGANIZED_WORKER("Unorganized Worker"),
    SMALL_BUSINESS("Small Business"),
    SENIOR_CITIZEN("Senior Citizen"),
    OTHER("Other");

    companion object {
        fun fromLabel(label: String?): OccupationCategory =
            entries.firstOrNull { it.label == label } ?: STUDENT
    }
}

data class CitizenProfile(
    val age: String = "21",
    val occupation: OccupationCategory = OccupationCategory.STUDENT,
    val annualIncomeInr: String = "180000",
    val state: String = "Karnataka",
    val additionalContext: String = "B.E. engineering student, BPL card holder"
)

data class AttachedDocument(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val sizeBytes: Long,
    val isImage: Boolean
) {
    val formattedSize: String
        get() = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format(Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AttachedDocument) return false
        return fileName == other.fileName &&
            mimeType == other.mimeType &&
            sizeBytes == other.sizeBytes &&
            isImage == other.isImage
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + sizeBytes.hashCode()
        result = 31 * result + isImage.hashCode()
        return result
    }
}

enum class EligibilityVerdict {
    ELIGIBLE,
    PARTIALLY_ELIGIBLE,
    NOT_ELIGIBLE;

    companion object {
        fun fromWire(value: String): EligibilityVerdict {
            val normalized = value.uppercase(Locale.US).trim()
            return when {
                normalized == "ELIGIBLE" -> ELIGIBLE
                normalized.contains("NOT") || normalized.contains("INELIGIBLE") -> NOT_ELIGIBLE
                else -> PARTIALLY_ELIGIBLE
            }
        }
    }
}

data class SchemeAnalysisResult(
    val documentTitle: String,
    val summaryPoints: List<String>,
    val eligibilityVerdict: EligibilityVerdict,
    val verdictDisplayBadge: String,
    val eligibilityExplanation: String,
    val keyBenefits: List<String>,
    val importantDeadlines: String,
    val requiredDocuments: List<String>,
    val applicationSteps: List<String>,
    val voiceScript: String,
    val analyzedLanguage: SupportedLanguage
)

data class FollowUpMessage(
    val id: Long,
    val question: String,
    val answer: String,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Loading(val stepIndex: Int) : AnalysisUiState
    data class Success(val result: SchemeAnalysisResult) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

data class SampleNoticePreset(
    val id: String,
    val shortTitle: String,
    val categoryTag: String,
    val subtitle: String,
    val noticeText: String,
    val suggestedProfile: CitizenProfile
)

object SamplePresets {
    val all: List<SampleNoticePreset> = listOf(
        SampleNoticePreset(
            id = "scholarship_ssp",
            shortTitle = "Post-Matric Scholarship",
            categoryTag = "Education",
            subtitle = "State SSP circular for college & engineering students",
            noticeText = """
GOVERNMENT OF KARNATAKA — DEPARTMENT OF BACKWARD CLASSES & SOCIAL WELFARE
CIRCULAR NO. SSP/PM-SCH/2026-27/412 | DATED: 02 NOVEMBER 2026
SUBJECT: INVITATION OF ONLINE APPLICATIONS FOR STATE POST-MATRIC SCHOLARSHIP (SSP) FOR ACADEMIC YEAR 2026-27

1. OBJECTIVE: Financial assistance toward tuition fee reimbursement, mandatory non-refundable institutional fees, and annual maintenance allowance (Vidyasiri / Food & Accommodation grant) for domicile students of Karnataka pursuing Post-Matric courses (PUC, ITI, Diploma, Undergraduate, B.E./B.Tech, MBBS, Postgraduate, Ph.D.) in Government or AICTE/UGC-recognized institutions.

2. ELIGIBILITY CRITERIA:
   a) Applicant must be a permanent resident / domicile of Karnataka State.
   b) Annual family income ceiling from all sources:
      - Up to ₹2,50,000 per annum for SC / ST and Category-1 students.
      - Up to ₹1,00,000 per annum for OBC (2A, 3A, 3B) and Minority communities; up to ₹2,50,000 for professional engineering/medical degree seats allotted via CET/KEA counseling.
      - BPL Ration Card holders receive priority verification for hostel/maintenance allowance.
   c) Minimum 75% attendance in the previous academic semester and promotion to next year.
   d) Bank account MUST be Aadhaar-seeded (NPCI DBT enabled).

3. KEY BENEFITS:
   - Full reimbursement of statutory tuition & examination fees (up to ₹65,000/year for CET Engineering seats).
   - Annual Maintenance / Vidyasiri allowance of ₹15,000/year (₹1,500 per month for 10 months) directly credited via DBT for day scholars not residing in government hostels.

4. MANDATORY DOCUMENTS & VERIFICATION:
   - Valid Caste & Income Certificate with RD Number (issued by Nadakacheri / Tahsildar).
   - Aadhaar Card of student and parent with e-KYC OTP verification.
   - College Study / Bonafide Certificate & Fee Receipt bearing SATS / University USN.
   - Previous year marks card and CET/KEA allotment letter (if applicable).
   - Aadhaar-linked Savings Bank Passbook front page.

5. IMPORTANT DEADLINES & SUBMISSION:
   - Online Portal: https://ssp.postmatric.karnataka.gov.in
   - Last Date for Fresh & Renewal Online Submission: 31 December 2026.
   - Institutional e-Attestation Deadline: 15 January 2027.
   - Helpline: 1902 (24x7 Citizen Scholarship Desk) or District Taluk Welfare Office.
            """.trimIndent(),
            suggestedProfile = CitizenProfile(
                age = "21",
                occupation = OccupationCategory.STUDENT,
                annualIncomeInr = "180000",
                state = "Karnataka",
                additionalContext = "B.E. engineering student via CET, BPL card holder"
            )
        ),
        SampleNoticePreset(
            id = "farmer_crop_subsidy",
            shortTitle = "Farmer Crop Subsidy",
            categoryTag = "Agriculture",
            subtitle = "Crop insurance, millet incentive & input subsidy notice",
            noticeText = """
DIRECTORATE OF AGRICULTURE — INTEGRATED FARMER WELFARE & CROP SUBSIDY CIRCULAR
NOTIFICATION NO. AGRI/DBT-SUB/RABI-2026/889 | STATE & CENTRAL CONVERGENCE SCHEME

1. SCHEME SUMMARY:
   Small and marginal landholding farmers registered on the FRUITS (Farmer Registration and Unified Beneficiary Information System) portal are eligible for direct input subsidy, micro-irrigation grants, and subsidized crop insurance under PMFBY & Raitha Siri Millet Incentive during the Rabi 2026-27 season.

2. ELIGIBILITY CONDITIONS:
   a) Must be a resident cultivator with cultivable agricultural landholding up to 2 Hectares (5 acres) recorded in RTC / Pahani (Record of Rights, Tenancy & Crops).
   b) Tenant farmers with registered lease agreements attested by the Village Accountant may apply for crop insurance coverage.
   c) Institutional landholders, current income-tax payers, and households with annual non-agricultural income exceeding ₹3,00,000 are NOT eligible for the additional state input grant.
   d) Mandatory FRUITS FID (Farmer ID) linked with Aadhaar e-KYC and active bank account.

3. FINANCIAL SUBSIDY & RIGHTS:
   - ₹10,000 per hectare Direct Benefit Transfer (DBT) incentive for certified millet & pulse growers (maximum 2 hectares = ₹20,000).
   - Subsidized Crop Insurance (PMFBY): Farmer pays only 1.5% premium for Rabi crops and 2% for Kharif crops; Government pays remaining premium.
   - Drip / Sprinkler Irrigation Subsidy: 90% subsidy for SC/ST/Small farmers, 75% for other categories.
   - RIGHT TO TIMELY CLAIM: In case of localized hailstorm, flood, or landslide crop loss, farmer has the right to report loss within 72 hours via the Krishi Rakshak Helpline (14447) or Samrakshane portal for mandatory field survey within 10 days.

4. REQUIRED DOCUMENTS:
   - FRUITS Farmer ID (FID) or Land Record RTC / Pahani copy.
   - Aadhaar Card and Bank Passbook linked to NPCI DBT.
   - Sowing Certificate / Crop Survey acknowledgment from Raitha Samparka Kendra (RSK).
   - Caste Certificate (only if claiming 90% SC/ST micro-irrigation tier).

5. DEADLINES & HOW TO APPLY:
   - Apply at nearest Raitha Samparka Kendra (RSK), Grama One, or online at fruits.karnataka.gov.in / pmfby.gov.in.
   - Last Date for Rabi Crop Insurance Enrollment & Input Subsidy Registration: 15 December 2026.
   - Toll-free Farmer Helpline: 14447 / 1800-425-3553.
            """.trimIndent(),
            suggestedProfile = CitizenProfile(
                age = "44",
                occupation = OccupationCategory.FARMER,
                annualIncomeInr = "145000",
                state = "Karnataka",
                additionalContext = "Small farmer with 3 acres dryland, growing ragi/millets, has RTC"
            )
        ),
        SampleNoticePreset(
            id = "tenant_consumer_rights",
            shortTitle = "Tenant & Consumer Rights",
            categoryTag = "Citizen Rights",
            subtitle = "Rental deposit caps, eviction protection & consumer refund rules",
            noticeText = """
PUBLIC CITIZEN RIGHTS ADVISORY — HOUSING TENANCY & CONSUMER PROTECTION GUIDELINES
ISSUED BY: DEPARTMENT OF FOOD, CIVIL SUPPLIES, CONSUMER AFFAIRS & URBAN HOUSING

PART A: RESIDENTIAL TENANT PROTECTIONS (MODEL TENANCY FRAMEWORK & STATE RENT GUIDELINES)
1. SECURITY DEPOSIT CAP: For residential premises, security deposit demanded by a landlord shall not exceed TWO (2) months' rent (and maximum 6 months for non-residential commercial shops), unless otherwise governed by older pre-enactment registered leases. Deposit MUST be refunded within 15 days of vacating after deducting only documented, itemized unpaid rent or proven physical damage beyond normal wear and tear.
2. WRITTEN AGREEMENT & RECEIPTS: Every tenancy must have a written rental agreement; landlords are obligated to issue rent receipts (digital UPI/bank transfer records count as valid proof of rent payment).
3. PRIVACY & NOTICE OF ENTRY: Landlords or property managers CANNOT enter a rented home without giving at least 24 hours' prior written or WhatsApp/SMS notice (except during fire/flood emergency), and only between 7:00 AM and 8:00 PM.
4. BAN ON ILLEGAL UTILITY DISCONNECTION: Landlords are strictly prohibited from cutting off electricity, drinking water, lift access, or locking out a tenant to force eviction or settle a rent dispute. Violation is punishable before the Rent Authority with restoration orders and compensation.

PART B: CITIZEN CONSUMER RIGHTS (CONSUMER PROTECTION ACT, 2019 — E-DAAKHIL)
1. DEFECTIVE GOODS & UNFAIR DEDUCTIONS: Any buyer or service user (including coaching centers, e-commerce platforms, hospitals, or housing builders) has the statutory right to a replacement, repair, or full refund if services are deficient or hidden charges are imposed.
2. ZERO-FEE FILING THRESHOLD: Filing a complaint on the national E-Daakhil / e-Jagriti portal (edaakhil.nic.in) for claims up to ₹5,00,000 carries ZERO court fee (₹0) and does NOT require hiring a lawyer.
3. TIME LIMIT: Consumer complaints can be filed within 2 years from the date of dispute.

HOW TO ENFORCE YOUR RIGHTS:
- Step 1: Send a clear written notice (email/registered post/WhatsApp) giving 7–15 days to resolve or refund.
- Step 2: Gather rental agreement, UPI/bank payment proof, bills, photos, and chat logs.
- Step 3: Call National Consumer Helpline 1915 (or WhatsApp 8800001915) for fast pre-litigation mediation, or file online at edaakhil.nic.in / District Rent Authority office.
            """.trimIndent(),
            suggestedProfile = CitizenProfile(
                age = "27",
                occupation = OccupationCategory.UNORGANIZED_WORKER,
                annualIncomeInr = "260000",
                state = "Karnataka",
                additionalContext = "Rented 1BHK apartment in Bengaluru, landlord withholding 5-month deposit"
            )
        )
    )
}
