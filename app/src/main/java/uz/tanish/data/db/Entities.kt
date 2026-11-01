package uz.tanish.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Bitta tanish odam (TZ, 4-bo'lim). */
@Entity(tableName = "person")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relation: String,
    val displayText: String,
    /** Ekranda ko'rsatiladigan asosiy rasm. Rasm bo'lmasa — bo'sh qator. */
    val photoPath: String,
    val voicePath: String?,
    val createdAt: Long,
)

/** Bitta odamning bitta yuz izi. Odam o'chirilsa birga o'chadi. */
@Entity(
    tableName = "face_sample",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("personId")],
)
class FaceSample(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    /** L2 bo'yicha normallashtirilgan float massivi (little-endian). */
    val embedding: ByteArray,
)

/** Ro'yxat uchun: odam va uning namunalari soni. */
data class PersonWithCount(
    @Embedded val person: Person,
    val sampleCount: Int,
)
