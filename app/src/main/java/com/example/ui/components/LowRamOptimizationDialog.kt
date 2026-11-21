package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.LowRamPromptState
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText
import com.example.ui.theme.PsWarningAmber

/**
 * Diálogo exibido antes de abrir um jogo caso a verificação automática detecte que a memória RAM
 * livre está abaixo do limite configurado pelo usuário.
 */
@Composable
fun LowRamOptimizationDialog(
  promptState: LowRamPromptState,
  focusedOption: Int,
  onOptimizeAndPlay: () -> Unit,
  onPlayAnyway: () -> Unit,
  onCancel: () -> Unit,
) {
  Dialog(onDismissRequest = onCancel) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = PsNavyDeep,
      border = BorderStroke(1.5.dp, PsCyanGlow.copy(alpha = 0.65f)),
      tonalElevation = 12.dp,
      modifier = Modifier.fillMaxWidth().testTag("low_ram_dialog"),
    ) {
      Column(modifier = Modifier.padding(24.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = PsWarningAmber.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, PsWarningAmber.copy(alpha = 0.6f)),
          ) {
            Icon(
              imageVector = Icons.Filled.Memory,
              contentDescription = "Alerta de RAM baixa",
              tint = PsWarningAmber,
              modifier = Modifier.padding(10.dp).size(26.dp),
            )
          }
          Column {
            Text(
              text = "Memória RAM Baixa",
              style = MaterialTheme.typography.titleLarge,
              color = PsIceWhite,
            )
            Text(
              text = "Antes de abrir ${promptState.game.appName}",
              style = MaterialTheme.typography.bodyMedium,
              color = PsCyanGlow,
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text =
            "RAM livre atual: ${promptState.availableRamMb} MB (abaixo do limite configurado de ${promptState.thresholdRamMb} MB). Deseja otimizar os processos em segundo plano antes de iniciar?",
          style = MaterialTheme.typography.bodyLarge,
          color = PsSlateText,
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
          onClick = onOptimizeAndPlay,
          modifier = Modifier.fillMaxWidth().testTag("btn_optimize_and_play"),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = if (focusedOption == 0) PsCyanGlow else PsNavySurfaceElevated,
              contentColor = if (focusedOption == 0) PsNavyDeep else PsIceWhite,
            ),
          border = if (focusedOption == 0) BorderStroke(2.dp, PsIceWhite) else null,
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.RocketLaunch,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text("Otimizar e Jogar", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onPlayAnyway,
          modifier = Modifier.fillMaxWidth().testTag("btn_play_anyway"),
          border =
            BorderStroke(
              width = if (focusedOption == 1) 2.dp else 1.dp,
              color = if (focusedOption == 1) PsCyanGlow else PsSlateText.copy(alpha = 0.4f),
            ),
          colors =
            ButtonDefaults.outlinedButtonColors(
              containerColor = if (focusedOption == 1) PsNavySurface else PsNavyDeep,
              contentColor = PsIceWhite,
            ),
          shape = RoundedCornerShape(14.dp),
        ) {
          Text("Iniciar Mesmo Assim")
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
          onClick = onCancel,
          modifier = Modifier.align(Alignment.End).testTag("btn_cancel_launch"),
        ) {
          Text(
            text = "Cancelar",
            color = if (focusedOption == 2) PsCyanGlow else PsSlateText,
            fontWeight = if (focusedOption == 2) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }
  }
}
