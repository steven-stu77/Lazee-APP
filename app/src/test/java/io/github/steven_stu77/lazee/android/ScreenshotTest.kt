package io.github.steven_stu77.lazee.android

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.steven_stu77.lazee.MainActivity
import io.github.steven_stu77.lazee.core.data.AppDatabase
import io.github.steven_stu77.lazee.core.prefs.SettingsRepository
import io.github.steven_stu77.lazee.core.repo.LedgerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import javax.inject.Inject

/**
 * Starts the real app (Hilt graph, navigation, DataStore) on an in-memory database and
 * saves screenshots to app/build/screenshots. Doubles as an end-to-end smoke test.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [35], qualifiers = "zh-w393dp-h852dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var ledger: LedgerRepository
    @Inject lateinit var settings: SettingsRepository

    private val outDir = File("build/screenshots").apply { mkdirs() }

    @Before fun setUp() = hilt.inject()

    private var scenario: ActivityScenario<MainActivity>? = null

    /** Draws the activity's root view into a bitmap (works with Robolectric native graphics). */
    private fun shot(name: String) {
        compose.waitForIdle()
        scenario!!.onActivity { a ->
            val v = a.window.decorView
            val bmp = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
            v.draw(Canvas(bmp))
            File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun launch(): ActivityScenario<MainActivity> =
        ActivityScenario.launch(MainActivity::class.java).also { scenario = it; compose.waitForIdle() }

    private fun tap(text: String) {
        waitFor(text)
        val node = compose.onAllNodesWithText(text, useUnmergedTree = true)[0]
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    /** Bottom-bar labels also appear in page content; the bar is always the last match. */
    private fun tapTab(text: String) {
        val nodes = compose.onAllNodesWithText(text, useUnmergedTree = true)
        nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        compose.waitForIdle()
    }

    /** Room and DataStore load on background threads that Compose idling does not track. */
    @OptIn(ExperimentalTestApi::class)
    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = true, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun walkThroughScreens() {
        launch().use {
            compose.waitForIdle()
            shot("01-onboarding")
        }
        runBlocking { SampleData.load(db, ledger, settings) }
        launch().use {
            waitFor("茶餐室 早餐")
            shot("02-home")
            tap("1 笔来自通知的账单待确认"); waitFor("ZUS Coffee"); shot("10-pending")
            compose.onAllNodesWithContentDescription("返回")[0].performClick(); compose.waitForIdle()
            tapTab("统计"); shot("08-stats")
            tapTab("预算"); shot("09-budget")
            tapTab("我的"); shot("03-me")
            tap("通知自动记账"); waitFor("记到："); shot("11-autolog")
            compose.onAllNodesWithContentDescription("返回")[0].performClick(); compose.waitForIdle()
            tap("分类管理"); shot("04-categories")
            tap("+ 新增分类"); shot("05-new-category")
        }
        launch().use {
            compose.waitForIdle()
            tap("耳机 #网购"); shot("06-txn-detail")
        }
        launch().use {
            compose.waitForIdle()
            compose.onAllNodesWithContentDescription("记一笔")[0].performClick()
            compose.waitForIdle()
            for (k in "12+8.5") tap(k.toString())
            shot("07-add")
        }
    }
}
