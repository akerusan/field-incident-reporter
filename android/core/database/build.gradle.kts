plugins {
    alias(libs.plugins.fir.android.library)
    alias(libs.plugins.fir.android.room)
    alias(libs.plugins.fir.android.hilt)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.database"
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
}
