package io.github.steven_stu77.lazee.core.data

/** Built-in categories: key → (kind, colorIndex). Display names come from string resources. */
object BuiltIns {
    val expense = listOf(
        "food" to 0, "transport" to 1, "shopping" to 2, "fun" to 3,
        "bills" to 4, "study" to 5, "gifts" to 6, "other" to 7,
    )
    val income = listOf("allowance" to 4, "parttime" to 4, "giftmoney" to 6, "other_income" to 7)

    const val OTHER = "other"
    const val BILLS = "bills"

    fun seedCategories(): List<Category> =
        expense.mapIndexed { i, (k, c) -> Category(builtInKey = k, kind = CategoryKind.EXPENSE, colorIndex = c, sortOrder = i) } +
            income.mapIndexed { i, (k, c) -> Category(builtInKey = k, kind = CategoryKind.INCOME, colorIndex = c, sortOrder = 100 + i) }
}
