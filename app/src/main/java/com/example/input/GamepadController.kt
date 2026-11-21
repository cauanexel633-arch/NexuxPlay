package com.example.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.model.ControllerType
import com.example.model.GamepadState
import kotlin.math.abs

/** Ações semânticas unificadas mapeadas de controles Xbox, PS4 DualShock, PS5 DualSense e USB/BT. */
enum class GamepadAction(val feedbackLabel: String) {
  NAV_LEFT("Esquerda"),
  NAV_RIGHT("Direita"),
  NAV_UP("Cima"),
  NAV_DOWN("Baixo"),
  SELECT("A / ✕ Selecionar"),
  BACK("B / ◯ Voltar"),
  MENU_START("Start / Options → Menu"),
  PERFORMANCE_SELECT("Select / Share → Desempenho"),
  TAB_PREV("L1 / LB → Aba Anterior"),
  TAB_NEXT("R1 / RB → Próxima Aba"),
}

/**
 * Gerenciador de compatibilidade com gamepads Bluetooth e USB. Detecta controles conectados via
 * InputManager e traduz KeyEvent + MotionEvent (analógicos e D-Pad) em comandos de navegação.
 */
class GamepadController(context: Context) {

  private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
  private var lastAxisNavTimestampMs = 0L
  private var lastAxisAction: GamepadAction? = null

  private var deviceListener: InputManager.InputDeviceListener? = null

  /** Registra listener para detectar em tempo real quando um controle conecta ou desconecta. */
  fun startListening(onStateChanged: (GamepadState) -> Unit) {
    // Atualiza estado inicial imediatamente
    onStateChanged(detectConnectedGamepad())

    val listener =
      object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
          onStateChanged(detectConnectedGamepad())
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
          onStateChanged(detectConnectedGamepad())
        }

        override fun onInputDeviceChanged(deviceId: Int) {
          onStateChanged(detectConnectedGamepad())
        }
      }
    deviceListener = listener
    inputManager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
  }

  /** Remove o listener de dispositivos ao encerrar a Activity. */
  fun stopListening() {
    deviceListener?.let { inputManager.unregisterInputDeviceListener(it) }
    deviceListener = null
  }

  /** Procura dispositivos de entrada ativos que sejam gamepads ou joysticks. */
  fun detectConnectedGamepad(): GamepadState {
    val deviceIds = inputManager.inputDeviceIds
    for (id in deviceIds) {
      val device = inputManager.getInputDevice(id) ?: continue
      if (isGamepadDevice(device)) {
        val type = classifyController(device.name)
        return GamepadState(
          isConnected = true,
          deviceName = device.name.ifBlank { type.displayLabel },
          controllerType = type,
        )
      }
    }
    return GamepadState(isConnected = false, controllerType = ControllerType.NONE)
  }

  private fun isGamepadDevice(device: InputDevice): Boolean {
    if (device.isVirtual) return false
    val sources = device.sources
    val hasGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
    val hasJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    return hasGamepad || hasJoystick
  }

  private fun classifyController(name: String): ControllerType {
    val lower = name.lowercase()
    return when {
      lower.contains("dualsense") || lower.contains("ps5") -> ControllerType.DUALSENSE
      lower.contains("dualshock") ||
        lower.contains("wireless controller") ||
        lower.contains("sony") ||
        lower.contains("ps4") -> ControllerType.DUALSHOCK
      lower.contains("xbox") || lower.contains("microsoft") || lower.contains("x-box") ->
        ControllerType.XBOX
      else -> ControllerType.GENERIC
    }
  }

  /**
   * Processa eventos digitais de botões e D-Pad (KeyEvent). Retorna a GamepadAction correspondente
   * ou null se a tecla não pertencer ao mapeamento.
   */
  fun handleKeyEvent(event: KeyEvent): GamepadAction? {
    val device = event.device
    val fromGamepad = device != null && isGamepadDevice(device)
    val isDpadOrAction =
      when (event.keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_BUTTON_A,
        KeyEvent.KEYCODE_BUTTON_B,
        KeyEvent.KEYCODE_BUTTON_X,
        KeyEvent.KEYCODE_BUTTON_Y,
        KeyEvent.KEYCODE_BUTTON_L1,
        KeyEvent.KEYCODE_BUTTON_R1,
        KeyEvent.KEYCODE_BUTTON_START,
        KeyEvent.KEYCODE_BUTTON_SELECT,
        KeyEvent.KEYCODE_BUTTON_MODE -> true
        else -> false
      }

    if (!fromGamepad && !isDpadOrAction) return null
    if (event.action != KeyEvent.ACTION_DOWN) return null
    // Evita repetição excessiva ao segurar botões de ação
    val isNav =
      event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
        event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
        event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
        event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
    if (!isNav && event.repeatCount > 0) return null

    return when (event.keyCode) {
      KeyEvent.KEYCODE_DPAD_LEFT -> GamepadAction.NAV_LEFT
      KeyEvent.KEYCODE_DPAD_RIGHT -> GamepadAction.NAV_RIGHT
      KeyEvent.KEYCODE_DPAD_UP -> GamepadAction.NAV_UP
      KeyEvent.KEYCODE_DPAD_DOWN -> GamepadAction.NAV_DOWN
      KeyEvent.KEYCODE_BUTTON_A,
      KeyEvent.KEYCODE_DPAD_CENTER,
      KeyEvent.KEYCODE_ENTER -> GamepadAction.SELECT
      KeyEvent.KEYCODE_BUTTON_B -> GamepadAction.BACK
      KeyEvent.KEYCODE_BUTTON_START,
      KeyEvent.KEYCODE_MENU -> GamepadAction.MENU_START
      KeyEvent.KEYCODE_BUTTON_SELECT,
      KeyEvent.KEYCODE_BUTTON_MODE -> GamepadAction.PERFORMANCE_SELECT
      KeyEvent.KEYCODE_BUTTON_L1 -> GamepadAction.TAB_PREV
      KeyEvent.KEYCODE_BUTTON_R1 -> GamepadAction.TAB_NEXT
      else -> null
    }
  }

  /**
   * Processa movimentos dos analógicos esquerdos (AXIS_X, AXIS_Y) e chapéu D-Pad (AXIS_HAT_X,
   * AXIS_HAT_Y) via MotionEvent com zona morta (deadzone) e debounce suave.
   */
  fun handleMotionEvent(event: MotionEvent): GamepadAction? {
    if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK) {
      return null
    }
    if (event.action != MotionEvent.ACTION_MOVE) return null

    val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
    val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
    val stickX = event.getAxisValue(MotionEvent.AXIS_X)
    val stickY = event.getAxisValue(MotionEvent.AXIS_Y)

    val x = if (abs(hatX) > abs(stickX)) hatX else stickX
    val y = if (abs(hatY) > abs(stickY)) hatY else stickY

    val deadzone = 0.5f
    val candidate =
      when {
        abs(x) >= abs(y) && x < -deadzone -> GamepadAction.NAV_LEFT
        abs(x) >= abs(y) && x > deadzone -> GamepadAction.NAV_RIGHT
        abs(y) > abs(x) && y < -deadzone -> GamepadAction.NAV_UP
        abs(y) > abs(x) && y > deadzone -> GamepadAction.NAV_DOWN
        else -> {
          lastAxisAction = null
          return null
        }
      }

    val now = SystemClock.uptimeMillis()
    val minRepeatMs = if (candidate == lastAxisAction) 210L else 0L
    if (now - lastAxisNavTimestampMs < minRepeatMs) {
      return null
    }

    lastAxisAction = candidate
    lastAxisNavTimestampMs = now
    return candidate
  }
}
