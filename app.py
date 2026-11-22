import io
import os
from google import genai
from google.genai import types
from gtts import gTTS
import streamlit as st

st.set_page_config(
    page_title="JanSetu AI — Multilingual Scheme & Rights Explainer",
    page_icon="🏛️",
    layout="wide",
)

# Initialize Gemini Client using Streamlit Secrets or Environment Variable
api_key = st.secrets.get("GEMINI_API_KEY") or os.environ.get("GEMINI_API_KEY")
if not api_key:
  st.error("Please add GEMINI_API_KEY to your Streamlit Secrets or environment.")
  st.stop()

client = genai.Client(api_key=api_key)

# Language mapping for Gemini + gTTS voice output
LANGUAGES = {
    "Kannada (ಕನ್ನಡ)": "kn",
    "Hindi (हिन्दी)": "hi",
    "Telugu (తెలుగు)": "te",
    "Tamil (தமிழ்)": "ta",
    "English": "en",
}

st.title("🏛️ JanSetu AI: Multilingual Policy, Rights & Scheme Explainer")
st.caption(
    "Upload any government notice, welfare scheme PDF, or legal circular to"
    " understand your rights, check eligibility, and listen in your language."
)

# Sidebar for User Profile (Eligibility Check)
with st.sidebar:
  st.header("👤 Citizen Profile (For Eligibility)")
  selected_lang = st.selectbox("Preferred Language", list(LANGUAGES.keys()))
  lang_code = LANGUAGES[selected_lang]

  age = st.number_input("Age", min_value=1, max_value=100, value=20)
  occupation = st.selectbox(
      "Occupation / Category",
      [
          "Student",
          "Farmer",
          "Small Business / Worker",
          "Senior Citizen",
          "Unemployed",
          "Other",
      ],
  )
  annual_income = st.text_input("Annual Family Income (INR)", value="2,50,000")
  state = st.text_input("State", value="Karnataka")
  extra_details = st.text_area(
      "Additional Details (Optional)",
      placeholder="e.g., B.E. 3rd year student, BPL card holder, etc.",
  )

# Built-in Sample Notice for One-Click Testing
SAMPLE_SCHEMES = {
    "None (Use my uploaded PDF or pasted text)": "",
    "Post-Matric Scholarship Circular (Karnataka SSP)": (
        "Government of Karnataka - State Scholarship Portal (SSP) Post-Matric"
        " Scholarship 2026-27. Eligibility: Permanent resident of Karnataka;"
        " studying in Post-Matric courses (PUC, Degree, B.E./B.Tech, Diploma,"
        " PG); Annual family income must not exceed Rs. 2,50,000 for SC/ST/OBC"
        " categories. Benefits: Full tuition fee reimbursement and maintenance"
        " allowance up to Rs. 15,000/year directly via DBT. Required"
        " Documents: Aadhaar card, Caste & Income Certificate (RD number),"
        " Previous year marks card, College admission registration ID, Bank"
        " account seeded with Aadhaar."
    ),
    "PM-KISAN & State Crop Insurance Advisory": (
        "Pradhan Mantri Kisan Samman Nidhi (PM-KISAN) & Fasal Bima Yojana."
        " Eligibility: All landholding farmer families with cultivable land"
        " records. Benefits: Rs. 6,000 per year in three equal installments of"
        " Rs. 2,000 directly into Aadhaar-linked bank accounts, plus crop loss"
        " coverage against unseasonal rain and drought. Required Documents:"
        " Land ownership papers (RTC/Pahani), Aadhaar card, active bank"
        " passbook, e-KYC completion."
    ),
}

# Main Input Area
col1, col2 = st.columns([1, 1])

with col1:
  st.subheader("1️⃣ Upload Notice or Select Sample")
  sample_choice = st.selectbox(
      "Quick Test with a Sample Scheme:", list(SAMPLE_SCHEMES.keys())
  )
  uploaded_pdf = st.file_uploader(
      "Or Upload Government Notice / Scheme PDF", type=["pdf"]
  )
  pasted_text = st.text_area(
      "Or paste scheme text / notice here:",
      value=SAMPLE_SCHEMES[sample_choice],
      height=180,
      placeholder=(
          "Paste any scholarship rule, government scheme circular, or"
          " citizen right notice..."
      ),
  )

  analyze_btn = st.button(
      "✨ Explain & Check My Eligibility",
      type="primary",
      use_container_width=True,
  )

with col2:
  st.subheader(f"2️⃣ Simplified Guide & Eligibility ({selected_lang})")

  if analyze_btn:
    if not uploaded_pdf and not pasted_text.strip():
      st.warning(
          "Please select a sample scheme, upload a PDF, or paste some text"
          " first."
      )
    else:
      with st.spinner(
          "Analyzing official document and checking eligibility..."
      ):
        prompt = f"""
                You are an expert civic assistant helping Indian citizens understand government schemes, policies, and legal notices.
                Analyze the provided document/text and respond strictly in {selected_lang} (keep key official document/scheme titles clear).

                Structure your response clearly with these 4 sections:
                1. **Simple Summary (ಜನಸಾಮಾನ್ಯರಿಗೆ ಸರಳ ವಿವರಣೆ / Plain Language):** Explain what this notice/scheme is about in 3-4 simple bullet points without bureaucratic jargon.
                2. **Your Key Rights & Benefits:** What money, scholarship, subsidy, or legal protection does the citizen get?
                3. **Eligibility Verdict for This User:**
                   Evaluate against this user profile:
                   - Age: {age}
                   - Occupation: {occupation}
                   - Annual Income: ₹{annual_income}
                   - State: {state}
                   - Other details: {extra_details}
                   Clearly state **ELIGIBLE**, **NOT ELIGIBLE**, or **PARTIALLY ELIGIBLE / NEEDS VERIFICATION**, and explain why in 2 lines.
                4. **Action Checklist & Documents Needed:** Exact documents required and next steps to apply.
                """

        contents = []
        if uploaded_pdf is not None:
          pdf_bytes = uploaded_pdf.read()
          contents.append(
              types.Part.from_bytes(
                  data=pdf_bytes, mime_type="application/pdf"
              )
          )
        if pasted_text.strip():
          contents.append(f"Document Text:\n{pasted_text}")
        contents.append(prompt)

        try:
          response = client.models.generate_content(
              model="gemini-2.5-flash",
              contents=contents,
          )
          result_text = response.text
          st.markdown(result_text)

          # Generate Voice Output using gTTS
          clean_speech_text = result_text.replace("*", "").replace("#", "")[
              :1200
          ]
          tts = gTTS(text=clean_speech_text, lang=lang_code, slow=False)
          audio_fp = io.BytesIO()
          tts.write_to_fp(audio_fp)
          audio_fp.seek(0)

          st.divider()
          st.markdown("🔊 **Listen to Audio Explanation:**")
          st.audio(audio_fp, format="audio/mp3")

        except Exception as e:
          st.error(f"Error generating explanation: {e}")
