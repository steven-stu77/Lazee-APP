package io.github.steven_stu77.lazee.core.repo

import androidx.room.withTransaction
import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.AccountType
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.BuiltIns
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.Tag
import io.github.steven_stu77.lazee.core.data.Template
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TransactionTag
import io.github.steven_stu77.lazee.core.data.TxnType
import io.github.steven_stu77.lazee.core.logic.Entry
import io.github.steven_stu77.lazee.core.logic.LedgerMath
import io.github.steven_stu77.lazee.core.logic.Tags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

data class AccountWithBalance(val account: Account, val balance: Long)

/** Accounts, categories, transactions, tags and templates. Screens reach the database only through repositories. */
@Singleton
class LedgerRepository @Inject constructor(private val db: AppDatabase) {
    private val accounts = db.accounts()
    private val categories = db.categories()
    private val txns = db.transactions()
    private val tags = db.tags()
    private val templates = db.templates()

    // ---- Seeding ----

    suspend fun isSeeded(): Boolean = categories.all().isNotEmpty()

    /** Creates built-in categories and the chosen accounts. Safe to call once from onboarding. */
    suspend fun seed(newAccounts: List<Account>) = db.withTransaction {
        if (categories.all().isEmpty()) BuiltIns.seedCategories().forEach { categories.insert(it) }
        newAccounts.forEachIndexed { i, a -> accounts.insert(a.copy(sortOrder = i)) }
    }

    // ---- Accounts ----

    fun observeAccounts(): Flow<List<Account>> = accounts.observeAll()

    fun observeAccountsWithBalance(): Flow<List<AccountWithBalance>> =
        combine(accounts.observeAll(), txns.observeAll()) { accs, all ->
            val entries = all.map { it.toEntry() }
            accs.map { AccountWithBalance(it, LedgerMath.balance(it.id, it.openingBalance, entries)) }
        }

    suspend fun account(id: Long) = accounts.get(id)
    suspend fun allAccounts() = accounts.all()
    suspend fun saveAccount(a: Account): Long =
        if (a.id == 0L) accounts.insert(a.copy(sortOrder = accounts.all().size)) else { accounts.update(a); a.id }

    // ---- Categories ----

    fun observeCategories(): Flow<List<Category>> = categories.observeAll()
    suspend fun allCategories() = categories.all()
    suspend fun category(id: Long) = categories.get(id)
    suspend fun categoryByKey(key: String) = categories.byKey(key)

    suspend fun saveCategory(c: Category): Long =
        if (c.id == 0L) categories.insert(c.copy(sortOrder = categories.maxSort() + 1)) else { categories.update(c); c.id }

    suspend fun reorderCategories(ordered: List<Category>) =
        categories.updateAll(ordered.mapIndexed { i, c -> c.copy(sortOrder = i) })

    // ---- Transactions ----

    fun observeMonth(ym: YearMonth): Flow<List<Transaction>> = txns.observeRange(ym.atDay(1), ym.atEndOfMonth())
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<Transaction>> = txns.observeRange(from, to)
    fun observeAllTransactions(): Flow<List<Transaction>> = txns.observeAll()
    fun observeTransaction(id: Long): Flow<Transaction?> = txns.observe(id)
    suspend fun transaction(id: Long) = txns.get(id)
    suspend fun range(from: LocalDate, to: LocalDate) = txns.range(from, to)
    suspend fun allTransactions() = txns.all()
    suspend fun firstDate(): LocalDate? = txns.firstDate()

    /** Inserts or updates a transaction and re-links the `#tags` found in its note. */
    suspend fun saveTransaction(t: Transaction): Long = db.withTransaction {
        val id = if (t.id == 0L) txns.insert(t) else { txns.update(t); t.id }
        tags.unlinkAll(id)
        for (name in Tags.parse(t.note)) {
            val tagId = tags.byName(name)?.id ?: tags.insert(Tag(name = name))
            tags.link(TransactionTag(id, tagId))
        }
        id
    }

    suspend fun deleteTransactions(ids: List<Long>) = db.withTransaction {
        ids.forEach { tags.unlinkAll(it); db.bills().deletePaymentForTxn(it) }
        txns.deleteIds(ids)
    }

    suspend fun setCategory(ids: List<Long>, categoryId: Long) = txns.setCategory(ids, categoryId)

    suspend fun tagsFor(txnId: Long): List<Tag> = tags.forTxn(txnId)

    /** txnId → tag names, for search and lists. */
    fun observeTagNames(): Flow<Map<Long, List<String>>> =
        combine(tags.observeAll(), tags.observeLinks()) { all, links ->
            val byId = all.associateBy { it.id }
            links.groupBy { it.transactionId }.mapValues { (_, l) -> l.mapNotNull { byId[it.tagId]?.name } }
        }

    // ---- Templates ----

    fun observeTemplates(): Flow<List<Template>> = templates.observeAll()
    suspend fun saveTemplate(t: Template) = templates.insert(t)
    suspend fun deleteTemplate(t: Template) = templates.delete(t)
}

fun Transaction.toEntry() = Entry(
    id = id,
    type = when (type) {
        TxnType.EXPENSE -> Entry.Kind.EXPENSE
        TxnType.INCOME -> Entry.Kind.INCOME
        TxnType.TRANSFER -> Entry.Kind.TRANSFER
    },
    amount = amount,
    date = date,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
)

/** Balance sign: credit cards show what you owe as a negative balance. */
fun Account.isLiability() = type == AccountType.CREDIT_CARD
