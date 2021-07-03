package com.github.saboteur.excelfileloader.exception

/**
 * For Java compatibility
 */
abstract class ExcelFileLoaderException : RuntimeException {

    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)
}
