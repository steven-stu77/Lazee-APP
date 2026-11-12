package io.github.steven_stu77.lazee.core.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.steven_stu77.lazee.MainActivity
import io.github.steven_stu77.lazee.R
import javax.inject.Inject
import javax.inject.Singleton

/** Channels (spec §7) and a safe way to post. Anything with an amount is VISIBILITY_PRIVATE with a public version. */
@Singleton
class Notifier @Inject constructor(@ApplicationContext private val context: Context) {

    enum class Channel(val id: String, val nameRes: Int, val importance: Int) {
        AUTOLOG("autolog", R.string.channel_autolog, NotificationManager.IMPORTANCE_DEFAULT),
        BILLS("bills", R.string.channel_bills, NotificationManager.IMPORTANCE_HIGH),
        BUDGET("budget", R.string.channel_budget, NotificationManager.IMPORTANCE_DEFAULT),
        SUMMARY("summary", R.string.channel_summary, NotificationManager.IMPORTANCE_LOW),
        BACKUP("backup", R.string.channel_backup, NotificationManager.IMPORTANCE_LOW),
    }

    /** Called at startup and after a language change so channel names stay translated. */
    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        Channel.entries.forEach { nm.createNotificationChannel(NotificationChannel(it.id, context.getString(it.nameRes), it.importance)) }
    }

    fun canPost(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    /** Opens the app, optionally at a deep-link route (see MainActivity.EXTRA_ROUTE). */
    fun openApp(route: String? = null, requestCode: Int = 0): PendingIntent {
        val i = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(MainActivity.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(context, requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun builder(channel: Channel, title: String, text: String, publicTitle: String? = null): NotificationCompat.Builder {
        val b = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_stat_lazee)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
        if (publicTitle != null) {
            // Lock screen sees only this version: no amounts.
            b.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            b.setPublicVersion(
                NotificationCompat.Builder(context, channel.id).setSmallIcon(R.drawable.ic_stat_lazee)
                    .setContentTitle(context.getString(R.string.app_name)).setContentText(publicTitle).build(),
            )
        }
        return b
    }

    fun post(id: Int, b: NotificationCompat.Builder) {
        if (!canPost()) return
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; nothing to do.
        }
    }

    fun cancel(id: Int) = NotificationManagerCompat.from(context).cancel(id)

    companion object {
        const val ID_BUDGET = 2000
        const val ID_DAILY = 3000
        const val ID_MONTHLY = 3001
        const val ID_BACKUP = 4000
        const val ID_BILL_BASE = 10_000
        const val ID_AUTOLOG_BASE = 100_000
    }
}
