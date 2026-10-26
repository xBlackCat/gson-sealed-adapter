package org.xblackcat.gson.adapter.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

/**
 * KSP processor that generates a `<T>SealedTypeAdapterFactory` class for every
 * `@SealedAdapter`-annotated sealed class.
 *
 * The generated class extends `ASealedTypeAdapterFactory<T>` and registers all
 * subclasses (recursively) in its constructor.
 *
 * Example generated code for:
 * ```kotlin
 * @SealedAdapter
 * sealed class Target {
 *     data class Sub1(val x: Int) : Target()
 *     data class Sub2(val y: String) : Target()
 * }
 * ```
 * produces:
 * ```kotlin
 * class TargetSealedTypeAdapterFactory : ASealedTypeAdapterFactory<Target>(
 *     Target::class,
 *     "Sub1" to Sub1::class,
 *     "Sub2" to Sub2::class,
 * )
 * ```
 */
class SealedAdapterKspProcessor(
    private val environment: SymbolProcessorEnvironment,
) : SymbolProcessor {

    private val logger: KSPLogger = environment.logger
    private val codeGenerator: CodeGenerator = environment.codeGenerator

    companion object {
        const val SEALED_ADAPTER_ANNOTATION = "org.xblackcat.gson.adapter.SealedAdapter"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotatedSymbols = resolver.getSymbolsWithAnnotation(SEALED_ADAPTER_ANNOTATION)

        annotatedSymbols.forEach { symbol ->
            val classDeclaration = symbol as? KSClassDeclaration
                ?: run {
                    logger.warn("@SealedAdapter can only be applied to classes, found: $symbol")
                    return@forEach
                }

            if (!classDeclaration.validate()) {
                logger.warn("Skipping unvalidated class: ${classDeclaration.qualifiedName?.asString()}")
                return@forEach
            }

            if (!classDeclaration.isSealed()) {
                logger.error(
                    "@SealedAdapter can only be applied to sealed classes or interfaces, " +
                            "but ${classDeclaration.qualifiedName?.asString()} is not sealed.",
                    classDeclaration,
                )
                return@forEach
            }

            val subclasses = collectAllSubclasses(classDeclaration)
            if (subclasses.isEmpty()) {
                logger.warn(
                    "Sealed class ${classDeclaration.qualifiedName?.asString()} has no subclasses. " +
                            "Generated factory will be empty.",
                    classDeclaration,
                )
            }

            generateFactory(classDeclaration, subclasses)
        }

        return emptyList()
    }

    /**
     * Recursively collects ALL subclasses of [root] (intermediate sealed classes
     * and concrete leaves), assigning each a unique **path-based name**.
     *
     * Direct children get their simple name (`"One"`), nested children get a
     * dot-separated path (`"BaseBlock.One"`). This avoids collisions when
     * different branches of the hierarchy contain classes with the same simple name.
     */
    private fun collectAllSubclasses(root: KSClassDeclaration): List<Pair<String, KSClassDeclaration>> {
        val result = mutableListOf<Pair<String, KSClassDeclaration>>()

        fun visit(cls: KSClassDeclaration, prefix: String) {
            cls.getSealedSubclasses().forEach { sub ->
                val name = if (prefix.isEmpty()) sub.simpleName.asString()
                           else "$prefix.${sub.simpleName.asString()}"
                result.add(name to sub)
                if (sub.isSealed()) {
                    visit(sub, name)
                }
            }
        }

        visit(root, "")
        return result
    }

    /**
     * Generates a `<Target>SealedTypeAdapterFactory` class in the same package as [target].
     */
    private fun generateFactory(
        target: KSClassDeclaration,
        subclasses: List<Pair<String, KSClassDeclaration>>,
    ) {
        val targetQualifiedName = target.qualifiedName?.asString()
            ?: error("Cannot generate factory for class without qualified name")

        val packageName = target.packageName.asString()
        val targetSimpleName = target.simpleName.asString()
        val factoryName = "${targetSimpleName}SealedTypeAdapterFactory"

        // Build the vararg pairs: "Sub1" to Sub1::class, "Nested.Sub2" to Nested.Sub2::class, ...
        val pairs = subclasses.joinToString(",\n    ") { (name, sub) ->
            val subQualifiedName = sub.qualifiedName?.asString()
                ?: error("Subclass without qualified name")
            "\"$name\" to $subQualifiedName::class"
        }

        codeGenerator.createNewFile(
            packageName = packageName,
            fileName = factoryName,
            extensionName = "kt",
            dependencies = Dependencies(aggregating = false),
        ).use { outputStream ->
            val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)
            writer.appendLine("// Generated by sealed-adapter-ksp. Do not edit.")
            writer.appendLine("package $packageName")
            writer.appendLine()
            writer.appendLine("import org.xblackcat.gson.adapter.ASealedTypeAdapterFactory")
            writer.appendLine()
            writer.appendLine("class $factoryName : ASealedTypeAdapterFactory<$targetQualifiedName>(")
            writer.appendLine("    $targetQualifiedName::class,")
            if (subclasses.isNotEmpty()) {
                writer.appendLine("    $pairs,")
            }
            writer.appendLine(")")
            writer.flush()
        }

        logger.info("Generated $packageName.$factoryName for $targetQualifiedName")
    }

    /**
     * Checks whether this class is sealed (sealed class or sealed interface).
     */
    private fun KSClassDeclaration.isSealed(): Boolean {
        return modifiers.contains(Modifier.SEALED)
    }
}
