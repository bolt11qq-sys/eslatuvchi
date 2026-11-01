package uz.tanish.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import uz.tanish.data.db.AppDatabase
import uz.tanish.data.db.FaceSample
import uz.tanish.data.db.Person
import uz.tanish.data.db.PersonWithCount
import uz.tanish.face.EmbeddingCodec

class PersonRepository(
    private val db: AppDatabase,
    private val files: FileStore,
) {
    private val personDao = db.personDao()
    private val sampleDao = db.faceSampleDao()

    fun observePeople(): Flow<List<PersonWithCount>> = personDao.observeAll()

    suspend fun get(id: Long): Person? = personDao.get(id)

    suspend fun sampleCount(personId: Long): Int = sampleDao.countFor(personId)

    suspend fun insert(person: Person): Long = personDao.insert(person)

    suspend fun update(person: Person) = personDao.update(person)

    /** Galereyadan rasm: eski asosiy rasm o'chiriladi. */
    suspend fun setPhotoFromUri(personId: Long, uri: Uri): Person? = withContext(Dispatchers.IO) {
        val person = personDao.get(personId) ?: return@withContext null
        val newPath = files.importPhoto(uri, personId)
        files.delete(person.photoPath)
        person.copy(photoPath = newPath).also { personDao.update(it) }
    }

    suspend fun setPhotoFromBitmap(personId: Long, bitmap: Bitmap): Person? = withContext(Dispatchers.IO) {
        val person = personDao.get(personId) ?: return@withContext null
        val newPath = files.savePhoto(bitmap, personId)
        files.delete(person.photoPath)
        person.copy(photoPath = newPath).also { personDao.update(it) }
    }

    /** Odamning barcha yuz namunalarini yangilari bilan almashtiradi. */
    suspend fun replaceSamples(personId: Long, embeddings: List<FloatArray>) {
        db.withTransaction {
            sampleDao.deleteFor(personId)
            sampleDao.insertAll(embeddings.map { FaceSample(personId = personId, embedding = EmbeddingCodec.encode(it)) })
        }
    }

    /** Barcha namunalar: (personId, embedding). */
    suspend fun allEmbeddings(): List<Pair<Long, FloatArray>> =
        sampleDao.getAll().map { it.personId to EmbeddingCodec.decode(it.embedding) }

    suspend fun allPeople(): List<Person> = personDao.getAll()

    fun voiceFileFor(personId: Long) = files.voiceFileFor(personId)

    suspend fun setVoice(personId: Long, path: String?) {
        val person = personDao.get(personId) ?: return
        if (path == null) files.delete(person.voicePath)
        personDao.update(person.copy(voicePath = path))
    }

    /** Odamni va uning barcha rasmlari, ovozi va yuz izlarini to'liq o'chiradi (TZ, 4-bo'lim). */
    suspend fun delete(personId: Long) = withContext(Dispatchers.IO) {
        val person = personDao.get(personId)
        db.withTransaction {
            sampleDao.deleteFor(personId)
            personDao.delete(personId)
        }
        if (person != null) {
            files.delete(person.photoPath)
            files.delete(person.voicePath)
        }
        files.deleteAllFor(personId)
    }
}
