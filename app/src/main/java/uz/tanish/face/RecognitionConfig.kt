package uz.tanish.face

import uz.tanish.data.ThresholdLevel

/**
 * Tanish uchun barcha chegaralar bitta joyda (TZ, 3-bo'lim).
 *
 * Model: MobileFaceNet (sirius-ai/MobileFaceNet_TF, 192 o'lchamli embedding).
 * Muallif kodida bir xil odam uchun chegara L2² < 0.8 deb olingan — L2 bo'yicha normallashtirilgan
 * vektorlarda bu kosinus > 0.6 ga teng. "Noto'g'ri ism aytish ism aytmaslikdan yomonroq" qoidasiga ko'ra
 * biz undan qattiqroq boshlang'ich qiymatlarni olamiz. Yakuniy qiymatlar "Tanishni sinash" ekranida
 * real odamlarda aniqlanadi.
 */
object RecognitionConfig {
    // ---- Model ----
    const val MODEL_FILE = "MobileFaceNet.tflite"
    const val INPUT_SIZE = 112
    const val IMAGE_MEAN = 127.5f
    const val IMAGE_STD = 128f

    // ---- Qaror qoidasi (kosinus o'xshashlik) ----
    /** s1 shundan kichik bo'lsa — "Notanish". */
    const val LOWER_THRESHOLD = 0.45f

    /** s1 shundan katta bo'lishi kerak — "Tanildi" (sozlamaga qarab). */
    fun upperThreshold(level: ThresholdLevel): Float = when (level) {
        ThresholdLevel.STRICT -> 0.75f
        ThresholdLevel.MEDIUM -> 0.70f
        ThresholdLevel.SOFT -> 0.65f
    }

    /** s1 − s2 kamida shuncha bo'lishi kerak. */
    const val MARGIN = 0.08f

    /** Oxirgi N kadrdan kamida M tasida bir xil odam bo'lishi kerak. */
    const val CONSENSUS_WINDOW = 5
    const val CONSENSUS_REQUIRED = 4

    /** Shuncha sifatli yuzli kadrdan keyin ham qaror bo'lmasa — "Aniq bilmayman". */
    const val MAX_FACE_FRAMES = 15

    // ---- Kamera ----
    /** Sekundiga ~5 kadr. */
    const val FRAME_INTERVAL_MS = 200L
    const val ANALYSIS_WIDTH = 640
    const val ANALYSIS_HEIGHT = 480

    // ---- Yuz sifati ----
    /** Tanishda: yuzning eng kichik o'lchami (piksel, kadr ~640x480). */
    const val MIN_FACE_PX_RECOGNITION = 80
    const val MIN_FACE_PX_ENROLL = 100
    const val MAX_YAW_RECOGNITION = 25f
    const val MAX_PITCH_RECOGNITION = 25f
    const val MAX_ROLL = 20f
    /** Laplasian dispersiyasi — kichik bo'lsa yuz xira. */
    const val MIN_SHARPNESS_RECOGNITION = 25.0
    const val MIN_SHARPNESS_ENROLL = 40.0
    const val MIN_BRIGHTNESS = 45.0
    const val MAX_BRIGHTNESS = 225.0

    // ---- Ro'yxatga olish ----
    const val ENROLL_TARGET = 12
    const val ENROLL_MIN = 10
    const val ENROLL_INTERVAL_MS = 400L
    /** Ko'rsatilgan holatga shuncha vaqtda erishilmasa, istalgan sifatli yuz qabul qilinadi. */
    const val ENROLL_STEP_RELAX_MS = 6_000L
    const val POSE_MIN_ANGLE = 8f
    const val POSE_MAX_ANGLE = 35f
    const val FRONT_MAX_ANGLE = 10f
}
