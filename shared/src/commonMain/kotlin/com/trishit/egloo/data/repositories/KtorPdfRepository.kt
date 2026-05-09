package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.PdfListResponse
import com.trishit.egloo.data.api.PdfUploadResponse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.UploadedPdf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorPdfRepository(private val client: HttpClient) : PdfRepository {
    
    override fun getUploadedPdfs(): Flow<List<UploadedPdf>> = flow {
        try {
            println("KtorPdfRepository: Fetching PDF list...")
            val response = client.get("/api/v1/ingest/pdfs")
            println("KtorPdfRepository: PDF List Status: ${response.status}")
            
            val list = try {
                val listResponse = response.body<PdfListResponse>()
                println("KtorPdfRepository: Parsed PdfListResponse with ${listResponse.pdfs.size} items")
                listResponse.pdfs
            } catch (e: Exception) {
                // Backend might return the list directly without the wrapper
                println("KtorPdfRepository: Wrapper parse failed, trying direct list...")
                response.body<List<PdfUploadResponse>>()
            }
            
            println("KtorPdfRepository: Final count: ${list.size}")
            emit(list.map { it.toDomain() })
        } catch (e: Exception) {
            println("KtorPdfRepository: Error fetching PDFs: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun uploadPdf(filename: String, fileBytes: ByteArray): Result<UploadedPdf> {
        println("KtorPdfRepository: uploadPdf started for $filename")
        return try {
            val response = client.post("/api/v1/ingest/pdf") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                                append(HttpHeaders.ContentType, "application/pdf")
                            })
                        }
                    )
                )
            }
            
            println("KtorPdfRepository: Server response: ${response.status}")

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created || response.status == HttpStatusCode.Accepted) {
                val dto = response.body<PdfUploadResponse>()
                println("KtorPdfRepository: Parsed DTO: $dto")
                Result.success(dto.toDomain())
            } else {
                val errorBody = try { response.bodyAsText() } catch (_: Exception) { "" }
                println("KtorPdfRepository: Upload failed: $errorBody")
                Result.failure(Exception("Upload failed (${response.status}): $errorBody"))
            }
        } catch (e: Exception) {
            println("KtorPdfRepository: Exception during upload: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deletePdf(pdfId: String): Result<Unit> {
        return try {
            val response = client.delete("/api/v1/ingest/pdf/$pdfId")
            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.NoContent) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reindexPdf(pdfId: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/ingest/pdf/$pdfId/reindex")
            if (response.status == HttpStatusCode.OK) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reindex failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

