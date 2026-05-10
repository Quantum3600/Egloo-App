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
    private val json: Json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
        explicitNulls = false
    }
) : ChatRepository {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override fun getChatHistory(): Flow<List<ChatMessage>> = _messages.asStateFlow()

    override suspend fun loadHistory() {
        try {
            val response = client.get("/api/v1/query/history")
            if (response.status.isSuccess()) {
                // Try to parse as wrapped EglooResponse first, fallback to direct object
                val historyItems = try {
                    val wrapped = response.body<EglooResponse<QueryHistoryResponse>>()
                    wrapped.getOrNull()?.history ?: response.body<QueryHistoryResponse>().history
                } catch (e: Exception) {
                    println("Ktor: Direct parse fallback for history: ${e.message}")
                    response.body<QueryHistoryResponse>().history
                }

                val domainMessages = mutableListOf<ChatMessage>()
                
                historyItems.forEach { item ->
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
                                ChatSource(it.title, it.source_type_to_domain())
                            } ?: emptyList(),
                            metadata = AIMetadata(
                                model = item.model_used,
                                sourcesRetrieved = item.sources_used?.size ?: 0
                            )
                        )
                        domainMessages.add(pingoMsg)
                    }
                }
                _messages.value = domainMessages
            }
        } catch (e: Exception) {
            println("Ktor Error loading history: ${e.message}")
        }
    }

    override suspend fun sendMessage(text: String, model: String?) {
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

        var retryCount = 0
        val maxRetries = 3
        var success = false

        while (retryCount < maxRetries && !success) {
            try {
                client.preparePost("/api/v1/query/ask/stream") {
                    contentType(ContentType.Application.Json)
                    setBody(AskRequest(question = text, model = model))
                }.execute { response ->
                    if (response.status.isSuccess()) {
                        success = true
                        val channel = response.bodyAsChannel()
                        println("Ktor: Stream channel opened for $pingoId")
                        var fullText = ""
                        
                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line() ?: break
                            
                            if (line.isBlank()) continue
                            
                            println("Ktor-SSE-Raw: $line")

                            if (line.startsWith("data:")) {
                                val data = line.removePrefix("data:").trim()
                                
                                if (data == "[DONE]") {
                                    println("Ktor: Stream [DONE] for $pingoId")
                                    updatePingoMessage(pingoId, fullText, isStreaming = false)
                                    break
                                }
                                
                                try {
                                    val event = json.decodeFromString<ChatEventDto>(data)
                                    println("Ktor-SSE-Parsed: type=${event.type} token=${event.token}")
                                    
                                    when (event.type) {
                                        "token" -> {
                                            fullText += event.token ?: ""
                                            updatePingoMessage(pingoId, fullText, isStreaming = true)
                                        }
                                        "sources" -> {
                                            val sources = event.sources?.map { 
                                                ChatSource(it.title, it.source_type_to_domain())
                                            } ?: emptyList()
                                            updatePingoMessage(
                                                pingoId, 
                                                fullText, 
                                                isStreaming = true, 
                                                sources = sources,
                                                metadata = AIMetadata(
                                                    model = event.model
                                                )
                                            )
                                        }
                                        "done" -> {
                                            updatePingoMessage(
                                                pingoId, 
                                                fullText, 
                                                isStreaming = false, 
                                                metadata = AIMetadata(
                                                    model = event.model
                                                )
                                            )
                                        }
                                        "error" -> {
                                            val errorMessage = event.token ?: "Unknown server error"
                                            updatePingoMessage(pingoId, "Error: $errorMessage", isStreaming = false)
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("Ktor SSE Parse Error: ${e.message} on data: $data")
                                }
                            }
                        }
                    } else {
                        throw Exception("Server returned ${response.status}")
                    }
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    updatePingoMessage(pingoId, "Error: ${e.message}. Please try again later.", isStreaming = false)
                } else {
                    // Exponential backoff
                    kotlinx.coroutines.delay(1000L * retryCount)
                }
            }
        }
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
