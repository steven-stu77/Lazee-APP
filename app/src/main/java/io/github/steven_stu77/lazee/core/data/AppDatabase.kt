package io.github.steven_stu77.lazee.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 1. Every future schema change needs a Migration — never destructive fallback,
 * because that would wipe the user's records.
 */
@Database(
    entities = [
        Account::class, Category::class, Transaction::class, Tag::class, TransactionTag::class,
        Template::class, CategoryBudget::class, SavingGoal::class, GoalContribution::class,
        Bill::class, BillPayment::class, Loan::class, LoanRepayment::class, PendingTxn::class,
        MerchantRule::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accounts(): AccountDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun tags(): TagDao
    abstract fun templates(): TemplateDao
    abstract fun budgets(): BudgetDao
    abstract fun goals(): GoalDao
    abstract fun bills(): BillDao
    abstract fun loans(): LoanDao
    abstract fun pending(): PendingDao
    abstract fun merchantRules(): MerchantRuleDao
    abstract fun restore(): RestoreDao

    companion object {
        const val NAME = "lazee.db"
    }
}
