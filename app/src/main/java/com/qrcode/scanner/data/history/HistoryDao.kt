package com.qrcode.scanner.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity): Long

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<HistoryEntity>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<HistoryEntity>

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Query("SELECT * FROM scan_history WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<HistoryEntity?>

    @Query(
        """
        SELECT * FROM scan_history
        WHERE rawValue = :rawValue
          AND barcodeFormat = :barcodeFormat
          AND timestamp >= :sinceTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
        """
    )
    suspend fun findRecentDuplicate(
        rawValue: String,
        barcodeFormat: Int,
        sinceTimestamp: Long
    ): HistoryEntity?

    @Query("UPDATE scan_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    @Query("DELETE FROM scan_history WHERE isFavorite = 0")
    suspend fun deleteAllNonFavorites()

    @Query("SELECT COUNT(*) FROM scan_history")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_history WHERE source = :source")
    fun observeCountBySource(source: String): Flow<Int>
}
