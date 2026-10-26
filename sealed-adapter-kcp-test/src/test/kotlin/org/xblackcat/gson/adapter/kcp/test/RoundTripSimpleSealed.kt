package org.xblackcat.gson.adapter.kcp.test

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.xblackcat.gson.adapter.registerSealedAdapter
import org.xblackcat.gson.adapter.sealedClass

class RoundTripSimpleSealed {

    private val gson: Gson = GsonBuilder()
        .registerSealedAdapter<SimpleSealed>()
        .create()

    @Test
    fun `roundtrip all subtypes`() {
        val samples: List<SimpleSealed> = listOf(
            SimpleSealed.One("hello"),
            SimpleSealed.Two,
            SimpleSealed.Three(42, "world"),
        )

        samples.forEach { sample ->
            val json = gson.toJson(sample, SimpleSealed::class.java)
            val deserialized = gson.fromJson<SimpleSealed>(json, SimpleSealed::class.java)
            assertEquals(sample, deserialized)
        }
    }
}
