package uz.tanish.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import uz.tanish.log.ErrorLog
import java.io.File

/** Qarindosh ovozini `.m4a` (AAC) formatida yozadi, maksimum 15 soniya (TZ, 5-bo'lim). */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    @Volatile private var maxReached = false

    val isRecording: Boolean get() = recorder != null

    fun start(output: File, onMaxReached: () -> Unit) {
        stopQuietly()
        maxReached = false
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(MAX_DURATION_MS)
            r.setOutputFile(output.absolutePath)
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    maxReached = true
                    onMaxReached()
                }
            }
            r.prepare()
            r.start()
            recorder = r
            file = output
        } catch (e: Exception) {
            r.release()
            throw e
        }
    }

    /** Yozuvni to'xtatadi. Fayl yaroqli bo'lsa true. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        val f = file
        recorder = null
        file = null
        val ok = try {
            r.stop()
            true
        } catch (e: RuntimeException) {
            // Maksimal vaqtga yetganda yozuv o'zi to'xtaydi — bu xato emas.
            if (!maxReached) ErrorLog.error("Ovoz", "Yozuvni to'xtatishda xato (juda qisqa bo'lishi mumkin)", e)
            maxReached
        } finally {
            r.release()
        }
        return ok && f != null && f.exists() && f.length() > 0
    }

    fun stopQuietly() {
        val r = recorder ?: return
        recorder = null
        try { r.stop() } catch (_: RuntimeException) { }
        r.release()
    }

    companion object {
        const val MAX_DURATION_MS = 15_000
        const val MAX_SECONDS = 15
    }
}

/** Ovoz yozuvini ijro etadi. */
class VoicePlayer {
    private var player: MediaPlayer? = null

    val isPlaying: Boolean get() = player != null

    /** Ijro boshlansa true. Tugaganda [onDone] chaqiriladi. */
    fun play(path: String, onDone: () -> Unit): Boolean {
        stop()
        if (!File(path).exists()) {
            ErrorLog.error("Ovoz", "Ovoz fayli topilmadi")
            return false
        }
        return try {
            val p = MediaPlayer()
            p.setDataSource(path)
            p.setOnCompletionListener {
                stop()
                onDone()
            }
            p.setOnErrorListener { _, what, extra ->
                ErrorLog.error("Ovoz", "Ijroda xato: what=$what extra=$extra")
                stop()
                onDone()
                true
            }
            p.prepare()
            p.start()
            player = p
            true
        } catch (e: Exception) {
            ErrorLog.error("Ovoz", "Ovozni ijro etib bo'lmadi", e)
            stop()
            false
        }
    }

    fun stop() {
        val p = player ?: return
        player = null
        try {
            if (p.isPlaying) p.stop()
        } catch (_: IllegalStateException) {
        }
        p.release()
    }
}
