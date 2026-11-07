package com.akerusan.fieldincidentreporter

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Provides WorkManager's configuration so workers are created by Hilt
 * (the default WorkManager initializer is removed in the manifest).
 */
@HiltAndroidApp
class FieldIncidentReporterApplication :
    Application(),
    Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
