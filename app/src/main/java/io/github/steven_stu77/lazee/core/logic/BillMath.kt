package io.github.steven_stu77.lazee.core.logic

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Date rules for monthly bills. yearMonth ints are yyyyMM (202611). */
object BillMath {
    fun ym(y: YearMonth): Int = y.year * 100 + y.monthValue
    fun ym(i: Int): YearMonth = YearMonth.of(i / 100, i % 100)

    /** Day 31 in a 30-day month or February means the last day of that month. */
    fun dueDate(dayOfMonth: Int, month: YearMonth): LocalDate = month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

    /** 1-based instalment number for [month], or null before the start month. */
    fun installmentNumber(startMonth: Int, month: YearMonth): Int? {
        val n = ChronoUnit.MONTHS.between(ym(startMonth), month).toInt() + 1
        return if (n < 1) null else n
    }

    /** True if the bill is due at all in [month] (started, and instalments not finished). */
    fun isDueIn(startMonth: Int, totalInstallments: Int?, month: YearMonth): Boolean {
        val n = installmentNumber(startMonth, month) ?: return false
        return totalInstallments == null || n <= totalInstallments
    }

    /** Reminder days: [remindDaysBefore] days before the due date, and the due date itself. */
    fun shouldRemind(today: LocalDate, dayOfMonth: Int, remindDaysBefore: Int): Boolean {
        val due = dueDate(dayOfMonth, YearMonth.from(today))
        return today == due || today == due.minusDays(remindDaysBefore.toLong())
    }

    fun daysUntil(today: LocalDate, due: LocalDate): Long = ChronoUnit.DAYS.between(today, due)
}
