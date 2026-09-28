package com.qrcode.scanner.ui.screens.scan

import java.io.Serializable
import java.util.Collections
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * One accepted Continuous Batch code for the active scanner session.
 * [historyId] is filled when the session is persisted to History (finish).
 */
data class ContinuousBatchItem(
    val rawValue: String,
    val barcodeFormat: Int,
    val barcodeFormatName: String,
    val acceptedAtMillis: Long = System.currentTimeMillis(),
    val historyId: Long = -1L
) : Serializable

/**
 * In-memory Continuous Batch session.
 * Identity key = rawValue + barcodeFormat. Not DataStore / Room.
 */
internal class ContinuousBatchSession {
    private val lock = Any()
    private val acceptedKeys = Collections.synchronizedSet(mutableSetOf<String>())
    private val _items = MutableStateFlow<List<ContinuousBatchItem>>(emptyList())
    val items: StateFlow<List<ContinuousBatchItem>> = _items.asStateFlow()

    fun identityKey(rawValue: String, format: Int): String = "$rawValue|$format"

    /**
     * Returns true if newly accepted. Stores [ContinuousBatchItem] for UI.
     */
    fun tryAccept(rawValue: String, format: Int, formatName: String): Boolean {
        if (rawValue.isBlank()) return false
        val key = identityKey(rawValue, format)
        synchronized(lock) {
            if (!acceptedKeys.add(key)) return false
            val item = ContinuousBatchItem(
                rawValue = rawValue,
                barcodeFormat = format,
                barcodeFormatName = formatName
            )
            _items.update { it + item }
            return true
        }
    }

    fun snapshot(): List<ContinuousBatchItem> = _items.value

    fun size(): Int = _items.value.size

    fun clear() {
        synchronized(lock) {
            acceptedKeys.clear()
            _items.value = emptyList()
        }
    }
}
