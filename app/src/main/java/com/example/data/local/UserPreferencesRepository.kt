package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.SupportedLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "linguabridge_prefs")

data class UserPreferences(
    val recordingConsentAccepted: Boolean = false,
    val sourceLanguage: SupportedLanguage = SupportedLanguage.TAGALOG,
    val targetLanguage: SupportedLanguage = SupportedLanguage.SIMPLIFIED_CHINESE,
    val isOnlineAiEnabled: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val RECORDING_CONSENT_ACCEPTED = booleanPreferencesKey("recording_consent_accepted")
        val SOURCE_LANGUAGE_CODE = stringPreferencesKey("source_language_code")
        val TARGET_LANGUAGE_CODE = stringPreferencesKey("target_language_code")
        val IS_ONLINE_AI_ENABLED = booleanPreferencesKey("is_online_ai_enabled")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            recordingConsentAccepted = prefs[Keys.RECORDING_CONSENT_ACCEPTED] ?: false,
            sourceLanguage = SupportedLanguage.fromCode(
                prefs[Keys.SOURCE_LANGUAGE_CODE] ?: SupportedLanguage.TAGALOG.code
            ),
            targetLanguage = SupportedLanguage.fromCode(
                prefs[Keys.TARGET_LANGUAGE_CODE] ?: SupportedLanguage.SIMPLIFIED_CHINESE.code
            ),
            isOnlineAiEnabled = prefs[Keys.IS_ONLINE_AI_ENABLED] ?: true
        )
    }

    suspend fun setRecordingConsentAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RECORDING_CONSENT_ACCEPTED] = accepted
        }
    }

    suspend fun setLanguagePair(source: SupportedLanguage, target: SupportedLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SOURCE_LANGUAGE_CODE] = source.code
            prefs[Keys.TARGET_LANGUAGE_CODE] = target.code
        }
    }

    suspend fun setOnlineAiEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ONLINE_AI_ENABLED] = enabled
        }
    }
}
