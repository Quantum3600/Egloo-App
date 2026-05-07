package com.trishit.egloo.domain.viewmodels

import com.trishit.egloo.data.repositories.PdfRepository
import com.trishit.egloo.domain.models.UploadedPdf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── PDF Upload ────────────────────────────────────────────────────────────────

enum class PdfProgressStatus {
    IDLE, UPLOADING, PROCESSING, INDEXED, FAILED
}

data class PdfUiState(
    val uploadedPdfs: List<UploadedPdf> = emptyList(),
    val uploadProgress: Int = 0,
    val isUploading: Boolean = false,
    val uploadStatus: PdfProgressStatus = PdfProgressStatus.IDLE,
    val statusMessage: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

class PdfViewModel(private val pdfRepo: PdfRepository) : BaseViewModel() {

    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    init {
        loadUploadedPdfs()
    }

    private fun loadUploadedPdfs() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            pdfRepo.getUploadedPdfs().collect { pdfs ->
                _uiState.update { it.copy(uploadedPdfs = pdfs, isLoading = false) }
            }
        }
    }

    fun uploadPdf(filename: String, fileBytes: ByteArray) {
        // Validate file
        when {
            fileBytes.isEmpty() -> {
                _uiState.update { it.copy(errorMessage = "File is empty") }
                return
            }
            fileBytes.size > 50 * 1024 * 1024 -> { // 50MB limit
                _uiState.update { it.copy(errorMessage = "File is too large (max 50MB)") }
                return
            }
            !filename.endsWith(".pdf", ignoreCase = true) -> {
                _uiState.update { it.copy(errorMessage = "Only PDF files are allowed") }
                return
            }
        }

        _uiState.update {
            it.copy(
                isUploading = true,
                uploadStatus = PdfProgressStatus.UPLOADING,
                statusMessage = "Uploading...",
                uploadProgress = 0,
                errorMessage = null
            )
        }

        scope.launch {
            val result = pdfRepo.uploadPdf(filename, fileBytes)

            result.onSuccess { pdf ->
                // Simulate progress
                _uiState.update {
                    it.copy(
                        uploadProgress = 50,
                        uploadStatus = PdfProgressStatus.PROCESSING,
                        statusMessage = "Processing PDF..."
                    )
                }

                delay(1500)

                // Check final status from backend
                val finalStatus = when (pdf.status) {
                    "processing" -> {
                        _uiState.update { it.copy(uploadProgress = 75) }
                        delay(1000)
                        PdfProgressStatus.PROCESSING
                    }
                    "indexed" -> {
                        _uiState.update { it.copy(uploadProgress = 100) }
                        PdfProgressStatus.INDEXED
                    }
                    "failed" -> PdfProgressStatus.FAILED
                    else -> PdfProgressStatus.IDLE
                }

                _uiState.update {
                    it.copy(
                        uploadStatus = finalStatus,
                        statusMessage = when (finalStatus) {
                            PdfProgressStatus.INDEXED -> "Ready to search!"
                            PdfProgressStatus.FAILED -> "Processing failed"
                            else -> pdf.status
                        }
                    )
                }

                // Refresh list
                refreshUploadedPdfs()

                // Auto-dismiss after 2 seconds if success
                if (finalStatus == PdfProgressStatus.INDEXED) {
                    delay(2000)
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            errorMessage = null,
                            uploadProgress = 0,
                            uploadStatus = PdfProgressStatus.IDLE,
                            statusMessage = ""
                        )
                    }
                }
            }

            result.onFailure { error ->
                val errorMsg = when {
                    error.message?.contains("size") == true -> "File is too large"
                    error.message?.contains("invalid") == true -> "Invalid PDF format"
                    error.message?.contains("empty") == true -> "PDF is empty"
                    error.message?.contains("parse") == true -> "Failed to parse PDF"
                    error.message?.contains("network") == true -> "Network error. Check connection."
                    else -> error.message ?: "Upload failed"
                }

                _uiState.update {
                    it.copy(
                        uploadStatus = PdfProgressStatus.FAILED,
                        statusMessage = "Failed",
                        errorMessage = errorMsg,
                        isUploading = false,
                        uploadProgress = 0
                    )
                }
            }
        }
    }

    fun deletePdf(pdfId: String) {
        scope.launch {
            val result = pdfRepo.deletePdf(pdfId)
            result.onSuccess {
                refreshUploadedPdfs()
            }
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Delete failed") }
            }
        }
    }

    fun reindexPdf(pdfId: String) {
        scope.launch {
            _uiState.update {
                it.copy(
                    statusMessage = "Re-indexing...",
                    uploadStatus = PdfProgressStatus.PROCESSING
                )
            }

            val result = pdfRepo.reindexPdf(pdfId)
            result.onSuccess {
                delay(2000)
                refreshUploadedPdfs()
            }
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Reindex failed") }
            }
        }
    }

    private fun refreshUploadedPdfs() {
        scope.launch {
            pdfRepo.getUploadedPdfs().collect { pdfs ->
                _uiState.update { it.copy(uploadedPdfs = pdfs) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun resetUploadState() {
        _uiState.update {
            it.copy(
                isUploading = false,
                uploadProgress = 0,
                uploadStatus = PdfProgressStatus.IDLE,
                statusMessage = "",
                errorMessage = null
            )
        }
    }
}

