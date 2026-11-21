package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap

/** Abas principais da interface estilo PlayStation 5. */
enum class HubTab(val title: String) {
  LIBRARY("Biblioteca"),
  STORE("Loja/Destaques"),
  PERFORMANCE("Desempenho"),
  MANAGE_GAMES("Gerenciar Jogos"),
  SETTINGS("Configurações"),
}

/** Representa um aplicativo ou jogo instalado no aparelho via PackageManager. */
data class InstalledAppInfo(
  val packageName: String,
  val appName: String,
  val isGame: Boolean,
  val versionName: String,
  val icon: ImageBitmap?,
)

/** Representa um processo/app em segundo plano elegível para otimização (não sistema). */
data class BackgroundProcessInfo(
  val packageName: String,
  val appName: String,
  val icon: ImageBitmap?,
  val lastTimeUsedMs: Long,
  val sourceLabel: String,
  val isSelectedToKill: Boolean = true,
)

/** Status de rede atual exibido na barra superior. */
enum class NetworkConnectionType(val label: String) {
  WIFI("Wi-Fi"),
  CELLULAR("Dados Móveis"),
  ETHERNET("Cabo / Ethernet"),
  OFFLINE("Sem conexão"),
}

/** Telemetria em tempo real de CPU, RAM, temperatura, bateria e permissões. */
data class SystemTelemetry(
  val ramTotalMb: Long = 0L,
  val ramAvailableMb: Long = 0L,
  val ramUsedMb: Long = 0L,
  val ramUsagePercent: Int = 0,
  val cpuUsagePercent: Int = 0,
  val cpuMetricSource: String = "Medindo...",
  val temperatureCelsius: Float = 0f,
  val batteryPercent: Int = 100,
  val isCharging: Boolean = false,
  val networkType: NetworkConnectionType = NetworkConnectionType.WIFI,
  val hasUsageStatsPermission: Boolean = false,
  val currentTimeFormatted: String = "--:--",
)

/** Resultado da última execução de otimização de memória e cache. */
data class OptimizationResult(
  val freedMb: Long,
  val killedCount: Int,
  val clearedCacheKb: Long,
  val timestampMs: Long,
)

/** Tipo de controle conectado via Bluetooth ou USB. */
enum class ControllerType(val displayLabel: String) {
  NONE("Sem controle"),
  DUALSENSE("PS5 DualSense"),
  DUALSHOCK("PS4 DualShock"),
  XBOX("Xbox Controller"),
  GENERIC("Gamepad USB/BT"),
}

/** Estado atual do gamepad detectado no dispositivo. */
data class GamepadState(
  val isConnected: Boolean = false,
  val deviceName: String = "",
  val controllerType: ControllerType = ControllerType.NONE,
  val lastButtonFeedback: String = "",
)

/** Card de destaque exibido na aba Loja/Destaques. */
data class StoreHighlight(
  val id: String,
  val title: String,
  val subtitle: String,
  val genre: String,
  val searchQuery: String,
  @DrawableRes val imageRes: Int,
  val isHero: Boolean = false,
)

/** Estado do diálogo de sugestão de otimização antes de abrir um jogo. */
data class LowRamPromptState(
  val game: InstalledAppInfo,
  val availableRamMb: Long,
  val thresholdRamMb: Int,
)

/** Estado da tela de carregamento ao iniciar um jogo da biblioteca. */
data class GameLaunchState(
  val game: InstalledAppInfo,
  val shouldOptimize: Boolean,
  val statusMessage: String,
  val progress: Float,
)
