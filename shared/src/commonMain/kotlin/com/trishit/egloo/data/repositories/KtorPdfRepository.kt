package com.trishit.egloo.data.repositories

import com.trishit.egloo.data.api.PdfListResponse
import com.trishit.egloo.data.api.PdfUploadResponse
import com.trishit.egloo.data.api.toDomain
import com.trishit.egloo.domain.models.UploadedPdf
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.http.HttpHeaders.ContentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class KtorPdfRepository(private val client: HttpClient) : PdfRepository {
    
    override fun getUploadedPdfs(): Flow<List<UploadedPdf>> = flow {
        try {
            val response = client.get("/api/v1/ingest/pdfs")
            if (response.status == HttpStatusCode.OK) {
                val listResponse = response.body<PdfListResponse>()
                emit(listResponse.pdfs.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun uploadPdf(filename: String, fileBytes: ByteArray): Result<UploadedPdf> {
        return try {
            val response = client.post("/api/v1/ingest/pdf") {
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", fileBytes, Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            })
                        }
                    )
                )
            }

            if (response.status == HttpStatusCode.OK) {
                val dto = response.body<PdfUploadResponse>()
                Result.success(dto.toDomain())
            } else {
                Result.failure(Exception("Upload failed: ${response.status}"))
            }
        } catch (e: Exception) {
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

