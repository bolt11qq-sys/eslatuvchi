package uz.tanish.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.tanish.AppContainer
import uz.tanish.data.ThresholdLevel
import uz.tanish.face.Consensus
import uz.tanish.face.Decision
import uz.tanish.face.DecisionRule
import uz.tanish.face.FaceEmbedder
import uz.tanish.face.FaceFrame
import uz.tanish.face.FaceIndex
import uz.tanish.face.FaceQuality
import uz.tanish.face.FrameVerdict
import uz.tanish.face.MatchScores
import uz.tanish.log.ErrorLog

enum class CameraHint { NO_FACE, WAIT, CLOSER }

data class RecognitionUiState(
    val ready: Boolean = false,
    val modelError: Boolean = false,
    val cameraError: Boolean = false,
    val embedder: FaceEmbedder? = null,
    val hint: CameraHint = CameraHint.NO_FACE,
    /** Bemor rejimi: yakuniy qaror (bir marta). */
    val decision: Decision? = null,
    // ---- "Tanishni sinash" uchun ----
    val emptyDatabase: Boolean = false,
    val scores: MatchScores? = null,
    val bestName: String? = null,
    val secondName: String? = null,
    val lastVerdict: FrameVerdict? = null,
    val lastDecision: Decision? = null,
    val threshold: ThresholdLevel = ThresholdLevel.MEDIUM,
)

/**
 * Kamera kadrlarini bazadagi yuz izlari bilan solishtiradi (TZ, 3-bo'lim).
 * [testMode] = true bo'lsa ("Tanishni sinash") qaror qabul qilingandan keyin ham davom etadi.
 */
class RecognitionViewModel(private val container: AppContainer, private val testMode: Boolean) : ViewModel() {
    private val _state = MutableStateFlow(RecognitionUiState())
    val state: StateFlow<RecognitionUiState> = _state

    private var index: FaceIndex? = null
    private var names: Map<Long, String> = emptyMap()
    private val consensus = Consensus()
    private var decided = false

    init {
        viewModelScope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    val embedder = container.embedder()
                    val samples = container.people.allEmbeddings()
                    val people = container.people.allPeople().associate { it.id to it.name }
                    val level = container.settings.current().threshold
                    Loaded(embedder, FaceIndex(samples), people, level)
                }
                index = loaded.index
                names = loaded.names
                _state.update {
                    it.copy(
                        ready = loaded.embedder != null,
                        modelError = loaded.embedder == null,
                        embedder = loaded.embedder,
                        emptyDatabase = loaded.index.isEmpty,
                        threshold = loaded.level,
                    )
                }
            } catch (e: Exception) {
                ErrorLog.error("Tanish", "Ma'lumotlarni yuklashda xato", e)
                _state.update { it.copy(modelError = true) }
            }
        }
    }

    fun onCameraError() = _state.update { it.copy(cameraError = true) }

    fun nameOf(personId: Long): String? = names[personId]

    /** Asosiy oqimda chaqiriladi. */
    fun onFrame(frame: FaceFrame) {
        val idx = index ?: return
        if (decided && !testMode) return

        when (frame.quality) {
            FaceQuality.NO_FACE -> {
                consensus.breakSequence()
                _state.update { it.copy(hint = CameraHint.NO_FACE) }
                return
            }
            FaceQuality.TOO_SMALL -> {
                _state.update { it.copy(hint = CameraHint.CLOSER) }
                return
            }
            FaceQuality.OK -> Unit
            else -> {
                _state.update { it.copy(hint = CameraHint.WAIT) }
                return
            }
        }
        val embedding = frame.embedding ?: return
        val scores = idx.score(embedding)
        val level = _state.value.threshold
        val verdict = DecisionRule.classify(scores, level)
        val decision = consensus.add(verdict)

        if (testMode) {
            _state.update {
                it.copy(
                    hint = CameraHint.WAIT,
                    scores = scores,
                    bestName = scores.bestId?.let { id -> names[id] },
                    secondName = scores.secondId?.let { id -> names[id] },
                    lastVerdict = verdict,
                    lastDecision = decision ?: it.lastDecision,
                )
            }
            if (decision != null) {
                ErrorLog.info("Sinash", "Qaror: $decision, s1=${"%.3f".format(scores.best)}, s2=${"%.3f".format(scores.second)}")
                consensus.reset()
            }
            return
        }

        _state.update { it.copy(hint = CameraHint.WAIT) }
        if (decision != null) {
            decided = true
            _state.update { it.copy(decision = decision) }
        }
    }

    private class Loaded(
        val embedder: FaceEmbedder?,
        val index: FaceIndex,
        val names: Map<Long, String>,
        val level: ThresholdLevel,
    )
}
