plugins {
    alias(libs.plugins.fir.android.library)
    alias(libs.plugins.fir.android.room)
    alias(libs.plugins.fir.android.hilt)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.database"
}

dependencies {
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
