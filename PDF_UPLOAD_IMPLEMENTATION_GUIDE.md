# PDF Upload Feature Implementation Guide - Egloo

## Overview
This guide outlines the complete implementation of PDF upload functionality for the Egloo KMP app with file selection, upload progress, processing status, and search integration.

---

## Completed Components

✅ **DTOs** (`data/api/Dtos.kt`)
- `PdfUploadResponse`
- `PdfListResponse`
- `PdfDeleteRequest`
- Mapper: `PdfUploadResponse.toDomain()`

✅ **Domain Models** (`domain/models/Models.kt`)
- `UploadedPdf` - Domain model for uploaded PDFs
- `PdfUploadStatus` - Enum for upload states
- Mapper: `PdfUploadResponse.toDomain()`

✅ **Repository Interface** (`data/repositories/Repositories.kt`)
- `PdfRepository` interface with methods:
  - `getUploadedPdfs()` - Fetch list
  - `uploadPdf()` - Upload file
  - `deletePdf()` - Delete PDF
  - `reindexPdf()` - Re-process PDF

✅ **Ktor Implementation** (`data/repositories/KtorPdfRepository.kt`)
- Real backend API integration
- Multipart form-data upload
- Error handling

---

## Remaining Components to Implement

### 1. ViewModel (data/domain/viewmodels/PdfViewModel.kt)

```kotlin
data class PdfUiState(
    val uploadedPdfs: List<UploadedPdf> = emptyList(),
    val uploadProgress: Int = 0,
    val isUploading: Boolean = false,
    val uploadStatus: String = "",  // "pending", "uploading", "processing", "indexed", "failed"
    val errorMessage: String? = null,
    val selectedFile: String? = null
)

class PdfViewModel(private val pdfRepo: PdfRepository) : BaseViewModel() {
    private val _uiState = MutableStateFlow(PdfUiState())
    val uiState: StateFlow<PdfUiState> = _uiState.asStateFlow()

    init {
        // Load PDFs on init
        scope.launch {
            pdfRepo.getUploadedPdfs().collect { pdfs ->
                _uiState.update { it.copy(uploadedPdfs = pdfs) }
            }
        }
    }

    fun uploadPdf(filename: String, fileBytes: ByteArray) {
        _uiState.update { 
            it.copy(
                isUploading = true,
                uploadStatus = "uploading",
                uploadProgress = 0
            )
        }
        
        scope.launch {
            val result = pdfRepo.uploadPdf(filename, fileBytes)
            
            result.onSuccess { pdf ->
                _uiState.update {
                    it.copy(
                        uploadStatus = when (pdf.status) {
                            "processing" -> "Processing PDF..."
                            "indexed" -> "Ready to search!"
                            else -> pdf.status
                        },
                        uploadProgress = when (pdf.status) {
                            "processing" -> 50
                            "indexed" -> 100
                            else -> 0
                        }
                    )
                }
                refreshPdfs()
                
                // Auto-dismiss success after 2 seconds
                delay(2000)
                _uiState.update { 
                    it.copy(isUploading = false, errorMessage = null)
                }
            }
            
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        uploadStatus = "failed",
                        errorMessage = error.message ?: "Upload failed",
                        isUploading = false
                    )
                }
            }
        }
    }

    fun deletePdf(pdfId: String) {
        scope.launch {
            val result = pdfRepo.deletePdf(pdfId)
            result.onSuccess { refreshPdfs() }
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun reindexPdf(pdfId: String) {
        _uiState.update { it.copy(uploadStatus = "Re-indexing...") }
        scope.launch {
            val result = pdfRepo.reindexPdf(pdfId)
            result.onSuccess { refreshPdfs() }
            result.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    private fun refreshPdfs() {
        scope.launch {
            pdfRepo.getUploadedPdfs().collect { pdfs ->
                _uiState.update { it.copy(uploadedPdfs = pdfs) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

### 2. UI Components (ui/screens/OtherScreens.kt)

Add to SourcesScreen or create separate PdfUploadSection:

```kotlin
@Composable
fun PdfUploadSection(viewModel: PdfViewModel = koinInject()) {
    val state by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            "Manual Uploads",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Upload Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // PDF Icon
                Icon(
                    Icons.Default.Description,
                    null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    "Upload PDF for Pingo",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    "Upload notes, books, syllabus, docs, research papers.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Progress Bar
                if (state.isUploading) {
                    LinearProgressIndicator(
                        progress = { state.uploadProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        state.uploadStatus,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Upload Button
                Button(
                    onClick = { /* open file picker */ },
                    enabled = !state.isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose PDF")
                }
            }
        }
        
        // Error Message
        if (state.errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            }
        }
        
        // Upload History
        if (state.uploadedPdfs.isNotEmpty()) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                "Uploaded Documents",
                style = MaterialTheme.typography.titleMedium
            )
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.uploadedPdfs) { pdf ->
                    PdfHistoryCard(
                        pdf = pdf,
                        onDelete = { viewModel.deletePdf(pdf.id) },
                        onReindex = { viewModel.reindexPdf(pdf.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfHistoryCard(
    pdf: UploadedPdf,
    onDelete: () -> Unit,
    onReindex: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Description, null, Modifier.size(24.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(pdf.filename, style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "${pdf.pages} pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        pdf.uploadedAt,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pdf.status == "indexed") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EglooColors.TealSurface
                        ) {
                            Text(
                                "Searchable",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(4.dp),
                                color = EglooColors.TealDark
                            )
                        }
                    }
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null)
            }
            
            if (pdf.status == "failed") {
                IconButton(onClick = onReindex) {
                    Icon(Icons.Default.Refresh, null)
                }
            }
        }
    }
}
```

### 3. File Picker Integration

For Compose Multiplatform, use platform-specific solutions:

**Android:** Use `ActivityResultContracts.PickDocument()`
**iOS:** No built-in solution, consider file sharing via apps
**Desktop:** Use Java `JFileChooser` or system dialog
**Web:** Use HTML input element

### 4. Update Koin Bindings

```kotlin
// In EglooModule.kt
single<PdfRepository> { KtorPdfRepository(get()) }
factoryOf(::PdfViewModel)
```

### 5. Integrate with Chat Screen

Add PDF citation cards to ChatScreen when answers reference PDFs:

```kotlin
@Composable
fun PdfCitationCard(pdfName: String, page: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = EglooColors.TealSurface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Description, null, tint = EglooColors.TealDark)
            Column(modifier = Modifier.weight(1f)) {
                Text(pdfName, style = MaterialTheme.typography.titleSmall)
                Text("Page $page", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { /* open preview */ }) {
                Icon(Icons.Default.OpenInNew, null)
            }
        }
    }
}
```

---

## Backend API Endpoints Required

```
POST /api/v1/ingest/pdf
  Content-Type: multipart/form-data
  Field: file (binary)
  Response: PdfUploadResponse

