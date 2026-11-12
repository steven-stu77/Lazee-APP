package io.github.steven_stu77.lazee.feature.autolog.match

import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.feature.autolog.classify.Memory
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A recent pending item that could be the other half of a transfer between own accounts. */
data class RecentPending(val id: Long, val pkg: String, val amount: Long, val incoming: Boolean, val postedAt: Instant)

data class OpenBill(val id: Long, val amount: Long, val due: LocalDate)

/** Records that make a new notification a *possible* duplicate (spec §6 step 7). */
data class RecentRecord(val id: Long, val amount: Long, val date: LocalDate)

/** Steps 5–7 and 9 of spec §6. Pure, so every rule is unit-tested. */
object Matcher {
    val TRANSFER_WINDOW: Duration = Duration.ofMinutes(10)
    const val BILL_TOLERANCE = 100L // RM 1
    const val DUPLICATE_DAYS = 3L
    const val FAMILIAR_CONFIRMS = 3

    /** Same app + amount + merchant + minute → same notification, even if re-posted. */
    fun dedupeKey(pkg: String, amount: Long, merchantKey: String, postedAt: Instant): String =
        "$pkg|$amount|$merchantKey|${postedAt.truncatedTo(ChronoUnit.MINUTES).epochSecond}"

    /** An opposite-direction pending item of the same amount from a different app within 10 minutes. */
    fun transferPair(pkg: String, amount: Long, incoming: Boolean, postedAt: Instant, recent: List<RecentPending>): RecentPending? =
        recent.firstOrNull {
            it.pkg != pkg && it.amount == amount && it.incoming != incoming &&
                Duration.between(it.postedAt, postedAt).abs() <= TRANSFER_WINDOW
        }

    /** Unpaid bill within RM 1, dated from 10 days before to 7 days after the due date. Closest amount wins. */
    fun matchBill(amount: Long, on: LocalDate, bills: List<OpenBill>): OpenBill? =
        bills.filter { kotlin.math.abs(it.amount - amount) <= BILL_TOLERANCE && !on.isBefore(it.due.minusDays(10)) && !on.isAfter(it.due.plusDays(7)) }
            .minByOrNull { kotlin.math.abs(it.amount - amount) }

    /** Same amount recorded by hand (or by a bill) in the last 3 days. */
    fun possibleDuplicate(amount: Long, on: LocalDate, records: List<RecentRecord>): RecentRecord? =
        records.firstOrNull { it.amount == amount && !it.date.isAfter(on) && !it.date.isBefore(on.minusDays(DUPLICATE_DAYS)) }

    /** Lazy mode "auto-log familiar shops": only when nothing at all is uncertain. */
    fun shouldAutoLog(
        lazyAutoFamiliar: Boolean,
        confidence: Confidence,
        memory: Memory?,
        matchedBill: Boolean,
        possibleDuplicate: Boolean,
        transferPair: Boolean,
    ): Boolean = lazyAutoFamiliar && confidence == Confidence.HIGH && memory != null &&
        memory.confirmCount >= FAMILIAR_CONFIRMS && !memory.alwaysAsk && !matchedBill && !possibleDuplicate && !transferPair

    fun localDate(i: Instant, zone: ZoneId = ZoneId.systemDefault()): LocalDate = i.atZone(zone).toLocalDate()
}
