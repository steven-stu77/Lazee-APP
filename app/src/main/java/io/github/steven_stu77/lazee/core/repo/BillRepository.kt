package io.github.steven_stu77.lazee.core.repo

import io.github.steven_stu77.lazee.core.data.AppDatabase
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
class BillRepository @Inject constructor(private val db: AppDatabase) {
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
    suspend fun save(b: Bill): Long = if (b.id == 0L) dao.insert(b) else { dao.update(b); b.id }
    suspend fun delete(b: Bill) = dao.delete(b)
}
