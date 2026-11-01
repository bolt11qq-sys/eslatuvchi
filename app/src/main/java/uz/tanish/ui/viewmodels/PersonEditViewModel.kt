package uz.tanish.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.tanish.AppContainer
import uz.tanish.R
import uz.tanish.audio.VoicePlayer
import uz.tanish.audio.VoiceRecorder
import uz.tanish.data.db.Person
import uz.tanish.log.ErrorLog
import java.io.File

enum class EditMessage { NONE, REQUIRED, SAVED, PHOTO_ERROR, VOICE_ERROR, ERROR }

data class PersonEditUiState(
    val loading: Boolean = true,
    val id: Long = 0,
    val name: String = "",
    val relation: String = "",
    val displayText: String = "",
    val displayEdited: Boolean = false,
    val photoPath: String = "",
    val pendingPhoto: Uri? = null,
    val voicePath: String? = null,
    val sampleCount: Int = 0,
    val recording: Boolean = false,
    val recordSeconds: Int = 0,
    val playing: Boolean = false,
    val message: EditMessage = EditMessage.NONE,
    val deleted: Boolean = false,
    val busy: Boolean = false,
)

class PersonEditViewModel(
    private val container: AppContainer,
    private val appContext: Context,
    initialId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(PersonEditUiState(id = initialId))
    val state: StateFlow<PersonEditUiState> = _state

    private val recorder = VoiceRecorder(appContext)
    private val player = VoicePlayer()
    private var timerJob: Job? = null

    init {
        if (initialId > 0) {
            viewModelScope.launch {
                val p = safe { container.people.get(initialId) }
                if (p != null) {
                    _state.update {
                        it.copy(
                            loading = false,
                            name = p.name,
                            relation = p.relation,
                            displayText = p.displayText,
                            displayEdited = p.displayText != autoText(p.relation, p.name),
                            photoPath = p.photoPath,
                            voicePath = p.voicePath,
                        )
                    }
                    refresh()
                } else {
                    _state.update { it.copy(loading = false) }
                }
            }
        } else {
            _state.update { it.copy(loading = false) }
        }
    }

    /** Boshqa ekrandan qaytilganda (namunalar soni, rasm o'zgargan bo'lishi mumkin). */
    fun refresh() {
        val id = _state.value.id
        if (id <= 0) return
        viewModelScope.launch {
            val p = safe { container.people.get(id) } ?: return@launch
            val count = safe { container.people.sampleCount(id) } ?: 0
            _state.update { it.copy(photoPath = p.photoPath, voicePath = p.voicePath, sampleCount = count) }
        }
    }

    private fun autoText(relation: String, name: String): String =
        appContext.getString(R.string.display_text_format, relation.trim(), name.trim()).trim()

    fun setName(v: String) = _state.update {
        it.copy(name = v, displayText = if (it.displayEdited) it.displayText else autoText(it.relation, v), message = EditMessage.NONE)
    }

    fun setRelation(v: String) = _state.update {
        it.copy(relation = v, displayText = if (it.displayEdited) it.displayText else autoText(v, it.name), message = EditMessage.NONE)
    }

    fun setDisplayText(v: String) = _state.update { it.copy(displayText = v, displayEdited = true, message = EditMessage.NONE) }

    fun pickPhoto(uri: Uri) {
        val id = _state.value.id
        if (id <= 0) {
            _state.update { it.copy(pendingPhoto = uri) }
            return
        }
        viewModelScope.launch {
            val p = safe { container.people.setPhotoFromUri(id, uri) }
            if (p == null) _state.update { it.copy(message = EditMessage.PHOTO_ERROR) }
            else _state.update { it.copy(photoPath = p.photoPath) }
        }
    }

    fun save() {
        val s = _state.value
        if (s.busy) return
        val name = s.name.trim()
        val relation = s.relation.trim()
        if (name.isEmpty() || relation.isEmpty()) {
            _state.update { it.copy(message = EditMessage.REQUIRED) }
            return
        }
        val display = s.displayText.trim().ifEmpty { autoText(relation, name) }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val id: Long = withContext(Dispatchers.IO) {
                    if (s.id <= 0) {
                        container.people.insert(
                            Person(
                                name = name,
                                relation = relation,
                                displayText = display,
                                photoPath = "",
                                voicePath = null,
                                createdAt = System.currentTimeMillis(),
                            )
                        )
                    } else {
                        val old = container.people.get(s.id)
                        if (old != null) {
                            container.people.update(old.copy(name = name, relation = relation, displayText = display))
                        }
                        s.id
                    }
                }
                var photoPath = s.photoPath
                var photoFailed = false
                s.pendingPhoto?.let { uri ->
                    val p = safe { container.people.setPhotoFromUri(id, uri) }
                    if (p != null) photoPath = p.photoPath else photoFailed = true
                }
                _state.update {
                    it.copy(
                        id = id,
                        name = name,
                        relation = relation,
                        displayText = display,
                        photoPath = photoPath,
                        pendingPhoto = null,
                        busy = false,
                        message = if (photoFailed) EditMessage.PHOTO_ERROR else EditMessage.SAVED,
                    )
                }
                refresh()
            } catch (e: Exception) {
                ErrorLog.error("Odam", "Odamni saqlashda xato", e)
                _state.update { it.copy(busy = false, message = EditMessage.ERROR) }
            }
        }
    }

    fun delete() {
        val id = _state.value.id
        stopAll()
        if (id <= 0) {
            _state.update { it.copy(deleted = true) }
            return
        }
        viewModelScope.launch {
            try {
                container.people.delete(id)
                ErrorLog.info("Odam", "Odam #$id va uning fayllari o'chirildi")
                _state.update { it.copy(deleted = true) }
            } catch (e: Exception) {
                ErrorLog.error("Odam", "Odamni o'chirishda xato", e)
                _state.update { it.copy(message = EditMessage.ERROR) }
            }
        }
    }

    // ---- Ovoz (TZ, 5-bo'lim, 5-band) ----

    private fun tempVoiceFile(id: Long): File =
        File(container.people.voiceFileFor(id).parentFile, "person_${id}_tmp.m4a")

    fun startRecording() {
        val id = _state.value.id
        if (id <= 0 || recorder.isRecording) return
        player.stop()
        val tmp = tempVoiceFile(id)
        try {
            tmp.delete()
            recorder.start(tmp) { viewModelScope.launch { stopRecording() } }
        } catch (e: Exception) {
            ErrorLog.error("Ovoz", "Yozishni boshlab bo'lmadi", e)
            _state.update { it.copy(recording = false, playing = false, message = EditMessage.VOICE_ERROR) }
            return
        }
        val started = SystemClock.elapsedRealtime()
        _state.update { it.copy(recording = true, playing = false, recordSeconds = 0, message = EditMessage.NONE) }
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val sec = ((SystemClock.elapsedRealtime() - started) / 1000).toInt()
                _state.update { it.copy(recordSeconds = sec.coerceAtMost(VoiceRecorder.MAX_SECONDS)) }
                if (sec > VoiceRecorder.MAX_SECONDS) {
                    stopRecording()
                    break
                }
                delay(250)
            }
        }
    }

    fun stopRecording() {
        if (!recorder.isRecording) return
        timerJob?.cancel()
        val id = _state.value.id
        val ok = recorder.stop()
        _state.update { it.copy(recording = false) }
        if (!ok) {
            tempVoiceFile(id).delete()
            _state.update { it.copy(message = EditMessage.VOICE_ERROR) }
            return
        }
        viewModelScope.launch {
            try {
                val finalFile = container.people.voiceFileFor(id)
                withContext(Dispatchers.IO) {
                    finalFile.delete()
                    if (!tempVoiceFile(id).renameTo(finalFile)) throw IllegalStateException("Ovoz faylini ko'chirib bo'lmadi")
                    container.people.setVoice(id, finalFile.absolutePath)
                }
                _state.update { it.copy(voicePath = finalFile.absolutePath) }
            } catch (e: Exception) {
                ErrorLog.error("Ovoz", "Ovozni saqlashda xato", e)
                _state.update { it.copy(message = EditMessage.VOICE_ERROR) }
            }
        }
    }

    fun togglePlay() {
        val path = _state.value.voicePath ?: return
        if (player.isPlaying) {
            player.stop()
            _state.update { it.copy(playing = false) }
            return
        }
        val started = player.play(path) { _state.update { it.copy(playing = false) } }
        _state.update { it.copy(playing = started, message = if (started) it.message else EditMessage.VOICE_ERROR) }
    }

    fun deleteVoice() {
        val id = _state.value.id
        stopAll()
        viewModelScope.launch {
            safe { container.people.setVoice(id, null) }
            _state.update { it.copy(voicePath = null) }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = EditMessage.NONE) }

    /** Ekrandan chiqilganda yoki ilova fonga o'tganda. */
    fun stopAll() {
        if (recorder.isRecording) stopRecording()
        player.stop()
        _state.update { it.copy(playing = false) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        recorder.stopQuietly()
        player.stop()
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: Exception) {
        ErrorLog.error("Odam", "Ma'lumotlar bilan ishlashda xato", e)
        null
    }
}
