package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.InstalledAppsRepository
import com.example.data.PreferencesRepository
import com.example.data.SystemPerformanceRepository
import com.example.data.UserPreferences
import com.example.input.GamepadAction
import com.example.model.BackgroundProcessInfo
import com.example.model.GameLaunchState
import com.example.model.GamepadState
import com.example.model.HubTab
import com.example.model.InstalledAppInfo
import com.example.model.LowRamPromptState
import com.example.model.OptimizationResult
import com.example.model.StoreHighlight
import com.example.model.SystemTelemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Eventos únicos enviados para a Activity (abrir jogo, Play Store ou mensagem de feedback). */
sealed interface HubUiEvent {
  data class LaunchIntent(val intent: Intent) : HubUiEvent

  data class ShowToast(val message: String) : HubUiEvent
}

/** Estado completo da interface principal do Game Hub. */
data class GameHubUiState(
  val showSplash: Boolean = true,
  val splashProgress: Float = 0f,
  val currentTab: HubTab = HubTab.LIBRARY,
  val isFocusOnTabBar: Boolean = false,
  val focusedLibraryIndex: Int = 0,
  val focusedStoreIndex: Int = 0,
  val focusedManageIndex: Int = 0,
  val focusedLowRamOption: Int = 0,
  val isLoadingApps: Boolean = true,
  val manageSearchQuery: String = "",
  val manageOnlyGamesFilter: Boolean = false,
  val isOptimizing: Boolean = false,
  val lastOptimizationResult: OptimizationResult? = null,
  val lowRamPrompt: LowRamPromptState? = null,
  val gameLaunchState: GameLaunchState? = null,
  val isAutoStartGuideExpanded: Boolean = false,
)

/**
 * ViewModel central do Game Hub. Coordena o carregamento de jogos instalados, persistência via
 * DataStore, telemetria em tempo real, otimização de RAM/cache e navegação por gamepad.
 */
class GameHubViewModel(application: Application) : AndroidViewModel(application) {

  private val preferencesRepo = PreferencesRepository(application)
  private val installedAppsRepo = InstalledAppsRepository(application)
  private val performanceRepo = SystemPerformanceRepository(application, installedAppsRepo)

  private val _uiState = MutableStateFlow(GameHubUiState())
  val uiState: StateFlow<GameHubUiState> = _uiState.asStateFlow()

  private val _allInstalledApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
  val allInstalledApps: StateFlow<List<InstalledAppInfo>> = _allInstalledApps.asStateFlow()

  private val _telemetry = MutableStateFlow(SystemTelemetry())
  val telemetry: StateFlow<SystemTelemetry> = _telemetry.asStateFlow()

  private val _backgroundProcesses = MutableStateFlow<List<BackgroundProcessInfo>>(emptyList())
  val backgroundProcesses: StateFlow<List<BackgroundProcessInfo>> =
    _backgroundProcesses.asStateFlow()

  private val _gamepadState = MutableStateFlow(GamepadState())
  val gamepadState: StateFlow<GamepadState> = _gamepadState.asStateFlow()

  private val _events = MutableSharedFlow<HubUiEvent>()
  val events: SharedFlow<HubUiEvent> = _events.asSharedFlow()

