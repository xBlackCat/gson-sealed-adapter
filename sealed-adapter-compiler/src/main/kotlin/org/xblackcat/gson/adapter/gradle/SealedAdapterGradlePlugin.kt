package org.xblackcat.gson.adapter.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Gradle side of the `sealed-adapter-compiler` plugin.
 *
 * Applies the Kotlin compiler plugin (see [org.xblackcat.gson.adapter.compiler.SealedAdapterComponentRegistrar])
 * to every Kotlin compilation of the project it is applied to.
 */
class SealedAdapterGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        // No project-level configuration needed; the plugin is applied to every
        // Kotlin compilation via [applyToCompilation].
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> =
        kotlinCompilation.target.project.objects.listProperty(SubpluginOption::class.java).convention(emptyList())

    override fun getCompilerPluginId(): String = "org.xblackcat.gson.sealed-adapter"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = "org.xblackcat.gson",
            artifactId = "sealed-adapter-compiler",
            version = "0.1.0",
        )
}
