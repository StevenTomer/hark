package org.hark.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.hark.domain.QuietHours
import org.hark.domain.Source
import java.io.IOException

data class Settings(
    val enabled: Boolean = true,
    val readSms: Boolean = true,
    val readVoice: Boolean = true,
    val speechRate: Float = 1.0f,
    val quietHours: QuietHours = QuietHours(false, QuietHours.DEFAULT_START, QuietHours.DEFAULT_END),
) {
    fun reads(source: Source) = when (source) {
        Source.SMS -> readSms
        Source.VOICE -> readVoice
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hark_settings")

private object Keys {
    val enabled = booleanPreferencesKey("enabled")
    val readSms = booleanPreferencesKey("read_sms")
    val readVoice = booleanPreferencesKey("read_voice")
    val speechRate = floatPreferencesKey("speech_rate")
    val quietEnabled = booleanPreferencesKey("quiet_enabled")
    val quietStart = intPreferencesKey("quiet_start_minute")
    val quietEnd = intPreferencesKey("quiet_end_minute")
}

class SettingsRepository(private val context: Context, scope: CoroutineScope) {
    val settings: StateFlow<Settings> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { p ->
            val defaults = Settings()
            Settings(
                enabled = p[Keys.enabled] ?: defaults.enabled,
                readSms = p[Keys.readSms] ?: defaults.readSms,
                readVoice = p[Keys.readVoice] ?: defaults.readVoice,
                speechRate = p[Keys.speechRate] ?: defaults.speechRate,
                quietHours = QuietHours(
                    enabled = p[Keys.quietEnabled] ?: defaults.quietHours.enabled,
                    startMinute = p[Keys.quietStart] ?: defaults.quietHours.startMinute,
                    endMinute = p[Keys.quietEnd] ?: defaults.quietHours.endMinute,
                ),
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, Settings())

    suspend fun setEnabled(value: Boolean) = context.dataStore.edit { it[Keys.enabled] = value }
    suspend fun setReadSms(value: Boolean) = context.dataStore.edit { it[Keys.readSms] = value }
    suspend fun setReadVoice(value: Boolean) = context.dataStore.edit { it[Keys.readVoice] = value }
    suspend fun setSpeechRate(value: Float) = context.dataStore.edit { it[Keys.speechRate] = value }
    suspend fun setQuietEnabled(value: Boolean) = context.dataStore.edit { it[Keys.quietEnabled] = value }
    suspend fun setQuietStart(minute: Int) = context.dataStore.edit { it[Keys.quietStart] = minute }
    suspend fun setQuietEnd(minute: Int) = context.dataStore.edit { it[Keys.quietEnd] = minute }
}
