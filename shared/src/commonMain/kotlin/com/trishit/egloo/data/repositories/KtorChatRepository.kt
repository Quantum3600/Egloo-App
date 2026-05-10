package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.*
import com.trishit.egloo.domain.models.*
import com.trishit.egloo.platform.currentTimeMillis
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class KtorChatRepository(
    private val client: HttpClient
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override fun getChatHistory(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun loadHistory() {
        withContext(Dispatchers.Default) {
            try {
                val response = client.get("/api/v1/query/history")
                response.safeParse<QueryHistoryResponse>().onSuccess { historyItems ->
                    val domainMessages = mutableListOf<ChatMessage>()
                    historyItems.history.forEach { item ->
                        domainMessages.add(ChatMessage.User(
                            id = "u_${item.id}",
                            text = item.question,
                            sentAt = 0
                        ))
                        if (item.answer != null) {
                            domainMessages.add(ChatMessage.Pingo(
                                id = "p_${item.id}",
                                text = item.answer,
                                sentAt = 1,
                                sources = item.sources_used?.map {
                                    it.toDomain()
                                } ?: emptyList(),
                                metadata = AIMetadata(
                                    model = item.model_used,
                                    sourcesRetrieved = item.sources_used?.size ?: 0
                                )
                            ))
                        }
                    }
                    _messages.value = domainMessages
                }
            } catch (e: Exception) {
                println("Ktor Error loading history: ${e.message}")
            }
        }
    }

    override suspend fun sendMessage(text: String, model: String?) {
        withContext(Dispatchers.Default) {
            val now = currentTimeMillis()
            val userMsg = ChatMessage.User(id = "u_$now", text = text, sentAt = now)
            _messages.update { it + userMsg }

            val pingoId = "p_${now + 1}"
            val pingoMsg = ChatMessage.Pingo(id = pingoId, text = "", sentAt = now, isStreaming = true)
            _messages.update { it + pingoMsg }

            try {
                val response = client.post("/api/v1/query/ask") {
                    contentType(ContentType.Application.Json)
                    setBody(AskRequest(question = text))
                }

                response.safeParse<AskResponse>().onSuccess { askResult ->
                    val fullAnswer = askResult.answer
                    val sources = askResult.sources.map { it.toDomain() }
                    val metadata = AIMetadata(
                        model = askResult.model_used,
                        cached = askResult.cached,
                        sourcesRetrieved = askResult.chunks_retrieved
                    )

                    // Client-side typing animation
                    animateTyping(pingoId, fullAnswer, sources, metadata)
                    
                }.onFailure { e ->
                    updatePingoMessage(pingoId, "Error: ${e.message}", false)
                }
            } catch (e: Exception) {
                updatePingoMessage(pingoId, "Connection failed. Please check your internet.", false)
            }
        }
    }

    private suspend fun animateTyping(
        id: String,
        fullText: String,
        sources: List<ChatSource>,
        metadata: AIMetadata
    ) {
        val words = fullText.split(" ")
        var currentText = ""
        
        for (i in words.indices) {
            currentText += if (i == 0) words[i] else " ${words[i]}"
            updatePingoMessage(id, currentText, true, sources, metadata)
            
            // Adjust delay for typing feel
            val delayMillis = if (words[i].length > 8) 100L else 50L
            delay(delayMillis)
        }
        
        // Final update to set isStreaming = false
        updatePingoMessage(id, fullText, false, sources, metadata)
    }

    private fun updatePingoMessage(
        id: String,
        text: String,
        isStreaming: Boolean,
        sources: List<ChatSource>? = null,
        metadata: AIMetadata? = null
    ) {
        _messages.update { msgs ->
            msgs.map { msg ->
                if (msg.id == id && msg is ChatMessage.Pingo) {
                    msg.copy(
                        text = text,
                        isStreaming = isStreaming,
                        sources = sources ?: msg.sources,
                        metadata = msg.metadata?.merge(metadata) ?: metadata
                    )
                } else msg
            }
        }
    }

    override suspend fun clearHistory() {
        _messages.value = emptyList()
    }

    override fun getSuggestions(): Flow<List<String>> = flow {
        try {
            val response = client.get("/api/v1/query/suggest")
            response.safeParse<Map<String, List<String>>>().onSuccess { body ->
                emit(body["suggestions"] ?: emptyList())
            }
        } catch (_: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun saveMessage(id: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val queryId = id.removePrefix("p_").removePrefix("u_")
                val response = client.post("/api/v1/saved") {
                    contentType(ContentType.Application.Json)
                    setBody(SaveItemRequest(item_id = queryId, item_type = "query_result"))
                }
                response.safeParse<Unit>()
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
