plugins {
    alias(libs.plugins.fir.android.application)
    alias(libs.plugins.fir.android.hilt)
}

android {
    namespace = "com.akerusan.fieldincidentreporter"

    defaultConfig {
        applicationId = "com.akerusan.fieldincidentreporter"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope.set(setOf("androidx.**", "kotlin.**", "kotlinx.**"))
            }
        }
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.feature.incidentlist)
    implementation(projects.feature.incidentdetail)
    implementation(projects.feature.incidenteditor)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.hilt.compiler)
}
