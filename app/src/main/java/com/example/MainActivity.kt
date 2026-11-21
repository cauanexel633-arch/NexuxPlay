package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.input.GamepadController
import com.example.model.HubTab
import com.example.ui.components.AppSplashScreen
import com.example.ui.components.GameLaunchingScreen
import com.example.ui.components.LowRamOptimizationDialog
import com.example.ui.components.Ps5AtmosphereBackground
import com.example.ui.components.Ps5GamepadFooterHints
import com.example.ui.components.Ps5TopNavigationTabs
import com.example.ui.components.Ps5TopStatusBar
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ManageGamesScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoreHighlightsScreen
import com.example.ui.theme.GameHubTheme
import com.example.viewmodel.GameHubViewModel
import com.example.viewmodel.HubUiEvent

/**
 * Activity principal do Game Hub (interface estilo PlayStation 5). Responsável por:
 * - Ativar modo edge-to-edge;
 * - Capturar eventos de Gamepad (KeyEvent e MotionEvent) para navegação completa por controle;
 * - Encaminhar Intents reais (lançamento de jogos, Play Store e telas de permissão do Android).
 */
class MainActivity : ComponentActivity() {

  private val viewModel: GameHubViewModel by viewModels()
  private lateinit var gamepadController: GamepadController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    gamepadController = GamepadController(this)
    gamepadController.startListening { state -> viewModel.updateGamepadState(state) }

    setContent {
      GameHubTheme {
        GameHubRootScreen(
          viewModel = viewModel,
          onLaunchIntent = { intent -> launchIntentSafely(intent) },
          onOpenUsageAccessSettings = { openUsageAccessSettings() },
          onOpenAppDetailsSettings = { openAppDetailsSettings() },
          onOpenBatterySettings = { openBatterySettings() },
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    // Atualiza status da permissão PACKAGE_USAGE_STATS e apps quando o usuário retorna das configs
    viewModel.refreshBackgroundProcesses()
  }

  override fun onDestroy() {
    super.onDestroy()
    gamepadController.stopListening()
  }

  /** Intercepta botões físicos de controles Bluetooth/USB (A/✕, B/◯, Start, Select, L1/R1, D-Pad). */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    val action = gamepadController.handleKeyEvent(event)
    if (action != null) {
      viewModel.onGamepadAction(action)
      return true
    }
    return super.dispatchKeyEvent(event)
  }

  /** Intercepta movimentos de analógicos e D-Pad analógico (MotionEvent) dos controles. */
  override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
    val action = gamepadController.handleMotionEvent(ev)
    if (action != null) {
      viewModel.onGamepadAction(action)
      return true
    }
    return super.dispatchGenericMotionEvent(ev)
  }

  private fun launchIntentSafely(intent: Intent) {
    runCatching { startActivity(intent) }
      .onFailure {
        // Fallback caso o link market:// não encontre Play Store instalada (abre no navegador)
        val dataString = intent.dataString
        if (dataString != null && dataString.startsWith("market://search?q=")) {
          val query = dataString.removePrefix("market://search?q=").substringBefore("&")
          val webIntent =
            Intent(
              Intent.ACTION_VIEW,
              Uri.parse("https://play.google.com/store/search?q=$query&c=apps"),
            )
          runCatching { startActivity(webIntent) }
            .onFailure {
              Toast.makeText(this, "Nenhum navegador ou loja disponível.", Toast.LENGTH_SHORT).show()
            }
        } else {
          Toast.makeText(this, "Não foi possível abrir o aplicativo.", Toast.LENGTH_SHORT).show()
        }
      }
  }

  private fun openUsageAccessSettings() {
    runCatching { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
      .onFailure { openAppDetailsSettings() }
  }

  private fun openAppDetailsSettings() {
    val intent =
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
      }
    runCatching { startActivity(intent) }
  }

  private fun openBatterySettings() {
    runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
      .onFailure { openAppDetailsSettings() }
  }
}

