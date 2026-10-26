package org.xblackcat.gson.adapter

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Базовый класс Gson-фабрики для sealed-иерархии.
 *
 * Сериализует каждый подкласс в контейнер `{"type": <имя>, "data": {...}}` и
 * десериализует обратно, используя мапу [nameToSubclass].
 *
 * Конкретные фабрики (`<T>SealedTypeAdapterFactory`) генерируются компиляторным
 * плагином `sealed-adapter-compiler` для каждого класса, помеченного [SealedAdapter].
 *
 * @param T базовый sealed-класс/интерфейс.
 * @param baseType KClass базового sealed-класса.
 * @param subclassPairs пары «имя → KClass подкласса».
 */
abstract class ASealedTypeAdapterFactory<T : Any>(
    protected val baseType: KClass<T>,
    protected vararg val subclassPairs: Pair<String, KClass<*>>,
) : TypeAdapterFactory {
    init {
        if (!baseType.isSealed) throw IllegalArgumentException("$baseType is not a sealed class")
    }

    val nameToSubclass: Map<String, KClass<*>> = subclassPairs.toMap()

    protected val subclasses: Collection<KClass<*>>
        get() = nameToSubclass.values

    @Suppress("UNCHECKED_CAST")
    protected fun <R : Any> nameToSubclass(name: String): KClass<R> =
        nameToSubclass[name] as? KClass<R>
            ?: throw IllegalArgumentException("Unknown subclass name: $name")

    protected fun <R : Any> subclassToName(srcType: KClass<R>): String =
        nameToSubclass.entries.firstOrNull { it.value == srcType }?.key
            ?: throw IllegalArgumentException("Unknown subclass: $srcType")

    private fun <R : Any> supported(type: TypeToken<R>) =
        nameToSubclass.isNotEmpty() && subclasses.any { type.rawType.isAssignableFrom(it.java) }

    @Suppress("UNCHECKED_CAST")
    protected fun <R : Any> getDelegate(gson: Gson, type: KClass<out R>): TypeAdapter<R> =
        subclassesToDelegate.computeIfAbsent(type) {
            gson.getDelegateAdapter(this, TypeToken.get(type.java))
        } as TypeAdapter<R>

    private val subclassesToDelegate = ConcurrentHashMap<KClass<*>, TypeAdapter<*>>()

    override fun <R : Any> create(gson: Gson, type: TypeToken<R>?): TypeAdapter<R>? {
        if (type == null || !supported(type)) return null

        val elementTypeAdapter = gson.getAdapter(JsonElement::class.java)
        return object : TypeAdapter<R>() {
            override fun write(writer: JsonWriter, value: R?) {
                if (value == null) {
                    writer.nullValue()
                    return
                }

                val srcType = value::class
                try {
                    val delegate = getDelegate(gson, srcType)
                    val jsonObject = delegate.toJsonTree(value).asJsonObject

                    val container = JsonObject()
                    val typeName = subclassToName(srcType)
                    container.add("type", JsonPrimitive(typeName))
                    container.add("data", jsonObject)
                    elementTypeAdapter.write(writer, container)
                } catch (e: Exception) {
                    throw JsonIOException("Failed to save object of type $srcType", e)
                }
            }

            override fun read(reader: JsonReader): R? {
                val container = elementTypeAdapter.read(reader)
                if (container is JsonNull) return null
                val name = ((container as? JsonObject)?.get("type") as? JsonPrimitive)?.asString
                if (name == null) {
                    throw JsonParseException("cannot deserialize $baseType because it has not proper structure")
                }
                val subclass = nameToSubclass<R>(name)
                @Suppress("UNCHECKED_CAST")
                return subclass.objectInstance ?: (getDelegate(gson, subclass).fromJsonTree(container.get("data")) as R)
            }
        }
    }

    override fun toString() = "Adapter factory for $baseType"
}
