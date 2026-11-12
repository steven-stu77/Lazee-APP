package io.github.steven_stu77.lazee.core.repo

import androidx.room.withTransaction
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.BuiltIns
import io.github.steven_stu77.lazee.core.data.Loan
import io.github.steven_stu77.lazee.core.data.LoanDirection
import io.github.steven_stu77.lazee.core.data.LoanRepayment
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.LoanMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class LoanTotals(val lent: Long, val borrowed: Long, val openCount: Int)
data class LoanView(val loan: Loan, val repaid: Long, val outstanding: Long, val interest: Long)

/**
 * Loans with a person. Principal moves are TRANSFERs with no target account:
 * a positive amount leaves the account, a negative amount comes in. Interest is income or expense.
 */
@Singleton
class LoanRepository @Inject constructor(private val db: AppDatabase, private val ledger: LedgerRepository) {
    private val dao = db.loans()

    fun observeLoans(): Flow<List<LoanView>> = combine(dao.observeAll(), dao.observeRepayments()) { loans, reps ->
        val today = LocalDate.now()
        loans.map { l ->
            val repaid = reps.filter { it.loanId == l.id }.sumOf { it.amount }
            LoanView(l, repaid, if (l.settled) 0 else LoanMath.outstanding(l.principal, l.annualRatePercent, l.startDate, repaid, today),
                LoanMath.interestUntil(l.principal, l.annualRatePercent, l.startDate, today))
        }
    }

    fun observeOutstanding(): Flow<LoanTotals> = combine(observeLoans(), dao.observeAll()) { views, _ ->
        val open = views.filter { !it.loan.settled }
        LoanTotals(
            lent = open.filter { it.loan.direction == LoanDirection.LENT }.sumOf { it.outstanding },
            borrowed = open.filter { it.loan.direction == LoanDirection.BORROWED }.sumOf { it.outstanding },
            openCount = open.size,
        )
    }

    /** Creates the loan and the principal transfer on its account. */
    suspend fun create(l: Loan): Long = db.withTransaction {
        val id = dao.insert(l)
        val out = if (l.direction == LoanDirection.LENT) l.principal else -l.principal
        ledger.saveTransaction(
            Transaction(type = TxnType.TRANSFER, amount = out, accountId = l.accountId, date = l.startDate, createdAt = Instant.now(),
                note = l.person, source = TxnSource.LOAN, loanId = id),
        )
        id
    }

    /** Records a repayment: interest part (income/expense) + principal part (transfer). Settles when nothing is owed. */
    suspend fun repay(loanId: Long, amount: Long, date: LocalDate) = db.withTransaction {
        val l = dao.get(loanId) ?: return@withTransaction
        val earlier = dao.allRepayments().filter { it.loanId == loanId }
        val part = LoanMath.allocate(l.principal, l.annualRatePercent, l.startDate, earlier.map { it.date to it.amount } + (date to amount)).last()
        val lent = l.direction == LoanDirection.LENT
        var txnId = 0L
        if (part.principal > 0) txnId = ledger.saveTransaction(
            Transaction(type = TxnType.TRANSFER, amount = if (lent) -part.principal else part.principal, accountId = l.accountId,
                date = date, createdAt = Instant.now(), note = l.person, source = TxnSource.LOAN, loanId = l.id),
        )
        if (part.interest > 0) {
            val cat = ledger.categoryByKey(if (lent) "other_income" else BuiltIns.OTHER)
            val id = ledger.saveTransaction(
                Transaction(type = if (lent) TxnType.INCOME else TxnType.EXPENSE, amount = part.interest, categoryId = cat?.id,
                    accountId = l.accountId, date = date, createdAt = Instant.now(), note = l.person, source = TxnSource.LOAN, loanId = l.id),
            )
            if (txnId == 0L) txnId = id
        }
        dao.insertRepayment(LoanRepayment(loanId = l.id, amount = amount, date = date, transactionId = txnId))
        val repaid = earlier.sumOf { it.amount } + amount
        if (LoanMath.outstanding(l.principal, l.annualRatePercent, l.startDate, repaid, date) == 0L) dao.update(l.copy(settled = true))
    }
}
