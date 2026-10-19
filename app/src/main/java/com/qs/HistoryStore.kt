package com.qs

import android.content.Context

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)

    fun savePosition(uri: String, position: Long) {
        prefs.edit().putLong(uri, position).apply()
    }

    fun getPosition(uri: String): Long {
        return prefs.getLong(uri, 0L)
    }
}
