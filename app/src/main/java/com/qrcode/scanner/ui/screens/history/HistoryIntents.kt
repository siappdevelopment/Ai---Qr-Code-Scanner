package com.qrcode.scanner.ui.screens.history

import android.content.Context
import android.content.Intent

object HistoryIntents {
    const val EXTRA_HISTORY_ID = "extra_history_id"

    fun openDetail(context: Context, historyId: Long): Intent =
        Intent(context, HistoryDetailActivity::class.java).apply {
            putExtra(EXTRA_HISTORY_ID, historyId)
        }
}
