package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.GamepadState
import com.example.model.HubTab
import com.example.model.NetworkConnectionType
import com.example.model.SystemTelemetry
import com.example.ui.theme.ActionCircleRed
import com.example.ui.theme.ActionCrossBlue
import com.example.ui.theme.ActionSquarePink
import com.example.ui.theme.ActionTriangleGreen
import com.example.ui.theme.PsCobaltAccent
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsMutedText
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavyMid
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsNavyVoid
import com.example.ui.theme.PsSlateText
import com.example.ui.theme.PsSuccessGreen
import com.example.ui.theme.PsWarningAmber

/**
 * Fundo escuro azul-marinho com gradiente atmosférico dinâmico inspirado na tela inicial do PS5.
 */
@Composable
fun Ps5AtmosphereBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "ps5_bg_glow")
  val drift by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "glow_drift",
    )

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(PsNavyVoid, PsNavyDeep, PsNavyMid, Color(0xFF050C1B))
          )
        )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Halo azul-ciano suave no topo esquerdo
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(PsCyanGlow.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * (0.18f + 0.08f * drift), size.height * 0.18f),
            radius = size.maxDimension * 0.55f,
          ),
        center = Offset(size.width * (0.18f + 0.08f * drift), size.height * 0.18f),
        radius = size.maxDimension * 0.55f,
      )
      // Halo azul cobalto suave na direita inferior
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(PsCobaltAccent.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(size.width * (0.84f - 0.08f * drift), size.height * 0.78f),
            radius = size.maxDimension * 0.6f,
          ),
        center = Offset(size.width * (0.84f - 0.08f * drift), size.height * 0.78f),
        radius = size.maxDimension * 0.6f,
      )
    }
    content()
  }
}

/**
 * Barra superior estilo PS5 contendo logo do hub, indicador de controle conectado, status de rede,
 * nível de bateria e relógio em tempo real.
 */
@Composable
fun Ps5TopStatusBar(
  telemetry: SystemTelemetry,
  gamepadState: GamepadState,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    // Identidade visual do console e indicador do controle
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.weight(1f),
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = PsNavySurfaceElevated.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.45f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Box(
            modifier = Modifier.size(8.dp).clip(CircleShape).background(PsCyanGlow)
          )
          Text(
            text = "GAME HUB",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PsIceWhite,
          )
        }
      }

      // Indicador visual do controle Bluetooth/USB conectado
      Surface(
        shape = RoundedCornerShape(20.dp),
        color =
          if (gamepadState.isConnected) PsCyanGlow.copy(alpha = 0.16f)
          else PsNavySurface.copy(alpha = 0.65f),
        border =
          BorderStroke(
            1.dp,
            if (gamepadState.isConnected) PsCyanGlow.copy(alpha = 0.65f)
            else PsMutedText.copy(alpha = 0.3f),
          ),
        modifier = Modifier.testTag("gamepad_status_badge"),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector =
              if (gamepadState.isConnected) Icons.Filled.SportsEsports
              else Icons.Outlined.SportsEsports,
            contentDescription =
              if (gamepadState.isConnected) "Controle conectado" else "Sem controle conectado",
            tint = if (gamepadState.isConnected) PsCyanGlow else PsMutedText,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text =
              if (gamepadState.isConnected) gamepadState.controllerType.displayLabel
              else "Sem controle",
            style = MaterialTheme.typography.labelSmall,
            color = if (gamepadState.isConnected) PsIceWhite else PsMutedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    // Status de conexão, bateria e hora atual
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Rede
      val (netIcon, netTint) =
        when (telemetry.networkType) {
          NetworkConnectionType.WIFI -> Icons.Filled.Wifi to PsCyanGlow
          NetworkConnectionType.CELLULAR -> Icons.Filled.CellTower to PsSuccessGreen
          NetworkConnectionType.ETHERNET -> Icons.Filled.SettingsEthernet to PsCyanGlow
          NetworkConnectionType.OFFLINE -> Icons.Filled.WifiOff to PsWarningAmber
        }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = netIcon,
          contentDescription = "Status de rede: ${telemetry.networkType.label}",
          tint = netTint,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = telemetry.networkType.label,
          style = MaterialTheme.typography.labelSmall,
          color = PsSlateText,
        )
      }

      // Bateria
      val batIcon =
        when {
          telemetry.isCharging -> Icons.Filled.BatteryChargingFull
          telemetry.batteryPercent >= 85 -> Icons.Filled.BatteryFull
          else -> Icons.Filled.BatteryStd
        }
      val batColor =
        when {
          telemetry.isCharging -> PsSuccessGreen
          telemetry.batteryPercent <= 20 -> PsWarningAmber
          else -> PsIceWhite
        }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Icon(
          imageVector = batIcon,
          contentDescription = "Bateria em ${telemetry.batteryPercent}%",
          tint = batColor,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "${telemetry.batteryPercent}%",
          style = MaterialTheme.typography.labelMedium,
          color = batColor,
        )
      }

      // Relógio do console
      Text(
        text = telemetry.currentTimeFormatted,
        style = MaterialTheme.typography.titleLarge,
        color = PsIceWhite,
        modifier = Modifier.testTag("top_bar_clock"),
      )
    }
  }
}

