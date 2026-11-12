package io.github.steven_stu77.lazee.core.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account ORDER BY archived, sortOrder, id")
    fun observeAll(): Flow<List<Account>>

    @Query("SELECT * FROM Account ORDER BY sortOrder, id")
    suspend fun all(): List<Account>

    @Query("SELECT * FROM Account WHERE id = :id")
    suspend fun get(id: Long): Account?

    @Insert suspend fun insert(a: Account): Long
    @Update suspend fun update(a: Account)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category ORDER BY kind, sortOrder, id")
    fun observeAll(): Flow<List<Category>>

    @Query("SELECT * FROM Category ORDER BY kind, sortOrder, id")
    suspend fun all(): List<Category>

    @Query("SELECT * FROM Category WHERE id = :id")
    suspend fun get(id: Long): Category?

    @Query("SELECT * FROM Category WHERE builtInKey = :key LIMIT 1")
    suspend fun byKey(key: String): Category?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM Category")
    suspend fun maxSort(): Int

    @Insert suspend fun insert(c: Category): Long
    @Update suspend fun update(c: Category)
    @Update suspend fun updateAll(c: List<Category>)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM Txn WHERE date BETWEEN :from AND :to ORDER BY date DESC, createdAt DESC")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    @Query("SELECT * FROM Txn WHERE date BETWEEN :from AND :to ORDER BY date DESC, createdAt DESC")
    suspend fun range(from: LocalDate, to: LocalDate): List<Transaction>

    @Query("SELECT * FROM Txn ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM Txn ORDER BY date DESC, createdAt DESC")
    suspend fun all(): List<Transaction>

    @Query("SELECT * FROM Txn WHERE id = :id")
    fun observe(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM Txn WHERE id = :id")
    suspend fun get(id: Long): Transaction?

    @Query("SELECT * FROM Txn WHERE amount = :amount AND date >= :since AND type = 'EXPENSE'")
    suspend fun sameAmountSince(amount: Long, since: LocalDate): List<Transaction>

    @Query("SELECT MIN(date) FROM Txn")
    suspend fun firstDate(): LocalDate?

    /** Per-category spending in a range; refunds are negative expenses so they net out. */
    @Query(
        "SELECT categoryId, SUM(amount) AS total FROM Txn " +
            "WHERE type = 'EXPENSE' AND date BETWEEN :from AND :to GROUP BY categoryId ORDER BY total DESC",
    )
    suspend fun spendByCategory(from: LocalDate, to: LocalDate): List<CategoryTotal>

    @Query(
        "SELECT merchant, COUNT(*) AS count, SUM(amount) AS total FROM Txn " +
            "WHERE type = 'EXPENSE' AND merchant IS NOT NULL AND date BETWEEN :from AND :to " +
            "GROUP BY UPPER(merchant) ORDER BY total DESC LIMIT 5",
    )
    suspend fun topMerchants(from: LocalDate, to: LocalDate): List<MerchantTotal>

    @Query(
        "SELECT type, SUM(amount) AS total FROM Txn WHERE date BETWEEN :from AND :to " +
            "AND type != 'TRANSFER' GROUP BY type",
    )
    suspend fun totalsByType(from: LocalDate, to: LocalDate): List<TypeTotal>

    @Insert suspend fun insert(t: Transaction): Long
    @Update suspend fun update(t: Transaction)
    @Delete suspend fun delete(t: Transaction)

    @Query("DELETE FROM Txn WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<Long>)

    @Query("UPDATE Txn SET categoryId = :categoryId WHERE id IN (:ids) AND type != 'TRANSFER'")
    suspend fun setCategory(ids: List<Long>, categoryId: Long)
}

data class CategoryTotal(val categoryId: Long?, val total: Long)
data class MerchantTotal(val merchant: String, val count: Int, val total: Long)
data class TypeTotal(val type: TxnType, val total: Long)

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag WHERE name = :name")
    suspend fun byName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(t: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun link(l: TransactionTag)

    @Query("DELETE FROM TransactionTag WHERE transactionId = :txnId")
    suspend fun unlinkAll(txnId: Long)

    @Query("SELECT Tag.* FROM Tag JOIN TransactionTag ON Tag.id = TransactionTag.tagId WHERE transactionId = :txnId")
    suspend fun forTxn(txnId: Long): List<Tag>

    @Query("SELECT * FROM TransactionTag")
    fun observeLinks(): Flow<List<TransactionTag>>

    @Query("SELECT * FROM Tag") fun observeAll(): Flow<List<Tag>>
    @Query("SELECT * FROM Tag") suspend fun all(): List<Tag>
    @Query("SELECT * FROM TransactionTag") suspend fun allLinks(): List<TransactionTag>
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM Template ORDER BY id DESC") fun observeAll(): Flow<List<Template>>
    @Query("SELECT * FROM Template") suspend fun all(): List<Template>
    @Insert suspend fun insert(t: Template): Long
    @Delete suspend fun delete(t: Template)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM CategoryBudget") fun observeAll(): Flow<List<CategoryBudget>>
    @Query("SELECT * FROM CategoryBudget") suspend fun all(): List<CategoryBudget>
    @Upsert suspend fun upsert(b: CategoryBudget)
    @Query("DELETE FROM CategoryBudget WHERE categoryId = :categoryId") suspend fun delete(categoryId: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM SavingGoal WHERE archived = 0") fun observeGoals(): Flow<List<SavingGoal>>
    @Query("SELECT * FROM GoalContribution") fun observeContributions(): Flow<List<GoalContribution>>
    @Query("SELECT * FROM SavingGoal") suspend fun allGoals(): List<SavingGoal>
    @Query("SELECT * FROM GoalContribution") suspend fun allContributions(): List<GoalContribution>
    @Insert suspend fun insert(g: SavingGoal): Long
    @Update suspend fun update(g: SavingGoal)
    @Insert suspend fun contribute(c: GoalContribution): Long
}

@Dao
interface BillDao {
    @Query("SELECT * FROM Bill ORDER BY dayOfMonth") fun observeAll(): Flow<List<Bill>>
    @Query("SELECT * FROM Bill") suspend fun all(): List<Bill>
    @Query("SELECT * FROM Bill WHERE active = 1") suspend fun active(): List<Bill>
    @Query("SELECT * FROM Bill WHERE id = :id") suspend fun get(id: Long): Bill?
    @Insert suspend fun insert(b: Bill): Long
    @Update suspend fun update(b: Bill)
    @Delete suspend fun delete(b: Bill)

    @Query("SELECT * FROM BillPayment") fun observePayments(): Flow<List<BillPayment>>
    @Query("SELECT * FROM BillPayment") suspend fun allPayments(): List<BillPayment>
    @Query("SELECT * FROM BillPayment WHERE yearMonth = :ym") suspend fun paymentsFor(ym: Int): List<BillPayment>
    @Query("SELECT COUNT(*) FROM BillPayment WHERE billId = :billId") suspend fun paidCount(billId: Long): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertPayment(p: BillPayment): Long
    @Query("DELETE FROM BillPayment WHERE transactionId = :txnId") suspend fun deletePaymentForTxn(txnId: Long)
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM Loan ORDER BY settled, startDate DESC") fun observeAll(): Flow<List<Loan>>
    @Query("SELECT * FROM LoanRepayment") fun observeRepayments(): Flow<List<LoanRepayment>>
    @Query("SELECT * FROM Loan") suspend fun all(): List<Loan>
    @Query("SELECT * FROM LoanRepayment") suspend fun allRepayments(): List<LoanRepayment>
    @Query("SELECT * FROM Loan WHERE id = :id") suspend fun get(id: Long): Loan?
    @Insert suspend fun insert(l: Loan): Long
    @Update suspend fun update(l: Loan)
    @Insert suspend fun insertRepayment(r: LoanRepayment): Long
}

@Dao
interface PendingDao {
    @Query("SELECT * FROM PendingTxn WHERE status = 'PENDING' ORDER BY postedAt DESC")
    fun observePending(): Flow<List<PendingTxn>>

    @Query("SELECT * FROM PendingTxn WHERE status = 'PENDING'") suspend fun pending(): List<PendingTxn>
    @Query("SELECT * FROM PendingTxn WHERE id = :id") suspend fun get(id: Long): PendingTxn?
    @Query("SELECT * FROM PendingTxn WHERE postedAt >= :sinceMillis") suspend fun since(sinceMillis: Long): List<PendingTxn>
    @Query("SELECT * FROM PendingTxn") suspend fun all(): List<PendingTxn>

    /** Returns -1 when the dedupe key already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(p: PendingTxn): Long
    @Update suspend fun update(p: PendingTxn)

    @Query("SELECT COUNT(*) FROM PendingTxn WHERE dedupeKey = :key") suspend fun countKey(key: String): Int
}

@Dao
interface MerchantRuleDao {
    @Query("SELECT * FROM MerchantRule WHERE merchantKey = :key") suspend fun get(key: String): MerchantRule?
    @Query("SELECT * FROM MerchantRule") suspend fun all(): List<MerchantRule>
    @Upsert suspend fun upsert(r: MerchantRule)
}
