package io.github.steven_stu77.lazee.core.backup

import io.github.steven_stu77.lazee.core.data.Account
import io.github.steven_stu77.lazee.core.data.Bill
import io.github.steven_stu77.lazee.core.data.BillPayment
import io.github.steven_stu77.lazee.core.data.Category
import io.github.steven_stu77.lazee.core.data.CategoryBudget
import io.github.steven_stu77.lazee.core.data.GoalContribution
import io.github.steven_stu77.lazee.core.data.Loan
import io.github.steven_stu77.lazee.core.data.LoanRepayment
import io.github.steven_stu77.lazee.core.data.MerchantRule
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.data.SavingGoal
import io.github.steven_stu77.lazee.core.data.Tag
import io.github.steven_stu77.lazee.core.data.Template
import io.github.steven_stu77.lazee.core.data.Transaction
import io.github.steven_stu77.lazee.core.data.TransactionTag
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** User-facing settings kept in a backup (not device secrets such as the DB key). */
@Serializable
data class BackupSettings(
    val language: String = "",
    val theme: String = "green",
    val monthlyBudget: Long = 0,
    val autoLogEnabled: Boolean = false,
    val enabledPackages: Set<String> = emptySet(),
    val lazyQuickConfirm: Boolean = true,
    val lazyAutoFamiliar: Boolean = false,
    val nightlySummary: Boolean = true,
    val nightlySummaryMinutes: Int = 21 * 60,
    val budgetAlerts: Boolean = true,
    val packageAccounts: Set<String> = emptySet(),
)

/** Everything in data.json. Bump [formatVersion] when the shape changes. */
@Serializable
data class BackupData(
    val formatVersion: Int = 1,
    val createdAt: Long = 0,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val transactionTags: List<TransactionTag> = emptyList(),
    val templates: List<Template> = emptyList(),
    val budgets: List<CategoryBudget> = emptyList(),
    val goals: List<SavingGoal> = emptyList(),
    val contributions: List<GoalContribution> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val billPayments: List<BillPayment> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val repayments: List<LoanRepayment> = emptyList(),
    val pending: List<PendingTxn> = emptyList(),
    val merchantRules: List<MerchantRule> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
)

private object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

private object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilli())
    override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}

/** Zip layout: data.json + photos/<name>. */
object BackupFormat {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        serializersModule = SerializersModule {
            contextual(LocalDate::class, LocalDateSerializer)
            contextual(Instant::class, InstantSerializer)
        }
    }

    fun zip(data: BackupData, photos: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().also { out ->
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry("data.json"))
            z.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
            z.closeEntry()
            photos.forEach { (name, bytes) -> z.putNextEntry(ZipEntry("photos/$name")); z.write(bytes); z.closeEntry() }
        }
    }.toByteArray()

    fun unzip(bytes: ByteArray): Pair<BackupData, Map<String, ByteArray>> {
        var data: BackupData? = null
        val photos = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { z ->
            generateSequence { z.nextEntry }.forEach { e ->
                val content = z.readBytes()
                when {
                    e.name == "data.json" -> data = json.decodeFromString(BackupData.serializer(), content.decodeToString())
                    // Only plain file names: never let a crafted zip write outside the photos folder.
                    e.name.startsWith("photos/") && !e.name.contains("..") && e.name.count { it == '/' } == 1 -> photos[e.name.removePrefix("photos/")] = content
                }
            }
        }
        return (data ?: throw NotABackupException()) to photos
    }
}
