package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesRepository
import com.example.input.GamepadAction
import com.example.input.GamepadController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitários e Robolectric para validar o mapeamento de controles e a persistência via
 * DataStore do Game Hub.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GameHubCoreTest {

  @Test
  fun `gamepad key events map to expected console actions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val controller = GamepadController(context)

    assertEquals(
      GamepadAction.SELECT,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_A)),
    )
    assertEquals(
      GamepadAction.BACK,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B)),
    )
    assertEquals(
      GamepadAction.MENU_START,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START)),
    )
    assertEquals(
      GamepadAction.PERFORMANCE_SELECT,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_SELECT)),
    )
    assertEquals(
      GamepadAction.TAB_PREV,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_L1)),
    )
    assertEquals(
      GamepadAction.TAB_NEXT,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_R1)),
    )
    assertEquals(
      GamepadAction.NAV_RIGHT,
      controller.handleKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT)),
    )
  }

  @Test
  fun `datastore persists selected game packages and ram threshold`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = PreferencesRepository(context)

    repo.toggleGameSelection("com.example.gameone", true)
    repo.toggleGameSelection("com.example.gametwo", true)
    repo.setMinFreeRamThresholdMb(1536)

    val prefs = repo.userPreferencesFlow.first()
    assertTrue("com.example.gameone" in prefs.selectedGamePackages)
    assertTrue("com.example.gametwo" in prefs.selectedGamePackages)
    assertEquals(1536, prefs.minFreeRamThresholdMb)
  }
}