/**
 * Barra de navegação superior em estilo PS5 (Biblioteca, Loja/Destaques, Desempenho, Gerenciar
 * Jogos e Configurações) com destaque luminoso para toque e controle.
 */
@Composable
fun Ps5TopNavigationTabs(
  currentTab: HubTab,
  isFocusOnTabBar: Boolean,
  onSelectTab: (HubTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .horizontalScroll(scrollState)
        .padding(horizontal = 20.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HubTab.entries.forEach { tab ->
      val isSelected = tab == currentTab
      val bgColor by
        animateColorAsState(
          targetValue =
            when {
              isSelected && isFocusOnTabBar -> PsCyanGlow.copy(alpha = 0.28f)
              isSelected -> PsIceWhite.copy(alpha = 0.16f)
              else -> Color.Transparent
            },
          label = "tab_bg",
        )
      val borderColor by
        animateColorAsState(
          targetValue =
            when {
              isSelected && isFocusOnTabBar -> PsCyanGlow
              isSelected -> PsIceWhite.copy(alpha = 0.65f)
              else -> PsNavySurfaceElevated.copy(alpha = 0.6f)
            },
          label = "tab_border",
        )

      Box(
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(width = if (isSelected) 1.5.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
            .clickable { onSelectTab(tab) }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("tab_${tab.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = tab.title,
          style = MaterialTheme.typography.labelLarge,
          color = if (isSelected) PsIceWhite else PsSlateText,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
      }
    }
  }
}

/** Barra discreta inferior com atalhos do controle (respeita WindowInsets da barra de navegação). */
@Composable
fun Ps5GamepadFooterHints(gamepadState: GamepadState, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth().navigationBarsPadding(),
    color = PsNavyVoid.copy(alpha = 0.78f),
    border = BorderStroke(0.5.dp, PsNavySurfaceElevated.copy(alpha = 0.7f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        LegendChip(symbol = "✕/A", label = "Selecionar", accent = ActionCrossBlue)
        LegendChip(symbol = "◯/B", label = "Voltar", accent = ActionCircleRed)
        LegendChip(symbol = "L1/R1", label = "Abas", accent = PsCyanGlow)
        LegendChip(symbol = "START", label = "Config", accent = ActionTriangleGreen)
        LegendChip(symbol = "SELECT", label = "RAM/CPU", accent = ActionSquarePink)
      }

      if (gamepadState.lastButtonFeedback.isNotBlank()) {
        Text(
          text = gamepadState.lastButtonFeedback,
          style = MaterialTheme.typography.labelSmall,
          color = PsCyanGlow,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun LegendChip(symbol: String, label: String, accent: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Box(
      modifier =
        Modifier.clip(RoundedCornerShape(6.dp))
          .background(accent.copy(alpha = 0.18f))
          .border(1.dp, accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
          .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
      Text(text = symbol, style = MaterialTheme.typography.labelSmall, color = accent)
    }
    Text(text = label, style = MaterialTheme.typography.labelSmall, color = PsSlateText)
  }
}
