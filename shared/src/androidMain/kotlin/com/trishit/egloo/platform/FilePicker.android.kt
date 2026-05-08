package com.trishit.egloo.platform

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Android file picker bridge. MainActivity must call AndroidFilePicker.init(this) in onCreate. */
object AndroidFilePicker {
    private var launcher: ActivityResultLauncher<Array<String>>? = null
    private var pending: CompletableDeferred<PickedFile?>? = null

    fun init(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val deferred = pending
            pending = null
            if (uri == null) {
                deferred?.complete(null)
                return@registerForActivityResult
            }
            // Read bytes off the main thread
            activity.lifecycleScope.launchWhenStarted {
                try {
                    val bytes = readBytes(activity.contentResolver, uri)
                    val name = queryFileName(activity.contentResolver, uri) ?: "file.pdf"
                    deferred?.complete(PickedFile(name, bytes))
                } catch (e: Exception) {
                    deferred?.completeExceptionally(e)
                }
            }
        }
    }

    suspend fun pickPdf(): PickedFile? {
        val l = launcher ?: throw IllegalStateException("AndroidFilePicker not initialized. Call AndroidFilePicker.init(activity) from your Activity onCreate().")
        pending = CompletableDeferred()
        l.launch(arrayOf("application/pdf"))
        return pending!!.await()
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

