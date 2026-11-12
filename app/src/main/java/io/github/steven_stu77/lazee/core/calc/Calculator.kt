package io.github.steven_stu77.lazee.core.calc

import io.github.steven_stu77.lazee.core.money.Money
import net.objecthunter.exp4j.ExpressionBuilder
import java.math.BigDecimal

/**
 * Keypad state machine for the add screen. Pure Kotlin so it is unit-tested.
 * Rules: no leading operator; a second operator replaces the first; max 2 decimals
 * and 7 integer digits per number; result rounded to cents; divide by zero → 0.
 */
data class Calculator(val expr: String = "") {

    val hasOperator: Boolean get() = expr.drop(1).any { it in OPS }

    /** Pending means there is something to evaluate (an operator followed by a number). */
    val isPending: Boolean get() = hasOperator

    fun press(key: Char): Calculator = when (key) {
        in OPS -> operator(key)
        '.' -> dot()
        '⌫' -> Calculator(expr.dropLast(1))
        in '0'..'9' -> digit(key)
        else -> this
    }

    private fun operator(op: Char): Calculator {
        if (expr.isEmpty()) return this
        val last = expr.last()
        if (last in OPS) return Calculator(expr.dropLast(1) + op)
        val base = if (last == '.') expr.dropLast(1) else expr
        return Calculator(base + op)
    }

    private fun currentNumber(): String = expr.takeLastWhile { it !in OPS }

    private fun dot(): Calculator {
        val n = currentNumber()
        if (n.contains('.')) return this
        return Calculator(expr + if (n.isEmpty()) "0." else ".")
    }

    private fun digit(d: Char): Calculator {
        val n = currentNumber()
        if (n.contains('.')) {
            if (n.substringAfter('.').length >= 2) return this
        } else {
            if (n == "0") return Calculator(expr.dropLast(1) + d)
            if (n.length >= 7) return this
        }
        return Calculator(expr + d)
    }

    /** Result in cents and whether a divide-by-zero happened. */
    fun evaluate(): Result {
        if (expr.isEmpty()) return Result(0, false)
        val trimmed = expr.trimEnd { it in OPS || it == '.' }
        if (trimmed.isEmpty()) return Result(0, false)
        val js = trimmed.replace('×', '*').replace('÷', '/').replace('−', '-')
        return try {
            val v = ExpressionBuilder(js).build().evaluate()
            if (v.isNaN() || v.isInfinite()) Result(0, true)
            else Result(Money.fromDecimal(BigDecimal(v.toString())), false)
        } catch (e: ArithmeticException) {
            Result(0, true)
        }
    }

    /** Collapse to the evaluated result so the user can keep typing from it. */
    fun collapse(): Calculator {
        val r = evaluate()
        if (r.cents <= 0) return Calculator("")
        return Calculator(Money.toInput(r.cents).let { stripZeros(it) })
    }

    private fun stripZeros(s: String): String =
        if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s

    data class Result(val cents: Long, val divideByZero: Boolean)

    companion object {
        const val OPS = "+−×÷"

        fun fromCents(cents: Long): Calculator =
            if (cents <= 0) Calculator("") else Calculator(Money.toInput(cents).let { s ->
                if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
            })
    }
}
