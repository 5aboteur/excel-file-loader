package com.github.saboteur.excelfileloader.exception

class InconsistencyFileFormatException(message: String) :
    ExcelFileLoaderException("Format inconsistency error: $message")
