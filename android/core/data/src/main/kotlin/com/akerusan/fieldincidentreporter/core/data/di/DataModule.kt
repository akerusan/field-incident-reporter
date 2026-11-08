package com.akerusan.fieldincidentreporter.core.data.di

import com.akerusan.fieldincidentreporter.core.data.connectivity.NetworkConnectivityObserver
import com.akerusan.fieldincidentreporter.core.data.photo.FilePhotoStore
import com.akerusan.fieldincidentreporter.core.data.repository.OfflineFirstIncidentRepository
import com.akerusan.fieldincidentreporter.core.data.sync.WorkManagerSyncScheduler
import com.akerusan.fieldincidentreporter.core.data.util.SystemClock
import com.akerusan.fieldincidentreporter.core.data.util.UuidIdGenerator
import com.akerusan.fieldincidentreporter.core.domain.Clock
import com.akerusan.fieldincidentreporter.core.domain.ConnectivityObserver
import com.akerusan.fieldincidentreporter.core.domain.IdGenerator
import com.akerusan.fieldincidentreporter.core.domain.IncidentRepository
import com.akerusan.fieldincidentreporter.core.domain.PhotoStore
import com.akerusan.fieldincidentreporter.core.domain.SyncMonitor
import com.akerusan.fieldincidentreporter.core.domain.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Binds the domain interfaces to their implementations in this module. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun bindIncidentRepository(impl: OfflineFirstIncidentRepository): IncidentRepository

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    @Binds
    abstract fun bindSyncMonitor(impl: WorkManagerSyncScheduler): SyncMonitor

    @Binds
    abstract fun bindPhotoStore(impl: FilePhotoStore): PhotoStore

    @Binds
    abstract fun bindConnectivityObserver(impl: NetworkConnectivityObserver): ConnectivityObserver

    @Binds
    abstract fun bindClock(impl: SystemClock): Clock

    @Binds
    abstract fun bindIdGenerator(impl: UuidIdGenerator): IdGenerator

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }
}
