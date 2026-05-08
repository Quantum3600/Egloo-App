package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.currentTimeMillis
import io.ktor.client.*
import io.ktor.client.call.*
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

    override suspend fun loadHistory() {
        try {
            val response = client.get("/api/v1/query/history")
            if (response.status.isSuccess()) {
                val historyResponse = response.body<QueryHistoryResponse>()
                val domainMessages = mutableListOf<ChatMessage>()
                
                historyResponse.history.forEach { item ->
                    val userMsg = ChatMessage.User(
                        id = "u_${item.id}",
                        text = item.question,
                        sentAt = 0 
                    )
                    domainMessages.add(userMsg)
                    
                    if (item.answer != null) {
                        val pingoMsg = ChatMessage.Pingo(
                            id = "p_${item.id}",
                            text = item.answer,
                            sentAt = 1,
                            sources = item.sources_used?.map { 
                                ChatSource(it.source_name_fallback(), it.source_type_to_domain())
                            } ?: emptyList(),
                            modelUsed = item.model_used
                        )
                        domainMessages.add(pingoMsg)
                    }
                }
                _messages.value = domainMessages
            }
        } catch (e: Exception) {
            // Log error
        }
    }

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

    override fun getSuggestions(): Flow<List<String>> = flow {
        try {
            val response = client.get("/api/v1/query/suggest")
            if (response.status.isSuccess()) {
                val body = response.body<Map<String, List<String>>>()
                emit(body["suggestions"] ?: emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun saveMessage(id: String): Result<Unit> {
        return try {
            // Mapping chat message id to query history id
            // Assuming the id passed is the one from QueryHistoryItem
            val queryId = id.removePrefix("p_").removePrefix("u_")
            val response = client.post("/api/v1/saved") {
                contentType(ContentType.Application.Json)
                setBody(SaveItemRequest(item_id = queryId, item_type = "query_result"))
            }
            if (response.status.isSuccess()) Result.success(Unit)
            else Result.failure(Exception("Failed to save: ${response.status}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
