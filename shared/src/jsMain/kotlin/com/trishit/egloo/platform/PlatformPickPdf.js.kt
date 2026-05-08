package com.trishit.egloo.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Browser (kotlin/js) file picker for PDF files.
 * Attempts to replicate the wasm-js behavior using standard DOM APIs.
 */
actual suspend fun platformPickPdf(): PickedFile? = suspendCancellableCoroutine { cont ->
    try {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "application/pdf"
        input.style.display = "none"
        document.body?.appendChild(input)

        input.onchange = {
            val file = input.files?.get(0)
            if (file == null) {
                document.body?.removeChild(input)
                if (!cont.isCompleted) cont.resume(null)
            } else {
                val reader = FileReader()
                reader.onload = {
                    val array = reader.result as? ArrayBuffer
                    if (array == null) {
                        document.body?.removeChild(input)
                        if (!cont.isCompleted) cont.resume(null)
                    } else {
                        val i8 = Int8Array(array)
                        val len = i8.length.toInt()
                        val bytes = ByteArray(len)
                        var idx = 0
                        while (idx < len) {
                            // use get() to avoid operator overload issues in some JS targets
                            bytes[idx] = i8.get(idx).toInt().toByte()
                            idx++
                        }
                        val name = file.name
                        document.body?.removeChild(input)
                        if (!cont.isCompleted) cont.resume(PickedFile(name, bytes))
                    }
                }

                reader.onerror = {
                    document.body?.removeChild(input)
                    if (!cont.isCompleted) cont.resumeWithException(RuntimeException("Failed to read file"))
                }

                reader.readAsArrayBuffer(file)
            }
            null
        }

        input.click()

        cont.invokeOnCancellation {
            try { document.body?.removeChild(input) } catch (_: Throwable) {}
        }
    } catch (e: Throwable) {
        cont.resumeWithException(e)
    }
}

