plugins {
    alias(libs.plugins.fir.android.library.compose)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(projects.core.model)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
