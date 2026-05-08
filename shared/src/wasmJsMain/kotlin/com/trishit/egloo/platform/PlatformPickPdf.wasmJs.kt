package com.trishit.egloo.platform

import io.ktor.client.request.invoke
import js.buffer.ArrayBuffer
import js.typedarrays.Int8Array
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
					// Import ArrayBuffer from org.khronos.webgl
					val array = reader.result as? ArrayBuffer
					if (array == null) {
						document.body?.removeChild(input)
						if (!cont.isCompleted) cont.resume(null)
					} else {
						// Use Int8Array so it directly maps to Kotlin's signed ByteArray
						val i8 = Int8Array(array)
						val bytes = ByteArray(i8.length) { i -> i8[i].toInt().toByte() }
						val name = file.name ?: "file.pdf"
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
			null // Explicitly return null if Wasm JS interop expects a dynamic return for the event
		}

		input.click()

		cont.invokeOnCancellation {
			try { document.body?.removeChild(input) } catch (_: Throwable) {}
		}
	} catch (e: Throwable) {
		cont.resumeWithException(e)
	}
}


