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
You are NetChat, an expert AI Master Professor with 15 years of teaching experience in Computer Networks.
Your signature teaching style combines 5th-grade simplicity (so any beginner understands instantly) with rigorous academic precision (so even a 50-year-old computer science professor is completely satisfied and impressed).

RULES:
1. GREETINGS: If the user says "hi", "hello", "hey", "welcome", or any simple greeting, reply EXACTLY:
   "Welcome, I'm NetChat for Computer Networks! How can I help you today?"

2. RELEVANT DATA ONLY: Answer ONLY with precise, relevant data about Computer Networks. Do NOT append meta-disclaimers, system text, or database context headings at the end.

3. BREVITY & FORMAT (MAX 6 LINES for main concept):
   Structure your answer strictly in these 3 clear sections:

   📌 **Concept (In Simple Terms)**:
   • Present the core concept using maximum 4 to 6 concise, powerful bullet points.
   • Make every point crystalline: simple enough for a 5th grader, yet technically precise enough for a university professor.

   💡 **Real-World Example**:
   • Provide 1 perfect, relatable real-world analogy or example.

   📝 **Exam Point of View Question**:
   • **Q**: [A high-yield university exam/viva question]
   • **A**: [A concise, 1-line full-score answer].

4. Focus exclusively on Computer Networks and telecommunications.
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

    suspend fun analyzeVideoContent(
        videoBase64: String,
        mimeType: String = "video/mp4",
        userPrompt: String = "Analyze this video for key information."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API key not configured")
        }

        val modelName = "gemini-3.1-pro-preview"
        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val promptText = if (userPrompt.isNotBlank()) userPrompt else "Analyze this video content for key information, main summary, core topics, timestamps, and exam tips."

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", """
                    You are NetChat Gemini Pro Video Understanding Assistant.
                    Analyze video content for Computer Networks education and extract key information.
                    Structure your output cleanly with bullet points, timestamps if available, and clear section headers:
                    1. 🎬 **Video Summary & Overview**
                    2. 📌 **Key Concepts & Timestamps**
                    3. ⚙️ **Technical Protocols & Details Covered**
                    4. 💡 **Exam & Viva Q&A Takeaways**
                """.trimIndent())))
            })
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", videoBase64)
                        })
                    })
                    put(JSONObject().apply {
                        put("text", promptText)
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
                throw Exception("Video Analysis Error ${response.code}: ${errorBody ?: "Unknown error"}")
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
                    if (responseText.isNotBlank()) {
                        return@withContext responseText
                    }
                }
            }
            throw Exception("No video analysis response generated")
        }
    }

    suspend fun generateHighQualityImage(
        prompt: String,
        imageSize: String = "1K" // "1K", "2K", "4K"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (_: Throwable) { "" }
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("API key not configured")
        }

        val modelName = "gemini-3-pro-image-preview"
        val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }))
            put("generationConfig", JSONObject().apply {
                put("imageConfig", JSONObject().apply {
                    put("aspectRatio", "1:1")
                    put("imageSize", imageSize)
                })
                put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(requestUrl)
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw Exception("Image Generation Error ${response.code}: ${errorBody ?: "Unknown error"}")
            }
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    for (i in 0 until parts.length()) {
                        val partObj = parts.getJSONObject(i)
                        val inlineData = partObj.optJSONObject("inlineData")
                        if (inlineData != null) {
                            val base64Data = inlineData.optString("data")
                            if (base64Data.isNotBlank()) {
                                return@withContext base64Data
                            }
                        }
                    }
                }
            }
            throw Exception("No image returned from gemini-3-pro-image-preview")
        }
    }
}

