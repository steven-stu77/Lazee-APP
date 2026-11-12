package io.github.steven_stu77.lazee.feature.home

import android.content.Context
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.ui.Labels
import java.time.LocalDate

/** One list row, already resolved to display strings. Shared by home, search and category detail. */
data class TxnRow(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val categoryName: String,
    val colorIndex: Int,
    val accountName: String,
    val amount: Long,
    val type: TxnType,
    val isAuto: Boolean,
)

fun Transaction.toRow(context: Context, categories: Map<Long, Category>, accounts: Map<Long, Account>): TxnRow {
    val cat = categoryId?.let(categories::get)
    val catName = if (type == TxnType.TRANSFER) Labels.categoryName(context, null).let { context.getString(io.github.steven_stu77.lazee.R.string.transfer) }
    else Labels.categoryName(context, cat)
    val acc = if (type == TxnType.TRANSFER) "${accounts[accountId]?.name.orEmpty()} → ${accounts[toAccountId]?.name.orEmpty()}"
    else accounts[accountId]?.name.orEmpty()
    return TxnRow(
        id = id, date = date, title = Labels.txnTitle(context, this, cat), categoryName = catName,
        colorIndex = if (type == TxnType.TRANSFER) 7 else (cat?.colorIndex ?: 7),
        accountName = acc, amount = amount, type = type, isAuto = isAuto,
    )
}
