package com.mathieu.blocker.data.db

import androidx.room.Entity

@Entity(tableName = "usage_stats", primaryKeys = ["date", "packageName"])
data class UsageStatsEntity(
    val date: String, // format: "yyyy-MM-dd"
    val packageName: String,
    val scrollCount: Int = 0,
    val scrollTimeMs: Long = 0,
    val videoCount: Int = 0,
    val nonVideoCount: Int = 0,
    val openCount: Int = 0,
    val returnCount: Int = 0  // times user pressed "Non, je reviens"
)
