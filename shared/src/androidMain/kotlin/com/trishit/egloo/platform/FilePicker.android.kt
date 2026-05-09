package com.trishit.egloo.platform

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Android file picker bridge. MainActivity must call AndroidFilePicker.init(this) in onCreate. */
object AndroidFilePicker {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<PickedFile?>? = null

    fun init(activity: ComponentActivity) {
        println("AndroidFilePicker: Initializing with activity $activity")
        launcher = activity.activityResultRegistry.register("egloo_pdf_picker", activity, ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            println("AndroidFilePicker: Received result URI: $uri")
            val deferred = pending
            pending = null
            if (uri == null) {
                deferred?.complete(null)
                return@register
            }
            // Read bytes off the main thread
            activity.lifecycleScope.launch {
                try {
                    val bytes = readBytes(activity.contentResolver, uri)
                    val name = queryFileName(activity.contentResolver, uri) ?: "file.pdf"
                    println("AndroidFilePicker: Picked file: $name (${bytes.size} bytes)")
                    deferred?.complete(PickedFile(name, bytes))
                } catch (e: Exception) {
                    println("AndroidFilePicker: Error reading file: ${e.message}")
                    deferred?.completeExceptionally(e)
                }
            }
        }
    }

    suspend fun pickPdf(): PickedFile? {
        val l = launcher ?: throw IllegalStateException("AndroidFilePicker not initialized. Call AndroidFilePicker.init(activity) from your Activity onCreate().")
        println("AndroidFilePicker: Launching picker...")
        
        // Cancel any previous pending request
        pending?.complete(null)
        
        pending = CompletableDeferred()
        try {
            l.launch(arrayOf("application/pdf"))
        } catch (e: Exception) {
            println("AndroidFilePicker: Error launching picker: ${e.message}")
            pending?.complete(null)
        }
        
        return try {
            pending!!.await()
        } catch (e: Exception) {
            println("AndroidFilePicker: Exception while awaiting: ${e.message}")
            null
        }
    }

    private suspend fun readBytes(resolver: ContentResolver, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        resolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArrayOutputStream()
            val tmp = ByteArray(8192)
            var read = input.read(tmp)
            while (read >= 0) {
                buffer.write(tmp, 0, read)
                read = input.read(tmp)
            }
            buffer.toByteArray()
        } ?: ByteArray(0)
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        // Best-effort: try ContentResolver query
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOpenableColumnsDisplayName()
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    // Helper extension to safely get display name column index across providers
    private fun android.database.Cursor.getColumnIndexOpenableColumnsDisplayName(): Int {
        return try { getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME) } catch (_: Exception) { -1 }
    }
}

// actual implementation
actual suspend fun platformPickPdf(): PickedFile? {
    return AndroidFilePicker.pickPdf()
}

