package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.InstalledAppInfo
import com.example.model.SystemTelemetry
import com.example.ui.theme.PsCobaltAccent
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsMutedText
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavyMid
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText
import com.example.ui.theme.PsSuccessGreen

/**
 * Aba principal "Biblioteca" estilo PS5: carrossel horizontal de jogos com efeito de zoom e brilho
 * ao selecionar, painel de destaque com botão "Jogar", e guia de inicialização automática.
 */
@Composable
fun LibraryScreen(
  games: List<InstalledAppInfo>,
  focusedIndex: Int,
  isLoadingApps: Boolean,
  telemetry: SystemTelemetry,
  showAutoStartGuide: Boolean,
  isAutoStartGuideExpanded: Boolean,
  onSelectIndex: (Int) -> Unit,
  onPlayGame: (InstalledAppInfo) -> Unit,
  onOpenManageGames: () -> Unit,
  onAutoDetectGames: () -> Unit,
  onToggleAutoStartGuide: () -> Unit,
  onDismissAutoStartGuide: () -> Unit,
  onOpenAppSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val safeIndex = if (games.isEmpty()) 0 else focusedIndex.coerceIn(0, games.lastIndex)
  val selectedGame = games.getOrNull(safeIndex)

  LaunchedEffect(safeIndex, games.size) {
    if (games.isNotEmpty()) {
      listState.animateScrollToItem(safeIndex)
    }
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    when {
      isLoadingApps -> {
        Box(
          modifier = Modifier.fillMaxWidth().height(260.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PsCyanGlow)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Lendo aplicativos instalados...",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
        }
      }

      games.isEmpty() -> {
        EmptyLibraryHeroCard(
          onOpenManageGames = onOpenManageGames,
          onAutoDetectGames = onAutoDetectGames,
          modifier = Modifier.padding(horizontal = 20.dp),
        )
      }

      else -> {
        // Carrossel horizontal de jogos estilo PS5 com zoom e brilho no card em foco
        LazyRow(
          state = listState,
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.spacedBy(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().testTag("library_games_carousel"),
        ) {
          itemsIndexed(games, key = { _, item -> item.packageName }) { index, game ->
            val isFocused = index == safeIndex
            Ps5GameCard(
              game = game,
              isFocused = isFocused,
              onClick = {
                if (isFocused) {
                  onPlayGame(game)
                } else {
                  onSelectIndex(index)
                }
              },
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Painel de detalhes e ação "Jogar" do item em foco
        if (selectedGame != null) {
          SelectedGameSpotlight(
            game = selectedGame,
            telemetry = telemetry,
            onPlayGame = { onPlayGame(selectedGame) },
            onOpenManageGames = onOpenManageGames,
            modifier = Modifier.padding(horizontal = 20.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Explicação na tela inicial sobre "Iniciar automaticamente" (Xiaomi, Samsung, Motorola)
    if (showAutoStartGuide) {
      HomeAutoStartGuideCard(
        expanded = isAutoStartGuideExpanded,
        onToggleExpanded = onToggleAutoStartGuide,
        onDismiss = onDismissAutoStartGuide,
        onOpenAppSettings = onOpenAppSettings,
        modifier = Modifier.padding(horizontal = 20.dp),
      )
    }
  }
}

@Composable
private fun Ps5GameCard(
  game: InstalledAppInfo,
  isFocused: Boolean,
  onClick: () -> Unit,
) {
  val scale by
    animateFloatAsState(
      targetValue = if (isFocused) 1.07f else 0.94f,
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      label = "card_zoom",
    )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(172.dp).scale(scale),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PsNavySurface,
      border =
        BorderStroke(
          width = if (isFocused) 2.5.dp else 1.dp,
          color = if (isFocused) PsIceWhite else PsNavySurfaceElevated,
        ),
      shadowElevation = if (isFocused) 20.dp else 4.dp,
      modifier =
        Modifier.size(width = 172.dp, height = 196.dp)
          .clip(RoundedCornerShape(24.dp))
          .clickable(onClick = onClick)
          .testTag("game_card_${game.packageName}"),
    ) {
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.verticalGradient(
                colors =
                  if (isFocused) {
                    listOf(PsCobaltAccent.copy(alpha = 0.45f), PsNavyMid, PsNavyDeep)
                  } else {
                    listOf(PsNavySurface, PsNavyDeep)
                  }
              )
            )
      ) {
        // Halo de brilho superior quando focado
        if (isFocused) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(64.dp)
                .background(
                  Brush.verticalGradient(
                    listOf(PsCyanGlow.copy(alpha = 0.28f), Color.Transparent)
                  )
                )
          )
        }

        // Capa / ícone em grande formato
        Box(
          modifier = Modifier.fillMaxSize().padding(22.dp),
          contentAlignment = Alignment.Center,
        ) {
          if (game.icon != null) {
            Image(
              bitmap = game.icon,
              contentDescription = "Capa do jogo ${game.appName}",
              modifier = Modifier.size(104.dp).clip(RoundedCornerShape(22.dp)),
              contentScale = ContentScale.Fit,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.SportsEsports,
              contentDescription = game.appName,
              tint = PsCyanGlow,
              modifier = Modifier.size(72.dp),
            )
          }
        }

        // Tag discreta de tipo
        if (game.isGame) {
          Box(
            modifier =
              Modifier.align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PsCyanGlow.copy(alpha = 0.2f))
                .border(1.dp, PsCyanGlow.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(text = "JOGO", style = MaterialTheme.typography.labelSmall, color = PsCyanGlow)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = game.appName,
      style =
        if (isFocused) MaterialTheme.typography.titleMedium
        else MaterialTheme.typography.bodyMedium,
      color = if (isFocused) PsIceWhite else PsSlateText,
      fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun SelectedGameSpotlight(
  game: InstalledAppInfo,
  telemetry: SystemTelemetry,
  onPlayGame: () -> Unit,
  onOpenManageGames: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth().widthIn(max = 720.dp),
    shape = RoundedCornerShape(22.dp),
    color = PsNavySurface.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.35f)),
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = game.appName,
            style = MaterialTheme.typography.headlineMedium,
            color = PsIceWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "${game.packageName} • v${game.versionName}",
            style = MaterialTheme.typography.labelSmall,
            color = PsMutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = PsNavyDeep.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, PsNavySurfaceElevated),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Memory,
              contentDescription = null,
              tint = PsSuccessGreen,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = "${telemetry.ramAvailableMb} MB livres",
              style = MaterialTheme.typography.labelSmall,
              color = PsIceWhite,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Button(
          onClick = onPlayGame,
          modifier = Modifier.weight(1f).height(50.dp).testTag("btn_play_selected_game"),
          colors = ButtonDefaults.buttonColors(containerColor = PsIceWhite, contentColor = PsNavyDeep),
          shape = RoundedCornerShape(26.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Jogar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onOpenManageGames,
          modifier = Modifier.height(50.dp).testTag("btn_manage_from_library"),
          border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.55f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = PsIceWhite),
          shape = RoundedCornerShape(26.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = "Gerenciar Jogos",
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Gerenciar")
        }
      }
    }
  }
}

@Composable
private fun EmptyLibraryHeroCard(
  onOpenManageGames: () -> Unit,
  onAutoDetectGames: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = PsNavySurface.copy(alpha = 0.9f),
    border = BorderStroke(1.5.dp, PsCyanGlow.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Icon(
        imageVector = Icons.Filled.SportsEsports,
        contentDescription = null,
        tint = PsCyanGlow,
        modifier = Modifier.size(56.dp),
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Sua Biblioteca está vazia",
        style = MaterialTheme.typography.headlineMedium,
        color = PsIceWhite,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text =
          "Adicione seus jogos instalados na aba \"Gerenciar Jogos\" ou detecte automaticamente os títulos instalados neste aparelho.",
        style = MaterialTheme.typography.bodyLarge,
        color = PsSlateText,
      )
      Spacer(modifier = Modifier.height(20.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Button(
          onClick = onOpenManageGames,
          modifier = Modifier.weight(1f).height(48.dp).testTag("btn_empty_manage_games"),
          colors = ButtonDefaults.buttonColors(containerColor = PsCyanGlow, contentColor = PsNavyDeep),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(imageVector = Icons.Filled.AddCircleOutline, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Escolher Jogos", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
          onClick = onAutoDetectGames,
          modifier = Modifier.weight(1f).height(48.dp).testTag("btn_auto_detect_games"),
          border = BorderStroke(1.dp, PsIceWhite.copy(alpha = 0.5f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = PsIceWhite),
          shape = RoundedCornerShape(16.dp),
        ) {
          Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Detectar")
        }
      }
    }
  }
}

/**
 * Card informativo exibido na tela inicial explicando como liberar "Iniciar automaticamente" nos
 * principais fabricantes (Xiaomi, Samsung, Motorola).
 */
@Composable
private fun HomeAutoStartGuideCard(
  expanded: Boolean,
  onToggleExpanded: () -> Unit,
  onDismiss: () -> Unit,
  onOpenAppSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth().testTag("home_autostart_guide_card"),
    shape = RoundedCornerShape(20.dp),
    color = PsNavyDeep.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, PsNavySurfaceElevated),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggleExpanded() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Filled.PowerSettingsNew,
            contentDescription = null,
            tint = PsCyanGlow,
            modifier = Modifier.size(22.dp),
          )
          Column {
            Text(
              text = "Inicialização Automática ao Ligar o Celular",
              style = MaterialTheme.typography.titleMedium,
              color = PsIceWhite,
            )
            Text(
              text = "Toque para ver como liberar em Xiaomi, Samsung e Motorola",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
        }
        IconButton(onClick = onToggleExpanded) {
          Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "Recolher guia" else "Expandir guia",
            tint = PsIceWhite,
          )
        }
      }

      AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(top = 12.dp)) {
          Text(
            text =
              "Fabricantes costumam bloquear apps de abrirem sozinhos após o boot (BOOT_COMPLETED) para economizar bateria:\n" +
                "• Xiaomi (MIUI / HyperOS): Configurações → Apps → Gerenciar apps → Game Hub → Ative \"Início automático\" e defina Bateria como \"Sem restrições\".\n" +
                "• Samsung (One UI): Configurações → Aplicativos → Game Hub → Bateria → Selecione \"Não restrito\" (e remova de \"Apps em suspensão profunda\").\n" +
                "• Motorola: Configurações → Apps → Game Hub → Uso da bateria do app → Marque \"Irrestrito\".",
            style = MaterialTheme.typography.bodyMedium,
            color = PsSlateText,
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            OutlinedButton(
              onClick = onOpenAppSettings,
              border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.6f)),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PsCyanGlow),
            ) {
              Text("Abrir Config. do App")
            }

            TextButton(onClick = onDismiss) {
              Text("Ocultar da tela inicial", color = PsMutedText)
            }
          }
        }
      }
    }
  }
}
