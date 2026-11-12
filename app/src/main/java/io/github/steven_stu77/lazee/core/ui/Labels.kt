package io.github.steven_stu77.lazee.core.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Transaction
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Display names and date labels. Built-in categories are translated; user-made names are shown as typed. */
object Labels {
    private val builtInNames = mapOf(
        "food" to R.string.cat_food, "transport" to R.string.cat_transport, "shopping" to R.string.cat_shopping,
        "fun" to R.string.cat_fun, "bills" to R.string.cat_bills, "study" to R.string.cat_study,
        "gifts" to R.string.cat_gifts, "other" to R.string.cat_other, "allowance" to R.string.cat_allowance,
        "parttime" to R.string.cat_parttime, "giftmoney" to R.string.cat_giftmoney, "other_income" to R.string.cat_other,
    )

    fun categoryName(context: Context, c: Category?): String = when {
        c == null -> context.getString(R.string.cat_other)
        c.customName != null -> c.customName
        else -> builtInNames[c.builtInKey]?.let(context::getString) ?: context.getString(R.string.cat_other)
    }

    fun isZh(context: Context): Boolean = context.resources.configuration.locales[0].language == "zh"

    private fun en(m: java.time.Month, style: TextStyle) = m.getDisplayName(style, Locale.ENGLISH)

    /** "11 月 12 日 · 星期四" or "12 Nov · Thu" (patterns live in strings.xml). */
    fun dayFull(context: Context, d: LocalDate): String = context.getString(
        R.string.fmt_day_full, d.monthValue, d.dayOfMonth, en(d.month, TextStyle.SHORT),
        d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE),
    )

    /** "今天 / 昨天 / 11 月 10 日" for list group headers. */
    fun dayGroup(context: Context, d: LocalDate, today: LocalDate = LocalDate.now()): String = when (d) {
        today -> context.getString(R.string.today)
        today.minusDays(1) -> context.getString(R.string.yesterday)
        else -> if (d.year == today.year) shortDate(context, d) else "${shortDate(context, d)} ${d.year}"
    }

    fun shortDate(context: Context, d: LocalDate): String =
        context.getString(R.string.fmt_day_short, d.monthValue, d.dayOfMonth, en(d.month, TextStyle.SHORT))

    fun isoDate(d: LocalDate): String = d.format(DateTimeFormatter.ISO_LOCAL_DATE)

    /** "2026 年 11 月" / "November 2026". */
    fun monthTitle(context: Context, ym: YearMonth): String =
        context.getString(R.string.fmt_month_title, ym.year, ym.monthValue, en(ym.month, TextStyle.FULL))

    /** Short month: "11月" / "Nov". */
    fun monthShort(context: Context, ym: YearMonth): String =
        context.getString(R.string.fmt_month_short, ym.monthValue, en(ym.month, TextStyle.SHORT))

    /** Row title: note, else merchant, else category name. */
    fun txnTitle(context: Context, t: Transaction, category: Category?): String =
        t.note?.takeIf { it.isNotBlank() } ?: t.merchant?.takeIf { it.isNotBlank() } ?: when (t.type) {
            io.github.steven_stu77.lazee.core.data.TxnType.TRANSFER -> context.getString(R.string.transfer)
            else -> categoryName(context, category)
        }
}

@Composable
fun categoryName(c: Category?): String {
    LocalConfiguration.current // recompose on language change
    return Labels.categoryName(LocalContext.current, c)
}
