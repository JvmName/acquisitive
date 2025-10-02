package dev.jvmname.acquisitive.di

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.logs.LogSqliteDriver
import dev.jvmname.acquisitive.BuildConfig
import dev.jvmname.acquisitive.db.CommentEntity
import dev.jvmname.acquisitive.db.CommentQueries
import dev.jvmname.acquisitive.db.StoryEntity
import dev.jvmname.acquisitive.db.StoryId
import dev.jvmname.acquisitive.db.StoryQueries
import dev.jvmname.acquisitive.repo.db.AcqDatabase
import dev.jvmname.acquisitive.repo.db.InstantColumnAdapter
import dev.jvmname.acquisitive.repo.db.IntLongColumnAdapter
import dev.jvmname.acquisitive.repo.db.ItemIdArrayColumnAdapter
import dev.jvmname.acquisitive.repo.db.ItemIdColumnAdapter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import logcat.logcat


@ContributesTo(AppScope::class)
interface DatabaseModule {

    @Provides
    @Suppress("KotlinConstantConditions")
    fun provideDriver(@AppContext context: Context): SqlDriver {
        val driver = AndroidSqliteDriver(
            schema = AcqDatabase.Schema,
            context = context,
            name = "acq.db",
            callback = object : AndroidSqliteDriver.Callback(AcqDatabase.Schema) {
                override fun onConfigure(db: SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            }
        )
        return if (BuildConfig.DEBUG) {
            LogSqliteDriver(driver, { logcat("SqliteDriver") { it } })
        } else driver;
    }

    @[Provides SingleIn(AppScope::class)]
    fun provideDatabase(driver: SqlDriver): AcqDatabase = AcqDatabase(
        driver = driver,
        StoryEntityAdapter = StoryEntity.Adapter(
            idAdapter = ItemIdColumnAdapter,
            fetchModeAdapter = EnumColumnAdapter(),
            typeAdapter = EnumColumnAdapter(),
            timeAdapter = InstantColumnAdapter,
            kidsAdapter = ItemIdArrayColumnAdapter,
            partsAdapter = ItemIdArrayColumnAdapter,
            scoreAdapter = IntLongColumnAdapter,
            descendantsAdapter = IntLongColumnAdapter,
            parentAdapter = ItemIdColumnAdapter,
            pollAdapter = ItemIdColumnAdapter,
            rankAdapter = IntLongColumnAdapter
        ),
        StoryIdAdapter = StoryId.Adapter(
            idAdapter = ItemIdColumnAdapter,
            fetchModeAdapter = EnumColumnAdapter(),
            rankAdapter = IntLongColumnAdapter,
        ),
        CommentEntityAdapter = CommentEntity.Adapter(
            idAdapter = ItemIdColumnAdapter,
            parentAdapter = ItemIdColumnAdapter,
            timeAdapter = InstantColumnAdapter,
            kidsAdapter = ItemIdArrayColumnAdapter,
            rankAdapter = IntLongColumnAdapter,
        ),
    )

    @Provides
    @SingleIn(AppScope::class)
    fun provideHnItemQueries(database: AcqDatabase): StoryQueries = database.storyQueries

    @[Provides SingleIn(AppScope::class)]
    fun providesHnItemIdQueries(database: AcqDatabase): CommentQueries = database.commentQueries
}