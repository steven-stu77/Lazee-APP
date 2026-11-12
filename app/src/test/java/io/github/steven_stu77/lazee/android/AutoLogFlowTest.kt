package io.github.steven_stu77.lazee.android

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.PendingStatus
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.feature.autolog.AutoLogProcessor
import io.github.steven_stu77.lazee.feature.autolog.AutoLogRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** The whole notification pipeline (spec §6) against a real in-memory database. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35])
class AutoLogFlowTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var ledger: LedgerRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var processor: AutoLogProcessor
    @Inject lateinit var repo: AutoLogRepository
    @Inject lateinit var bills: io.github.steven_stu77.lazee.core.repo.BillRepository

    private val tng = "my.com.tngdigital.ewallet"
    private val mae = "com.maybank2u.life"
    private val now: Instant = LocalDate.now().atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()

    @Before fun setUp() = runBlocking {
        hilt.inject()
        ledger.seed(listOf(Account(name = "TNG eWallet", type = AccountType.EWALLET), Account(name = "MAE", type = AccountType.BANK)))
        settings.setAutoLog(true)
        settings.setEnabledPackages(setOf(tng, mae))
    }

    private fun pendingItems() = runBlocking { db.pending().pending() }

    @Test fun sameNotificationTwiceGivesOnePending() = runBlocking {
        processor.process(tng, "You have paid RM6.80 to 7-Eleven.", now)
        processor.process(tng, "You have paid RM6.80 to 7-Eleven.", now.plusSeconds(10))
        assertThat(pendingItems()).hasSize(1)
        assertThat(ledger.categoryByKey("shopping")!!.id).isEqualTo(pendingItems().single().suggestedCategoryId)
    }

    @Test fun otpAndUnknownAppsLeaveNoTrace() = runBlocking {
        processor.process(tng, "Your OTP is 123456 for RM6.80", now)
        processor.process("com.whatsapp", "You have paid RM6.80 to 7-Eleven.", now)
        assertThat(db.pending().all()).isEmpty()
    }

    @Test fun fourthConfirmedVisitIsLoggedAutomatically() = runBlocking {
        settings.setAutoFamiliar(true)
        repeat(3) { i ->
            processor.process(tng, "You have paid RM${10 + i}.00 to ZUS Coffee.", now.plusSeconds(i * 600L))
            repo.confirm(pendingItems().single().id, null)
        }
        processor.process(tng, "You have paid RM15.00 to ZUS Coffee.", now.plusSeconds(4000))
        assertThat(pendingItems()).isEmpty()
        val auto = ledger.allTransactions().filter { it.isAuto }
        assertThat(auto).hasSize(1)
        assertThat(auto.single().amount).isEqualTo(1500)
        repo.undoAuto(auto.single().id)
        assertThat(ledger.allTransactions().count { it.isAuto }).isEqualTo(0)
    }

    @Test fun maeToTngReloadBecomesOneTransfer() = runBlocking {
        processor.process(mae, "Transfer of RM50.00 to your TNG eWallet was successful.", now)
        processor.process(tng, "Reload of RM 50.00 successful.", now.plusSeconds(90))
        val open = pendingItems()
        assertThat(open).hasSize(1)
        repo.confirm(open.single().id, null)
        val t = ledger.allTransactions().single()
        assertThat(t.type).isEqualTo(TxnType.TRANSFER)
        val accounts = ledger.allAccounts().associateBy { it.name }
        assertThat(t.accountId).isEqualTo(accounts["MAE"]!!.id)
        assertThat(t.toAccountId).isEqualTo(accounts["TNG eWallet"]!!.id)
    }

    @Test fun billPaymentIsMatchedAndMarkedPaid() = runBlocking {
        val today = LocalDate.now()
        val billId = db.bills().insert(Bill(name = "Unifi", amount = 12_900, dayOfMonth = today.dayOfMonth, categoryId = ledger.categoryByKey("bills")!!.id, startMonth = BillMath.ym(YearMonth.from(today))))
        processor.process(mae, "Payment of RM129.00 to UNIFI is successful.", now)
        val p = pendingItems().single()
        assertThat(p.matchedBillId).isEqualTo(billId)
        repo.confirm(p.id, null)
        assertThat(db.bills().paymentsFor(BillMath.ym(YearMonth.from(today))).single().billId).isEqualTo(billId)
    }

    @Test fun handLoggedAmountMakesPossibleDuplicate() = runBlocking {
        settings.setAutoFamiliar(true)
        val manual = ledger.saveTransaction(Transaction(type = TxnType.EXPENSE, amount = 4_590, categoryId = ledger.categoryByKey("transport")!!.id,
            accountId = ledger.allAccounts().first().id, date = LocalDate.now(), createdAt = Instant.now()))
        processor.process(mae, "Your card ending 1234 was used for RM 45.90 at SHELL JLN TUN RAZAK.", now)
        val p = pendingItems().single()
        assertThat(p.possibleDuplicateOf).isEqualTo(manual)
        assertThat(p.last4).isEqualTo("1234")
        assertThat(p.status).isEqualTo(PendingStatus.PENDING)
    }

    @Test fun manuallyPaidBillThenNotificationIsPossibleDuplicate() = runBlocking {
        settings.setAutoFamiliar(true)
        val today = LocalDate.now()
        val billId = db.bills().insert(Bill(name = "Unifi", amount = 12_900, dayOfMonth = today.dayOfMonth, categoryId = ledger.categoryByKey("bills")!!.id, startMonth = BillMath.ym(YearMonth.from(today))))
        bills.markPaid(billId)
        processor.process(mae, "Payment of RM129.00 to UNIFI is successful.", now)
        val p = pendingItems().single()
        assertThat(p.matchedBillId).isNull() // already paid this month
        assertThat(p.possibleDuplicateOf).isNotNull()
        assertThat(ledger.allTransactions()).hasSize(1) // only the manual one
    }
}
