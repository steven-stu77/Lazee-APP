package io.github.steven_stu77.lazee.autolog

import com.google.common.truth.Truth.assertThat
import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.feature.autolog.classify.Memory
import io.github.steven_stu77.lazee.feature.autolog.match.Matcher
import io.github.steven_stu77.lazee.feature.autolog.match.OpenBill
import io.github.steven_stu77.lazee.feature.autolog.match.RecentPending
import io.github.steven_stu77.lazee.feature.autolog.match.RecentRecord
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MatcherTest {
    private val t0 = Instant.parse("2026-11-12T10:00:30Z")

    @Test fun sameNotificationTwiceHasSameKey() {
        val a = Matcher.dedupeKey("tng", 680, "7-ELEVEN", t0)
        val b = Matcher.dedupeKey("tng", 680, "7-ELEVEN", t0.plusSeconds(20))
        assertThat(a).isEqualTo(b)
        assertThat(Matcher.dedupeKey("tng", 680, "7-ELEVEN", t0.plusSeconds(60))).isNotEqualTo(a)
    }

    @Test fun maeToTngBecomesTransfer() {
        val maeOut = RecentPending(1, "mae", 5000, incoming = false, postedAt = t0)
        assertThat(Matcher.transferPair("tng", 5000, incoming = true, postedAt = t0.plusSeconds(120), recent = listOf(maeOut))).isEqualTo(maeOut)
        // Same app, wrong amount, same direction, or too late: no pair.
        assertThat(Matcher.transferPair("mae", 5000, true, t0.plusSeconds(60), listOf(maeOut))).isNull()
        assertThat(Matcher.transferPair("tng", 5001, true, t0.plusSeconds(60), listOf(maeOut))).isNull()
        assertThat(Matcher.transferPair("tng", 5000, false, t0.plusSeconds(60), listOf(maeOut))).isNull()
        assertThat(Matcher.transferPair("tng", 5000, true, t0.plusSeconds(11 * 60), listOf(maeOut))).isNull()
    }

    @Test fun billWindowAndTolerance() {
        val bill = OpenBill(7, 12_900, LocalDate.of(2026, 11, 20))
        assertThat(Matcher.matchBill(12_900, LocalDate.of(2026, 11, 10), listOf(bill))).isEqualTo(bill) // 10 days before
        assertThat(Matcher.matchBill(13_000, LocalDate.of(2026, 11, 27), listOf(bill))).isEqualTo(bill) // +RM1, 7 days after
        assertThat(Matcher.matchBill(13_001, LocalDate.of(2026, 11, 20), listOf(bill))).isNull()
        assertThat(Matcher.matchBill(12_900, LocalDate.of(2026, 11, 9), listOf(bill))).isNull()
        assertThat(Matcher.matchBill(12_900, LocalDate.of(2026, 11, 28), listOf(bill))).isNull()
    }

    @Test fun manualRecordMakesPossibleDuplicate() {
        val manual = RecentRecord(3, 12_900, LocalDate.of(2026, 11, 10))
        assertThat(Matcher.possibleDuplicate(12_900, LocalDate.of(2026, 11, 12), listOf(manual))).isEqualTo(manual)
        assertThat(Matcher.possibleDuplicate(12_900, LocalDate.of(2026, 11, 14), listOf(manual))).isNull()
        assertThat(Matcher.possibleDuplicate(12_800, LocalDate.of(2026, 11, 12), listOf(manual))).isNull()
    }

    @Test fun lazyAutoLogNeedsEverythingCertain() {
        val familiar = Memory(1, 3, false)
        assertThat(Matcher.shouldAutoLog(true, Confidence.HIGH, familiar, false, false, false)).isTrue()
        assertThat(Matcher.shouldAutoLog(false, Confidence.HIGH, familiar, false, false, false)).isFalse()
        assertThat(Matcher.shouldAutoLog(true, Confidence.HIGH, Memory(1, 2, false), false, false, false)).isFalse()
        assertThat(Matcher.shouldAutoLog(true, Confidence.HIGH, Memory(1, 5, true), false, false, false)).isFalse()
        assertThat(Matcher.shouldAutoLog(true, Confidence.HIGH, familiar, true, false, false)).isFalse()
        assertThat(Matcher.shouldAutoLog(true, Confidence.HIGH, familiar, false, true, false)).isFalse()
        assertThat(Matcher.shouldAutoLog(true, Confidence.MEDIUM, null, false, false, false)).isFalse()
    }
}
