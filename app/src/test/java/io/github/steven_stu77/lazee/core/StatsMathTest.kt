package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.logic.Spend
import io.github.steven_stu77.lazee.core.logic.StatsMath
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsMathTest {
    private val nov = YearMonth.of(2026, 11)
    private fun d(day: Int) = nov.atDay(day)

    @Test fun savingsRate() {
        assertThat(StatsMath.savingsRate(170_000, 136_000)).isWithin(0.001f).of(0.2f)
        assertThat(StatsMath.savingsRate(100, 200)).isEqualTo(0f)
        assertThat(StatsMath.savingsRate(0, 200)).isEqualTo(0f)
    }

    @Test fun forecastSeparatesFixed() {
        // Day 10 of a 30-day month: fixed 250 + (100 / 10 × 30) = 550
        val s = listOf(Spend(d(1), 25_000, fixed = true), Spend(d(3), 6_000), Spend(d(9), 4_000))
        assertThat(StatsMath.forecast(s, nov, d(10))).isEqualTo(55_000)
    }

    @Test fun cumulativeAndRefunds() {
        val s = listOf(Spend(d(1), 1_000), Spend(d(2), 500), Spend(d(2), -200))
        assertThat(StatsMath.cumulative(s, nov, 3)).containsExactly(1_000L, 1_300L, 1_300L).inOrder()
    }

    @Test fun heatLevels() {
        assertThat(listOf(0L, 2_999, 3_000, 5_999, 11_999, 12_000).map(StatsMath::heatLevel)).containsExactly(0, 1, 2, 2, 3, 4).inOrder()
    }

    @Test fun noSpendDaysCountsOnlyPassedDays() {
        val s = listOf(Spend(d(1), 100), Spend(d(3), 100))
        assertThat(StatsMath.noSpendDays(s, nov, d(4))).isEqualTo(2)
    }

    @Test fun weekdayVsWeekend() {
        // 1 Nov 2026 is a Sunday. Days 1–7: weekend = 1 (Sun), 7 (Sat).
        val s = listOf(Spend(d(1), 9_000), Spend(d(7), 3_000), Spend(d(2), 5_000))
        val w = StatsMath.weekdayWeekend(s, nov, d(7))
        assertThat(w.weekendAvg).isEqualTo(6_000)
        assertThat(w.weekdayAvg).isEqualTo(1_000)
    }

    @Test fun smallSpends() {
        val s = listOf(Spend(d(1), 680), Spend(d(2), 999), Spend(d(3), 1_000), Spend(d(4), -500))
        val r = StatsMath.smallSpends(s, 150_000, nov) // RM 50/day
        assertThat(r.count).isEqualTo(2)
        assertThat(r.total).isEqualTo(1_679)
        assertThat(r.budgetDays).isWithin(0.01f).of(0.3358f)
    }

    @Test fun sharesSumToOne() {
        val r = StatsMath.shares(mapOf("TNG" to 300L, "Cash" to 100L, "None" to 0L))
        assertThat(r.map { it.first }).containsExactly("TNG", "Cash").inOrder()
        assertThat(r.sumOf { it.second.toDouble() }).isWithin(0.0001).of(1.0)
    }
}
