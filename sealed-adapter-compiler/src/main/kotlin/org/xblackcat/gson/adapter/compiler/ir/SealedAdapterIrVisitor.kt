package org.xblackcat.gson.adapter.compiler.ir

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName

private val SEALED_ADAPTER_ANNOTATION = FqName("org.xblackcat.gson.adapter.SealedAdapter")

internal data class GeneratedFactory(val factoryClass: IrClass, val irFile: IrFile)

/**
 * Walks the module fragment and generates a `<T>SealedTypeAdapterFactory` class
 * for every `@SealedAdapter`-annotated sealed class.
 *
 * Follows the MoshiX `MoshiIrVisitor` pattern:
 * - extends [IrElementTransformerVoidWithContext]
 * - overrides [visitClassNew] to detect the annotation
 * - delegates to [SealedAdapterFactoryGenerator] for IR generation
 * - collects results in [deferredFactories] for later attachment to files
 */
internal class SealedAdapterIrVisitor(
    private val pluginContext: IrPluginContext,
    private val deferredFactories: MutableList<GeneratedFactory>,
) : IrElementTransformerVoidWithContext() {

    override fun visitClassNew(declaration: IrClass): IrStatement {
        declaration.getAnnotation(SEALED_ADAPTER_ANNOTATION)?.let {
            val generator = SealedAdapterFactoryGenerator(pluginContext, declaration)
            val factoryClass = generator.generate()

            deferredFactories += GeneratedFactory(
                factoryClass = factoryClass,
                irFile = declaration.file,
            )
        }
        return super.visitClassNew(declaration)
    }
}
