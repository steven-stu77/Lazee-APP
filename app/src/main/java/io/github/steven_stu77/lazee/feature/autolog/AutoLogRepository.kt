package io.github.steven_stu77.lazee.feature.autolog

import androidx.room.withTransaction
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.BillPayment
import io.github.steven_stu77.lazee.core.data.CategoryKind
import io.github.steven_stu77.lazee.core.data.MerchantRule
import io.github.steven_stu77.lazee.core.data.PaymentMethod
import io.github.steven_stu77.lazee.core.data.PendingDirection
import io.github.steven_stu77.lazee.core.data.PendingStatus
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.BudgetAlerts
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.autolog.classify.MerchantKey
import io.github.steven_stu77.lazee.feature.autolog.match.Matcher
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** Turns confirmed pending items into transactions and keeps the merchant memory (spec §6 step 10). */
@Singleton
class AutoLogRepository @Inject constructor(
    private val db: AppDatabase,
    private val ledger: LedgerRepository,
    private val settings: SettingsRepository,
    private val config: AutoLogConfig,
    private val budgetAlerts: BudgetAlerts,
) {
    private val pending = db.pending()

    /** Default: the account mapped in settings, else one whose name matches the app, else the last used. */
    suspend fun accountFor(pkg: String): Account? {
        val accounts = ledger.allAccounts().filter { !it.archived }
        val s = settings.current()
        s.accountForPackage(pkg)?.let { id -> accounts.firstOrNull { it.id == id }?.let { return it } }
        val name = config.engine.config(pkg)?.displayName?.lowercase()
        if (name != null) accounts.firstOrNull { it.name.lowercase().contains(name) || name.contains(it.name.lowercase()) }?.let { return it }
        return accounts.firstOrNull { it.id == s.lastAccountId } ?: accounts.firstOrNull()
    }

    /** Category id for a built-in key such as "food"; income items use the income "other". */
    suspend fun categoryIdFor(key: String?, incoming: Boolean): Long? {
        if (incoming) return ledger.categoryByKey("other_income")?.id
        return (key?.let { ledger.categoryByKey(it) } ?: ledger.categoryByKey("other"))?.id
    }

    /** Builds the transaction a pending item stands for (transfer, refund, income or expense). */
    suspend fun toTransaction(p: PendingTxn, categoryId: Long?, isAuto: Boolean): Transaction {
        val account = accountFor(p.sourcePackage) ?: error("no account")
        val base = Transaction(
            type = TxnType.EXPENSE, amount = p.amount, categoryId = categoryId, accountId = account.id,
            date = Matcher.localDate(p.postedAt), createdAt = Instant.now(), merchant = p.merchant,
            source = TxnSource.NOTIFICATION, isAuto = isAuto, billId = p.matchedBillId,
        )
        return when {
            p.transferPairId != null -> {
                val other = pending.get(p.transferPairId)?.let { accountFor(it.sourcePackage) }
                    ?: ledger.allAccounts().firstOrNull { it.id != account.id && !it.archived }
                // Money leaves the OUT side and arrives on the IN side.
                val (from, to) = if (p.direction == PendingDirection.OUT) account.id to other?.id else (other?.id ?: account.id) to account.id
                base.copy(type = TxnType.TRANSFER, categoryId = null, accountId = from, toAccountId = to, merchant = null)
            }
            p.isRefund -> base.copy(amount = -p.amount)
            p.direction == PendingDirection.IN -> base.copy(type = TxnType.INCOME)
            else -> base
        }
    }

    /** Confirm: record it, remember the merchant, mark a matched bill as paid. Returns the new transaction id. */
    suspend fun confirm(id: Long, categoryId: Long?, alwaysAsk: Boolean? = null): Long? = db.withTransaction {
        val p = pending.get(id)?.takeIf { it.status == PendingStatus.PENDING } ?: return@withTransaction null
        val cat = categoryId ?: p.suggestedCategoryId
        val t = toTransaction(p, cat, isAuto = false)
        val txnId = ledger.saveTransaction(t)
        pending.update(p.copy(status = PendingStatus.CONFIRMED, suggestedCategoryId = cat))
        if (t.type == TxnType.EXPENSE && cat != null && !p.isPersonTransfer) remember(MerchantKey.of(p.merchant), cat, alwaysAsk)
        p.matchedBillId?.let { billId ->
            db.bills().insertPayment(BillPayment(billId = billId, yearMonth = BillMath.ym(YearMonth.from(t.date)), transactionId = txnId, method = PaymentMethod.NOTIFICATION))
        }
        txnId
    }.also { if (it != null) budgetAlerts.check(Matcher.localDate(Instant.now())) }

    suspend fun ignore(id: Long) {
        val p = pending.get(id) ?: return
        pending.update(p.copy(status = PendingStatus.IGNORED))
    }

    /** Same category → one more confirmation; a different one → switch and start counting again. */
    suspend fun remember(key: String, categoryId: Long, alwaysAsk: Boolean?) {
        val dao = db.merchantRules()
        val old = dao.get(key)
        val rule = when {
            old == null -> MerchantRule(merchantKey = key, categoryId = categoryId, confirmCount = 1, alwaysAsk = alwaysAsk ?: false)
            old.categoryId == categoryId -> old.copy(confirmCount = old.confirmCount + 1, alwaysAsk = alwaysAsk ?: old.alwaysAsk)
            else -> old.copy(categoryId = categoryId, confirmCount = 1, alwaysAsk = alwaysAsk ?: old.alwaysAsk)
        }
        dao.upsert(rule)
    }

    suspend fun rule(merchant: String): MerchantRule? = db.merchantRules().get(MerchantKey.of(merchant))

    /** Undo an auto-logged record: delete it. */
    suspend fun undoAuto(txnId: Long) {
        val t = ledger.transaction(txnId) ?: return
        if (t.isAuto) ledger.deleteTransactions(listOf(txnId))
    }

    suspend fun expenseCategories() = ledger.allCategories().filter { it.kind == CategoryKind.EXPENSE && !it.archived }
}
