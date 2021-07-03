package com.github.saboteur.excelfileloader.exception

class IncorrectFileFormatDeclarationException(message: String) :
    ExcelFileLoaderException("Format declaration error: $message")
