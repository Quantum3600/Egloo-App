package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class KtorChatRepository(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override fun getChatHistory(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun sendMessage(text: String) {
        val now = kotlin.time.Clock.System.now()
        val userMsg = ChatMessage.User(
            id = "u_${now.toEpochMilliseconds()}",
            text = text,
            sentAt = now
        )
        _messages.update { it + userMsg }

        val pingoId = "p_${now.toEpochMilliseconds() + 1}"
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
                setBody(AskRequest(text))
            }.execute { response ->
                val channel = response.bodyAsChannel()
                var fullText = ""
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        
                        try {
                            val chunk = json.decodeFromString<ChatChunkDto>(data)
                            fullText += chunk.delta
                            
                            _messages.update { msgs ->
                                msgs.map { 
                                    if (it.id == pingoId && it is ChatMessage.Pingo) {
                                        it.copy(
                                            text = fullText,
                                            isStreaming = !chunk.done,
                                            sources = chunk.citations?.map { c -> 
                                                ChatSource(c.source_name, SourceType.MANUAL)
                                            } ?: it.sources
                                        )
                                    } else it
                                }
                            }
                        } catch (e: Exception) {
                            // Skip malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _messages.update { msgs ->
                msgs.map { 
                    if (it.id == pingoId && it is ChatMessage.Pingo) {
                        it.copy(text = "Error: ${e.message}", isStreaming = false)
                    } else it
                }
            }
        }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }
}
