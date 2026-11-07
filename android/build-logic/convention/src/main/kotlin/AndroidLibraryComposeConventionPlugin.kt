import com.akerusan.fieldincidentreporter.buildlogic.configureAndroidCompose
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** An Android library module with Compose UI (`:core:designsystem`). */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("fir.android.library")
            extensions.configure<LibraryExtension> {
                configureAndroidCompose(this)
            }
        }
    }
}
