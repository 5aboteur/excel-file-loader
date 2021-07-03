package com.github.saboteur.excelfileloader

import com.github.saboteur.excelfileloader.exception.IncorrectFileFormatDeclarationException
import kotlin.reflect.KClass

class ExcelListFormat(block: ExcelListFormat.() -> Unit) {

    // Columns representation [header <=> parameters]
    private val columns = mutableMapOf<String, Column<out Any>>()

    @Suppress("unused")
    constructor() : this({})

    init {
        this.block()
    }

    fun <T : Any> put(
        name: String,
        type: KClass<T>,
        isKey: Boolean = false,
        isRequired: Boolean = false,
        default: T? = null,
        pattern: String? = null
    ) {
        if (name.isBlank()) {
            throw IncorrectFileFormatDeclarationException("Column name must be declared")
        }

        if (name in columns) {
            throw IncorrectFileFormatDeclarationException("Column with name $name already declared")
        }

        if (isKey && columns.values.any { it.isKey }) {
            throw IncorrectFileFormatDeclarationException("Key column already declared")
        }

        columns[name] = Column(
            num = columns.size,
            name = name,
            type = type,
            isKey = isKey,
            isRequired = isRequired,
            default = default,
            pattern = pattern

        )
    }

    @Suppress("unused")
    inline fun <reified T : Any> put(
        name: String,
        isKey: Boolean = false,
        isRequired: Boolean = false,
        default: T? = null,
        pattern: String? = null
    ): Unit =
        put(
            name = name,
            type = T::class,
            isKey = isKey,
            isRequired = isRequired,
            default = default,
            pattern = pattern
        )

    /**
     * For Java compatibility
     */
    @Suppress("unused")
    fun <T : Any> put(
        name: String,
        type: Class<T>,
        isKey: Boolean = false,
        isRequired: Boolean = false,
        default: T? = null,
        pattern: String? = null
    ): Unit =
        put(
            name = name,
            type = type.kotlin,
            isKey = isKey,
            isRequired = isRequired,
            default = default,
            pattern = pattern
        )

    @Suppress("unused")
    fun getKey(): Column<out Any>? =
        columns
            .values
            .firstOrNull { it.isKey }

    fun getColumn(name: String): Column<out Any>? =
        columns[name]

    fun getColumns(): Set<Column<out Any>> =
        columns
            .values
            .toHashSet()

}
