package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.core.designsystem.MizanTheme
import com.example.feature.setup.SetupScreen
import com.example.feature.setup.SetupUiState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun setup_screen_screenshot() {
    composeTestRule.setContent {
      MizanTheme(forceRtl = true) {
        SetupScreen(
          appState = com.example.core.model.AppState.SignedOut,
          onGoogleSignInClick = {},
          onManualInviteSubmit = {},
          onRetryClick = {},
          onSignOutClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}


