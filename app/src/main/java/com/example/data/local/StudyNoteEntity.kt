package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val unit: String,
    val content: String,
    val noteType: String = "NOTE", // "NOTE", "VIVA", "CUSTOM"
    val timestamp: Long = System.currentTimeMillis()
)
