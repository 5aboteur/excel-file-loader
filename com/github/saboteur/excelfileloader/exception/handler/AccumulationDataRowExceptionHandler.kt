package com.github.saboteur.excelfileloader.exception.handler

import mu.KotlinLogging

@Suppress("unused")
class AccumulationDataRowExceptionHandler(
    private val isLogging: Boolean = true
) : DataRowExceptionHandler {

    private val errors = mutableListOf<Message>()

    data class Message(
        val columnName: String,
        val colIndex: Int,
        val rowIndex: Int,
        val exception: Throwable
    )

    override fun doHandle(exception: Throwable, columnName: String, colIndex: Int, rowIndex: Int) {
        errors.add(
            Message(
                columnName = columnName,
                colIndex = colIndex,
                rowIndex = rowIndex,
                exception = exception
            )
        )

        if (isLogging) {
            logger.error(exception) {
                "Error upload: ${exception.message}, column [$columnName], line: $rowIndex, index: $colIndex"
            }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
