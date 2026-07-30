package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_documents")
data class UploadedDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fileName: String,
    val fileType: String = "PDF",
    val fileSizeFormatted: String = "1.2 MB",
    val extractedText: String = "",
    val category: String = "Syllabus Book",
    val pageCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
