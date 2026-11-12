package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.logic.LoanMath
import org.junit.Test
import java.time.LocalDate

class LoanMathTest {
    private val start = LocalDate.of(2026, 1, 1)

    @Test fun simpleInterest() {
        // RM 1,000 at 5% for 365 days = RM 50
        assertThat(LoanMath.interest(100_000, 5.0, 365)).isEqualTo(5_000)
        // RM 1,000 at 5% for 30 days = 4.109… → RM 4.11
        assertThat(LoanMath.interest(100_000, 5.0, 30)).isEqualTo(411)
        assertThat(LoanMath.interest(100_000, null, 30)).isEqualTo(0)
        assertThat(LoanMath.interest(100_000, 5.0, 0)).isEqualTo(0)
    }

    @Test fun outstandingNeverNegative() {
        assertThat(LoanMath.outstanding(100_000, null, start, 40_000, start.plusDays(10))).isEqualTo(60_000)
        assertThat(LoanMath.outstanding(100_000, null, start, 150_000, start.plusDays(10))).isEqualTo(0)
    }

    @Test fun repaymentsPayInterestFirst() {
        val d1 = start.plusDays(365) // RM 50 interest accrued
        val parts = LoanMath.allocate(100_000, 5.0, start, listOf(d1 to 20_000, d1 to 10_000))
        assertThat(parts[0]).isEqualTo(LoanMath.Part(5_000, 15_000))
        assertThat(parts[1]).isEqualTo(LoanMath.Part(0, 10_000))
    }

    @Test fun noRateMeansAllPrincipal() {
        val parts = LoanMath.allocate(50_000, null, start, listOf(start.plusDays(3) to 20_000))
        assertThat(parts.single()).isEqualTo(LoanMath.Part(0, 20_000))
    }
}
