package com.trishit.egloo.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

actual suspend fun platformPickPdf(): PickedFile? = withContext(Dispatchers.IO) {
    try {
        // Use AWT FileDialog to pick a PDF file. This will block the EDT but it's OK for desktop apps.
        val fd = FileDialog(Frame(), "Select PDF to upload", FileDialog.LOAD)
        fd.isVisible = true
        val dir = fd.directory
        val file = fd.file
        if (dir == null || file == null) return@withContext null
        val f = File(dir, file)
        val bytes = f.readBytes()
        return@withContext PickedFile(filename = f.name, bytes = bytes)
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

