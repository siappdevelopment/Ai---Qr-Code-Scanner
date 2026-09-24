package com.qrcode.scanner.data.history

import android.content.Context
import androidx.room.Room

/**
 * Singleton Room database — no Hilt required.
 */
object HistoryDatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "scanpulse_history.db"
            ).build().also { instance = it }
        }
    }
}
