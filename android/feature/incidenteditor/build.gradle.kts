plugins {
    alias(libs.plugins.fir.android.feature)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.feature.incidenteditor"
}

dependencies {
    // Camera and photo picker launchers, back handling.
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
