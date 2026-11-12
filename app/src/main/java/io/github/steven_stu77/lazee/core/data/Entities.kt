package io.github.steven_stu77.lazee.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

// All money columns are Long cents (RM 6.50 = 650).

@Serializable
@Entity
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AccountType,
    val openingBalance: Long = 0,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Serializable
@Entity
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val builtInKey: String? = null,
    val customName: String? = null,
    val kind: CategoryKind,
    val colorIndex: Int,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
)

@Serializable
@Entity(
    tableName = "Txn",
    indices = [Index("date"), Index("categoryId"), Index("accountId"), Index("billId"), Index("loanId")],
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TxnType,
    val amount: Long,
    val categoryId: Long? = null,
    val accountId: Long,
    val toAccountId: Long? = null,
    @Contextual val date: LocalDate,
    @Contextual val createdAt: Instant,
    val note: String? = null,
    val merchant: String? = null,
    val source: TxnSource = TxnSource.MANUAL,
    val isAuto: Boolean = false,
    val billId: Long? = null,
    val loanId: Long? = null,
    val photoPath: String? = null,
)

@Serializable
@Entity(indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

@Serializable
@Entity(primaryKeys = ["transactionId", "tagId"], indices = [Index("tagId")])
data class TransactionTag(val transactionId: Long, val tagId: Long)

@Serializable
@Entity
data class Template(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxnType,
    val amount: Long? = null,
    val categoryId: Long? = null,
    val accountId: Long,
    val note: String? = null,
)

@Serializable
@Entity(indices = [Index(value = ["categoryId"], unique = true)])
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val amountLimit: Long,
)

@Serializable
@Entity
data class SavingGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    @Contextual val deadline: LocalDate? = null,
    val colorIndex: Int = 4,
    val archived: Boolean = false,
)

@Serializable
@Entity(indices = [Index("goalId")])
data class GoalContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Long,
    @Contextual val date: LocalDate,
)

@Serializable
@Entity
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    val dayOfMonth: Int,
    val remindDaysBefore: Int = 2,
    val mode: BillMode = BillMode.REMIND,
    val categoryId: Long,
    val accountId: Long? = null,
    val isSubscription: Boolean = false,
    val totalInstallments: Int? = null,
    /** First month as yyyyMM, e.g. 202611. */
    val startMonth: Int,
    val active: Boolean = true,
)

@Serializable
@Entity(indices = [Index(value = ["billId", "yearMonth"], unique = true)])
data class BillPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    /** yyyyMM */
    val yearMonth: Int,
    val transactionId: Long? = null,
    val method: PaymentMethod,
)

@Serializable
@Entity
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val direction: LoanDirection,
    val principal: Long,
    val annualRatePercent: Double? = null,
    @Contextual val startDate: LocalDate,
    @Contextual val dueDate: LocalDate? = null,
    val accountId: Long,
    val settled: Boolean = false,
)

@Serializable
@Entity(indices = [Index("loanId")])
data class LoanRepayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val amount: Long,
    @Contextual val date: LocalDate,
    val transactionId: Long,
)

/** A parsed notification waiting for the user. Never holds the raw notification text. */
@Serializable
@Entity(indices = [Index(value = ["dedupeKey"], unique = true)])
data class PendingTxn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePackage: String,
    val merchant: String,
    val amount: Long,
    val direction: PendingDirection,
    @Contextual val postedAt: Instant,
    val suggestedCategoryId: Long? = null,
    val confidence: Confidence,
    val matchedBillId: Long? = null,
    val possibleDuplicateOf: Long? = null,
    val transferPairId: Long? = null,
    val status: PendingStatus = PendingStatus.PENDING,
    val dedupeKey: String,
    val isRefund: Boolean = false,
    val isPersonTransfer: Boolean = false,
    val last4: String? = null,
)

@Serializable
@Entity(indices = [Index(value = ["merchantKey"], unique = true)])
data class MerchantRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantKey: String,
    val categoryId: Long,
    val confirmCount: Int = 1,
    val alwaysAsk: Boolean = false,
)
