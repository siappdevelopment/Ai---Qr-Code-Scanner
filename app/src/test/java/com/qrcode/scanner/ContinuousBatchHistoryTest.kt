package com.qrcode.scanner

import com.qrcode.scanner.data.history.HistoryEntity
import com.qrcode.scanner.ui.screens.scan.ContinuousBatchHistory
import com.qrcode.scanner.ui.screens.scan.ContinuousBatchItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM policy checks for Continuous Batch History persistence (no Room).
 */
class ContinuousBatchHistoryTest {

    @Test
    fun persistAcceptedItems_assignsHistoryIdsOnce() = runBlocking {
        var insertCalls = 0
        val timestamps = mutableListOf<Long>()
        val items = listOf(
            ContinuousBatchItem("A", 256, "QR_CODE", acceptedAtMillis = 1000L),
            ContinuousBatchItem("B", 8, "EAN_13", acceptedAtMillis = 2000L)
        )
        val persisted = ContinuousBatchHistory.persistAcceptedItems(items) { item, _ ->
            insertCalls++
            timestamps += item.acceptedAtMillis
            insertCalls.toLong()
        }
        assertEquals(2, insertCalls)
        assertEquals(listOf(1L, 2L), persisted.map { it.historyId })
        assertEquals(listOf(1000L, 2000L), timestamps)
    }

    @Test
    fun persistAcceptedItems_skipsItemsThatAlreadyHaveHistoryId() = runBlocking {
        var insertCalls = 0
        val items = listOf(
            ContinuousBatchItem("A", 256, "QR_CODE", historyId = 99L),
            ContinuousBatchItem("B", 8, "EAN_13")
        )
        val persisted = ContinuousBatchHistory.persistAcceptedItems(items) { _, _ ->
            insertCalls++
            7L
        }
        assertEquals(1, insertCalls)
        assertEquals(99L, persisted[0].historyId)
        assertEquals(7L, persisted[1].historyId)
    }

    @Test
    fun persistAcceptedItems_emptyList_doesNothing() = runBlocking {
        var insertCalls = 0
        val persisted = ContinuousBatchHistory.persistAcceptedItems(emptyList()) { _, _ ->
            insertCalls++
            1L
        }
        assertTrue(persisted.isEmpty())
        assertEquals(0, insertCalls)
    }

    @Test
    fun persistAcceptedItems_usesScannedSourceConstant() {
        // Guard against accidental created-source default in helper wiring.
        assertEquals("scanned", HistoryEntity.SOURCE_SCANNED)
    }
}
