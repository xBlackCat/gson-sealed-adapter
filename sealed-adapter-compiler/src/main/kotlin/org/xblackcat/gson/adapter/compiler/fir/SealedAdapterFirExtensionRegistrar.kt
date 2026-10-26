package org.xblackcat.gson.adapter.compiler.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.xblackcat.gson.adapter.compiler.ir.compiler.SealedAdapterDiagnostic

private val SEALED_ADAPTER = ClassId.topLevel(FqName("org.xblackcat.gson.adapter.SealedAdapter"))

/**
 * FIR extension: validates that @SealedAdapter is applied correctly.
 *
 * Checks:
 * - The annotated class is sealed
 * - The class is a class or interface (not enum, annotation, etc.)
 * - The class is not inner
 * - The class is not local
 */
internal class SealedAdapterFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> SealedAdapterCheckers(session) }
    }
}

private class SealedAdapterCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers: Set<FirDeclarationChecker<FirRegularClass>>
            get() = setOf(SealedAdapterChecker())
    }
}

private class SealedAdapterChecker : FirDeclarationChecker<FirRegularClass>(MppCheckerKind.Common) {
    context(
        context: CheckerContext,
        reporter: DiagnosticReporter,
    )
    override fun check(declaration: FirRegularClass) {
        val annotation = declaration.getAnnotationByClassId(SEALED_ADAPTER, context.session) ?: return

        if (!declaration.isSealed) {
            declaration.report("@SealedAdapter can only be applied to sealed classes or interfaces")
            return
        }

        if (declaration.classKind != ClassKind.CLASS && declaration.classKind != ClassKind.INTERFACE) {
            declaration.report("@SealedAdapter can only be applied to sealed classes or interfaces, not ${declaration.classKind}")
            return
        }

        if (declaration.isInner) {
            declaration.report("@SealedAdapter cannot be applied to inner classes")
            return
        }

        if (declaration.isLocal) {
            declaration.report("@SealedAdapter cannot be applied to local classes")
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun FirRegularClass.report(message: String) {
        reporter.reportOn(source, SealedAdapterDiagnostic.SEALED_ADAPTER_ERROR, message)
    }
}
