package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NetChatDao {
    // Chat Messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, isBookmarked: Boolean)

    @Query("SELECT * FROM chat_messages WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedMessages(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // Study Notes
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllStudyNotes(): Flow<List<StudyNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNoteEntity): Long

    @Delete
    suspend fun deleteStudyNote(note: StudyNoteEntity)

    // Quiz Results
    @Query("SELECT * FROM quiz_results ORDER BY timestamp DESC")
    fun getAllQuizResults(): Flow<List<QuizResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizResult(result: QuizResultEntity): Long

    // Uploaded Documents & Syllabus Books
    @Query("SELECT * FROM uploaded_documents ORDER BY timestamp DESC")
    fun getAllUploadedDocuments(): Flow<List<UploadedDocumentEntity>>

    @Query("SELECT * FROM uploaded_documents ORDER BY timestamp DESC")
    suspend fun getUploadedDocumentsList(): List<UploadedDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedDocument(doc: UploadedDocumentEntity): Long

    @Delete
    suspend fun deleteUploadedDocument(doc: UploadedDocumentEntity)
}

