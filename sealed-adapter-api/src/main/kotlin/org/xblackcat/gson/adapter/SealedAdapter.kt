package org.xblackcat.gson.adapter

/**
 * Маркерная аннотация: sealed-класс требует сгенерированной Gson-фабрики.
 *
 * Компиляторный плагин `sealed-adapter-compiler` (FIR + IR) находит все классы с
 * этой аннотацией и генерирует [ASealedTypeAdapterFactory] для каждого:
 * `<T>SealedTypeAdapterFactory` в пакете `org.xblackcat.gson.adapter.generated`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class SealedAdapter
