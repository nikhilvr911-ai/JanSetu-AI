package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CitizenProfile
import com.example.model.OccupationCategory
import com.example.model.SupportedLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.janSetuDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jansetu_citizen_prefs"
)

data class StoredCitizenPrefs(
    val language: SupportedLanguage = SupportedLanguage.ENGLISH,
    val profile: CitizenProfile = CitizenProfile()
)

class ProfilePreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("preferred_language")
        val AGE = stringPreferencesKey("citizen_age")
        val OCCUPATION = stringPreferencesKey("citizen_occupation")
        val INCOME = stringPreferencesKey("citizen_income")
        val STATE = stringPreferencesKey("citizen_state")
        val CONTEXT = stringPreferencesKey("citizen_context")
    }

    val preferencesFlow: Flow<StoredCitizenPrefs> = context.janSetuDataStore.data
        .catch { e ->
            if (e is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw e
            }
        }
        .map { prefs ->
            val defaultProfile = CitizenProfile()
            StoredCitizenPrefs(
                language = SupportedLanguage.fromCode(prefs[Keys.LANGUAGE]),
                profile = CitizenProfile(
                    age = prefs[Keys.AGE] ?: defaultProfile.age,
                    occupation = OccupationCategory.fromLabel(prefs[Keys.OCCUPATION]),
                    annualIncomeInr = prefs[Keys.INCOME] ?: defaultProfile.annualIncomeInr,
                    state = prefs[Keys.STATE] ?: defaultProfile.state,
                    additionalContext = prefs[Keys.CONTEXT] ?: defaultProfile.additionalContext
                )
            )
        }

    suspend fun saveLanguage(language: SupportedLanguage) {
        context.janSetuDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun saveProfile(profile: CitizenProfile) {
        context.janSetuDataStore.edit { prefs ->
            prefs[Keys.AGE] = profile.age
            prefs[Keys.OCCUPATION] = profile.occupation.label
            prefs[Keys.INCOME] = profile.annualIncomeInr
            prefs[Keys.STATE] = profile.state
            prefs[Keys.CONTEXT] = profile.additionalContext
        }
    }
}
