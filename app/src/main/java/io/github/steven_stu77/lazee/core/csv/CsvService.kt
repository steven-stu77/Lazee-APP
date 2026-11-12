package io.github.steven_stu77.lazee.core.csv

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.ui.Labels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Writes a CSV for a date range; column names follow the current language (spec §5.13). */
@Singleton
class CsvService @Inject constructor(@ApplicationContext private val context: Context, private val ledger: LedgerRepository) {
    suspend fun export(uri: Uri, from: LocalDate, to: LocalDate): Int = withContext(Dispatchers.IO) {
        val c = context
        val cats = ledger.allCategories().associateBy { it.id }
        val accs = ledger.allAccounts().associateBy { it.id }
        val tags = ledger.observeTagNames().first()
        val txns = ledger.range(from, to).sortedBy { it.date }
        val rows = txns.map { t ->
            val cat = cats[t.categoryId]
            val parent = cat?.parentId?.let(cats::get)
            CsvRow(
                date = Labels.isoDate(t.date),
                type = c.getString(when (t.type) { TxnType.EXPENSE -> R.string.expense; TxnType.INCOME -> R.string.income; TxnType.TRANSFER -> R.string.transfer }),
                category = if (t.type == TxnType.TRANSFER) "" else Labels.categoryName(c, parent ?: cat),
                subCategory = if (parent != null) Labels.categoryName(c, cat) else "",
                amount = BigDecimal(t.amount).movePointLeft(2).toPlainString(),
                account = listOfNotNull(accs[t.accountId]?.name, t.toAccountId?.let { accs[it]?.name }).joinToString(" → "),
                merchant = t.merchant.orEmpty(),
                note = t.note.orEmpty(),
                tags = tags[t.id].orEmpty().joinToString(" ") { "#$it" },
                source = c.getString(when (t.source) {
                    TxnSource.MANUAL -> R.string.source_manual; TxnSource.NOTIFICATION -> R.string.source_notification
                    TxnSource.BILL -> R.string.source_bill; TxnSource.LOAN -> R.string.source_loan
                }),
            )
        }
        val headers = listOf(R.string.txn_date, R.string.txn_type, R.string.txn_category, R.string.csv_sub_category, R.string.bill_amount,
            R.string.txn_account, R.string.txn_merchant, R.string.txn_note, R.string.txn_tags, R.string.txn_source).map(c::getString)
        c.contentResolver.openOutputStream(uri, "wt")?.use { it.write(CsvExport.build(headers, rows)) }
        rows.size
    }

}
