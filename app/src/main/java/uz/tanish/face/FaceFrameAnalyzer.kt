package uz.tanish.face

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import uz.tanish.log.ErrorLog
import java.io.Closeable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class FaceQuality { NO_FACE, TOO_SMALL, TURNED, BLURRY, DARK, OK }

/** Bitta tahlil qilingan kadr. Koordinatalar burilgan (va old kamerada ko'zgulangan) kadrda. */
class FaceFrame(
    val imageWidth: Int,
    val imageHeight: Int,
    val box: RectF?,
    val quality: FaceQuality,
    /** Chap-o'ngga burilish. */
    val yaw: Float,
    /** Yuqori-pastga burilish. */
    val pitch: Float,
    val sharpness: Double,
    /** Faqat sifat OK bo'lganda va embedder berilganda. */
    val embedding: FloatArray?,
    /** Faqat so'ralganda: odamning asosiy rasmi uchun kengroq qirqim. */
    val portrait: Bitmap?,
)

enum class AnalyzerMode { RECOGNITION, ENROLL }

/**
 * CameraX kadrini oladi → ML Kit bilan eng katta yuzni topadi → sifatini tekshiradi →
 * yuzni qirqib embedding oladi (TZ, 3-bo'lim, 1–3-qadamlar).
 */
class FaceFrameAnalyzer(
    private val embedder: FaceEmbedder?,
    private val mode: AnalyzerMode,
    private val mirror: Boolean,
    private val wantPortrait: Boolean,
    private val onFrame: (FaceFrame) -> Unit,
) : ImageAnalysis.Analyzer, Closeable {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build()
    )

    @Volatile
    private var closed = false
    private var lastFrameAt = 0L
    private var errorCount = 0

    override fun analyze(image: ImageProxy) {
        var imageClosed = false
        try {
            val now = SystemClock.elapsedRealtime()
            if (closed || now - lastFrameAt < RecognitionConfig.FRAME_INTERVAL_MS) return
            lastFrameAt = now

            val raw = image.toBitmap()
            val rotation = image.imageInfo.rotationDegrees
            image.close()
            imageClosed = true
            if (closed) return
            onFrame(process(rotate(raw, rotation)))
        } catch (e: Exception) {
            // Har bir kadrda xato bo'lsa jurnal to'lib ketmasin.
            if (errorCount++ % 50 == 0) ErrorLog.error("Kamera", "Kadrni tahlil qilishda xato (#$errorCount)", e)
        } finally {
            if (!imageClosed) image.close()
        }
    }

    private fun process(frame: Bitmap): FaceFrame {
        val w = frame.width
        val h = frame.height
        val faces = Tasks.await(detector.process(InputImage.fromBitmap(frame, 0)))
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return FaceFrame(w, h, null, FaceQuality.NO_FACE, 0f, 0f, 0.0, null, null)

        val box = face.boundingBox
        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ
        val boxF = RectF(box)

        val minPx = if (mode == AnalyzerMode.ENROLL) RecognitionConfig.MIN_FACE_PX_ENROLL else RecognitionConfig.MIN_FACE_PX_RECOGNITION
        if (min(box.width(), box.height()) < minPx) {
            return FaceFrame(w, h, boxF, FaceQuality.TOO_SMALL, yaw, pitch, 0.0, null, null)
        }
        val turned = if (mode == AnalyzerMode.ENROLL) {
            abs(yaw) > RecognitionConfig.POSE_MAX_ANGLE || abs(pitch) > RecognitionConfig.POSE_MAX_ANGLE
        } else {
            abs(yaw) > RecognitionConfig.MAX_YAW_RECOGNITION || abs(pitch) > RecognitionConfig.MAX_PITCH_RECOGNITION
        }
        if (turned || abs(roll) > RecognitionConfig.MAX_ROLL) {
            return FaceFrame(w, h, boxF, FaceQuality.TURNED, yaw, pitch, 0.0, null, null)
        }

        val crop = cropSquare(frame, box, 1.0f) ?: return FaceFrame(w, h, boxF, FaceQuality.TOO_SMALL, yaw, pitch, 0.0, null, null)
        val small = Bitmap.createScaledBitmap(crop, RecognitionConfig.INPUT_SIZE, RecognitionConfig.INPUT_SIZE, true)
        val (brightness, sharpness) = brightnessAndSharpness(small)

        if (brightness < RecognitionConfig.MIN_BRIGHTNESS || brightness > RecognitionConfig.MAX_BRIGHTNESS) {
            return FaceFrame(w, h, boxF, FaceQuality.DARK, yaw, pitch, sharpness, null, null)
        }
        val minSharp = if (mode == AnalyzerMode.ENROLL) RecognitionConfig.MIN_SHARPNESS_ENROLL else RecognitionConfig.MIN_SHARPNESS_RECOGNITION
        if (sharpness < minSharp) {
            return FaceFrame(w, h, boxF, FaceQuality.BLURRY, yaw, pitch, sharpness, null, null)
        }

        val embedding = embedder?.embed(small)
        val portrait = if (wantPortrait) cropSquare(frame, box, 1.8f)?.let { downscale(it, 512) } else null
        return FaceFrame(w, h, boxF, FaceQuality.OK, yaw, pitch, sharpness, embedding, portrait)
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0 && !mirror) return src
        val m = Matrix()
        m.postRotate(degrees.toFloat())
        // Old kamera: oldindan ko'rish ko'zgulangan, tahlil ham shunday bo'lsin — ramka joyiga tushadi.
        if (mirror) m.postScale(-1f, 1f)
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /** Yuz markazida kvadrat qirqim; [factor] — yuz o'lchamiga nisbatan kattalik. */
    private fun cropSquare(src: Bitmap, box: Rect, factor: Float): Bitmap? {
        val side = min((max(box.width(), box.height()) * factor).roundToInt(), min(src.width, src.height))
        if (side < 16) return null
        val cx = box.centerX()
        val cy = box.centerY()
        val left = (cx - side / 2).coerceIn(0, src.width - side)
        val top = (cy - side / 2).coerceIn(0, src.height - side)
        return Bitmap.createBitmap(src, left, top, side, side)
    }

    private fun downscale(src: Bitmap, maxSide: Int): Bitmap {
        if (src.width <= maxSide) return src
        return Bitmap.createScaledBitmap(src, maxSide, maxSide, true)
    }

    /** O'rtacha yorqinlik va Laplasian dispersiyasi (xiralik o'lchovi). */
    private fun brightnessAndSharpness(img: Bitmap): Pair<Double, Double> {
        val w = img.width
        val h = img.height
        val px = IntArray(w * h)
        img.getPixels(px, 0, w, 0, 0, w, h)
        val gray = DoubleArray(w * h)
        var sum = 0.0
        for (i in px.indices) {
            val c = px[i]
            val g = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
            gray[i] = g
            sum += g
        }
        var lapSum = 0.0
        var lapSq = 0.0
        var n = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val l = gray[i - 1] + gray[i + 1] + gray[i - w] + gray[i + w] - 4 * gray[i]
                lapSum += l
                lapSq += l * l
                n++
            }
        }
        val mean = lapSum / n
        return (sum / px.size) to (lapSq / n - mean * mean)
    }

    override fun close() {
        closed = true
        try {
            detector.close()
        } catch (e: Exception) {
            ErrorLog.error("Kamera", "Yuz aniqlagichni yopishda xato", e)
        }
    }
}
