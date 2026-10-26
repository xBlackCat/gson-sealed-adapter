package org.xblackcat.gson.adapter.ksp.test

/**
 *
 */
sealed interface BaseCode : Base {
    data object One : BaseCode
    data class Two(val a: Int, val b: String) : BaseCode
}