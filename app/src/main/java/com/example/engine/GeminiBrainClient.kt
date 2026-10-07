package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiBrainClient(
    private var customApiKey: String? = null,
    private var modelName: String = "gemini-1.5-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    fun updateApiKey(newKey: String?) {
        this.customApiKey = newKey
    }

    suspend fun queryAi(userPrompt: String, systemContext: String): String? {
        val apiKey = customApiKey?.trim()
        if (apiKey.isNullOrBlank()) {
            return null // Fallback to local NLP rule brain
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

                val payload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", "$systemContext\n\nUser: $userPrompt"))
                            })
                        })
                    }
                    put("contents", contents)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(url).post(body).build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val respStr = response.body?.string() ?: return@withContext null
                        val json = JSONObject(respStr)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).getString("text").trim()
                            }
                        }
                    }
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
