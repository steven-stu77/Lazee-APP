package io.github.steven_stu77.lazee.core.repo

import androidx.room.withTransaction
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.PaymentMethod
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import java.time.Instant
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.BillPayment
import io.github.steven_stu77.lazee.core.logic.BillMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** One bill as seen in a given month. */
data class BillStatus(
    val bill: Bill,
    val due: LocalDate,
    val payment: BillPayment?,
    val installment: Int?,
) {
    val paid: Boolean get() = payment != null
}

@Singleton
class BillRepository @Inject constructor(
    private val db: AppDatabase,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
) {
    private val dao = db.bills()

    fun observeBills(): Flow<List<Bill>> = dao.observeAll()

    /** Active bills due this month with their paid state, unpaid first by due date. */
    fun observeMonth(month: YearMonth = YearMonth.now()): Flow<List<BillStatus>> =
        combine(dao.observeAll(), dao.observePayments()) { bills, payments -> statuses(bills, payments, month) }

    fun statuses(bills: List<Bill>, payments: List<BillPayment>, month: YearMonth): List<BillStatus> {
        val key = BillMath.ym(month)
        return bills.filter { it.active && BillMath.isDueIn(it.startMonth, it.totalInstallments, month) }
            .map { b ->
                BillStatus(
                    b, BillMath.dueDate(b.dayOfMonth, month),
                    payments.firstOrNull { it.billId == b.id && it.yearMonth == key },
                    if (b.totalInstallments != null) BillMath.installmentNumber(b.startMonth, month) else null,
                )
            }
            .sortedWith(compareBy<BillStatus> { it.paid }.thenBy { it.due })
    }

    suspend fun bill(id: Long) = dao.get(id)
    suspend fun allBills() = dao.all()
    suspend fun payments(month: YearMonth) = dao.paymentsFor(BillMath.ym(month))

    /** "Paid in cash? Mark as paid": logs the payment once and stops reminders this month. */
    suspend fun markPaid(billId: Long, month: YearMonth = YearMonth.now(), auto: Boolean = false): Long? = db.withTransaction {
        val b = dao.get(billId) ?: return@withTransaction null
        val key = BillMath.ym(month)
        if (dao.paymentsFor(key).any { it.billId == billId }) return@withTransaction null
        val accountId = b.accountId ?: settings.current().lastAccountId.takeIf { it > 0 } ?: ledger.allAccounts().first().id
        val due = BillMath.dueDate(b.dayOfMonth, month)
        val txnId = ledger.saveTransaction(
            Transaction(
                type = TxnType.EXPENSE, amount = b.amount, categoryId = b.categoryId, accountId = accountId,
                date = if (auto) due else minOf(LocalDate.now(), month.atEndOfMonth()), createdAt = Instant.now(),
                note = b.name, source = TxnSource.BILL, isAuto = auto, billId = b.id,
            ),
        )
        dao.insertPayment(BillPayment(billId = b.id, yearMonth = key, transactionId = txnId, method = if (auto) PaymentMethod.AUTO else PaymentMethod.MANUAL))
        retireIfFinished(b, month)
        txnId
    }

    /** After the last instalment is paid the bill switches itself off. */
    private suspend fun retireIfFinished(b: Bill, month: YearMonth) {
        val total = b.totalInstallments ?: return
        if ((BillMath.installmentNumber(b.startMonth, month) ?: 0) >= total) dao.update(b.copy(active = false))
    }

    suspend fun retireFinished(month: YearMonth) {
        val paid = dao.paymentsFor(BillMath.ym(month)).map { it.billId }.toSet()
        dao.active().filter { it.id in paid }.forEach { retireIfFinished(it, month) }
    }
    suspend fun save(b: Bill): Long = if (b.id == 0L) dao.insert(b) else { dao.update(b); b.id }
    suspend fun delete(b: Bill) = dao.delete(b)
}
