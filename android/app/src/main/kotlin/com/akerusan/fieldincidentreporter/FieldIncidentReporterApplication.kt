package com.akerusan.fieldincidentreporter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.akerusan.fieldincidentreporter.core.domain.usecase.RefreshIncidentsUseCase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Provides WorkManager's configuration so workers are created by Hilt
 * (the default WorkManager initializer is removed in the manifest), and requests a sync on every
 * cold start so pending changes and overdue deletes go out even if nothing else triggers one.
 */
@HiltAndroidApp
class FieldIncidentReporterApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var refreshIncidents: RefreshIncidentsUseCase

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        refreshIncidents()
    }
}
