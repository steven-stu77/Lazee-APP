package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.logic.BudgetMath
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BillMathTest {
    @Test fun day31FallsBackToMonthEnd() {
        assertThat(BillMath.dueDate(31, YearMonth.of(2026, 2))).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(BillMath.dueDate(31, YearMonth.of(2028, 2))).isEqualTo(LocalDate.of(2028, 2, 29))
        assertThat(BillMath.dueDate(31, YearMonth.of(2026, 11))).isEqualTo(LocalDate.of(2026, 11, 30))
        assertThat(BillMath.dueDate(31, YearMonth.of(2026, 12))).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test fun installments() {
        assertThat(BillMath.installmentNumber(202609, YearMonth.of(2026, 11))).isEqualTo(3)
        assertThat(BillMath.installmentNumber(202611, YearMonth.of(2026, 10))).isNull()
        assertThat(BillMath.installmentNumber(202612, YearMonth.of(2027, 2))).isEqualTo(3)
        assertThat(BillMath.isDueIn(202601, 12, YearMonth.of(2026, 12))).isTrue()
        assertThat(BillMath.isDueIn(202601, 12, YearMonth.of(2027, 1))).isFalse()
        assertThat(BillMath.isDueIn(202601, null, YearMonth.of(2030, 1))).isTrue()
    }

    @Test fun reminderDays() {
        assertThat(BillMath.shouldRemind(LocalDate.of(2026, 11, 13), 15, 2)).isTrue()
        assertThat(BillMath.shouldRemind(LocalDate.of(2026, 11, 15), 15, 2)).isTrue()
        assertThat(BillMath.shouldRemind(LocalDate.of(2026, 11, 14), 15, 2)).isFalse()
        assertThat(BillMath.shouldRemind(LocalDate.of(2026, 2, 28), 31, 0)).isTrue()
    }

    @Test fun budgetLevelsAndPerDay() {
        assertThat(BudgetMath.levelsReached(79_99, 100_00)).isEmpty()
        assertThat(BudgetMath.levelsReached(80_00, 100_00)).containsExactly(80)
        assertThat(BudgetMath.levelsReached(100_00, 100_00)).containsExactly(80, 100)
        assertThat(BudgetMath.levelsReached(50, 0)).isEmpty()
        // 12 Nov: 19 days left incl. today. (1500 − 600) / 19 = 47.37
        assertThat(BudgetMath.daysLeft(LocalDate.of(2026, 11, 12))).isEqualTo(19)
        assertThat(BudgetMath.perDay(150_000, 60_000, LocalDate.of(2026, 11, 12))).isEqualTo(4737)
        assertThat(BudgetMath.perDay(100, 200, LocalDate.of(2026, 11, 12))).isEqualTo(0)
    }
}

