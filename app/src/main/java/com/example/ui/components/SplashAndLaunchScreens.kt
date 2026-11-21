package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameLaunchState
import com.example.ui.theme.ActionCircleRed
import com.example.ui.theme.ActionCrossBlue
import com.example.ui.theme.ActionSquarePink
import com.example.ui.theme.ActionTriangleGreen
import com.example.ui.theme.PsCobaltAccent
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsNavyMid
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText

/**
 * 1. Tela de carregamento inicial (Splash) exibida ao abrir o aplicativo com logo animado "GAME
 * HUB" e barra de progresso estilizada.
 */
@Composable
fun AppSplashScreen(progress: Float, modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
  val pulse by
    infiniteTransition.animateFloat(
      initialValue = 0.94f,
      targetValue = 1.06f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1100, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "logo_pulse",
    )

  Ps5AtmosphereBackground(modifier = modifier.testTag("app_splash_screen")) {
    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Símbolos geométricos estilo PlayStation animados
      Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.scale(pulse),
      ) {
        ConsoleSymbolGlyph(symbol = "△", color = ActionTriangleGreen)
        ConsoleSymbolGlyph(symbol = "◯", color = ActionCircleRed)
        ConsoleSymbolGlyph(symbol = "✕", color = ActionCrossBlue)
        ConsoleSymbolGlyph(symbol = "□", color = ActionSquarePink)
      }

      Spacer(modifier = Modifier.height(26.dp))

      Text(
        text = "GAME HUB",
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
        color = PsIceWhite,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "CONSOLE EXPERIENCE",
        style = MaterialTheme.typography.labelMedium,
        color = PsCyanGlow,
        letterSpacing = 4.sp,
      )

      Spacer(modifier = Modifier.height(42.dp))

      Column(
        modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        LinearProgressIndicator(
          progress = { progress.coerceIn(0f, 1f) },
          modifier =
            Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).testTag("splash_progress"),
          color = PsCyanGlow,
          trackColor = PsNavySurfaceElevated,
          strokeCap = StrokeCap.Round,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Inicializando biblioteca e sistema... ${(progress * 100).toInt()}%",
          style = MaterialTheme.typography.labelMedium,
          color = PsSlateText,
        )
      }
    }
  }
}

@Composable
private fun ConsoleSymbolGlyph(symbol: String, color: Color) {
  Box(
    modifier =
      Modifier.size(44.dp)
        .clip(CircleShape)
        .background(color.copy(alpha = 0.14f))
        .border(1.5.dp, color.copy(alpha = 0.65f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = symbol,
      style = MaterialTheme.typography.titleLarge,
      color = color,
      fontWeight = FontWeight.Bold,
    )
  }
}

/**
 * Tela de carregamento própria exibida ao abrir um jogo da biblioteca, mostrando a capa (ícone em
 * destaque), nome do jogo, status da otimização e barra de progresso antes do lançamento.
 */
@Composable
fun GameLaunchingScreen(launchState: GameLaunchState, modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "game_launch_pulse")
  val haloAlpha by
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 0.85f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(900, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "halo_alpha",
    )

  Ps5AtmosphereBackground(modifier = modifier.testTag("game_launch_screen")) {
    // Anel luminoso de fundo
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        color = PsCyanGlow.copy(alpha = haloAlpha * 0.14f),
        radius = size.minDimension * 0.34f,
        center = Offset(size.width / 2f, size.height * 0.42f),
        style = Stroke(width = 4f),
      )
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // Capa / ícone em grande formato estilo PS5
      Surface(
        shape = RoundedCornerShape(32.dp),
        color = PsNavySurface,
        border = BorderStroke(2.dp, PsCyanGlow.copy(alpha = haloAlpha)),
        shadowElevation = 24.dp,
        modifier = Modifier.size(164.dp),
      ) {
        Box(
          modifier =
            Modifier.fillMaxSize()
              .background(
                Brush.linearGradient(listOf(PsNavyMid, PsCobaltAccent.copy(alpha = 0.35f)))
              ),
          contentAlignment = Alignment.Center,
        ) {
          val icon = launchState.game.icon
          if (icon != null) {
            Image(
              bitmap = icon,
              contentDescription = "Capa de ${launchState.game.appName}",
              modifier = Modifier.fillMaxSize().padding(18.dp).clip(RoundedCornerShape(24.dp)),
              contentScale = ContentScale.Fit,
            )
          } else {
            Icon(
              imageVector = Icons.Filled.SportsEsports,
              contentDescription = launchState.game.appName,
              tint = PsCyanGlow,
              modifier = Modifier.size(72.dp),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      Text(
        text = launchState.game.appName,
        style = MaterialTheme.typography.displaySmall,
        color = PsIceWhite,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = launchState.statusMessage,
        style = MaterialTheme.typography.bodyLarge,
        color = PsCyanGlow,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(28.dp))

      LinearProgressIndicator(
        progress = { launchState.progress.coerceIn(0f, 1f) },
        modifier =
          Modifier.widthIn(max = 380.dp)
            .fillMaxWidth()
            .height(7.dp)
            .clip(RoundedCornerShape(50)),
        color = PsCyanGlow,
        trackColor = PsNavySurfaceElevated,
        strokeCap = StrokeCap.Round,
      )
    }
  }
}
