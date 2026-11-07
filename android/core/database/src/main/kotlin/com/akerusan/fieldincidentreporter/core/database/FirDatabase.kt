package com.akerusan.fieldincidentreporter.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity

/** The local database: the app's single source of truth for incidents. */
@Database(
    entities = [IncidentEntity::class, IncidentPhotoEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 2: incidents.server_error_count (retry limit kept on the row)
        AutoMigration(from = 1, to = 2),
    ],
)
@TypeConverters(InstantConverters::class)
abstract class FirDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
}
