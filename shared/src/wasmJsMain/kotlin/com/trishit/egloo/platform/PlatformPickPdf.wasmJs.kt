package com.trishit.egloo.platform

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalWasmJsInterop::class)
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
                        // Use Uint8Array which is standard for file data
                        val u8 = Uint8Array(array)
                        val bytes = ByteArray(u8.length)
                        for (i in 0 until u8.length) {
                            bytes[i] = u8[i].toInt().toByte()
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


