package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.money.Money
import org.junit.Test

class MoneyTest {
    @Test fun formats() {
        assertThat(Money.format(123450)).isEqualTo("RM 1,234.50")
        assertThat(Money.format(650)).isEqualTo("RM 6.50")
        assertThat(Money.format(5)).isEqualTo("RM 0.05")
        assertThat(Money.format(-500)).isEqualTo("-RM 5.00")
        assertThat(Money.format(100000000)).isEqualTo("RM 1,000,000.00")
        assertThat(Money.format(650, hidden = true)).isEqualTo("RM ••••")
    }

    @Test fun parses() {
        assertThat(Money.parse("RM 1,234.50")).isEqualTo(123450)
        assertThat(Money.parse("RM6.80")).isEqualTo(680)
        assertThat(Money.parse("MYR 6.80")).isEqualTo(680)
        assertThat(Money.parse("6.8")).isEqualTo(680)
        assertThat(Money.parse("12")).isEqualTo(1200)
        assertThat(Money.parse("abc")).isNull()
        assertThat(Money.parse("")).isNull()
    }

    @Test fun roundTrip() {
        for (c in listOf(0L, 1L, 99L, 100L, 123456789L)) {
            assertThat(Money.parse(Money.format(c))).isEqualTo(c)
        }
    }
}
