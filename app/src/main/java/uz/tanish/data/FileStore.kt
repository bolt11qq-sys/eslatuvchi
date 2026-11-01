package uz.tanish.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import uz.tanish.log.ErrorLog
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Rasm va ovoz fayllari — faqat ilovaning ichki xotirasida (`filesDir`), galereyada ko'rinmaydi.
 */
class FileStore(private val context: Context) {

    private val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
    private val voicesDir = File(context.filesDir, "voices").apply { mkdirs() }

    fun voiceFileFor(personId: Long): File = File(voicesDir, "person_$personId.m4a")

    /** Galereyadan tanlangan rasmni kichraytirib ichki xotiraga saqlaydi. */
    fun importPhoto(uri: Uri, personId: Long): String {
        val bitmap = decodeScaled(uri, MAX_PHOTO_SIDE)
        return savePhoto(bitmap, personId)
    }

    fun savePhoto(bitmap: Bitmap, personId: Long): String {
        val file = File(photosDir, "person_${personId}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return file.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val f = File(path)
            if (f.exists() && !f.delete()) ErrorLog.error("Fayllar", "Fayl o'chmadi: ${f.name}")
        } catch (e: Exception) {
            ErrorLog.error("Fayllar", "Faylni o'chirishda xato", e)
        }
    }

    /** Odamga tegishli, bazada qayd etilmagan eski rasmlarni ham tozalaydi. */
    fun deleteAllFor(personId: Long) {
        photosDir.listFiles()?.filter { it.name.startsWith("person_${personId}_") }?.forEach { delete(it.absolutePath) }
        delete(voiceFileFor(personId).absolutePath)
    }

    private fun decodeScaled(uri: Uri, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            // ImageDecoder EXIF bo'yicha burishni o'zi hisobga oladi.
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val s = info.size
                val longest = max(s.width, s.height)
                if (longest > maxSide) {
                    val scale = maxSide.toFloat() / longest
                    decoder.setTargetSize((s.width * scale).toInt(), (s.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("Rasmni o'qib bo'lmadi")
    }

    companion object {
        const val MAX_PHOTO_SIDE = 1024

        /** Ekranda ko'rsatish uchun rasmni kichraytirib o'qiydi. */
        fun loadBitmap(path: String, maxSide: Int): Bitmap? {
            if (path.isBlank() || !File(path).exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
}
