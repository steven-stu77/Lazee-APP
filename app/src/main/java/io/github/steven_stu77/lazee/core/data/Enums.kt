package io.github.steven_stu77.lazee.core.data

enum class AccountType { CASH, BANK, EWALLET, CREDIT_CARD }
enum class CategoryKind { EXPENSE, INCOME }
enum class TxnType { EXPENSE, INCOME, TRANSFER }
enum class TxnSource { MANUAL, NOTIFICATION, BILL, LOAN }
enum class BillMode { REMIND, AUTO_RECORD }
enum class PaymentMethod { NOTIFICATION, MANUAL, AUTO }
enum class LoanDirection { LENT, BORROWED }
enum class PendingDirection { OUT, IN }
enum class Confidence { HIGH, MEDIUM, LOW }
enum class PendingStatus { PENDING, CONFIRMED, IGNORED }
