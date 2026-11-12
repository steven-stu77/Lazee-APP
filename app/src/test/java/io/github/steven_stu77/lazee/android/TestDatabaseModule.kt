package io.github.steven_stu77.lazee.android

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.data.DatabaseModule
import javax.inject.Singleton

/** Plain in-memory Room for JVM tests: SQLCipher's native library cannot load under Robolectric. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

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
