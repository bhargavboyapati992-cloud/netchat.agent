package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val MODEL_NAME = "gemini-1.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """
You are NetChat, an AI-powered Virtual Teaching Assistant for the Computer Networks course at the university.

Rules:
1. When user says a simple greeting or asks for a welcome message (e.g., 'hi', 'hello', 'welcome', 'hey'), reply concisely: "Hello! I'm NetChat for Computer Networks."
2. Answer ONLY Computer Networks, networking, telecommunications, protocols, network security, and lab/viva questions. If asked about unrelated topics (e.g. cooking, movies), politely redirect to Computer Networks.
3. For technical computer network questions, structure your answer in clear sections:
   - 📌 **Simple Concept**: Explain in plain, intuitive language first.
   - ⚙️ **Technical Details**: Provide exact protocols, layer numbers, packet header fields, packet flow, or RFC specifications.
   - 💡 **Real-World Example / Analogy**: Give a practical real-world scenario.
   - 📝 **Exam & Viva Quick Tips**: Key points for exams, 2-mark definitions, or common oral viva questions.
4. Use bullet points, bold key terms, and code blocks for commands/packets.
5. Keep the tone encouraging, professional, structured, and easy to read.
"""

    suspend fun generateAnswer(userPrompt: String, topicContext: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API key not configured")
        }

        val requestUrl = "$BASE_URL?key=$apiKey"

        val promptWithContext = if (!topicContext.isNullOrBlank()) {
            "Course Topic Context: $topicContext\n\nStudent Question: $userPrompt"
        } else {
            userPrompt
        }

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT)))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", promptWithContext)))
            }))
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("API Error ${response.code}: ${errorBody ?: "Unknown error"}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    var responseText = ""
                    for (i in 0 until parts.length()) {
                        val partObj = parts.getJSONObject(i)
                        val text = partObj.optString("text")
                        if (text.isNotBlank()) {
                            responseText = text
                            break
                        }
                    }
                    if (responseText.isBlank()) {
                        responseText = parts.getJSONObject(parts.length() - 1).optString("text")
                    }
                    if (responseText.isNotBlank()) {
                        return@withContext responseText
                    }
                }
            }
            throw Exception("No text response candidate found")
        }
    }

    suspend fun transcribeAudio(audioBase64: String, mimeType: String = "audio/wav"): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API key not configured")
        }

        val requestUrl = "$BASE_URL?key=$apiKey"
        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", audioBase64)
                        })
                    })
                    put(JSONObject().apply {
                        put("text", "Please transcribe this spoken audio accurately into text. Output only the transcribed text.")
                    })
                })
            }))
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Transcription Error ${response.code}: ${errorBody ?: "Unknown error"}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) return@withContext text
                }
            }
            throw Exception("Failed to transcribe audio")
        }
    }

    suspend fun generateLiveVoiceResponse(userAudioBase64OrText: String, isAudioInput: Boolean = false): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API key not configured")
        }

        val requestUrl = "$BASE_URL?key=$apiKey"

        val partsArray = JSONArray()
        if (isAudioInput) {
            partsArray.put(JSONObject().apply {
                put("inlineData", JSONObject().apply {
                    put("mimeType", "audio/wav")
                    put("data", userAudioBase64OrText)
                })
            })
            partsArray.put(JSONObject().put("text", "Listen to my question about computer networks and answer briefly in a clear, friendly voice tone."))
        } else {
            partsArray.put(JSONObject().put("text", userAudioBase64OrText))
        }

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", "You are NetChat Live Assistant. Answer computer network questions in 2-3 short, clear sentences ideal for live spoken response.")))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", partsArray)
            }))
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext generateAnswer(
                    if (isAudioInput) "Transcribe and answer the voice query" else userAudioBase64OrText
                )
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text")
                    if (text.isNotBlank()) return@withContext text
                }
            }
            throw Exception("No response candidate")
        }
    }
}