GET /api/v1/ingest/pdfs
  Response: PdfListResponse

DELETE /api/v1/ingest/pdf/{pdf_id}
  Response: 204 No Content

POST /api/v1/ingest/pdf/{pdf_id}/reindex
  Response: 200 OK
```

---

## Error Handling States

```kotlin
enum class PdfError {
    FILE_TOO_LARGE,
    INVALID_PDF,
    EMPTY_PDF,
    PARSING_FAILED,
    NETWORK_ERROR,
    SERVER_ERROR
}

private fun mapErrorMessage(error: Exception): String = when {
    error.message?.contains("size") == true -> "File is too large (max 50MB)"
    error.message?.contains("invalid") == true -> "Invalid PDF format"
    error.message?.contains("empty") == true -> "PDF appears to be empty"
    error.message?.contains("parse") == true -> "Failed to parse PDF"
    error.message?.contains("network") == true -> "Network error. Please check connection."
    else -> "Upload failed. Please try again."
}
```

---

## Implementation Checklist

- [ ] Add `mpfilepicker` or platform-specific file picker
- [ ] Create `PdfViewModel` with all states
- [ ] Create `PdfUploadSection` composable
- [ ] Create `PdfHistoryCard` composable
- [ ] Create `PdfCitationCard` for search results
- [ ] Implement file picker on Android
- [ ] Implement file picker on iOS
- [ ] Implement file picker on Desktop
- [ ] Implement file picker on Web
- [ ] Add Koin bindings for `PdfRepository` and `PdfViewModel`
- [ ] Integrate PDF upload section into Sources screen
- [ ] Integrate citations into Chat screen
- [ ] Add error handling and user feedback
- [ ] Test upload with actual PDFs
- [ ] Test error cases (too large, invalid, etc.)
- [ ] Add unit tests for `PdfViewModel`
- [ ] Add UI tests for PDF upload flow

---

## Architecture Summary

```
UI Layer:
  ├─ SourcesScreen
  ├─ PdfUploadSection
  ├─ PdfHistoryCard
  └─ PdfCitationCard

ViewModel Layer:
  └─ PdfViewModel

Repository Layer:
  ├─ PdfRepository (interface)
  └─ KtorPdfRepository (implementation)

Data Layer:
  ├─ DTOs (PdfUploadResponse, etc.)
  └─ API calls (/api/v1/ingest/pdf)

Domain Layer:
  └─ UploadedPdf model
```

This architecture follows the clean separation of concerns principle and allows for easy testing and maintenance.