@Composable
private fun GameHubRootScreen(
  viewModel: GameHubViewModel,
  onLaunchIntent: (Intent) -> Unit,
  onOpenUsageAccessSettings: () -> Unit,
  onOpenAppDetailsSettings: () -> Unit,
  onOpenBatterySettings: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
  val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
  val gamepadState by viewModel.gamepadState.collectAsStateWithLifecycle()
  val libraryGames by viewModel.libraryGames.collectAsStateWithLifecycle()
  val filteredManageApps by viewModel.filteredManageApps.collectAsStateWithLifecycle()
  val backgroundProcesses by viewModel.backgroundProcesses.collectAsStateWithLifecycle()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is HubUiEvent.LaunchIntent -> onLaunchIntent(event.intent)
        is HubUiEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
      }
    }
  }

  // Botão Voltar do Android retorna para a Biblioteca quando estiver em abas secundárias
  BackHandler(enabled = !uiState.showSplash && uiState.currentTab != HubTab.LIBRARY) {
    viewModel.selectTab(HubTab.LIBRARY)
  }

  when {
    uiState.showSplash -> {
      AppSplashScreen(progress = uiState.splashProgress)
    }
    uiState.gameLaunchState != null -> {
      GameLaunchingScreen(launchState = uiState.gameLaunchState!!)
    }
    else -> {
      Ps5AtmosphereBackground {
        Scaffold(
          containerColor = Color.Transparent,
          snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
          topBar = {
            Column {
              Ps5TopStatusBar(telemetry = telemetry, gamepadState = gamepadState)
              Ps5TopNavigationTabs(
                currentTab = uiState.currentTab,
                isFocusOnTabBar = uiState.isFocusOnTabBar,
                onSelectTab = { viewModel.selectTab(it) },
              )
            }
          },
          bottomBar = { Ps5GamepadFooterHints(gamepadState = gamepadState) },
        ) { innerPadding ->
          Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
              targetState = uiState.currentTab,
              transitionSpec = { fadeIn() togetherWith fadeOut() },
              label = "ps5_tab_content",
            ) { tab ->
              when (tab) {
                HubTab.LIBRARY -> {
                  LibraryScreen(
                    games = libraryGames,
                    focusedIndex = uiState.focusedLibraryIndex,
                    isLoadingApps = uiState.isLoadingApps,
                    telemetry = telemetry,
                    showAutoStartGuide = userPrefs.showAutoStartGuideOnHome,
                    isAutoStartGuideExpanded = uiState.isAutoStartGuideExpanded,
                    onSelectIndex = { viewModel.selectLibraryIndex(it) },
                    onPlayGame = { viewModel.requestLaunchGame(it) },
                    onOpenManageGames = { viewModel.selectTab(HubTab.MANAGE_GAMES) },
                    onAutoDetectGames = { viewModel.autoSelectDetectedGames() },
                    onToggleAutoStartGuide = { viewModel.toggleAutoStartGuideExpanded() },
                    onDismissAutoStartGuide = { viewModel.dismissHomeAutoStartGuide() },
                    onOpenAppSettings = onOpenAppDetailsSettings,
                  )
                }

                HubTab.STORE -> {
                  StoreHighlightsScreen(
                    highlights = viewModel.storeHighlights,
                    focusedIndex = uiState.focusedStoreIndex,
                    onSelectHighlight = { viewModel.onStoreHighlightSelected(it) },
                    onRefreshLibrary = { viewModel.refreshInstalledApps() },
                  )
                }

                HubTab.PERFORMANCE -> {
                  PerformanceScreen(
                    telemetry = telemetry,
                    backgroundProcesses = backgroundProcesses,
                    userPreferences = userPrefs,
                    isOptimizing = uiState.isOptimizing,
                    lastResult = uiState.lastOptimizationResult,
                    onOptimizeNow = { viewModel.optimizeNow() },
                    onToggleProcess = { pkg, checked ->
                      viewModel.toggleBackgroundProcessSelection(pkg, checked)
                    },
                    onSelectAllProcesses = { viewModel.setAllBackgroundProcessesSelected(it) },
                    onRefreshProcesses = { viewModel.refreshBackgroundProcesses() },
                    onToggleAutoCheck = { viewModel.setAutoOptimizeCheckEnabled(it) },
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                  )
                }

                HubTab.MANAGE_GAMES -> {
                  ManageGamesScreen(
                    apps = filteredManageApps,
                    selectedPackages = userPrefs.selectedGamePackages,
                    searchQuery = uiState.manageSearchQuery,
                    onlyGamesFilter = uiState.manageOnlyGamesFilter,
                    focusedIndex = uiState.focusedManageIndex,
                    onSearchQueryChange = { viewModel.updateManageSearchQuery(it) },
                    onFilterOnlyGamesChange = { viewModel.setManageOnlyGamesFilter(it) },
                    onToggleGame = { pkg, checked -> viewModel.toggleGameInLibrary(pkg, checked) },
                    onAutoDetectGames = { viewModel.autoSelectDetectedGames() },
                    onRefreshApps = { viewModel.refreshInstalledApps() },
                  )
                }

                HubTab.SETTINGS -> {
                  SettingsScreen(
                    userPreferences = userPrefs,
                    telemetry = telemetry,
                    gamepadState = gamepadState,
                    onToggleAutoOptimizeCheck = { viewModel.setAutoOptimizeCheckEnabled(it) },
                    onChangeMinFreeRamMb = { viewModel.setMinFreeRamThresholdMb(it) },
                    onToggleAutoBoot = { viewModel.setAutoBootEnabled(it) },
                    onRestoreHomeGuide = { viewModel.restoreHomeAutoStartGuide() },
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                    onOpenAppDetailsSettings = onOpenAppDetailsSettings,
                    onOpenBatterySettings = onOpenBatterySettings,
                  )
                }
              }
            }
          }
        }

        // Modal de sugestão de otimização quando RAM livre estiver abaixo do limite
        val prompt = uiState.lowRamPrompt
        if (prompt != null) {
          LowRamOptimizationDialog(
            promptState = prompt,
            focusedOption = uiState.focusedLowRamOption,
            onOptimizeAndPlay = { viewModel.confirmLowRamPrompt(optimizeFirst = true) },
            onPlayAnyway = { viewModel.confirmLowRamPrompt(optimizeFirst = false) },
            onCancel = { viewModel.dismissLowRamPrompt() },
          )
        }
      }
    }
  }
}
