package com.akerusan.fieldincidentreporter.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentEntity
import com.akerusan.fieldincidentreporter.core.database.entity.IncidentPhotoEntity

/** The local database: the app's single source of truth for incidents. */
@Database(
    entities = [IncidentEntity::class, IncidentPhotoEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(InstantConverters::class)
abstract class FirDatabase : RoomDatabase() {
    abstract fun incidentDao(): IncidentDao
}
