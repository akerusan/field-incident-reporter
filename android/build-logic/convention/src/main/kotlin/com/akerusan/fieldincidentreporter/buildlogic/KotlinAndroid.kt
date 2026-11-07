package com.akerusan.fieldincidentreporter.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Settings shared by every Android module (application and libraries): SDK levels, Java/Kotlin
 * toolchain and unit-test defaults. Values come from the version catalog so they live in one place.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    val jvmToolchain = libs.versionInt("jvmToolchain")
    val javaVersion = JavaVersion.toVersion(jvmToolchain)
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")
        defaultConfig.minSdk = libs.versionInt("minSdk")
        compileOptions.sourceCompatibility = javaVersion
        compileOptions.targetCompatibility = javaVersion
        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.isReturnDefaultValues = true
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        jvmToolchain(jvmToolchain)
    }
    configureRobolectricOffline()
    // Hilt generates unit-test sources in every module it is applied to, so a module without tests
    // of its own would otherwise fail Gradle's "no tests discovered" check.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
        // Robolectric reads FileDescriptor internals through jdk.internal.access on JDK 21+.
        jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    }
}

/** Settings for pure Kotlin/JVM modules (`:core:model`, `:core:domain`). */
internal fun Project.configureKotlinJvm() {
    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(libs.versionInt("jvmToolchain"))
    }
}
