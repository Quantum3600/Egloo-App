package com.trishit.egloo.platform

/** A picked file returned by platform file pickers. */
data class PickedFile(
	val filename: String,
	val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PickedFile

        if (filename != other.filename) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}


