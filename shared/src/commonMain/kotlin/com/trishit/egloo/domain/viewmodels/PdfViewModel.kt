package com.trishit.egloo.domain.viewmodels

import com.russhwolf.settings.Settings
import com.trishit.egloo.data.repositories.IngestRepository
import com.trishit.egloo.data.repositories.PdfRepository
import com.trishit.egloo.domain.models.UploadedPdf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

class PdfViewModel(
    private val pdfRepo: PdfRepository,
    private val ingestRepo: IngestRepository,
    private val settings: Settings
) : BaseViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private val STORAGE_KEY = "persisted_pdfs"

    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    init {
        loadPersistedPdfs()
    }

    private fun loadPersistedPdfs() {
        val savedJson = settings.getStringOrNull(STORAGE_KEY)
        if (savedJson != null) {
            try {
                val pdfs = json.decodeFromString<List<UploadedPdf>>(savedJson)
                _uiState.update { it.copy(uploadedPdfs = pdfs) }
            } catch (e: Exception) {
                println("PdfViewModel: Error loading persisted PDFs: ${e.message}")
            }
        }
        // Also try fetching from backend once, but don't rely on it
        refreshUploadedPdfs()
    }

    private fun persistPdfs(pdfs: List<UploadedPdf>) {
        try {
            val savedJson = json.encodeToString(pdfs)
            settings.putString(STORAGE_KEY, savedJson)
        } catch (e: Exception) {
            println("PdfViewModel: Error persisting PDFs: ${e.message}")
        }
    }

    fun uploadPdf(filename: String, fileBytes: ByteArray) {
        println("PdfViewModel: uploadPdf called for $filename (${fileBytes.size} bytes)")
        // Validate file
        when {
            fileBytes.isEmpty() -> {
                println("PdfViewModel: Validation failed - File is empty")
                _uiState.update { it.copy(errorMessage = "File is empty") }
                return
            }
            fileBytes.size > 50 * 1024 * 1024 -> { // 50MB limit
                println("PdfViewModel: Validation failed - File too large")
                _uiState.update { it.copy(errorMessage = "File is too large (max 50MB)") }
                return
            }
            !filename.endsWith(".pdf", ignoreCase = true) -> {
                println("PdfViewModel: Validation failed - Not a PDF: $filename")
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
            println("PdfViewModel: Launching repository upload request...")
            val result = pdfRepo.uploadPdf(filename, fileBytes)

            result.onSuccess { (pdf, jobId) ->
                println("PdfViewModel: Upload success! ID: ${pdf.id}, JobID: $jobId, Status: ${pdf.status}")
                
                // Add to local list and persist IMMEDIATELY
                val updatedPdf = pdf.copy(
                    filename = filename, // Ensure filename is correct from local
                    fileSize = fileBytes.size.toLong()
                )
                
                _uiState.update { state ->
                    val newList = (state.uploadedPdfs + updatedPdf).distinctBy { it.id }
                    persistPdfs(newList)
                    state.copy(
                        uploadedPdfs = newList,
                        uploadProgress = 50,
                        uploadStatus = PdfProgressStatus.PROCESSING,
                        statusMessage = "Processing PDF..."
                    )
                }

                if (jobId != null) {
                    pollPdfStatus(jobId, pdf.id)
                } else {
                    // Fallback to old behavior if no jobId returned
                    delay(1500)
                    val finalStatus = when (pdf.status) {
                        "processing" -> PdfProgressStatus.PROCESSING
                        "indexed" -> PdfProgressStatus.INDEXED
                        "failed" -> PdfProgressStatus.FAILED
                        else -> PdfProgressStatus.INDEXED
                    }
                    updatePdfStatus(pdf.id, finalStatus, pdf.status)
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

    private suspend fun pollPdfStatus(jobId: String, pdfId: String) {
        var pollCount = 0
        val maxPolls = 60 // 2 minutes at 2s interval
        var isComplete = false

        while (!isComplete && pollCount < maxPolls) {
            delay(2000)
            pollCount++

            try {
                ingestRepo.getJobStatus(jobId).firstOrNull()?.let { job ->
                    println("PdfViewModel: Polling job $jobId status: ${job.status}, progress: ${job.progress}%")
                    
                    _uiState.update { it.copy(uploadProgress = 50 + (job.progress / 2)) }

                    when (job.status) {
                        "success" -> {
                            updatePdfStatus(pdfId, PdfProgressStatus.INDEXED, "Ready to search!")
                            isComplete = true
                        }
                        "failed" -> {
                            updatePdfStatus(pdfId, PdfProgressStatus.FAILED, job.error ?: "Processing failed")
                            isComplete = true
                        }
                    }
                }
            } catch (e: Exception) {
                println("PdfViewModel: Polling error: ${e.message}")
            }
        }
    }

    private fun updatePdfStatus(pdfId: String, status: PdfProgressStatus, message: String) {
        _uiState.update { state ->
            val newList = state.uploadedPdfs.map { 
                if (it.id == pdfId) it.copy(status = if (status == PdfProgressStatus.INDEXED) "indexed" else it.status)
                else it
            }
            persistPdfs(newList)
            state.copy(
                uploadedPdfs = newList,
                uploadStatus = status,
                statusMessage = message,
                uploadProgress = if (status == PdfProgressStatus.INDEXED) 100 else state.uploadProgress
            )
        }

        if (status == PdfProgressStatus.INDEXED) {
            scope.launch {
                delay(2000)
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        uploadStatus = PdfProgressStatus.IDLE,
                        statusMessage = ""
                    )
                }
            }
        }
    }

    fun deletePdf(pdfId: String) {
        scope.launch {
            // Optimistically remove from local list
            _uiState.update { state ->
                val newList = state.uploadedPdfs.filter { it.id != pdfId }
                persistPdfs(newList)
                state.copy(uploadedPdfs = newList)
            }
            
            // Try notifying backend but don't care if it fails
            pdfRepo.deletePdf(pdfId)
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

    fun refreshUploadedPdfs() {
        println("PdfViewModel: refreshUploadedPdfs triggered")
        scope.launch {
            pdfRepo.getUploadedPdfs().collect { backendPdfs ->
                if (backendPdfs.isNotEmpty()) {
                    println("PdfViewModel: Received ${backendPdfs.size} PDFs from backend")
                    _uiState.update { state ->
                        val merged = (state.uploadedPdfs + backendPdfs).distinctBy { it.id }
                        persistPdfs(merged)
                        state.copy(uploadedPdfs = merged, isLoading = false)
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
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

