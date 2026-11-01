package uz.tanish.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.tanish.log.ErrorLog
import uz.tanish.security.PinHasher

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Ishonch chegarasi tanlovi (TZ, 3- va 5-bo'lim). */
enum class ThresholdLevel { STRICT, MEDIUM, SOFT }

/** Bemor ekranlaridagi shrift o'lchami. */
enum class FontSize(val scale: Float) { NORMAL(1.0f), LARGE(1.2f), XLARGE(1.4f) }

data class Settings(
    val hasPin: Boolean,
    val threshold: ThresholdLevel,
    val fontSize: FontSize,
    val voiceEnabled: Boolean,
    val disclaimerAccepted: Boolean,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val threshold = stringPreferencesKey("threshold")
        val fontSize = stringPreferencesKey("font_size")
        val voiceEnabled = booleanPreferencesKey("voice_enabled")
        val disclaimer = booleanPreferencesKey("disclaimer_accepted")
    }

    private val prefs: Flow<Preferences> = context.dataStore.data.catch { e ->
        ErrorLog.error("Sozlamalar", "Sozlamalarni o'qishda xato", e)
        emit(emptyPreferences())
    }

    val settings: Flow<Settings> = prefs.map { p ->
        Settings(
            hasPin = p[Keys.pinHash] != null,
            threshold = enumOr(p[Keys.threshold], ThresholdLevel.MEDIUM),
            fontSize = enumOr(p[Keys.fontSize], FontSize.NORMAL),
            voiceEnabled = p[Keys.voiceEnabled] ?: true,
            disclaimerAccepted = p[Keys.disclaimer] ?: false,
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun setPin(pin: String) {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        context.dataStore.edit {
            it[Keys.pinSalt] = salt
            it[Keys.pinHash] = hash
        }
    }

    suspend fun checkPin(pin: String): Boolean {
        val p = prefs.first()
        val salt = p[Keys.pinSalt] ?: return false
        val hash = p[Keys.pinHash] ?: return false
        return PinHasher.verify(pin, salt, hash)
    }

    suspend fun setThreshold(level: ThresholdLevel) = context.dataStore.edit { it[Keys.threshold] = level.name }
    suspend fun setFontSize(size: FontSize) = context.dataStore.edit { it[Keys.fontSize] = size.name }
    suspend fun setVoiceEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.voiceEnabled] = enabled }
    suspend fun acceptDisclaimer() = context.dataStore.edit { it[Keys.disclaimer] = true }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default
}
