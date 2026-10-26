package org.xblackcat.gson.adapter.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * IR generation extension: generates `<T>SealedTypeAdapterFactory` classes
 * for every `@SealedAdapter`-annotated sealed class.
 *
 * Follows the MoshiX IR pattern:
 * 1. Walk the module fragment with [SealedAdapterIrVisitor]
 * 2. For each annotated class, generate the factory IrClass
 * 3. Add the generated class to the file's declarations
 */
internal class SealedAdapterIrGenerationExtension : IrGenerationExtension {

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val deferred = mutableListOf<GeneratedFactory>()
        val visitor = SealedAdapterIrVisitor(pluginContext, deferred)
        moduleFragment.transform(visitor, null)

        // Add deferred classes to their files
        for ((file, factories) in deferred.groupBy { it.irFile }) {
            for ((factoryClass) in factories) {
                file.declarations.add(factoryClass)
                factoryClass.parent = file
            }
        }
    }
}
