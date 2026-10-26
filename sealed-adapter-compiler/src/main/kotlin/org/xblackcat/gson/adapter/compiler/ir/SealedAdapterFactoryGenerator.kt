package org.xblackcat.gson.adapter.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

/**
 * Generates a `<Target>SealedTypeAdapterFactory` class for a given `@SealedAdapter`-annotated
 * sealed class.
 *
 * The generated class:
 * - extends `ASealedTypeAdapterFactory<Target>`
 * - has a no-arg constructor that calls `super(Target::class, "Sub1" to Sub1::class, ...)`
 * - is placed in the same file as the target class
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
internal class SealedAdapterFactoryGenerator(
    private val pluginContext: IrPluginContext,
    private val target: IrClass,
) {

    private val baseClassId = ClassId.topLevel(FqName("org.xblackcat.gson.adapter.ASealedTypeAdapterFactory"))

    fun generate(): IrClass {
        val baseClassSymbol = pluginContext.finderForBuiltins().findClass(baseClassId)
            ?: error("Base class not found: $baseClassId")
        val baseClass = baseClassSymbol.owner
        val targetType = target.defaultType

        val adapterName = "${target.name.asString()}SealedTypeAdapterFactory"
        val subclasses = collectAllSubclasses(target)

        val adapterClass = pluginContext.irFactory.buildClass {
            name = Name.identifier(adapterName)
            modality = Modality.FINAL
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.PUBLIC
            origin = IrDeclarationOrigin.DEFINED
        }.apply {
            createThisReceiverParameter()

            // Supertype: ASealedTypeAdapterFactory<TargetType>
            val supertype = baseClassSymbol.createType(
                hasQuestionMark = false,
                arguments = listOf(targetType),
            )
            superTypes = listOf(supertype)

            // No-arg constructor calling super(TargetType::class, "Sub1" to Sub1::class, ...)
            val ctor = addConstructor {
                name = SpecialNames.INIT
                modality = Modality.FINAL
                visibility = DescriptorVisibilities.PUBLIC
                isPrimary = true
            }

            val builder = pluginContext.irBuiltIns.createIrBuilder(ctor.symbol)
            val superCall = builder.buildSuperCall(baseClassSymbol, targetType, subclasses)
            ctor.body = builder.irExprBody(superCall)
        }

        return adapterClass
    }

    /**
     * Recursively collects ALL subclasses of [root] (intermediate sealed classes
     * and concrete leaves). The old hand-written factories registered every level
     * of the hierarchy (e.g. `TargetAction`, `TargetScreen`, `TargetScreen.Chat`),
     * so deserialization of intermediate types must keep working.
     */
    private fun collectAllSubclasses(root: IrClass): List<IrClass> {
        val result = mutableListOf<IrClass>()
        fun visit(cls: IrClass) {
            for (sub in cls.sealedSubclasses) {
                val subOwner = sub.owner
                result.add(subOwner)
                if (subOwner.sealedSubclasses.isNotEmpty()) {
                    visit(subOwner)
                }
            }
        }
        visit(root)
        return result
    }

    /**
     * Builds the `super(TargetType::class, "Sub1" to Sub1::class, ...)` delegating constructor call.
     */
    private fun IrBuilderWithScope.buildSuperCall(
        baseClassSymbol: IrClassSymbol,
        targetType: IrType,
        subclasses: List<IrClass>,
    ): IrExpression {
        val baseClass = baseClassSymbol.owner
        val superCtor = baseClass.constructors.single()

        val superCall = irDelegatingConstructorCall(superCtor)
        superCall.typeArguments[0] = targetType

        // Argument 0: TargetType::class (KClass<TargetType>)
        val kClassSymbol = pluginContext.irBuiltIns.kClassClass
        val kClassTargetType = kClassSymbol.createType(
            hasQuestionMark = false,
            arguments = listOf(targetType),
        )
        superCall.arguments[0] = IrClassReferenceImpl(
            startOffset, endOffset, kClassTargetType, target.symbol, targetType,
        )

        // Argument 1: vararg Pair<String, KClass<*>>
        val pairSymbol = pluginContext.finderForBuiltins().findClass(ClassId.topLevel(FqName("kotlin.Pair")))
            ?: error("kotlin.Pair not found")
        val pairType = pairSymbol.createType(
            hasQuestionMark = false,
            arguments = listOf(
                pluginContext.irBuiltIns.stringType,
                pluginContext.irBuiltIns.kClassClass.starProjectedType,
            ),
        )
        val pairCtor = pairSymbol.owner.constructors.single()

        val pairExpressions = subclasses.map { sub ->
            val pairExpr = irCall(pairCtor.symbol, pairType)
            pairExpr.arguments[0] = irString(sub.name.asString())
            val kClassSubType = kClassSymbol.createType(
                hasQuestionMark = false,
                arguments = listOf(sub.defaultType),
            )
            pairExpr.arguments[1] = IrClassReferenceImpl(
                startOffset, endOffset, kClassSubType, sub.symbol, sub.defaultType,
            )
            pairExpr
        }

        superCall.arguments[1] = irVararg(pairType, pairExpressions)

        return superCall
    }
}
