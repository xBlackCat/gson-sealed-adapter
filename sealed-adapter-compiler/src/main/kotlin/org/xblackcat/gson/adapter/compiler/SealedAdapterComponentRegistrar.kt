package org.xblackcat.gson.adapter.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.compiler.plugin.registerExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.xblackcat.gson.adapter.compiler.fir.SealedAdapterFirExtensionRegistrar
import org.xblackcat.gson.adapter.compiler.ir.SealedAdapterIrGenerationExtension

/**
 * Entry point of the `sealed-adapter-compiler` Kotlin compiler plugin.
 *
 * Registers the FIR and IR extensions that turn every `@SealedAdapter`-annotated
 * sealed class into a generated `ASealedTypeAdapterFactory<T>` subclass.
 */
@ExperimentalCompilerApi
class SealedAdapterComponentRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "org.xblackcat.gson.sealed-adapter"
    override val supportsK2: Boolean = true

    @ExperimentalCompilerApi
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(SealedAdapterFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(SealedAdapterIrGenerationExtension())
    }
}

/**
 * Compiler-option keys understood by the plugin.
 */
object SealedAdapterCompilerOptions {
    val ENABLED: CompilerConfigurationKey<Boolean> =
        CompilerConfigurationKey.create("org.xblackcat.gson.sealed-adapter.enabled")
}
