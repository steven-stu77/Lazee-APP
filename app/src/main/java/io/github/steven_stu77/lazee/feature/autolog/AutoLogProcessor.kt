package io.github.steven_stu77.lazee.feature.autolog

import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.core.data.PaymentMethod
import io.github.steven_stu77.lazee.core.data.PendingDirection
import io.github.steven_stu77.lazee.core.data.PendingStatus
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.BillRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.autolog.classify.MerchantKey
import io.github.steven_stu77.lazee.feature.autolog.classify.Memory
import io.github.steven_stu77.lazee.feature.autolog.match.Matcher
import io.github.steven_stu77.lazee.feature.autolog.match.OpenBill
import io.github.steven_stu77.lazee.feature.autolog.match.RecentPending
import io.github.steven_stu77.lazee.feature.autolog.match.RecentRecord
import io.github.steven_stu77.lazee.feature.autolog.parser.Direction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spec §6 steps 2–9 for one notification. The raw text only lives in this call's parameters:
 * it is never stored, written to a file, or logged.
 */
@Singleton
class AutoLogProcessor @Inject constructor(
    private val db: AppDatabase,
    private val config: AutoLogConfig,
    private val settings: SettingsRepository,
    private val ledger: LedgerRepository,
    private val bills: BillRepository,
    private val repo: AutoLogRepository,
    private val notifications: AutoLogNotifications,
) {
    // Notifications can arrive in bursts; process one at a time so dedupe and pairing see each other.
    private val lock = Mutex()

    suspend fun process(pkg: String, text: String, postedAt: Instant = Instant.now()): Outcome = lock.withLock {
        val parsed = config.engine.parse(pkg, text) ?: return@withLock Outcome.NOT_PAYMENT
        val s = settings.current()
        val key = MerchantKey.of(parsed.merchant)
        val dedupe = Matcher.dedupeKey(pkg, parsed.amount, key, postedAt)
        val dao = db.pending()
        if (dao.countKey(dedupe) > 0) return@withLock Outcome.DUPLICATE

        val incoming = parsed.incoming
        val date = Matcher.localDate(postedAt)

        // Step 6: transfers between own accounts.
        val recent = dao.since(postedAt.minus(Matcher.TRANSFER_WINDOW).toEpochMilli())
            .filter { it.status == PendingStatus.PENDING && (it.transferPairId == null || it.transferPairId == OWN_TRANSFER_MARK) }
            .map { RecentPending(it.id, it.sourcePackage, it.amount, it.direction == PendingDirection.IN, it.postedAt) }
        val pair = if (parsed.direction == Direction.OUT || parsed.direction == Direction.IN || parsed.direction == Direction.OWN_TRANSFER)
            Matcher.transferPair(pkg, parsed.amount, incoming, postedAt, recent) else null

        // Step 8: classify.
        val rule = db.merchantRules().get(key)
        val memory = rule?.let { Memory(it.categoryId, it.confirmCount, it.alwaysAsk) }
        val person = parsed.direction == Direction.PERSON_TRANSFER
        val cls = config.classifier.classify(key, memory, person)
        val categoryId = cls.categoryId ?: repo.categoryIdFor(cls.builtInKey, parsed.direction == Direction.IN)

        // Step 7: bills and possible duplicates (only for money going out).
        val ym = YearMonth.from(date)
        val payments = db.bills().paymentsFor(BillMath.ym(ym))
        val open = bills.statuses(db.bills().all(), payments, ym).filter { !it.paid }.map { OpenBill(it.bill.id, it.bill.amount, it.due) }
        val outgoing = !incoming && parsed.direction != Direction.REFUND && pair == null && parsed.direction != Direction.OWN_TRANSFER
        val bill = if (outgoing) Matcher.matchBill(parsed.amount, date, open) else null
        val dupCandidates = ledger.range(date.minusDays(Matcher.DUPLICATE_DAYS), date)
            .filter { it.type == TxnType.EXPENSE && (it.source == TxnSource.MANUAL || it.source == TxnSource.BILL) }
            .map { RecentRecord(it.id, it.amount, it.date) } +
            db.bills().allPayments().filter { it.method == PaymentMethod.MANUAL && it.transactionId != null }
                .mapNotNull { p -> ledger.transaction(p.transactionId!!)?.let { RecentRecord(it.id, it.amount, it.date) } }
        val dup = if (outgoing) Matcher.possibleDuplicate(parsed.amount, date, dupCandidates) else null

        val item = PendingTxn(
            sourcePackage = pkg, merchant = parsed.merchant, amount = parsed.amount,
            direction = if (incoming) PendingDirection.IN else PendingDirection.OUT,
            postedAt = postedAt, suggestedCategoryId = categoryId,
            confidence = if (person) Confidence.LOW else cls.confidence,
            matchedBillId = bill?.id, possibleDuplicateOf = dup?.id,
            transferPairId = pair?.id ?: if (parsed.direction == Direction.OWN_TRANSFER) OWN_TRANSFER_MARK else null,
            dedupeKey = dedupe, isRefund = parsed.direction == Direction.REFUND, isPersonTransfer = person, last4 = parsed.last4,
        )

        if (pair != null) {
            // Merge: the earlier half becomes the transfer; this half is kept only for dedupe.
            val first = dao.get(pair.id) ?: return@withLock Outcome.PENDING
            val newId = dao.insert(item.copy(status = PendingStatus.IGNORED, transferPairId = first.id))
            dao.update(first.copy(transferPairId = newId, matchedBillId = null, possibleDuplicateOf = null, confidence = Confidence.HIGH))
            notifications.cancelPending(first.id)
            notifications.showPending(dao.get(first.id)!!)
            return@withLock Outcome.PENDING
        }

        if (Matcher.shouldAutoLog(s.lazyAutoFamiliar, item.confidence, memory, bill != null, dup != null, item.transferPairId != null)) {
            val id = dao.insert(item.copy(status = PendingStatus.CONFIRMED))
            if (id < 0) return@withLock Outcome.DUPLICATE
            val txnId = ledger.saveTransaction(repo.toTransaction(item, categoryId, isAuto = true))
            repo.remember(key, categoryId!!, null)
            notifications.showAutoLogged(txnId, item, categoryId)
            return@withLock Outcome.AUTO_LOGGED
        }

        val id = dao.insert(item)
        if (id < 0) return@withLock Outcome.DUPLICATE
        if (s.lazyQuickConfirm) notifications.showPending(item.copy(id = id))
        Outcome.PENDING
    }

    /** What happened to one notification; shown (without any text) on the auto-log page. */
    enum class Outcome { PENDING, AUTO_LOGGED, DUPLICATE, NOT_PAYMENT, APP_OFF, ERROR }

    companion object {
        /** transferPairId placeholder: "between own accounts" but the other half has not arrived. */
        const val OWN_TRANSFER_MARK = -1L
    }
}
