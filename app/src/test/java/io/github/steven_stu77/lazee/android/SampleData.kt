package io.github.steven_stu77.lazee.android

import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.CategoryBudget
import io.github.steven_stu77.lazee.core.data.Confidence
import io.github.steven_stu77.lazee.core.data.PendingDirection
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TxnSource
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.BillMath
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** Demo data shaped like the prototype's, relative to today. */
object SampleData {
    suspend fun load(db: AppDatabase, ledger: LedgerRepository, settings: SettingsRepository) {
        ledger.seed(
            listOf(
                Account(name = "现金", type = AccountType.CASH, openingBalance = 32_000),
                Account(name = "TNG eWallet", type = AccountType.EWALLET, openingBalance = 41_000),
                Account(name = "MAE", type = AccountType.BANK, openingBalance = 120_000),
            ),
        )
        val cats = ledger.allCategories().associateBy { it.builtInKey }
        val acc = ledger.allAccounts()
        val today = LocalDate.now()
        fun t(daysAgo: Long, cents: Long, cat: String, accIdx: Int, note: String? = null, merchant: String? = null, auto: Boolean = false, type: TxnType = TxnType.EXPENSE) =
            Transaction(type = type, amount = cents, categoryId = cats[cat]?.id, accountId = acc[accIdx].id,
                date = today.minusDays(daysAgo).coerceAtLeast(YearMonth.from(today).atDay(1)), createdAt = Instant.now().minusSeconds(daysAgo * 86_400 + cents),
                note = note, merchant = merchant, isAuto = auto, source = if (merchant != null) TxnSource.NOTIFICATION else TxnSource.MANUAL)
        listOf(
            t(0, 1250, "food", 0, note = "茶餐室 早餐"),
            t(0, 680, "shopping", 1, merchant = "7-Eleven", auto = true),
            t(0, 1590, "transport", 1, merchant = "Grab"),
            t(1, 4400, "shopping", 2, merchant = "Shopee", note = "耳机 #网购"),
            t(1, 900, "food", 0, note = "Kopitiam"),
            t(2, 25_000, "bills", 2, note = "宿舍网费"),
            t(3, 3_000, "fun", 1, merchant = "GSC"),
            t(4, 170_000, "allowance", 2, note = "生活费", type = TxnType.INCOME),
        ).forEach { ledger.saveTransaction(it) }
        db.budgets().upsert(CategoryBudget(categoryId = cats["food"]!!.id, amountLimit = 45_000))
        db.bills().insert(Bill(name = "Unifi", amount = 12_900, dayOfMonth = (today.dayOfMonth + 3).coerceAtMost(28), categoryId = cats["bills"]!!.id, startMonth = BillMath.ym(YearMonth.from(today))))
        db.pending().insert(
            PendingTxn(sourcePackage = "my.com.tngdigital.ewallet", merchant = "ZUS Coffee", amount = 1290, direction = PendingDirection.OUT,
                postedAt = Instant.now(), suggestedCategoryId = cats["food"]!!.id, confidence = Confidence.MEDIUM, dedupeKey = "k1"),
        )
        settings.setMonthlyBudget(150_000)
        settings.setOnboardingDone(true)
    }
}
