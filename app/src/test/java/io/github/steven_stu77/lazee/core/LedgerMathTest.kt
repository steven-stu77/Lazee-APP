package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.logic.Entry
import io.github.steven_stu77.lazee.core.logic.Entry.Kind
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.logic.Tags
import org.junit.Test
import java.time.LocalDate

class LedgerMathTest {
    private val d = LocalDate.of(2026, 11, 12)

    @Test fun tags() {
        assertThat(Tags.parse("dinner #旅行 with #team_a #旅行")).containsExactly("旅行", "team_a").inOrder()
        assertThat(Tags.parse(null)).isEmpty()
        assertThat(Tags.parse("no tags")).isEmpty()
    }

    @Test fun balanceCountsTransfersBothWays() {
        val e = listOf(
            Entry(1, Kind.INCOME, 10000, d, 1),
            Entry(2, Kind.EXPENSE, 650, d, 1),
            Entry(3, Kind.TRANSFER, 2000, d, 1, toAccountId = 2),
            Entry(4, Kind.EXPENSE, -300, d, 1), // refund
        )
        assertThat(LedgerMath.balance(1, 500, e)).isEqualTo(500 + 10000 - 650 - 2000 + 300)
        assertThat(LedgerMath.balance(2, 0, e)).isEqualTo(2000)
    }

    @Test fun transfersAreNotIncomeOrExpense() {
        val e = listOf(Entry(1, Kind.TRANSFER, 2000, d, 1, 2), Entry(2, Kind.EXPENSE, 100, d, 1))
        assertThat(LedgerMath.totalExpense(e)).isEqualTo(100)
        assertThat(LedgerMath.totalIncome(e)).isEqualTo(0)
    }

    @Test fun weekStartsMonday() {
        val dots = LedgerMath.weekDots(d, setOf(d, d.minusDays(1)))
        assertThat(dots.first().first).isEqualTo(LocalDate.of(2026, 11, 9))
        assertThat(dots.last().first).isEqualTo(LocalDate.of(2026, 11, 15))
        assertThat(dots.count { it.second }).isEqualTo(2)
    }

    @Test fun groupsNewestFirst() {
        val items = listOf(d.minusDays(1) to 100L, d to 50L, d to 25L)
        val g = LedgerMath.groupByDay(items, { it.first }, { it.second })
        assertThat(g.map { it.date }).containsExactly(d, d.minusDays(1)).inOrder()
        assertThat(g.first().spent).isEqualTo(75)
    }
}

class CategoryRulesTest {
    @org.junit.Test fun limits() {
        val r = io.github.steven_stu77.lazee.core.logic.CategoryRules
        com.google.common.truth.Truth.assertThat(r.clamp("宠物用品好多")).isEqualTo("宠物用品")
        com.google.common.truth.Truth.assertThat(r.clamp("Pets and more stuff")).isEqualTo("Pets and mor")
        com.google.common.truth.Truth.assertThat(r.isDuplicate(" food ", listOf("Food", "Fun"))).isTrue()
        com.google.common.truth.Truth.assertThat(r.isDuplicate("餐饮", listOf("交通"))).isFalse()
    }
}