  val userPreferences: StateFlow<UserPreferences> =
    preferencesRepo.userPreferencesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = UserPreferences(),
    )

  /** Jogos selecionados pelo usuário e persistidos no DataStore para aparecer na Biblioteca. */
  val libraryGames: StateFlow<List<InstalledAppInfo>> =
    combine(_allInstalledApps, userPreferences) { apps, prefs ->
        apps.filter { it.packageName in prefs.selectedGamePackages }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

  /** Lista filtrada de apps instalados exibida na aba "Gerenciar Jogos". */
  val filteredManageApps: StateFlow<List<InstalledAppInfo>> =
    combine(_allInstalledApps, _uiState) { apps, state ->
        apps.filter { app ->
          val matchesFilter = !state.manageOnlyGamesFilter || app.isGame
          val q = state.manageSearchQuery.trim()
          val matchesQuery =
            q.isEmpty() ||
              app.appName.contains(q, ignoreCase = true) ||
              app.packageName.contains(q, ignoreCase = true)
          matchesFilter && matchesQuery
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
      )

  /** Destaques curados para a aba "Loja/Destaques" com redirecionamento real à Play Store. */
  val storeHighlights: List<StoreHighlight> =
    listOf(
      StoreHighlight(
        id = "hero_cyber",
        title = "Nova Geração Mobile",
        subtitle = "Explore jogos compatíveis com DualSense e Xbox Controller na Play Store",
        genre = "Destaque • Suporte a Controle",
        searchQuery = "controller supported games",
        imageRes = R.drawable.img_store_hero_cyber,
        isHero = true,
      ),
      StoreHighlight(
        id = "rpg_shadow",
        title = "Ação & RPG Cinematográfico",
        subtitle = "Títulos épicos de mundo aberto e combate em tempo real",
        genre = "Ação • RPG",
        searchQuery = "action rpg offline game",
        imageRes = R.drawable.img_store_cover_rpg,
      ),
      StoreHighlight(
        id = "racing_apex",
        title = "Velocidade & Simulação",
        subtitle = "Corridas arcade e simuladores de alta taxa de quadros",
        genre = "Corrida • 60+ FPS",
        searchQuery = "racing simulator gamepad game",
        imageRes = R.drawable.img_store_cover_racing,
      ),
    )

  init {
    runSplashSequence()
    refreshInstalledApps()
    startTelemetryLoop()
  }

  /** Anima a tela de abertura "GAME HUB" enquanto carrega aplicativos e telemetria iniciais. */
  private fun runSplashSequence() {
    viewModelScope.launch {
      val steps = 25
      for (i in 1..steps) {
        delay(55L)
        _uiState.update { it.copy(splashProgress = i / steps.toFloat()) }
      }
      delay(120L)
      _uiState.update { it.copy(showSplash = false) }
    }
  }

  /** Recarrega todos os aplicativos lançáveis do aparelho via PackageManager. */
  fun refreshInstalledApps() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingApps = true) }
      val apps = installedAppsRepo.loadInstalledLaunchableApps()
      _allInstalledApps.value = apps
      _uiState.update { it.copy(isLoadingApps = false) }
      refreshBackgroundProcesses()
    }
  }

  /** Atualiza a lista de processos em segundo plano mantendo as escolhas do usuário. */
  fun refreshBackgroundProcesses() {
    viewModelScope.launch {
      val existingSelections =
        _backgroundProcesses.value.associate { it.packageName to it.isSelectedToKill }
      val processes = performanceRepo.loadKillableBackgroundApps(existingSelections)
      _backgroundProcesses.value = processes
    }
  }

  /** Loop contínuo de telemetria (CPU, RAM, temperatura, bateria, rede e hora). */
  private fun startTelemetryLoop() {
    viewModelScope.launch {
      while (true) {
        val snap = performanceRepo.readTelemetry()
        _telemetry.value = snap
        delay(1500L)
      }
    }
  }

  /** Atualiza o estado do controle Bluetooth/USB conectado. */
  fun updateGamepadState(newState: GamepadState) {
    _gamepadState.update { current ->
      newState.copy(lastButtonFeedback = current.lastButtonFeedback)
    }
  }

  /** Troca a aba ativa da interface estilo PS5. */
  fun selectTab(tab: HubTab) {
    _uiState.update { it.copy(currentTab = tab, isFocusOnTabBar = false) }
    if (tab == HubTab.PERFORMANCE) {
      refreshBackgroundProcesses()
    }
  }

  fun selectLibraryIndex(index: Int) {
    val size = libraryGames.value.size
    if (size > 0) {
      _uiState.update {
        it.copy(focusedLibraryIndex = index.coerceIn(0, size - 1), isFocusOnTabBar = false)
      }
    }
  }

  fun toggleGameInLibrary(packageName: String, checked: Boolean) {
    viewModelScope.launch { preferencesRepo.toggleGameSelection(packageName, checked) }
  }

  /** Adiciona rapidamente todos os jogos detectados automaticamente (CATEGORY_GAME). */
  fun autoSelectDetectedGames() {
    viewModelScope.launch {
      val detectedGames = _allInstalledApps.value.filter { it.isGame }.map { it.packageName }.toSet()
      if (detectedGames.isEmpty()) {
        _events.emit(
          HubUiEvent.ShowToast("Nenhum jogo marcado como CATEGORY_GAME foi detectado. Selecione manualmente abaixo.")
        )
      } else {
        val merged = userPreferences.value.selectedGamePackages + detectedGames
        preferencesRepo.setSelectedGames(merged)
        _events.emit(
          HubUiEvent.ShowToast("${detectedGames.size} jogo(s) adicionado(s) à Biblioteca.")
        )
      }
    }
  }

  fun updateManageSearchQuery(query: String) {
    _uiState.update { it.copy(manageSearchQuery = query, focusedManageIndex = 0) }
  }

  fun setManageOnlyGamesFilter(onlyGames: Boolean) {
    _uiState.update { it.copy(manageOnlyGamesFilter = onlyGames, focusedManageIndex = 0) }
  }

  fun toggleBackgroundProcessSelection(packageName: String, selectedToKill: Boolean) {
    _backgroundProcesses.update { list ->
      list.map { item ->
        if (item.packageName == packageName) item.copy(isSelectedToKill = selectedToKill)
        else item
      }
    }
  }

  fun setAllBackgroundProcessesSelected(selected: Boolean) {
    _backgroundProcesses.update { list -> list.map { it.copy(isSelectedToKill = selected) } }
  }

  /** Executa a otimização manual na aba Desempenho. */
  fun optimizeNow() {
    if (_uiState.value.isOptimizing) return
    viewModelScope.launch {
      _uiState.update { it.copy(isOptimizing = true) }
      val selectedToKill =
        _backgroundProcesses.value.filter { it.isSelectedToKill }.map { it.packageName }
      val result = performanceRepo.runOptimization(selectedToKill)
      _telemetry.value = performanceRepo.readTelemetry()
      refreshBackgroundProcesses()
      _uiState.update { it.copy(isOptimizing = false, lastOptimizationResult = result) }
      _events.emit(
        HubUiEvent.ShowToast(
          "Otimização concluída: ${result.killedCount} app(s) encerrado(s), +${result.freedMb} MB livres."
        )
      )
    }
  }

  fun setAutoOptimizeCheckEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepo.setAutoOptimizeCheckEnabled(enabled) }
  }

  fun setMinFreeRamThresholdMb(thresholdMb: Int) {
    viewModelScope.launch { preferencesRepo.setMinFreeRamThresholdMb(thresholdMb) }
  }

  fun setAutoBootEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesRepo.setAutoBootEnabled(enabled) }
  }

  fun toggleAutoStartGuideExpanded() {
    _uiState.update { it.copy(isAutoStartGuideExpanded = !it.isAutoStartGuideExpanded) }
  }

  fun dismissHomeAutoStartGuide() {
    viewModelScope.launch { preferencesRepo.setShowAutoStartGuideOnHome(false) }
  }

  fun restoreHomeAutoStartGuide() {
    viewModelScope.launch { preferencesRepo.setShowAutoStartGuideOnHome(true) }
  }

  /**
   * Inicia o fluxo de abertura de um jogo da Biblioteca:
   * - Verifica se a checagem automática está ativada e se a RAM livre está abaixo do limite.
   * - Se estiver baixa, exibe diálogo sugerindo otimização antes de iniciar.
   * - Caso contrário, exibe a tela de carregamento própria do jogo e abre via PackageManager.
   */
  fun requestLaunchGame(game: InstalledAppInfo) {
    viewModelScope.launch {
      val currentTelemetry = performanceRepo.readTelemetry()
      _telemetry.value = currentTelemetry
      val prefs = userPreferences.value

      if (
        prefs.autoOptimizeCheckEnabled &&
          currentTelemetry.ramAvailableMb < prefs.minFreeRamThresholdMb
      ) {
        _uiState.update {
          it.copy(
            lowRamPrompt =
              LowRamPromptState(
                game = game,
                availableRamMb = currentTelemetry.ramAvailableMb,
                thresholdRamMb = prefs.minFreeRamThresholdMb,
              ),
            focusedLowRamOption = 0,
          )
        }
      } else {
        startGameLoadingAndLaunch(game = game, optimizeBeforeLaunch = false)
      }
    }
  }

  fun dismissLowRamPrompt() {
    _uiState.update { it.copy(lowRamPrompt = null) }
  }

  fun confirmLowRamPrompt(optimizeFirst: Boolean) {
    val prompt = _uiState.value.lowRamPrompt ?: return
    _uiState.update { it.copy(lowRamPrompt = null) }
    startGameLoadingAndLaunch(game = prompt.game, optimizeBeforeLaunch = optimizeFirst)
  }

  /** Exibe a tela de carregamento com nome e capa do jogo, otimiza se solicitado e lança o app. */
  private fun startGameLoadingAndLaunch(game: InstalledAppInfo, optimizeBeforeLaunch: Boolean) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          gameLaunchState =
            GameLaunchState(
              game = game,
              shouldOptimize = optimizeBeforeLaunch,
              statusMessage = "Preparando ambiente do console...",
              progress = 0.15f,
            )
        )
      }
      delay(350L)

      if (optimizeBeforeLaunch) {
        _uiState.update {
          it.copy(
            gameLaunchState =
              it.gameLaunchState?.copy(
                statusMessage = "Otimizando RAM e limpando cache...",
                progress = 0.45f,
              )
          )
        }
        val toKill = _backgroundProcesses.value.filter { it.isSelectedToKill }.map { it.packageName }
        val result = performanceRepo.runOptimization(toKill)
        _telemetry.value = performanceRepo.readTelemetry()
        _uiState.update { it.copy(lastOptimizationResult = result) }
        delay(300L)
      }

      _uiState.update {
        it.copy(
          gameLaunchState =
            it.gameLaunchState?.copy(
              statusMessage = "Iniciando ${game.appName}...",
              progress = 0.88f,
            )
        )
      }
      delay(400L)

      _uiState.update {
        it.copy(gameLaunchState = it.gameLaunchState?.copy(progress = 1f))
      }
      delay(160L)

      val launchIntent = installedAppsRepo.getLaunchIntent(game.packageName)
      _uiState.update { it.copy(gameLaunchState = null) }

      if (launchIntent != null) {
        _events.emit(HubUiEvent.LaunchIntent(launchIntent))
      } else {
        _events.emit(
          HubUiEvent.ShowToast(
            "Não foi possível abrir ${game.appName}. Verifique se o aplicativo ainda está instalado."
          )
        )
        refreshInstalledApps()
      }
    }
  }

  /**
   * Traduz comandos do controle Bluetooth/USB em navegação e ações em toda a interface do PS5.
   */
  fun onGamepadAction(action: GamepadAction) {
    _gamepadState.update { it.copy(lastButtonFeedback = action.feedbackLabel) }
    val state = _uiState.value

    // Não interceptar durante splash ou abertura de jogo
    if (state.showSplash || state.gameLaunchState != null) return

    // Caso o modal de RAM baixa esteja aberto, o controle navega pelas 3 opções do modal
    val prompt = state.lowRamPrompt
    if (prompt != null) {
      when (action) {
        GamepadAction.NAV_UP,
        GamepadAction.NAV_LEFT -> {
          _uiState.update {
            it.copy(focusedLowRamOption = (it.focusedLowRamOption - 1).coerceAtLeast(0))
          }
        }
        GamepadAction.NAV_DOWN,
        GamepadAction.NAV_RIGHT -> {
          _uiState.update {
            it.copy(focusedLowRamOption = (it.focusedLowRamOption + 1).coerceAtMost(2))
          }
        }
        GamepadAction.SELECT -> {
          when (state.focusedLowRamOption) {
            0 -> confirmLowRamPrompt(optimizeFirst = true)
            1 -> confirmLowRamPrompt(optimizeFirst = false)
            else -> dismissLowRamPrompt()
          }
        }
        GamepadAction.BACK -> dismissLowRamPrompt()
        else -> Unit
      }
      return
    }

    // Atalhos globais estilo console
    when (action) {
      GamepadAction.MENU_START -> {
        selectTab(HubTab.SETTINGS)
        return
      }
      GamepadAction.PERFORMANCE_SELECT -> {
        selectTab(HubTab.PERFORMANCE)
        return
      }
      GamepadAction.TAB_PREV -> {
        val tabs = HubTab.entries
        val prev = (state.currentTab.ordinal - 1 + tabs.size) % tabs.size
        selectTab(tabs[prev])
        return
      }
      GamepadAction.TAB_NEXT -> {
        val tabs = HubTab.entries
        val next = (state.currentTab.ordinal + 1) % tabs.size
        selectTab(tabs[next])
        return
      }
      GamepadAction.BACK -> {
        if (state.currentTab != HubTab.LIBRARY) {
          selectTab(HubTab.LIBRARY)
        }
        return
      }
      else -> Unit
    }

    // Navegação entre barra de abas superior e conteúdo da aba ativa
    if (state.isFocusOnTabBar) {
      val tabs = HubTab.entries
      when (action) {
        GamepadAction.NAV_LEFT -> {
          val prev = (state.currentTab.ordinal - 1).coerceAtLeast(0)
          _uiState.update { it.copy(currentTab = tabs[prev]) }
        }
        GamepadAction.NAV_RIGHT -> {
          val next = (state.currentTab.ordinal + 1).coerceAtMost(tabs.lastIndex)
          _uiState.update { it.copy(currentTab = tabs[next]) }
        }
        GamepadAction.NAV_DOWN,
        GamepadAction.SELECT -> {
          _uiState.update { it.copy(isFocusOnTabBar = false) }
        }
        else -> Unit
      }
      return
    }

    // Navegação dentro do conteúdo da aba atual
    when (state.currentTab) {
      HubTab.LIBRARY -> handleLibraryGamepad(action, state)
      HubTab.STORE -> handleStoreGamepad(action, state)
      HubTab.MANAGE_GAMES -> handleManageGamepad(action, state)
      HubTab.PERFORMANCE -> {
        when (action) {
          GamepadAction.NAV_UP -> _uiState.update { it.copy(isFocusOnTabBar = true) }
          GamepadAction.SELECT -> optimizeNow()
          else -> Unit
        }
      }
      HubTab.SETTINGS -> {
        if (action == GamepadAction.NAV_UP) {
          _uiState.update { it.copy(isFocusOnTabBar = true) }
        }
      }
    }
  }

  private fun handleLibraryGamepad(action: GamepadAction, state: GameHubUiState) {
    val games = libraryGames.value
    when (action) {
      GamepadAction.NAV_UP -> _uiState.update { it.copy(isFocusOnTabBar = true) }
      GamepadAction.NAV_LEFT -> {
        if (games.isNotEmpty()) {
          _uiState.update {
            it.copy(focusedLibraryIndex = (state.focusedLibraryIndex - 1).coerceAtLeast(0))
          }
        }
      }
      GamepadAction.NAV_RIGHT -> {
        if (games.isNotEmpty()) {
          _uiState.update {
            it.copy(
              focusedLibraryIndex = (state.focusedLibraryIndex + 1).coerceAtMost(games.lastIndex)
            )
          }
        }
      }
      GamepadAction.SELECT -> {
        if (games.isEmpty()) {
          selectTab(HubTab.MANAGE_GAMES)
        } else {
          val target = games.getOrNull(state.focusedLibraryIndex) ?: games.first()
          requestLaunchGame(target)
        }
      }
      else -> Unit
    }
  }

  private fun handleStoreGamepad(action: GamepadAction, state: GameHubUiState) {
    when (action) {
      GamepadAction.NAV_UP -> _uiState.update { it.copy(isFocusOnTabBar = true) }
      GamepadAction.NAV_LEFT -> {
        _uiState.update {
          it.copy(focusedStoreIndex = (state.focusedStoreIndex - 1).coerceAtLeast(0))
        }
      }
      GamepadAction.NAV_RIGHT -> {
        _uiState.update {
          it.copy(
            focusedStoreIndex =
              (state.focusedStoreIndex + 1).coerceAtMost(storeHighlights.lastIndex)
          )
        }
      }
      GamepadAction.SELECT -> {
        val item = storeHighlights.getOrNull(state.focusedStoreIndex) ?: return
        onStoreHighlightSelected(item)
      }
      else -> Unit
    }
  }

  private fun handleManageGamepad(action: GamepadAction, state: GameHubUiState) {
    val list = filteredManageApps.value
    when (action) {
      GamepadAction.NAV_UP -> {
        if (state.focusedManageIndex <= 0) {
          _uiState.update { it.copy(isFocusOnTabBar = true) }
        } else {
          _uiState.update { it.copy(focusedManageIndex = state.focusedManageIndex - 1) }
        }
      }
      GamepadAction.NAV_DOWN -> {
        if (list.isNotEmpty()) {
          _uiState.update {
            it.copy(focusedManageIndex = (state.focusedManageIndex + 1).coerceAtMost(list.lastIndex))
          }
        }
      }
      GamepadAction.SELECT -> {
        val target = list.getOrNull(state.focusedManageIndex) ?: return
        val currentlyChecked = target.packageName in userPreferences.value.selectedGamePackages
        toggleGameInLibrary(target.packageName, !currentlyChecked)
      }
      else -> Unit
    }
  }

  /** Abre busca real na Google Play Store para o destaque selecionado na aba Loja/Destaques. */
  fun onStoreHighlightSelected(highlight: StoreHighlight) {
    viewModelScope.launch {
      val query = android.net.Uri.encode(highlight.searchQuery)
      val marketIntent =
        Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://search?q=$query&c=apps")).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      _events.emit(HubUiEvent.LaunchIntent(marketIntent))
    }
  }
}
