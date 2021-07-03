package com.github.saboteur.excelfileloader

import kotlin.reflect.KClass

/**
 * Excel column representation class
 *
 * @property num column number
 * @property name column name
 * @property type type of data
 * @property isKey is it key field
 * @property isRequired is column requires value
 * @property default default column value
 * @property pattern data format
 */
data class Column<T : Any>(
    val num: Int,
    val name: String,
    val type: KClass<T>,
    val isKey: Boolean = false,
    val isRequired: Boolean = false,
    val default: T? = null,
    val pattern: String? = null
)
