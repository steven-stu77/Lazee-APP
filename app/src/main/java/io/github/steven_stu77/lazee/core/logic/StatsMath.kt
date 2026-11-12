package io.github.steven_stu77.lazee.core.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** One expense as the stats page needs it. Refunds are negative amounts. */
data class Spend(val date: LocalDate, val amount: Long, val fixed: Boolean = false, val accountId: Long = 0)

/** Pure maths for the stats page (spec §5.4). Money in cents. */
object StatsMath {
    /** (income − expense) / income, never below 0; 0 when there is no income. */
    fun savingsRate(income: Long, expense: Long): Float =
        if (income <= 0) 0f else ((income - expense).toFloat() / income).coerceIn(0f, 1f)

    /** Cumulative spend per day: index 0 = day 1. Stops at [upToDay] (inclusive). */
    fun cumulative(spends: List<Spend>, month: YearMonth, upToDay: Int = month.lengthOfMonth()): List<Long> {
        val byDay = LongArray(month.lengthOfMonth() + 1)
        spends.filter { YearMonth.from(it.date) == month }.forEach { byDay[it.date.dayOfMonth] += it.amount }
        var run = 0L
        return (1..upToDay.coerceAtMost(month.lengthOfMonth())).map { d -> run += byDay[d]; run }
    }

    /** Month-end estimate: fixed spending so far + daily rate of the rest × days in month. */
    fun forecast(spends: List<Spend>, month: YearMonth, today: LocalDate): Long {
        val inMonth = spends.filter { YearMonth.from(it.date) == month && !it.date.isAfter(today) }
        val fixed = inMonth.filter { it.fixed }.sumOf { it.amount }
        val rest = inMonth.filter { !it.fixed }.sumOf { it.amount }
        val passed = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        return fixed + Math.round(rest.toDouble() / passed * month.lengthOfMonth())
    }

    /** Heat level 0..4 for a day's spend: none / <30 / <60 / <120 / ≥120 ringgit. */
    fun heatLevel(cents: Long): Int = when {
        cents <= 0 -> 0
        cents < 3_000 -> 1
        cents < 6_000 -> 2
        cents < 12_000 -> 3
        else -> 4
    }

    fun dailyTotals(spends: List<Spend>, month: YearMonth): Map<Int, Long> =
        spends.filter { YearMonth.from(it.date) == month }.groupBy { it.date.dayOfMonth }.mapValues { (_, l) -> l.sumOf { it.amount } }

    /** Days so far this month (up to today) with no spending. */
    fun noSpendDays(spends: List<Spend>, month: YearMonth, today: LocalDate): Int {
        val last = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        val totals = dailyTotals(spends, month)
        return (1..last).count { (totals[it] ?: 0) <= 0 }
    }

    data class WeekSplit(val weekdayAvg: Long, val weekendAvg: Long)

    /** Average spend per weekday vs per weekend day, over the days that have passed. */
    fun weekdayWeekend(spends: List<Spend>, month: YearMonth, today: LocalDate): WeekSplit {
        val last = if (YearMonth.from(today) == month) today.dayOfMonth else month.lengthOfMonth()
        val days = (1..last).map { month.atDay(it) }
        fun weekend(d: LocalDate) = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
        val totals = dailyTotals(spends, month)
        val wd = days.filter { !weekend(it) }
        val we = days.filter { weekend(it) }
        fun avg(list: List<LocalDate>) = if (list.isEmpty()) 0L else Math.round(list.sumOf { totals[it.dayOfMonth] ?: 0L }.toDouble() / list.size)
        return WeekSplit(avg(wd), avg(we))
    }

    data class Small(val count: Int, val total: Long, val budgetDays: Float)

    /** Purchases under RM 10 and how many days of budget they add up to. */
    fun smallSpends(spends: List<Spend>, monthlyBudget: Long, month: YearMonth): Small {
        val small = spends.filter { YearMonth.from(it.date) == month && it.amount in 1 until 1_000 }
        val total = small.sumOf { it.amount }
        val perDay = if (monthlyBudget > 0) monthlyBudget.toFloat() / month.lengthOfMonth() else 0f
        return Small(small.size, total, if (perDay > 0) total / perDay else 0f)
    }

    /** Shares that add up to 1 (for "paid with" and category bars). */
    fun <K> shares(totals: Map<K, Long>): List<Pair<K, Float>> {
        val sum = totals.values.filter { it > 0 }.sum()
        if (sum <= 0) return emptyList()
        return totals.filter { it.value > 0 }.entries.sortedByDescending { it.value }.map { it.key to it.value.toFloat() / sum }
    }
}
