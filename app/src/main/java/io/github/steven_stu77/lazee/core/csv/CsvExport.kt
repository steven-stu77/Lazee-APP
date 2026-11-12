package io.github.steven_stu77.lazee.core.csv

import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.ByteArrayOutputStream

/** One exported line; column order matches spec §5.13. Amount is a plain number like 1234.50. */
data class CsvRow(
    val date: String,
    val type: String,
    val category: String,
    val subCategory: String,
    val amount: String,
    val account: String,
    val merchant: String,
    val note: String,
    val tags: String,
    val source: String,
) {
    fun cells() = listOf(date, type, category, subCategory, amount, account, merchant, note, tags, source)
}

object CsvExport {
    private const val BOM = "﻿"

    /** UTF-8 with a BOM so Excel shows Chinese correctly; quotes and commas escaped by kotlin-csv. */
    fun build(headers: List<String>, rows: List<CsvRow>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(BOM.toByteArray(Charsets.UTF_8))
        csvWriter { lineTerminator = "\r\n" }.writeAll(listOf(headers) + rows.map { it.cells() }, out)
        return out.toByteArray()
    }
}
