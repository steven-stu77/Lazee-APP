package io.github.steven_stu77.lazee.feature.autolog

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.R
import io.github.steven_stu77.lazee.core.data.PendingTxn
import io.github.steven_stu77.lazee.core.money.Money
import io.github.steven_stu77.lazee.core.notify.Notifier
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import io.github.steven_stu77.lazee.core.ui.Labels
import io.github.steven_stu77.lazee.feature.autolog.pending.PendingActionReceiver
import io.github.steven_stu77.lazee.nav.Routes
import javax.inject.Inject
import javax.inject.Singleton

/** "7-Eleven RM 6.80 → Shopping [Confirm] [Change] [Ignore]" and "Auto-logged … [Undo]". Lock screen never shows amounts. */
@Singleton
class AutoLogNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: Notifier,
    private val config: AutoLogConfig,
    private val ledger: LedgerRepository,
) {
    private fun action(action: String, id: Long, code: Int): PendingIntent {
        val i = Intent(context, PendingActionReceiver::class.java).setAction(action).putExtra(PendingActionReceiver.EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun appName(pkg: String) = config.engine.config(pkg)?.displayName ?: pkg

    suspend fun showPending(p: PendingTxn) {
        val cat = Labels.categoryName(context, p.suggestedCategoryId?.let { ledger.category(it) })
        val title = context.getString(R.string.an_pending_title, p.merchant, Money.format(p.amount), cat)
        val body = context.getString(R.string.an_from_app, appName(p.sourcePackage))
        val notifId = Notifier.ID_AUTOLOG_BASE + (p.id % 50_000).toInt()
        val b = notifier.builder(Notifier.Channel.AUTOLOG, title, body, context.getString(R.string.an_public))
            .setContentIntent(notifier.openApp(Routes.PENDING, notifId))
            .addAction(0, context.getString(R.string.confirm), action(PendingActionReceiver.CONFIRM, p.id, notifId))
            .addAction(0, context.getString(R.string.an_change), notifier.openApp(Routes.PENDING, notifId + 1))
            .addAction(0, context.getString(R.string.an_ignore), action(PendingActionReceiver.IGNORE, p.id, notifId + 2))
        notifier.post(notifId, b)
    }

    fun cancelPending(id: Long) = notifier.cancel(Notifier.ID_AUTOLOG_BASE + (id % 50_000).toInt())

    suspend fun showAutoLogged(txnId: Long, p: PendingTxn, categoryId: Long?) {
        val cat = Labels.categoryName(context, categoryId?.let { ledger.category(it) })
        val notifId = Notifier.ID_AUTOLOG_BASE + 50_000 + (txnId % 40_000).toInt()
        val b = notifier.builder(
            Notifier.Channel.AUTOLOG,
            context.getString(R.string.an_auto_title, p.merchant, Money.format(p.amount)),
            context.getString(R.string.an_auto_body, cat),
            context.getString(R.string.an_public),
        ).setSilent(true)
            .setContentIntent(notifier.openApp(Routes.txn(txnId), notifId))
            .addAction(0, context.getString(R.string.an_undo), action(PendingActionReceiver.UNDO, txnId, notifId))
        notifier.post(notifId, b)
    }

    fun cancelAuto(txnId: Long) = notifier.cancel(Notifier.ID_AUTOLOG_BASE + 50_000 + (txnId % 40_000).toInt())
}
