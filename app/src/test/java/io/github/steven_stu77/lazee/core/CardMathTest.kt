package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.logic.CardMath
import io.github.steven_stu77.lazee.core.logic.Entry
import io.github.steven_stu77.lazee.core.logic.Entry.Kind
import org.junit.Test
import java.time.LocalDate

class CardMathTest {
    private fun e(id: Long, kind: Kind, amt: Long, d: LocalDate, acc: Long = 9, to: Long? = null) = Entry(id, kind, amt, d, acc, to)

    @Test fun statementDate() {
        assertThat(CardMath.lastStatementDate(25, LocalDate.of(2026, 11, 12))).isEqualTo(LocalDate.of(2026, 10, 25))
        assertThat(CardMath.lastStatementDate(10, LocalDate.of(2026, 11, 12))).isEqualTo(LocalDate.of(2026, 11, 10))
        assertThat(CardMath.lastStatementDate(31, LocalDate.of(2026, 3, 5))).isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @Test fun amountDueCountsOnlyUpToStatement() {
        val today = LocalDate.of(2026, 11, 12)
        val entries = listOf(
            e(1, Kind.EXPENSE, 30_000, LocalDate.of(2026, 11, 3)),
            e(2, Kind.EXPENSE, 5_000, LocalDate.of(2026, 11, 11)), // after the statement on the 10th
            e(3, Kind.TRANSFER, 10_000, LocalDate.of(2026, 11, 11), acc = 1, to = 9), // repayment
        )
        assertThat(CardMath.amountDue(9, 0, entries, 10, today)).isEqualTo(20_000)
    }
}
