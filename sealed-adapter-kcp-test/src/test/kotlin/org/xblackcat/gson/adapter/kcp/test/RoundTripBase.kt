package org.xblackcat.gson.adapter.kcp.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.xblackcat.gson.adapter.sealedClass

class RoundTripBase {

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapterFactory(sealedClass<Base>())
        .create()

    @Test
    fun `roundtrip all subtypes`() {
        val samples = listOf(
            BaseBlock.One,
            BaseBlock.Two(1, "a"),
            BaseCode.One,
            BaseCode.Two(2, "b"),
        )

        samples.forEach { sample ->
            val json = gson.toJson(sample, Base::class.java)
            val deserialized = gson.fromJson<Base>(json, Base::class.java)
            assertEquals(sample, deserialized)
        }
    }
}
