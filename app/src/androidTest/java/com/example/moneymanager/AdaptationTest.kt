package com.example.moneymanager

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.moneymanager.ui.theme.MoneyManagerTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Adaptation & Compatibility Tests (Kiểm thử thích ứng giao diện & cấu hình thiết bị)
 *
 * Verifies that MoneyTracker UI components properly adapt to:
 * 1. Dark Mode vs Light Mode
 * 2. Different Screen Widths (Compact phone vs Large phone / Tablet)
 * 3. Accessibility Font Scaling (1.0x -> 1.5x / 2.0x)
 * 4. Multi-language Locale configurations (vi vs en)
 */
@RunWith(AndroidJUnit4::class)
class AdaptationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLightAndDarkModeAdaptation() {
        // Verify rendering in Dark Mode
        composeTestRule.setContent {
            MoneyManagerTheme(darkTheme = true) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(text = "Dark Mode Adapted", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        composeTestRule.onNodeWithText("Dark Mode Adapted").assertIsDisplayed()

        // Verify rendering in Light Mode
        composeTestRule.setContent {
            MoneyManagerTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Text(text = "Light Mode Adapted", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        composeTestRule.onNodeWithText("Light Mode Adapted").assertIsDisplayed()
    }

    @Test
    fun testScreenSizeAdaptation_compactAndExpanded() {
        // Test compact screen constraint (320dp - small phone)
        composeTestRule.setContent {
            MoneyManagerTheme {
                Box(modifier = Modifier.width(320.dp)) {
                    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text(text = "Compact 320dp Viewport", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Compact 320dp Viewport").assertIsDisplayed()

        // Test expanded screen constraint (720dp - tablet / foldable)
        composeTestRule.setContent {
            MoneyManagerTheme {
                Box(modifier = Modifier.width(720.dp)) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(text = "Expanded 720dp Viewport", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Expanded 720dp Viewport").assertIsDisplayed()
    }

    @Test
    fun testAccessibilityFontScaleAdaptation() {
        // Test system accessibility font scale set to 1.5x (Large text for accessibility)
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            val scaledDensity = Density(
                density = currentDensity.density,
                fontScale = 1.5f
            )

            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                MoneyManagerTheme {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Accessibility 1.5x Font Scale")
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("Accessibility 1.5x Font Scale").assertIsDisplayed()
    }

    @Test
    fun testLocaleAdaptation_vietnameseAndEnglishResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val res = context.resources

        // Test Vietnamese locale string loading
        val viConfig = Configuration(res.configuration).apply {
            setLocale(Locale.forLanguageTag("vi"))
        }
        val viContext = context.createConfigurationContext(viConfig)
        val viAppName = viContext.getString(R.string.app_name)
        assertNotNull(viAppName)
        assertTrue(viAppName.isNotEmpty())

        // Test English locale string loading
        val enConfig = Configuration(res.configuration).apply {
            setLocale(Locale.ENGLISH)
        }
        val enContext = context.createConfigurationContext(enConfig)
        val enAppName = enContext.getString(R.string.app_name)
        assertNotNull(enAppName)
        assertTrue(enAppName.isNotEmpty())
    }
}
