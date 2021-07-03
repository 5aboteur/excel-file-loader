package com.github.saboteur.excelfileloader.exception.handler

interface DataRowExceptionHandler {

    fun doHandle(exception: Throwable, columnName: String, colIndex: Int = -1, rowIndex: Int = -1)
}
