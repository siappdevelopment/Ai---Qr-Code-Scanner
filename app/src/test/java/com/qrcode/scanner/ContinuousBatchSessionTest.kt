package com.qrcode.scanner

import com.qrcode.scanner.ui.screens.scan.ContinuousBatchItem
import com.qrcode.scanner.ui.screens.scan.ContinuousBatchSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContinuousBatchSessionTest {

    @Test
    fun tryAccept_acceptsFirstOccurrenceOnly() {
        val session = ContinuousBatchSession()
        assertTrue(session.tryAccept("https://a.test", 256, "QR_CODE"))
        assertFalse(session.tryAccept("https://a.test", 256, "QR_CODE"))
        assertEquals(1, session.size())
        assertEquals("https://a.test", session.snapshot().single().rawValue)
        assertEquals("QR_CODE", session.snapshot().single().barcodeFormatName)
    }

    @Test
    fun tryAccept_differentRawValue_isAccepted() {
        val session = ContinuousBatchSession()
        assertTrue(session.tryAccept("A", 256, "QR_CODE"))
        assertTrue(session.tryAccept("B", 256, "QR_CODE"))
        assertEquals(2, session.size())
        assertEquals(listOf("A", "B"), session.snapshot().map { it.rawValue })
    }

    @Test
    fun tryAccept_sameRawDifferentFormat_isDistinct() {
        val session = ContinuousBatchSession()
        assertTrue(session.tryAccept("123456", 256, "QR_CODE"))
        assertTrue(session.tryAccept("123456", 8, "EAN_13"))
        assertFalse(session.tryAccept("123456", 256, "QR_CODE"))
        assertEquals(2, session.size())
    }

    @Test
    fun clear_resetsSession() {
        val session = ContinuousBatchSession()
        session.tryAccept("A", 1, "CODE_128")
        session.clear()
        assertEquals(0, session.size())
        assertTrue(session.snapshot().isEmpty())
        assertTrue(session.tryAccept("A", 1, "CODE_128"))
    }

    @Test
    fun blankRaw_isRejected() {
        val session = ContinuousBatchSession()
        assertFalse(session.tryAccept("", 256, "QR_CODE"))
        assertFalse(session.tryAccept("   ", 256, "QR_CODE"))
        assertEquals(0, session.size())
    }

    @Test
    fun itemsFlow_exposesAcceptedList() {
        val session = ContinuousBatchSession()
        assertTrue(session.items.value.isEmpty())
        session.tryAccept("one", 256, "QR_CODE")
        session.tryAccept("two", 8, "EAN_13")
        assertEquals(2, session.items.value.size)
        assertEquals(256, session.items.value[0].barcodeFormat)
        assertEquals(8, session.items.value[1].barcodeFormat)
    }

    @Test
    fun acceptedItem_historyIdDefaultsUnset() {
        val session = ContinuousBatchSession()
        session.tryAccept("A", 256, "QR_CODE")
        assertEquals(-1L, session.snapshot().single().historyId)
    }

    @Test
    fun continuousBatchItem_copyPreservesHistoryId() {
        val item = ContinuousBatchItem(
            rawValue = "A",
            barcodeFormat = 256,
            barcodeFormatName = "QR_CODE",
            historyId = 42L
        )
        assertEquals(42L, item.copy(rawValue = "A").historyId)
        assertEquals(-1L, item.copy(historyId = -1L).historyId)
    }
}
