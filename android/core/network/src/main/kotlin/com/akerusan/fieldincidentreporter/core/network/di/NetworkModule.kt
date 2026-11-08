package com.akerusan.fieldincidentreporter.core.network.di

import com.akerusan.fieldincidentreporter.core.network.BuildConfig
import com.akerusan.fieldincidentreporter.core.network.IncidentApi
import com.akerusan.fieldincidentreporter.core.network.NetworkConstants
import com.akerusan.fieldincidentreporter.core.network.NetworkFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkModule {
    @Provides
    @Named(NetworkConstants.BASE_URL)
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Singleton
    fun provideJson(): Json = NetworkFactory.json()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS) // photo uploads on slow field connections
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideIncidentApi(
        @Named(NetworkConstants.BASE_URL) baseUrl: String,
        okHttpClient: OkHttpClient,
        json: Json,
    ): IncidentApi = NetworkFactory.incidentApi(baseUrl, okHttpClient, json)
}
