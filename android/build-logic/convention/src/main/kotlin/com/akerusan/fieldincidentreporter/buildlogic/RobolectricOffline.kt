package com.akerusan.fieldincidentreporter.buildlogic

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

/**
 * Robolectric normally downloads its Android runtime jar from Maven Central the first time a test
 * needs it, outside Gradle: a network hiccup then fails every Robolectric test. Here Gradle resolves
 * the jar like any other dependency (so it is cached locally and in CI) and Robolectric runs offline,
 * reading it from Gradle's cache.
 */
internal fun Project.configureRobolectricOffline() {
    val runtime = configurations.create("robolectricRuntime") {
        isCanBeConsumed = false
        isTransitive = false
    }
    dependencies.add(runtime.name, libs.library("robolectric-android-all"))

    tasks.withType<Test>().configureEach {
        jvmArgumentProviders.add(
            RobolectricOfflineArguments(project.objects.fileCollection().from(runtime)),
        )
    }
}

/** Points Robolectric at the folder Gradle downloaded the runtime jar into. */
private class RobolectricOfflineArguments(@get:Classpath val runtimeJars: ConfigurableFileCollection) :
    CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> = listOf(
        "-Drobolectric.offline=true",
        "-Drobolectric.dependency.dir=${runtimeJars.singleFile.parentFile.absolutePath}",
    )
}
