package com.github.saboteur.excelfileloader

import com.github.saboteur.excelfileloader.ExcelFileLoader.Companion.defaultBufferSize
import com.github.saboteur.excelfileloader.ExcelFileLoader.Companion.defaultRowCacheSize
import com.github.saboteur.excelfileloader.exception.DataParsingException
import com.github.saboteur.excelfileloader.exception.InconsistencyFileFormatException
import com.github.saboteur.excelfileloader.exception.handler.BaseDataRowExceptionHandler
import com.github.saboteur.excelfileloader.exception.handler.DataRowExceptionHandler
import com.monitorjbl.xlsx.StreamingReader
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import java.io.InputStream
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Stream
import kotlin.streams.asStream

/**
 * Excel (.xlsx only) file data loader.
 *
 * @property excelListFormat table data format
 * @property fileInputStream file input data stream
 * @property dataRowExceptionHandler row parsing exception handler
 * @property columnMapping excel file columns mapping method (default - BY_NAME)
 */
class ExcelFileLoader(
    private val excelListFormat: ExcelListFormat,
    private val fileInputStream: InputStream,
    private val dataRowExceptionHandler: DataRowExceptionHandler = BaseDataRowExceptionHandler(),
    private val columnMapping: ColumnMapping = ColumnMapping.BY_NAME
) : AutoCloseable {

    // .xlsx data operation object
    private val workbook: Workbook = StreamingReader
        .builder()
        .rowCacheSize(defaultRowCacheSize)
        .bufferSize(defaultBufferSize)
        .open(fileInputStream)

    private val headerByName: Map<String, Int>
    private val headerByNum: Map<Int, String>
    private val dateFormatters = HashMap<String, DateTimeFormatter>()

    init {
        val sheet = workbook.getSheetAt(defaultSheetNum)

        val headerRow =
            if (sheet.rowIterator().hasNext()) {
                sheet.rowIterator().next()
            } else {
                throw InconsistencyFileFormatException("Error read file header")
            }

        this.headerByName =
            if (columnMapping == ColumnMapping.BY_NUMBER) {
                excelListFormat
                    .getColumns()
                    .associate { it.name to it.num }
            } else {
                headerRow
                    .asSequence()
                    .filterNotNull()
                    .filter { cell -> !cell.isBlank() }
                    .filter { cell -> excelListFormat.getColumn(cell.stringCellValue) != null }
                    .associate { cell -> cell.stringCellValue to cell.columnIndex }
            }

        this.headerByNum = this.headerByName
            .entries
            .associate { it.value to it.key }

        validateHeader()
    }

    private fun Cell.isBlank(): Boolean = (this.cellType == CellType.BLANK)

    /**
     * Validate header with previously declared format.
     */
    private fun validateHeader() {
        if (headerByName.isEmpty()) {
            throw InconsistencyFileFormatException("File header not found")
        }

        excelListFormat.getColumns()
            .forEach { column ->
                if (column.name !in headerByName) {
                    throw InconsistencyFileFormatException("Column ${column.name} not found")
                }
            }
    }

    /**
     * For Java compatibility
     */
    @Suppress("unused")
    fun getContentStream(): Stream<Map<String, Any?>> =
        getContent().asStream()

    private fun getContent(): Sequence<Map<String, Any?>> =
        workbook
            .getSheetAt(defaultSheetNum)
            .asSequence()
            .map(::toMap)

    private fun toMap(row: Row): Map<String, Any?> =
        try {
            headerByName.entries
                .asSequence()
                .map { (name, idx) ->
                    try {
                        val cell = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
                        name to getCellValue(cell)
                    } catch (e: Throwable) {
                        throw DataParsingException(
                            message = e.message ?: "Error read cell value",
                            columnName = name,
                            colIndex = idx,
                            rowIndex = row.rowNum,
                            causeException = e
                        )
                    }
                }
                .toMap()
        } catch (e: DataParsingException) {
            dataRowExceptionHandler.doHandle(e, e.columnName, e.colIndex, e.rowIndex)
            emptyMap()
        } catch (e: Throwable) {
            dataRowExceptionHandler.doHandle(e, "unknown", -1, row.rowNum)
            emptyMap()
        }

    private fun getCellValue(cell: Cell?): Any? {

        val columnName = headerByNum[cell?.columnIndex]
            ?: throw DataParsingException("Column declaration not found by index ${cell?.columnIndex}")

        val columnInfo = excelListFormat.getColumn(columnName)
            ?: throw DataParsingException("Column declaration not found by index $columnName", columnName)

        if (cell == null || cell.isBlank() || cell.stringCellValue?.trim().isNullOrBlank()) {
            return columnInfo.default
                .also { value ->
                    if (columnInfo.isRequired && value == null) {
                        throw DataParsingException("Value for required field not presented", columnName)
                    }
                }
        }

        return runCatching {
            return@runCatching when (columnInfo.type) {
                Integer::class, Int::class -> {
                    getIntCell(cell)
                }
                Double::class -> {
                    getDoubleCell(cell)
                }
                Boolean::class -> {
                    getBooleanCell(cell)
                }
                LocalDate::class -> {
                    getDateCell(cell, columnInfo.pattern)
                }
                else -> {
                    cell.stringCellValue?.trim()
                }
            }
        }.onFailure { e ->
            throw DataParsingException(
                message = "Error cell parsing: ${e.message}",
                columnName = columnName,
                colIndex = cell.columnIndex,
                rowIndex = cell.rowIndex,
                causeException = e
            )
        }.getOrNull()
            .let { result -> result ?: columnInfo.default }
            .let { result ->
                if (columnInfo.isRequired && result == null)
                    throw DataParsingException(
                        message = "Value for required field not presented",
                        columnName = columnName,
                        colIndex = cell.columnIndex,
                        rowIndex = cell.rowIndex
                    )
            }
    }

    override fun close() {
        fileInputStream.close()
        workbook.close()
    }

    private fun getIntCell(cell: Cell): Any =
        Integer.parseInt(cell.stringCellValue?.trim())

    private fun getDoubleCell(cell: Cell) =
        cell.numericCellValue

    private fun getBooleanCell(cell: Cell) =
        when (cell.stringCellValue?.trim()?.toLowerCase()) {
            "yes", "1", "true" -> true
            "no", "0", "false" -> false
            "" -> null
            else -> {
                throw DataParsingException("${cell.stringCellValue.trim()} is not Boolean type")
            }
        }

    private fun getDateCell(cell: Cell, pattern: String?) =
        runCatching {
            cell.dateCellValue
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
        }.getOrNull()
            ?: if (pattern.isNullOrBlank()) {
                LocalDate.parse(cell.stringCellValue?.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            } else {
                val formatter = dateFormatters.getOrPut(pattern) {
                    DateTimeFormatter.ofPattern(pattern)
                }
                LocalDate.parse(cell.stringCellValue?.trim(), formatter)
            }

    /**
     * @property defaultSheetNum sheet number
     * @property defaultRowCacheSize number of in-memory cached rows (default - 100)
     * @property defaultBufferSize data stream processor buffer size (default - 4096)
     */
    companion object {
        enum class ColumnMapping {
            BY_NAME, BY_NUMBER
        }

        private const val defaultSheetNum = 0
        private const val defaultRowCacheSize = 100
        private const val defaultBufferSize = 4096
    }
}
