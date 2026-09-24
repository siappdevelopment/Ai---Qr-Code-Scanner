package com.qrcode.scanner.data.history

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Single source of truth for scan history. Compose screens must go through this layer.
 */
class HistoryRepository(
    private val dao: HistoryDao
) {

    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    fun observeRecent(limit: Int = RECENT_HOME_LIMIT): Flow<List<HistoryEntity>> =
        dao.observeRecent(limit)

    fun observeById(id: Long): Flow<HistoryEntity?> = dao.observeById(id)

    fun observeCount(): Flow<Int> = dao.observeCount()

    fun observeScannedCount(): Flow<Int> =
        dao.observeCountBySource(HistoryEntity.SOURCE_SCANNED)

    fun observeCreatedCount(): Flow<Int> =
        dao.observeCountBySource(HistoryEntity.SOURCE_CREATED)

    suspend fun getById(id: Long): HistoryEntity? = dao.getById(id)

    suspend fun getRecent(limit: Int = RECENT_HOME_LIMIT): List<HistoryEntity> =
        dao.getRecent(limit)

    /**
     * Inserts a scan once. If an identical rawValue+format was saved within
     * [DUPLICATE_WINDOW_MS], returns that row's id instead (lifecycle re-entry safe).
     */
    suspend fun insertScanAvoidingDuplicate(
        rawValue: String,
        barcodeFormat: Int,
        barcodeFormatName: String,
        detectedType: String,
        timestamp: Long = System.currentTimeMillis(),
        isFavorite: Boolean = false,
        source: String = HistoryEntity.SOURCE_SCANNED
    ): Long {
        val existing = dao.findRecentDuplicate(
            rawValue = rawValue,
            barcodeFormat = barcodeFormat,
            sinceTimestamp = timestamp - DUPLICATE_WINDOW_MS
        )
        if (existing != null) return existing.id

        return dao.insert(
            HistoryEntity(
                rawValue = rawValue,
                barcodeFormat = barcodeFormat,
                barcodeFormatName = barcodeFormatName,
                detectedType = detectedType,
                timestamp = timestamp,
                isFavorite = isFavorite,
                source = source
            )
        )
    }

    suspend fun updateFavorite(id: Long, isFavorite: Boolean) {
        if (id <= 0L) return
        dao.updateFavorite(id, isFavorite)
    }

    suspend fun deleteById(id: Long) {
        if (id <= 0L) return
        dao.deleteById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    /** Stitch Clear dialog: purge history but keep pinned favorites. */
    suspend fun clearHistoryKeepingFavorites() {
        dao.deleteAllNonFavorites()
    }

    companion object {
        const val RECENT_HOME_LIMIT = 3
        private const val DUPLICATE_WINDOW_MS = 5_000L
    }
}

object HistoryRepositoryProvider {

    @Volatile
    private var instance: HistoryRepository? = null

    fun get(context: Context): HistoryRepository {
        return instance ?: synchronized(this) {
            instance ?: HistoryRepository(
                HistoryDatabaseProvider.get(context).historyDao()
            ).also { instance = it }
        }
    }
}
