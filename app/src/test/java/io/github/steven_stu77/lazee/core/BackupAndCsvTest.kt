package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.backup.BackupCrypto
import io.github.steven_stu77.lazee.core.backup.BackupData
import io.github.steven_stu77.lazee.core.backup.BackupFormat
import io.github.steven_stu77.lazee.core.backup.NotABackupException
import io.github.steven_stu77.lazee.core.backup.WrongPasswordException
import io.github.steven_stu77.lazee.core.csv.CsvExport
import io.github.steven_stu77.lazee.core.csv.CsvRow
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnType
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BackupAndCsvTest {
    private val data = BackupData(
        createdAt = 1,
        accounts = listOf(Account(id = 1, name = "现金", type = AccountType.CASH, openingBalance = 32_000)),
        transactions = listOf(Transaction(id = 5, type = TxnType.EXPENSE, amount = 680, categoryId = 3, accountId = 1,
            date = LocalDate.of(2026, 11, 12), createdAt = Instant.ofEpochMilli(1_700_000_000_000), note = "早餐 #旅行", photoPath = "a.jpg")),
    )

    @Test fun encryptDecryptRoundTrip() {
        val zip = BackupFormat.zip(data, mapOf("a.jpg" to byteArrayOf(1, 2, 3)))
        val file = BackupCrypto.encrypt(zip, "correct horse".toCharArray())
        val (back, photos) = BackupFormat.unzip(BackupCrypto.decrypt(file, "correct horse".toCharArray()))
        assertThat(back).isEqualTo(data)
        assertThat(photos["a.jpg"]).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun wrongPasswordIsAClearError() {
        val file = BackupCrypto.encrypt(byteArrayOf(9, 9, 9), "right".toCharArray())
        assertThrows(WrongPasswordException::class.java) { BackupCrypto.decrypt(file, "wrong".toCharArray()) }
    }

    @Test fun randomFileIsNotABackup() {
        assertThrows(NotABackupException::class.java) { BackupCrypto.decrypt(ByteArray(100) { it.toByte() }, "x".toCharArray()) }
    }

    @Test fun tamperingIsDetected() {
        val file = BackupCrypto.encrypt(byteArrayOf(1, 2, 3, 4), "pw".toCharArray())
        file[file.size - 1] = (file[file.size - 1] + 1).toByte()
        assertThrows(WrongPasswordException::class.java) { BackupCrypto.decrypt(file, "pw".toCharArray()) }
    }

    @Test fun csvHasBomAndEscapes() {
        val bytes = CsvExport.build(
            listOf("日期", "类型", "分类", "子分类", "金额", "账户", "商家", "备注", "标签", "来源"),
            listOf(CsvRow("2026-11-12", "支出", "餐饮", "", "6.80", "现金", "7-Eleven", "他说 \"好吃\", 下次再来", "#旅行", "手动")),
        )
        assertThat(bytes.copyOfRange(0, 3)).isEqualTo(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val text = bytes.decodeToString().removePrefix("﻿")
        assertThat(text).startsWith("日期,类型,分类")
        assertThat(text).contains("\"他说 \"\"好吃\"\", 下次再来\"")
    }
}
