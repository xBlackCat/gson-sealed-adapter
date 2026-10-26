package org.xblackcat.gson.adapter.ksp.test

/**
 *
 */
sealed interface BaseBlock : Base {
    data object One : BaseBlock
    data class Two(val a: Int, val b: String) : BaseBlock
}