package uz.tanish.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query(
        "SELECT p.*, (SELECT COUNT(*) FROM face_sample s WHERE s.personId = p.id) AS sampleCount " +
            "FROM person p ORDER BY p.name COLLATE NOCASE"
    )
    fun observeAll(): Flow<List<PersonWithCount>>

    @Query("SELECT * FROM person WHERE id = :id")
    suspend fun get(id: Long): Person?

    @Query("SELECT * FROM person")
    suspend fun getAll(): List<Person>

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Query("DELETE FROM person WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface FaceSampleDao {
    @Query("SELECT * FROM face_sample")
    suspend fun getAll(): List<FaceSample>

    @Query("SELECT COUNT(*) FROM face_sample WHERE personId = :personId")
    suspend fun countFor(personId: Long): Int

    @Insert
    suspend fun insertAll(samples: List<FaceSample>)

    @Query("DELETE FROM face_sample WHERE personId = :personId")
    suspend fun deleteFor(personId: Long)
}
