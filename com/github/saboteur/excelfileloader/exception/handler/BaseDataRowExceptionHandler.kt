package com.github.saboteur.excelfileloader.exception.handler

import mu.KotlinLogging

class BaseDataRowExceptionHandler : DataRowExceptionHandler {

    override fun doHandle(exception: Throwable, columnName: String, colIndex: Int, rowIndex: Int) {
        logger.error(exception) {
            "Error upload: ${exception.message}, column [$columnName], line: $rowIndex, index: $colIndex"
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
