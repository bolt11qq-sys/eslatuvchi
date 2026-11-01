package uz.tanish.ui.viewmodels

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.tanish.AppContainer
import uz.tanish.face.FaceEmbedder
import uz.tanish.face.FaceFrame
import uz.tanish.face.FaceQuality
import uz.tanish.face.RecognitionConfig
import uz.tanish.log.ErrorLog
import kotlin.math.abs
import kotlin.math.sign

enum class Pose { FRONT, LEFT, RIGHT, UP, DOWN }

/** Ko'rsatma va undagi namunalar soni. Jami — [RecognitionConfig.ENROLL_TARGET]. */
private val STEPS = listOf(
    Pose.FRONT to 3,
    Pose.LEFT to 2,
    Pose.RIGHT to 2,
    Pose.UP to 2,
    Pose.DOWN to 2,
    Pose.FRONT to 1,
)

enum class CaptureStatus { NO_FACE, CLOSER, BLURRY, DARK, TURNED, FOLLOW, GOOD }

data class CaptureUiState(
    val ready: Boolean = false,
    val modelError: Boolean = false,
    val cameraError: Boolean = false,
    val embedder: FaceEmbedder? = null,
    val count: Int = 0,
    val target: Int = RecognitionConfig.ENROLL_TARGET,
    val pose: Pose = Pose.FRONT,
    val status: CaptureStatus = CaptureStatus.NO_FACE,
    val done: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val saveError: Boolean = false,
) {
    val canSave: Boolean get() = count >= RecognitionConfig.ENROLL_MIN && !saving
}

/** "Yuz namunalarini olish" ekrani (TZ, 5-bo'lim, 4-band). */
class FaceCaptureViewModel(private val container: AppContainer, private val personId: Long) : ViewModel() {
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state

    private val samples = mutableListOf<FloatArray>()
    private var portrait: Bitmap? = null
    private var stepIndex = 0
    private var stepTaken = 0
    private var stepStartedAt = SystemClock.elapsedRealtime()
    private var lastSampleAt = 0L
    private var yawSign = 0f
    private var pitchSign = 0f

    init {
        viewModelScope.launch {
            val embedder = withContext(Dispatchers.IO) { container.embedder() }
            _state.update { it.copy(ready = embedder != null, modelError = embedder == null, embedder = embedder) }
        }
    }

    fun onCameraError() = _state.update { it.copy(cameraError = true) }

    fun restart() {
        samples.clear()
        portrait = null
        stepIndex = 0
        stepTaken = 0
        yawSign = 0f
        pitchSign = 0f
        stepStartedAt = SystemClock.elapsedRealtime()
        _state.update { it.copy(count = 0, pose = STEPS[0].first, done = false, status = CaptureStatus.NO_FACE) }
    }

    fun onFrame(frame: FaceFrame) {
        val s = _state.value
        if (s.done || s.saving || s.saved) return

        val status = when (frame.quality) {
            FaceQuality.NO_FACE -> CaptureStatus.NO_FACE
            FaceQuality.TOO_SMALL -> CaptureStatus.CLOSER
            FaceQuality.BLURRY -> CaptureStatus.BLURRY
            FaceQuality.DARK -> CaptureStatus.DARK
            FaceQuality.TURNED -> CaptureStatus.TURNED
            FaceQuality.OK -> null
        }
        if (status != null) {
            _state.update { it.copy(status = status) }
            return
        }
        val embedding = frame.embedding ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastSampleAt < RecognitionConfig.ENROLL_INTERVAL_MS) return

        val pose = STEPS[stepIndex].first
        val relaxed = now - stepStartedAt > RecognitionConfig.ENROLL_STEP_RELAX_MS
        if (!relaxed && !matchesPose(pose, frame.yaw, frame.pitch)) {
            _state.update { it.copy(status = CaptureStatus.FOLLOW) }
            return
        }
        rememberSigns(pose, frame.yaw, frame.pitch)

        samples += embedding
        lastSampleAt = now
        if (portrait == null && pose == Pose.FRONT && frame.portrait != null) portrait = frame.portrait

        stepTaken++
        if (stepTaken >= STEPS[stepIndex].second && stepIndex < STEPS.lastIndex) {
            stepIndex++
            stepTaken = 0
            stepStartedAt = now
        }
        val done = samples.size >= RecognitionConfig.ENROLL_TARGET
        _state.update {
            it.copy(count = samples.size, pose = STEPS[stepIndex].first, status = CaptureStatus.GOOD, done = done)
        }
    }

    /**
     * Chap/o'ng va yuqori/past yo'nalishlarining ML Kit'dagi ishorasi kameraga bog'liq bo'lishi mumkin,
     * shuning uchun birinchi yo'nalishda burilish istalgan tomonga qabul qilinadi, ikkinchisida — teskari tomonga.
     */
    private fun matchesPose(pose: Pose, yaw: Float, pitch: Float): Boolean {
        val minA = RecognitionConfig.POSE_MIN_ANGLE
        val maxA = RecognitionConfig.POSE_MAX_ANGLE
        return when (pose) {
            Pose.FRONT -> abs(yaw) <= RecognitionConfig.FRONT_MAX_ANGLE && abs(pitch) <= RecognitionConfig.FRONT_MAX_ANGLE
            Pose.LEFT -> abs(yaw) in minA..maxA
            Pose.RIGHT -> abs(yaw) in minA..maxA && (yawSign == 0f || sign(yaw) == -yawSign)
            Pose.UP -> abs(pitch) in minA..maxA
            Pose.DOWN -> abs(pitch) in minA..maxA && (pitchSign == 0f || sign(pitch) == -pitchSign)
        }
    }

    private fun rememberSigns(pose: Pose, yaw: Float, pitch: Float) {
        if (pose == Pose.LEFT && yawSign == 0f && abs(yaw) >= RecognitionConfig.POSE_MIN_ANGLE) yawSign = sign(yaw)
        if (pose == Pose.UP && pitchSign == 0f && abs(pitch) >= RecognitionConfig.POSE_MIN_ANGLE) pitchSign = sign(pitch)
    }

    fun save() {
        if (!_state.value.canSave) return
        _state.update { it.copy(saving = true, saveError = false) }
        val toSave = samples.toList()
        val photo = portrait
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    container.people.replaceSamples(personId, toSave)
                    val person = container.people.get(personId)
                    if (person != null && person.photoPath.isBlank() && photo != null) {
                        container.people.setPhotoFromBitmap(personId, photo)
                    }
                }
                ErrorLog.info("Namunalar", "Odam #$personId uchun ${toSave.size} ta namuna saqlandi")
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                ErrorLog.error("Namunalar", "Namunalarni saqlashda xato", e)
                _state.update { it.copy(saving = false, saveError = true) }
            }
        }
    }
}
