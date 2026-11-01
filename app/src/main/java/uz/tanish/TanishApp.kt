package uz.tanish

import android.app.Application
import android.content.Context
import uz.tanish.data.FileStore
import uz.tanish.data.PersonRepository
import uz.tanish.data.SettingsRepository
import uz.tanish.data.db.AppDatabase
import uz.tanish.face.FaceEmbedder
import uz.tanish.log.ErrorLog

class TanishApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        ErrorLog.init(this)
        container = AppContainer(this)
    }
}

/** Ilova bo'ylab yagona obyektlar. */
class AppContainer(private val context: Context) {
    private val database by lazy { AppDatabase.create(context) }
    val files by lazy { FileStore(context) }
    val people by lazy { PersonRepository(database, files) }
    val settings by lazy { SettingsRepository(context) }

    private var embedder: FaceEmbedder? = null
    private var embedderFailed = false

    /** Modelni bir marta yuklaydi. Yuklanmasa null (ilova yopilmaydi). Asosiy oqimda chaqirmang. */
    @Synchronized
    fun embedder(): FaceEmbedder? {
        embedder?.let { return it }
        if (embedderFailed) return null
        return try {
            FaceEmbedder(context).also {
                embedder = it
                ErrorLog.info("Model", "Yuz modeli yuklandi")
            }
        } catch (e: Throwable) {
            embedderFailed = true
            ErrorLog.error("Model", "Yuz modelini yuklab bo'lmadi", e)
            null
        }
    }
}
