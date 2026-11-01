package uz.tanish.log

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Xatolar jurnali (TZ, 0-bosqich va 8-bo'lim, 6-qoida).
 *
 * Barcha xatolar va ilova yopilishlari ilovaning ichki xotirasidagi
 * `filesDir/logs/jurnal.txt` fayliga yoziladi. Fayl telefondan tashqariga chiqmaydi.
 * Fayl [MAX_BYTES] dan oshsa, eski yarmi tashlab yuboriladi.
 */
object ErrorLog {

    private const val TAG = "Tanish"
    private const val MAX_BYTES = 256 * 1024
    private const val FILE_NAME = "jurnal.txt"

    private val lock = Any()
    private lateinit var file: File
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** Ilova ishga tushganda bir marta chaqiriladi. */
    fun init(context: Context) {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        file = File(dir, FILE_NAME)
        installCrashHandler()
        info(
            "Ilova",
            "Ishga tushdi. Qurilma: ${Build.MANUFACTURER} ${Build.MODEL}, " +
                "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }

    fun info(where: String, message: String) = write("INFO", where, message, null)

    fun error(where: String, message: String, throwable: Throwable? = null) =
        write("XATO", where, message, throwable)

    /** Jurnalning to'liq matni. */
    fun read(): String = synchronized(lock) {
        if (!::file.isInitialized || !file.exists()) "" else file.readText()
    }

    fun clear() = synchronized(lock) {
        if (::file.isInitialized && file.exists()) file.writeText("")
    }

    private fun write(level: String, where: String, message: String, throwable: Throwable?) {
        if (throwable != null) Log.e(TAG, "[$where] $message", throwable) else Log.i(TAG, "[$where] $message")
        if (!::file.isInitialized) return
        val line = buildString {
            append(synchronized(timeFormat) { timeFormat.format(Date()) }).append(' ')
            append(level).append(" [").append(where).append("] ").append(message).append('\n')
            if (throwable != null) append(stackTraceOf(throwable)).append('\n')
        }
        try {
            synchronized(lock) {
                file.appendText(line)
                trimIfNeeded()
            }
        } catch (e: Exception) {
            // Jurnalning o'zi yozilmasa, faqat logcat'ga chiqaramiz — aylanma xatoga tushmaslik uchun.
            Log.e(TAG, "Jurnalga yozib bo'lmadi", e)
        }
    }

    private fun trimIfNeeded() {
        if (file.length() <= MAX_BYTES) return
        val text = file.readText()
        val keepFrom = text.length / 2
        val cut = text.indexOf('\n', keepFrom).let { if (it < 0) keepFrom else it + 1 }
        file.writeText("… (eski yozuvlar o'chirildi)\n" + text.substring(cut))
    }

    private fun stackTraceOf(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString().trimEnd()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                write("YOPILISH", "Oqim: ${thread.name}", "Ilova kutilmagan xato bilan yopildi", throwable)
            } catch (_: Throwable) {
                // Hech narsa qilmaymiz — asosiysi tizim ishlovchisiga uzatish.
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
