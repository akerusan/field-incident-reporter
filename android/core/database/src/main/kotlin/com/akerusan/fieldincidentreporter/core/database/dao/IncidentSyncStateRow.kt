package com.akerusan.fieldincidentreporter.core.database.dao

import androidx.room.ColumnInfo
import com.akerusan.fieldincidentreporter.core.model.SyncState

/** Projection used by the pull to decide which rows it may overwrite. */
data class IncidentSyncStateRow(val id: String, @ColumnInfo(name = "sync_state") val syncState: SyncState)
