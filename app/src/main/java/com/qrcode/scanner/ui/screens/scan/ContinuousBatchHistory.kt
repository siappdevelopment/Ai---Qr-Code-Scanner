package com.qrcode.scanner.ui.screens.scan

import com.qrcode.scanner.data.history.HistoryEntity
import com.qrcode.scanner.data.history.HistoryRepository
import com.qrcode.scanner.data.history.ScanPayloadMapper

/**
 * Persists Continuous Batch session items to History exactly once per item.
 * Does not use the 5-second duplicate window — session uniqueness already
 * handled by [ContinuousBatchSession].
 */
internal object ContinuousBatchHistory {

    /**
     * Inserts each item that does not yet have a [ContinuousBatchItem.historyId].
     * Returns the same list with positive history ids filled in.
     */
    suspend fun persistAcceptedItems(
        repository: HistoryRepository,
        items: List<ContinuousBatchItem>
    ): List<ContinuousBatchItem> =
        persistAcceptedItems(items) { item, detectedType ->
            repository.insertScan(
                rawValue = item.rawValue,
                barcodeFormat = item.barcodeFormat,
                barcodeFormatName = item.barcodeFormatName,
                detectedType = detectedType,
                timestamp = item.acceptedAtMillis,
                source = HistoryEntity.SOURCE_SCANNED
            )
        }

    /**
     * Testable core: [insert] is invoked once per unset historyId.
     */
    internal suspend fun persistAcceptedItems(
        items: List<ContinuousBatchItem>,
        insert: suspend (item: ContinuousBatchItem, detectedType: String) -> Long
    ): List<ContinuousBatchItem> {
        if (items.isEmpty()) return items
        return items.map { item ->
            if (item.historyId > 0L) {
                item
            } else {
                val detectedType = ScanPayloadMapper.detectType(
                    rawValue = item.rawValue,
                    format = item.barcodeFormat,
                    formatName = item.barcodeFormatName
                )
                val id = insert(item, detectedType)
                item.copy(historyId = id)
            }
        }
    }
}
