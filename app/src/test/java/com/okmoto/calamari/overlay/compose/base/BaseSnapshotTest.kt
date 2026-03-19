package com.okmoto.calamari.overlay.compose.base

import androidx.compose.runtime.Composable
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.provideRoborazziContext
import org.junit.After
import org.junit.Ignore
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(manifest = Config.NONE, sdk = [35])
open class BaseSnapshotTest {

    // This is a helper base class for snapshot tests.
    // Gradle/JUnit may still attempt to run it because it matches `*Test` naming,
    // but it doesn't declare any @Test methods.
    // An ignored dummy test avoids `InvalidTestClassError`.
    @Ignore("Base class for snapshot helpers; subclasses contain the real assertions.")
    @Test
    fun baseSnapshotHelper_noop() = Unit

    companion object {
        private const val SCREENSHOT_DIR = "src/test/screenshots/"
    }

    @get:Rule
    val testNameRule = TestName()

    @Before
    fun disableAnimations() {
        setAnimatorDurationScale(0f)
    }

    @After
    fun restoreAnimations() {
        setAnimatorDurationScale(1f)
    }

    @OptIn(ExperimentalRoborazziApi::class)
    fun captureImage(
        roborazziOptions: RoborazziOptions = provideRoborazziContext().options,
        content: @Composable () -> Unit,
    ) {
        captureRoboImage(
            filePath = screenshotPath(),
            roborazziOptions = roborazziOptions.copy(
                captureType = RoborazziOptions.CaptureType.Screenshot()
            ),
            content = content
        )
    }

    private fun screenshotPath(): String {
        val classDir = this::class.java.simpleName
        val methodName = testNameRule.methodName
        return "$SCREENSHOT_DIR$classDir/$methodName.png"
    }


    private fun setAnimatorDurationScale(scale: Float) {
        try {
            val clazz = Class.forName("android.animation.ValueAnimator")
            val method = clazz.getDeclaredMethod("setDurationScale", Float::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(null, scale)
        } catch (_: Throwable) {
            // Best-effort only; snapshots still work if reflection is unavailable.
        }
    }
}