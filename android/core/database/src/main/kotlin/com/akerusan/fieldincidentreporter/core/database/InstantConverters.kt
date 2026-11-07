package com.akerusan.fieldincidentreporter.core.database

import androidx.room.TypeConverter
import java.time.Instant

/** Stores instants as epoch milliseconds, so they sort and compare correctly in SQL. */
internal class InstantConverters {
    @TypeConverter
    fun toEpochMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromEpochMillis(epochMillis: Long?): Instant? = epochMillis?.let(Instant::ofEpochMilli)
}
