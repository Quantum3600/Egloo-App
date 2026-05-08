package com.trishit.egloo.platform

/** Opens a platform-native file picker and returns the selected PDF bytes and filename.
 *  Returns null if the user cancels. Must be implemented per-platform. */
expect suspend fun platformPickPdf(): PickedFile?

