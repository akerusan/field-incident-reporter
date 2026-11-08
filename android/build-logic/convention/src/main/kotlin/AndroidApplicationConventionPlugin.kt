import com.akerusan.fieldincidentreporter.buildlogic.configureAndroidCompose
import com.akerusan.fieldincidentreporter.buildlogic.configureKotlinAndroid
import com.akerusan.fieldincidentreporter.buildlogic.libs
import com.akerusan.fieldincidentreporter.buildlogic.versionInt
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The `:app` module: Android application with Compose. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                configureAndroidCompose(this)
                defaultConfig.targetSdk = libs.versionInt("targetSdk")
            }
        }
    }
}
