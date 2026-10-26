package org.xblackcat.gson.adapter.compiler.ir.compiler

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtElement

/**
 *
 */
internal object SealedAdapterDiagnostic : KtDiagnosticsContainer() {
    val SEALED_ADAPTER_ERROR by error1<KtElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = SealedAdapterIrMessages
}

private object SealedAdapterIrMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by
    KtDiagnosticFactoryToRendererMap("SealedAdapterIr") { map ->
        map.put(SealedAdapterDiagnostic.SEALED_ADAPTER_ERROR, "{0}", TO_STRING)
    }
}