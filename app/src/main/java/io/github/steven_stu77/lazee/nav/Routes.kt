package io.github.steven_stu77.lazee.nav

/** String routes for Navigation Compose. Arguments are plain path/query params. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STATS = "stats"
    const val BUDGET = "budget"
    const val ME = "me"
    const val ADD = "add?txnId={txnId}&copy={copy}&categoryId={categoryId}"
    fun add(txnId: Long = 0, copy: Boolean = false, categoryId: Long = 0) = "add?txnId=$txnId&copy=$copy&categoryId=$categoryId"
    const val TXN = "txn/{id}"
    fun txn(id: Long) = "txn/$id"
    const val SEARCH = "search"
    const val CATEGORIES = "categories"
    const val NEW_CATEGORY = "newCategory?kind={kind}&parentId={parentId}&editId={editId}"
    fun newCategory(kind: String = "EXPENSE", parentId: Long = 0, editId: Long = 0) = "newCategory?kind=$kind&parentId=$parentId&editId=$editId"
    const val CATEGORY_DETAIL = "categoryDetail/{id}?ym={ym}"
    fun categoryDetail(id: Long, ym: Int) = "categoryDetail/$id?ym=$ym"
    const val YEAR = "year/{year}"
    fun year(y: Int) = "year/$y"
    const val ACCOUNT = "account/{id}"
    fun account(id: Long) = "account/$id"
    const val ACCOUNT_EDIT = "accountEdit/{id}"
    fun accountEdit(id: Long = 0) = "accountEdit/$id"
    const val PENDING = "pending"
    const val AUTOLOG = "autolog"
    const val PARSER_TEST = "parserTest"
    const val BILLS = "bills"
    const val BILL_EDIT = "billEdit/{id}"
    fun billEdit(id: Long = 0) = "billEdit/$id"
    const val LOANS = "loans"
    const val BACKUP = "backup"
    const val LOOK = "look"
    const val REMINDERS = "reminders"
    const val ABOUT = "about"

    val tabs = setOf(HOME, STATS, BUDGET, ME)
}
