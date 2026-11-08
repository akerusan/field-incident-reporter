package com.akerusan.fieldincidentreporter.core.database.di

import android.content.Context
import androidx.room.Room
import com.akerusan.fieldincidentreporter.core.database.FirDatabase
import com.akerusan.fieldincidentreporter.core.database.dao.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FirDatabase =
        Room.databaseBuilder(context, FirDatabase::class.java, "field-incident-reporter.db").build()

    @Provides
    fun provideIncidentDao(database: FirDatabase): IncidentDao = database.incidentDao()
}
