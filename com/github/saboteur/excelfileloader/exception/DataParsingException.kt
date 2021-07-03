package com.github.saboteur.excelfileloader.exception

class DataParsingException(
    message: String,
    val columnName: String = "unknown",
    val colIndex: Int = -1,
    val rowIndex: Int = -1,
    causeException: Throwable? = null
) : ExcelFileLoaderException(message, causeException)
