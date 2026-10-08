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

class OpenAiClient(
    private var customApiKey: String? = null,
    private var modelName: String = "gpt-4o-mini"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun updateApiKey(newKey: String?) {
        this.customApiKey = newKey
    }

    fun hasApiKey(): Boolean = !customApiKey.isNullOrBlank()

    suspend fun queryAi(userPrompt: String, systemPrompt: String): AiResponse {
        val apiKey = customApiKey?.trim()
        if (apiKey.isNullOrBlank()) {
            return AiResponse.Error("ChatGPT API key configure nahi hai. Kripya Settings se apni OpenAI API key enter karein.")
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openai.com/v1/chat/completions"

                val payload = JSONObject().apply {
                    put("model", modelName)
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        })
                    }
                    put("messages", messages)
                    put("temperature", 0.7)
                    put("max_tokens", 800)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val respStr = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val json = JSONObject(respStr)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val message = choice.getJSONObject("message")
                            val text = message.getString("content").trim()
                            AiResponse.Success(text)
                        } else {
                            AiResponse.Error("ChatGPT se response decode nahi ho saka.")
                        }
                    } else {
                        val errorMsg = try {
                            JSONObject(respStr).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                        } catch (_: Exception) {
                            "HTTP ${response.code}"
                        }
                        AiResponse.Error("ChatGPT error: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                AiResponse.Error("ChatGPT connection unavailable hai: ${e.message ?: "Network error"}")
            }
        }
    }
}

sealed class AiResponse {
    data class Success(val text: String) : AiResponse()
    data class Error(val errorMessage: String) : AiResponse()
}
