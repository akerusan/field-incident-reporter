package com.akerusan.fieldincidentreporter.core.data.di

import javax.inject.Qualifier

/** The dispatcher for blocking I/O (files, image decoding). Replaced by a test dispatcher in tests. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
