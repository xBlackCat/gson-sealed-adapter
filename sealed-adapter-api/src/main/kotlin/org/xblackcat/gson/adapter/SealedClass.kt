package org.xblackcat.gson.adapter

import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapterFactory

/**
 * Возвращает [TypeAdapterFactory] для sealed-класса, сгенерированную
 * компиляторным плагином `sealed-adapter-compiler`.
 *
 * Фабрика ищется по конвенции: `<package>.<SimpleName>SealedTypeAdapterFactory`.
 *
 * @param clazz sealed-класс, для которого нужна фабрика.
 * @throws IllegalStateException если фабрика не найдена.
 */
fun sealedClass(clazz: Class<*>): TypeAdapterFactory {
    val packageName = clazz.`package`?.name ?: ""
    val simpleName = clazz.simpleName
    val factoryClassName = if (packageName.isEmpty())
        "${simpleName}SealedTypeAdapterFactory"
    else
        "$packageName.${simpleName}SealedTypeAdapterFactory"

    val factoryClass = try {
        Class.forName(factoryClassName)
    } catch (e: ClassNotFoundException) {
        throw IllegalStateException(
            "Sealed adapter factory '$factoryClassName' not found. " +
                    "Ensure that ${clazz.simpleName} is annotated with @SealedAdapter " +
                    "and the sealed-adapter-compiler plugin is applied.",
            e,
        )
    }

    @Suppress("UNCHECKED_CAST")
    return factoryClass.getDeclaredConstructor().newInstance() as TypeAdapterFactory
}

/**
 * Возвращает [TypeAdapterFactory] для sealed-класса [T], сгенерированную
 * компиляторным плагином `sealed-adapter-compiler`.
 *
 * Сгенерированный класс не нужно импортировать — достаточно указать сам sealed-класс:
 *
 * ```kotlin
 * val gson = GsonBuilder()
 *     .registerTypeAdapterFactory(sealedClass<Text>())
 *     .registerTypeAdapterFactory(sealedClass<GalleryItem>())
 *     .create()
 * ```
 */
inline fun <reified T : Any> sealedClass(): TypeAdapterFactory = sealedClass(T::class.java)

inline fun <reified T : Any> GsonBuilder.registerSealedAdapter(): GsonBuilder = registerTypeAdapterFactory(sealedClass<T>())