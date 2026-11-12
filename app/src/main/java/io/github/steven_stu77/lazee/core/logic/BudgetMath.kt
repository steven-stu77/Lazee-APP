package io.github.steven_stu77.lazee.core.logic

import java.time.LocalDate
import java.time.YearMonth

/** Budget maths shared by the budget page, stats and alerts. All values in cents. */
object BudgetMath {
    /** Alert levels reached: 80 and/or 100. Nothing when there is no limit. */
    fun levelsReached(spent: Long, limit: Long): List<Int> = when {
        limit <= 0 -> emptyList()
        spent >= limit -> listOf(80, 100)
        spent * 100 >= limit * 80 -> listOf(80)
        else -> emptyList()
    }

    /** Days left in the month including today. */
    fun daysLeft(today: LocalDate): Int = YearMonth.from(today).lengthOfMonth() - today.dayOfMonth + 1

    /** Remaining budget ÷ days left (incl. today); 0 when nothing is left. */
    fun perDay(limit: Long, spent: Long, today: LocalDate): Long {
        val left = limit - spent
        if (left <= 0) return 0
        return Math.round(left.toDouble() / daysLeft(today))
    }

    /** Fraction used, for progress bars. */
    fun ratio(spent: Long, limit: Long): Float = if (limit <= 0) 0f else (spent.toFloat() / limit)
}
