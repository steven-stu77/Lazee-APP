package io.github.steven_stu77.lazee.core

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.calc.Calculator
import org.junit.Test

class CalculatorTest {
    private fun type(keys: String): Calculator = keys.fold(Calculator()) { c, k -> c.press(k) }

    @Test fun addsDecimals() {
        assertThat(type("12+8.5").evaluate().cents).isEqualTo(2050)
    }

    @Test fun precedence() {
        assertThat(type("2+3×4").evaluate().cents).isEqualTo(1400)
        assertThat(type("10−4÷2").evaluate().cents).isEqualTo(800)
    }

    @Test fun noLeadingOperator() {
        assertThat(type("+").expr).isEmpty()
        assertThat(type("×5").expr).isEqualTo("5")
    }

    @Test fun secondOperatorReplacesFirst() {
        assertThat(type("5+×3").expr).isEqualTo("5×3")
        assertThat(type("5+×3").evaluate().cents).isEqualTo(1500)
    }

    @Test fun decimalLimits() {
        assertThat(type("1.234").expr).isEqualTo("1.23")
        assertThat(type("12345678").expr).isEqualTo("1234567")
        assertThat(type("1..5").expr).isEqualTo("1.5")
        assertThat(type(".5").expr).isEqualTo("0.5")
    }

    @Test fun divideByZero() {
        val r = type("5÷0").evaluate()
        assertThat(r.cents).isEqualTo(0)
        assertThat(r.divideByZero).isTrue()
    }

    @Test fun roundsToCents() {
        assertThat(type("10÷3").evaluate().cents).isEqualTo(333)
        assertThat(type("2÷3").evaluate().cents).isEqualTo(67)
        assertThat(type("0.1+0.2").evaluate().cents).isEqualTo(30)
    }

    @Test fun trailingOperatorIgnored() {
        assertThat(type("12+").evaluate().cents).isEqualTo(1200)
        assertThat(type("12+").isPending).isTrue()
    }

    @Test fun backspaceAndCollapse() {
        assertThat(type("12⌫").expr).isEqualTo("1")
        assertThat(type("12+8.5").collapse().expr).isEqualTo("20.5")
    }
}
