package io.github.steven_stu77.lazee.core.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.security.DbPassphrase
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context, settings: SettingsRepository): AppDatabase {
        System.loadLibrary("sqlcipher")
        // One-time read at startup; the key must exist before Room opens the file.
        val pass = runBlocking { DbPassphrase.get(settings) }
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .openHelperFactory(SupportOpenHelperFactory(pass))
            .build()
    }

    @Provides fun accounts(db: AppDatabase) = db.accounts()
    @Provides fun categories(db: AppDatabase) = db.categories()
    @Provides fun transactions(db: AppDatabase) = db.transactions()
    @Provides fun tags(db: AppDatabase) = db.tags()
    @Provides fun templates(db: AppDatabase) = db.templates()
    @Provides fun budgets(db: AppDatabase) = db.budgets()
    @Provides fun goals(db: AppDatabase) = db.goals()
    @Provides fun bills(db: AppDatabase) = db.bills()
    @Provides fun loans(db: AppDatabase) = db.loans()
    @Provides fun pending(db: AppDatabase) = db.pending()
    @Provides fun merchantRules(db: AppDatabase) = db.merchantRules()
}
