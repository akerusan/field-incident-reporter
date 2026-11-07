plugins {
    alias(libs.plugins.fir.android.library)
    alias(libs.plugins.fir.android.room)
    alias(libs.plugins.fir.android.hilt)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.database"
    // Migration tests read the exported schemas.
    sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
