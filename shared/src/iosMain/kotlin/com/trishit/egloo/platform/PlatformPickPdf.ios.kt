package com.trishit.egloo.platform

/**
 * iOS actual for platformPickPdf.
 *
 * NOTE: This is a minimal stub that returns null. A full implementation should
 * present a UIDocumentPickerViewController and read the selected file bytes,
 * then return a PickedFile. Implementing a robust picker requires UIKit interop
 * and wiring a continuation/promise to the picker delegate.
 */
actual suspend fun platformPickPdf(): PickedFile? {
    // TODO: Implement using UIDocumentPickerViewController + read file into ByteArray
    return null
}

