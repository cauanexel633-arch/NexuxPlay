package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Instância única do DataStore Preferences para persistência dos jogos e preferências do hub
private val Context.gameHubDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "game_hub_preferences")

/** Estado persistido das configurações do usuário no Game Hub. */
data class UserPreferences(
  val selectedGamePackages: Set<String> = emptySet(),
  val autoOptimizeCheckEnabled: Boolean = true,
  val minFreeRamThresholdMb: Int = 1024,
  val autoBootEnabled: Boolean = true,
  val showAutoStartGuideOnHome: Boolean = true,
)

/**
 * Repositório responsável por persistir no Jetpack DataStore a lista de jogos escolhidos e todas as
 * configurações de desempenho e inicialização automática.
 */
class PreferencesRepository(private val context: Context) {

  private object Keys {
    val SELECTED_GAMES = stringSetPreferencesKey("selected_game_packages")
    val AUTO_OPTIMIZE_CHECK = booleanPreferencesKey("auto_optimize_check_enabled")
    val MIN_FREE_RAM_MB = intPreferencesKey("min_free_ram_threshold_mb")
    val AUTO_BOOT_ENABLED = booleanPreferencesKey("auto_boot_enabled")
    val SHOW_AUTO_START_GUIDE = booleanPreferencesKey("show_auto_start_guide_home")
  }

  // Espelho síncrono leve usado exclusivamente pelo BootReceiver no momento em que o sistema liga
  private val bootMirrorPrefs =
    context.getSharedPreferences("game_hub_boot_mirror", Context.MODE_PRIVATE)

  /** Fluxo reativo com todas as preferências salvas no DataStore. */
  val userPreferencesFlow: Flow<UserPreferences> =
    context.gameHubDataStore.data.map { prefs ->
      UserPreferences(
        selectedGamePackages = prefs[Keys.SELECTED_GAMES] ?: emptySet(),
        autoOptimizeCheckEnabled = prefs[Keys.AUTO_OPTIMIZE_CHECK] ?: true,
        minFreeRamThresholdMb = prefs[Keys.MIN_FREE_RAM_MB] ?: 1024,
        autoBootEnabled = prefs[Keys.AUTO_BOOT_ENABLED] ?: true,
        showAutoStartGuideOnHome = prefs[Keys.SHOW_AUTO_START_GUIDE] ?: true,
      )
    }

  /** Adiciona ou remove um aplicativo/jogo da lista persistida na biblioteca. */
  suspend fun toggleGameSelection(packageName: String, isSelected: Boolean) {
    context.gameHubDataStore.edit { prefs ->
      val current = prefs[Keys.SELECTED_GAMES]?.toMutableSet() ?: mutableSetOf()
      if (isSelected) {
        current.add(packageName)
      } else {
        current.remove(packageName)
      }
      prefs[Keys.SELECTED_GAMES] = current
    }
  }

  /** Substitui de uma vez o conjunto de pacotes selecionados para a biblioteca. */
  suspend fun setSelectedGames(packages: Set<String>) {
    context.gameHubDataStore.edit { prefs -> prefs[Keys.SELECTED_GAMES] = packages }
  }

  /** Ativa ou desativa a verificação automática de RAM antes de abrir um jogo. */
  suspend fun setAutoOptimizeCheckEnabled(enabled: Boolean) {
    context.gameHubDataStore.edit { prefs -> prefs[Keys.AUTO_OPTIMIZE_CHECK] = enabled }
  }

  /** Define o limite mínimo de RAM livre (em MB) para sugerir otimização automática. */
  suspend fun setMinFreeRamThresholdMb(thresholdMb: Int) {
    context.gameHubDataStore.edit { prefs ->
      prefs[Keys.MIN_FREE_RAM_MB] = thresholdMb.coerceIn(256, 4096)
    }
  }

  /** Ativa ou desativa a abertura automática do Game Hub ao ligar o dispositivo. */
  suspend fun setAutoBootEnabled(enabled: Boolean) {
    bootMirrorPrefs.edit().putBoolean("auto_boot_enabled", enabled).apply()
    context.gameHubDataStore.edit { prefs -> prefs[Keys.AUTO_BOOT_ENABLED] = enabled }
  }

  /** Controla a exibição do card explicativo de início automático na tela inicial. */
  suspend fun setShowAutoStartGuideOnHome(show: Boolean) {
    context.gameHubDataStore.edit { prefs -> prefs[Keys.SHOW_AUTO_START_GUIDE] = show }
  }

  companion object {
    /** Leitura rápida síncrona para o BroadcastReceiver de inicialização (BOOT_COMPLETED). */
    fun isAutoBootEnabledSync(context: Context): Boolean {
      val prefs = context.getSharedPreferences("game_hub_boot_mirror", Context.MODE_PRIVATE)
      return prefs.getBoolean("auto_boot_enabled", true)
    }
  }
}
