package io.github.steven_stu77.lazee.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

/** Bulk inserts that keep the original ids; used only when restoring a backup. */
@Dao
interface RestoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun accounts(l: List<Account>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun categories(l: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun transactions(l: List<Transaction>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun tags(l: List<Tag>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun transactionTags(l: List<TransactionTag>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun templates(l: List<Template>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun budgets(l: List<CategoryBudget>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun goals(l: List<SavingGoal>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun contributions(l: List<GoalContribution>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun bills(l: List<Bill>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun billPayments(l: List<BillPayment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun loans(l: List<Loan>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun repayments(l: List<LoanRepayment>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun pending(l: List<PendingTxn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun merchantRules(l: List<MerchantRule>)
}
