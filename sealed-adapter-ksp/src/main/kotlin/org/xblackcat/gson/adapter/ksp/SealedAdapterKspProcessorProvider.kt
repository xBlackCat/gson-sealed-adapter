package org.xblackcat.gson.adapter.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Entry point for the KSP processor. Discovered via
 * `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`.
 */
class SealedAdapterKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SealedAdapterKspProcessor(environment)
    }
}
