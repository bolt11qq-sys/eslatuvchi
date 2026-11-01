package uz.tanish.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.tanish.AppContainer
import uz.tanish.audio.VoicePlayer
import uz.tanish.data.FontSize
import uz.tanish.data.Settings
import uz.tanish.data.ThresholdLevel
import uz.tanish.data.db.Person
import uz.tanish.data.db.PersonWithCount
import uz.tanish.log.ErrorLog

class PeopleViewModel(container: AppContainer) : ViewModel() {
    val people: StateFlow<List<PersonWithCount>?> = container.people.observePeople()
        .catch { e ->
            ErrorLog.error("Odamlar", "Ro'yxatni o'qishda xato", e)
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val settings: StateFlow<Settings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setThreshold(level: ThresholdLevel) = launchSafe { container.settings.setThreshold(level) }
    fun setFontSize(size: FontSize) = launchSafe { container.settings.setFontSize(size) }
    fun setVoiceEnabled(enabled: Boolean) = launchSafe { container.settings.setVoiceEnabled(enabled) }

    private fun launchSafe(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                ErrorLog.error("Sozlamalar", "Sozlamani saqlashda xato", e)
            }
        }
    }
}

data class ResultUiState(
    val loading: Boolean = true,
    val person: Person? = null,
    val voiceEnabled: Boolean = true,
    val playing: Boolean = false,
    val fontScale: Float = 1f,
)

/** Natija ekrani: odam va uning ovozi (TZ, 5-bo'lim, bemor rejimi 3-band). */
class ResultViewModel(private val container: AppContainer, personId: Long) : ViewModel() {
    private val _state = MutableStateFlow(ResultUiState())
    val state: StateFlow<ResultUiState> = _state
    private val player = VoicePlayer()
    private var autoPlayed = false

    init {
        viewModelScope.launch {
            try {
                val (person, settings) = withContext(Dispatchers.IO) {
                    container.people.get(personId) to container.settings.current()
                }
                _state.update {
                    it.copy(loading = false, person = person, voiceEnabled = settings.voiceEnabled, fontScale = settings.fontSize.scale)
                }
                if (!autoPlayed) {
                    autoPlayed = true
                    play()
                }
            } catch (e: Exception) {
                ErrorLog.error("Natija", "Odamni yuklashda xato", e)
                _state.update { it.copy(loading = false) }
            }
        }
    }

    val canPlay: Boolean get() = _state.value.let { it.voiceEnabled && !it.person?.voicePath.isNullOrBlank() }

    fun play() {
        val s = _state.value
        val path = s.person?.voicePath
        if (!s.voiceEnabled || path.isNullOrBlank()) return
        val started = player.play(path) { _state.update { it.copy(playing = false) } }
        _state.update { it.copy(playing = started) }
    }

    fun stop() {
        player.stop()
        _state.update { it.copy(playing = false) }
    }

    override fun onCleared() = player.stop()
}
