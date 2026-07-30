package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.StudyNoteEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        try {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) {
                FirebaseApp.initializeApp(context.applicationContext)
            }
        } catch (e: Throwable) {
            Log.d(TAG, "Firebase initialization notice: ${e.message}")
        }
    }

    private val isFirebaseAppAvailable: Boolean
        get() = try {
            val ctx = appContext
            ctx != null && FirebaseApp.getApps(ctx).isNotEmpty()
        } catch (_: Throwable) {
            false
        }

    private val auth: FirebaseAuth?
        get() = try {
            if (isFirebaseAppAvailable) FirebaseAuth.getInstance() else null
        } catch (e: Throwable) {
            null
        }

    private val firestore: FirebaseFirestore?
        get() = try {
            if (isFirebaseAppAvailable) FirebaseFirestore.getInstance() else null
        } catch (e: Throwable) {
            null
        }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Throwable) {
            null
        }

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val safeAuth = try { auth } catch (e: Throwable) { null }
        if (safeAuth == null) {
            trySend(null)
            awaitClose { }
        } else {
            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                try {
                    trySend(firebaseAuth.currentUser)
                } catch (_: Throwable) {
                    trySend(null)
                }
            }
            try {
                safeAuth.addAuthStateListener(listener)
            } catch (e: Throwable) {
                trySend(null)
            }
            awaitClose {
                try { safeAuth.removeAuthStateListener(listener) } catch (_: Throwable) {}
            }
        }
    }

    suspend fun signInAnonymously(): FirebaseUser? {
        val safeAuth = auth ?: return null
        return try {
            val result = safeAuth.signInAnonymously().await()
            result.user
        } catch (e: Exception) {
            Log.d(TAG, "Anonymous sign in info: ${e.message}")
            null
        }
    }

    suspend fun syncChatMessageToFirestore(message: ChatMessageEntity) {
        val safeFirestore = firestore ?: return
        val user = currentUser ?: signInAnonymously() ?: return
        try {
            val data = mapOf(
                "id" to message.id,
                "sender" to message.sender,
                "text" to message.text,
                "topicTag" to message.topicTag,
                "timestamp" to message.timestamp,
                "isBookmarked" to message.isBookmarked
            )
            safeFirestore.collection("users")
                .document(user.uid)
                .collection("chats")
                .document(message.id.toString())
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.d(TAG, "Sync chat info: ${e.message}")
        }
    }

    suspend fun syncStudyNoteToFirestore(note: StudyNoteEntity) {
        val safeFirestore = firestore ?: return
        val user = currentUser ?: signInAnonymously() ?: return
        try {
            val data = mapOf(
                "id" to note.id,
                "title" to note.title,
                "unit" to note.unit,
                "content" to note.content,
                "noteType" to note.noteType,
                "timestamp" to note.timestamp
            )
            safeFirestore.collection("users")
                .document(user.uid)
                .collection("notes")
                .document(note.id.toString())
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.d(TAG, "Sync note info: ${e.message}")
        }
    }

    suspend fun fetchFirestoreChats(): List<Map<String, Any>> {
        val safeFirestore = firestore ?: return emptyList()
        val user = currentUser ?: return emptyList()
        return try {
            val snapshot = safeFirestore.collection("users")
                .document(user.uid)
                .collection("chats")
                .get()
                .await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.d(TAG, "Fetch chats info: ${e.message}")
            emptyList()
        }
    }

    fun signOut() {
        try { auth?.signOut() } catch (e: Exception) { Log.d(TAG, "Sign out info: ${e.message}") }
    }
}
