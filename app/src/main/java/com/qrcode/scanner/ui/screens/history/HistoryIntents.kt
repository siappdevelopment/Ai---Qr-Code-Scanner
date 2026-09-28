package com.qrcode.scanner.ui.screens.history

import android.content.Context
import android.content.Intent

object HistoryIntents {
    const val EXTRA_HISTORY_ID = "extra_history_id"

    fun openFavorites(context: Context): Intent =
        Intent(context, FavoritesActivity::class.java)

    fun openHistory(context: Context): Intent =
        Intent(context, HistoryActivity::class.java)

    fun openDetail(context: Context, historyId: Long): Intent =
        Intent(context, HistoryDetailActivity::class.java).apply {
            putExtra(EXTRA_HISTORY_ID, historyId)
        }
}
