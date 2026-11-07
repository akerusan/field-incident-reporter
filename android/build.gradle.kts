// Plugins are declared here once, with their versions, and applied in modules through the
// convention plugins in `build-logic/`.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
}

// Source files only: the excludes also stop Spotless from walking build directories that other
// tasks are writing to in parallel.
val formattedSources = fileTree(rootDir) {
    exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
}

spotless {
    kotlin {
        target(formattedSources.matching { include("**/*.kt") })
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target(formattedSources.matching { include("**/*.gradle.kts") })
        ktlint(libs.versions.ktlint.get())
    }
}
