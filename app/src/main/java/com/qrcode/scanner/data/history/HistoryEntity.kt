package com.qrcode.scanner.data.history

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted scan / create history row.
 */
@Entity(
    tableName = "scan_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isFavorite"]),
        Index(value = ["source"]),
        Index(value = ["rawValue", "barcodeFormat", "timestamp"])
    ]
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawValue: String,
    val barcodeFormat: Int,
    val barcodeFormatName: String,
    val detectedType: String,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    /** "scanned" or "created" — used by History filter tabs. */
    val source: String = SOURCE_SCANNED
) {
    companion object {
        const val SOURCE_SCANNED = "scanned"
        const val SOURCE_CREATED = "created"
    }
}
