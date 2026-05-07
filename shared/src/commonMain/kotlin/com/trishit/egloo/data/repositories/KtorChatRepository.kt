package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.currentTimeMillis
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json

class KtorChatRepository(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override fun getChatHistory(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(text: String) {
        val now = currentTimeMillis()
        val userMsg = ChatMessage.User(
            id = "u_$now",
            text = text,
            sentAt = now
        )
        _messages.update { it + userMsg }

        val pingoId = "p_${now + 1}"
        val pingoMsg = ChatMessage.Pingo(
            id = pingoId,
            text = "",
            sentAt = now,
            isStreaming = true
        )
        _messages.update { it + pingoMsg }

        try {
            client.preparePost("/api/v1/query/ask/stream") {
                contentType(ContentType.Application.Json)
                setBody(AskRequest(question = text))
            }.execute { response ->
                val channel = response.bodyAsChannel()
                var fullText = ""
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        
                        try {
                            val event = json.decodeFromString<ChatEventDto>(data)
                            when (event.type) {
                                "token" -> {
                                    fullText += event.token ?: ""
                                    updatePingoMessage(pingoId, fullText, isStreaming = true)
                                }
                                "sources" -> {
                                    val sources = event.sources?.map { 
                                        ChatSource(it.source_name_fallback(), it.source_type_to_domain())
                                    } ?: emptyList()
                                    updatePingoMessage(
                                        pingoId, 
                                        fullText, 
                                        isStreaming = true, 
                                        sources = sources,
                                        modelUsed = event.model
                                    )
                                }
                                "done" -> {
                                    updatePingoMessage(pingoId, fullText, isStreaming = false, modelUsed = event.model)
                                }
                            }
                        } catch (e: Exception) {
                            // Skip malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            updatePingoMessage(pingoId, "Error: ${e.message}", isStreaming = false)
        }
    }

    private fun updatePingoMessage(
        id: String, 
        text: String, 
        isStreaming: Boolean, 
        sources: List<ChatSource>? = null,
        modelUsed: String? = null
    ) {
        _messages.update { msgs ->
            msgs.map { 
                if (it.id == id && it is ChatMessage.Pingo) {
                    it.copy(
                        text = text,
                        isStreaming = isStreaming,
                        sources = sources ?: it.sources,
                        modelUsed = modelUsed ?: it.modelUsed
                    )
                } else it
            }
        }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
