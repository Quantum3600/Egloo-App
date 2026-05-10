package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.PdfUploadResponse
import com.trishit.egloo.data.api.safeParse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.UploadedPdf
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorPdfRepository(private val client: HttpClient) : PdfRepository {
    
    override fun getUploadedPdfs(): Flow<List<UploadedPdf>> = flow {
        // Backend does not have a GET /pdfs endpoint. 
        println("KtorPdfRepository: getUploadedPdfs called (deprecated/no backend endpoint)")
        emit(emptyList())
    }

    override suspend fun uploadPdf(filename: String, fileBytes: ByteArray): Result<Pair<UploadedPdf, String?>> {
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
            
            response.safeParse<PdfUploadResponse>().map { resp ->
                resp.toDomain() to resp.job_id
            }
        } catch (e: Exception) {
            println("KtorPdfRepository: Exception during upload: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deletePdf(pdfId: String): Result<Unit> {
        return try {
            // Updated to use UUID if available, or string if that's what backend expects
            val response = client.delete("/api/v1/ingest/pdf/$pdfId")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorMsg = response.safeParse<Map<String, String>>().fold(
                    onSuccess = { it["detail"] ?: "Unknown error" },
                    onFailure = { it.message ?: "Delete failed: ${response.status}" }
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reindexPdf(pdfId: String): Result<Unit> {
        return try {
            val response = client.post("/api/v1/ingest/pdf/$pdfId/reindex")
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Reindex failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
