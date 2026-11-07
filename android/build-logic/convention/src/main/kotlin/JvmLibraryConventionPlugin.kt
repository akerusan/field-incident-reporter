import com.akerusan.fieldincidentreporter.buildlogic.configureKotlinJvm
import com.akerusan.fieldincidentreporter.buildlogic.library
import com.akerusan.fieldincidentreporter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A pure Kotlin/JVM module with no Android dependency (`:core:model`, `:core:domain`). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
