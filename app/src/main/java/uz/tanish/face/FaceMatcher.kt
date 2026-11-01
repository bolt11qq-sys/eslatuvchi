package uz.tanish.face

import uz.tanish.data.ThresholdLevel

/** Bitta kadr uchun eng yaqin ikki odam. */
data class MatchScores(
    val bestId: Long?,
    val best: Float,
    val secondId: Long?,
    val second: Float,
)

/** Bazadagi barcha yuz izlari — xotirada. */
class FaceIndex(samples: List<Pair<Long, FloatArray>>) {
    private val byPerson: Map<Long, List<FloatArray>> = samples.groupBy({ it.first }, { it.second })

    val isEmpty: Boolean get() = byPerson.isEmpty()

    /** Har bir odam uchun uning namunalari ichidagi eng yuqori o'xshashlik olinadi (TZ, 3-bo'lim, 4-qadam). */
    fun score(embedding: FloatArray): MatchScores {
        var bestId: Long? = null
        var best = -1f
        var secondId: Long? = null
        var second = -1f
        for ((id, list) in byPerson) {
            var s = -1f
            for (v in list) {
                val c = EmbeddingCodec.cosine(embedding, v)
                if (c > s) s = c
            }
            if (s > best) {
                secondId = bestId; second = best
                bestId = id; best = s
            } else if (s > second) {
                secondId = id; second = s
            }
        }
        return MatchScores(bestId, best.coerceAtLeast(0f), secondId, second.coerceAtLeast(0f))
    }
}

sealed interface FrameVerdict {
    data class Match(val personId: Long) : FrameVerdict
    data object Unknown : FrameVerdict
    data object Unsure : FrameVerdict
}

sealed interface Decision {
    data class Recognized(val personId: Long) : Decision
    data object Unknown : Decision
    data object Unsure : Decision
}

object DecisionRule {
    /** TZ, 3-bo'lim: qaror qoidasi (bitta kadr uchun). */
    fun classify(s: MatchScores, level: ThresholdLevel): FrameVerdict {
        if (s.bestId == null || s.best < RecognitionConfig.LOWER_THRESHOLD) return FrameVerdict.Unknown
        val upper = RecognitionConfig.upperThreshold(level)
        return if (s.best >= upper && s.best - s.second >= RecognitionConfig.MARGIN) {
            FrameVerdict.Match(s.bestId)
        } else {
            FrameVerdict.Unsure
        }
    }
}

/**
 * Bir necha ketma-ket kadr natijasini birlashtiradi — bitta kadr bilan qaror qilinmaydi.
 * - Oxirgi 5 kadrdan kamida 4 tasida bir xil odam → Tanildi.
 * - Oxirgi 5 kadrning hammasi "uzoq" → Notanish.
 * - Yetarlicha kadrdan keyin ham aniq emas → Aniq bilmayman.
 */
class Consensus(
    private val window: Int = RecognitionConfig.CONSENSUS_WINDOW,
    private val required: Int = RecognitionConfig.CONSENSUS_REQUIRED,
    private val maxFrames: Int = RecognitionConfig.MAX_FACE_FRAMES,
) {
    private val recent = ArrayDeque<FrameVerdict>()
    private var total = 0

    @Synchronized
    fun add(v: FrameVerdict): Decision? {
        recent.addLast(v)
        if (recent.size > window) recent.removeFirst()
        total++

        val topMatch = recent.filterIsInstance<FrameVerdict.Match>()
            .groupingBy { it.personId }.eachCount()
            .maxByOrNull { it.value }
        if (topMatch != null && topMatch.value >= required) return Decision.Recognized(topMatch.key)
        if (recent.size == window && recent.all { it == FrameVerdict.Unknown }) return Decision.Unknown
        if (total >= maxFrames) return Decision.Unsure
        return null
    }

    /** Yuz kadrdan yo'qolsa ketma-ketlik uziladi. */
    @Synchronized
    fun breakSequence() = recent.clear()

    @Synchronized
    fun reset() {
        recent.clear()
        total = 0
    }
}
