package org.xblackcat.gson.adapter.ksp.test

import org.xblackcat.gson.adapter.SealedAdapter

@SealedAdapter
sealed interface SimpleSealed {
    data class One(val value: String) : SimpleSealed
    data object Two : SimpleSealed
    data class Three(val value1: Int, val value2: String) : SimpleSealed
}
