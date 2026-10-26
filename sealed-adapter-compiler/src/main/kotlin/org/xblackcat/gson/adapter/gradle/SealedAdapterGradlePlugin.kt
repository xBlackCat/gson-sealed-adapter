package org.xblackcat.gson.adapter.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.InputStream

/**
 * Gradle side of the `sealed-adapter-compiler` plugin.
 *
 * - Applies the Kotlin compiler plugin (see [org.xblackcat.gson.adapter.compiler.SealedAdapterComponentRegistrar])
 *   to every Kotlin compilation of the project it is applied to.
 * - Automatically adds the `org.xblackcat.gson:sealed-adapter-api` dependency
 *   (same version as the plugin) to the `implementation` configuration.
 */
class SealedAdapterGradlePlugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        private const val API_GROUP = "org.xblackcat.gson"
        private const val API_ARTIFACT = "sealed-adapter-api"

        /**
         * Reads the plugin version from `version.properties` bundled in the plugin jar.
         * Falls back to a default if the resource is missing (e.g. during local development).
         */
        private fun pluginVersion(): String {
            val stream: InputStream? =
                SealedAdapterGradlePlugin::class.java.classLoader.getResourceAsStream("version.properties")
            return stream?.use { it.reader().readText() }
                ?.let { text ->
                    text.lineSequence()
                        .firstOrNull { it.startsWith("version=") }
                        ?.substringAfter("version=")
                        ?.trim()
                }
                ?: "0.1"
        }
    }

    override fun apply(target: Project) {
        val version = pluginVersion()
        val apiCoordinate = "$API_GROUP:$API_ARTIFACT:$version"

        // Add the API dependency if it's not already declared.
        target.dependencies.add("implementation", apiCoordinate)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.objects.listProperty(SubpluginOption::class.java).convention(emptyList())

    override fun getCompilerPluginId(): String = "org.xblackcat.gson.sealed-adapter"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = API_GROUP,
            artifactId = "sealed-adapter-compiler",
            version = pluginVersion(),
        )
}
