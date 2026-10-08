package com.mabsSD.toolbox.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per tool result ever produced, for the Home "Recent" list and the
 * Files tab. This is the persisted record; the working file it points at
 * lives in cache and can be evicted independently (see WorkingFileManager) —
 * a history row surviving after its file is gone is possible and handled at
 * read time, not treated as a database bug.
 */
@Entity(tableName = "history")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolId: String,
    val outputUri: String,
    val outputName: String,
    val outputSize: Long,
    val createdAtMillis: Long,
)
