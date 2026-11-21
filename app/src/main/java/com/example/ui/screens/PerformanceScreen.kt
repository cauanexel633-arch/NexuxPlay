package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.BackgroundProcessInfo
import com.example.model.OptimizationResult
import com.example.model.SystemTelemetry
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsDangerRed
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsMutedText
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText
import com.example.ui.theme.PsSuccessGreen
import com.example.ui.theme.PsWarningAmber

/**
 * Aba "Desempenho": exibe métricas reais de CPU, RAM e temperatura, trata a permissão
 * PACKAGE_USAGE_STATS, permite desmarcar apps que não devem ser encerrados e executa otimização.
 */
@Composable
fun PerformanceScreen(
  telemetry: SystemTelemetry,
  backgroundProcesses: List<BackgroundProcessInfo>,
  userPreferences: UserPreferences,
  isOptimizing: Boolean,
  lastResult: OptimizationResult?,
  onOptimizeNow: () -> Unit,
  onToggleProcess: (String, Boolean) -> Unit,
  onSelectAllProcesses: (Boolean) -> Unit,
  onRefreshProcesses: () -> Unit,
  onToggleAutoCheck: (Boolean) -> Unit,
  onOpenUsageAccessSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val selectedCount = backgroundProcesses.count { it.isSelectedToKill }

  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Cards de Telemetria (CPU, RAM, Temperatura)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        TelemetryMetricCard(
          title = "CPU",
          valueText = "${telemetry.cpuUsagePercent}%",
          subtitle = telemetry.cpuMetricSource,
          progress = telemetry.cpuUsagePercent / 100f,
          icon = Icons.Filled.Speed,
          accent = if (telemetry.cpuUsagePercent > 80) PsWarningAmber else PsCyanGlow,
          modifier = Modifier.weight(1f).testTag("metric_card_cpu"),
        )

        TelemetryMetricCard(
          title = "RAM",
          valueText = "${telemetry.ramUsagePercent}%",
          subtitle = "${telemetry.ramAvailableMb} MB livres",
          progress = telemetry.ramUsagePercent / 100f,
          icon = Icons.Filled.Memory,
          accent =
            if (telemetry.ramAvailableMb < userPreferences.minFreeRamThresholdMb) PsWarningAmber
            else PsSuccessGreen,
          modifier = Modifier.weight(1f).testTag("metric_card_ram"),
        )

        val tempColor =
          when {
            telemetry.temperatureCelsius >= 43f -> PsDangerRed
            telemetry.temperatureCelsius >= 38f -> PsWarningAmber
            else -> PsCyanGlow
          }
        val tempLabel =
          if (telemetry.temperatureCelsius > 0f)
            String.format("%.1f °C", telemetry.temperatureCelsius)
          else "-- °C"

        TelemetryMetricCard(
          title = "Temp.",
          valueText = tempLabel,
          subtitle =
            when {
              telemetry.temperatureCelsius >= 43f -> "Elevada"
              telemetry.temperatureCelsius >= 38f -> "Aquecido"
              else -> "Normal"
            },
          progress = (telemetry.temperatureCelsius / 55f).coerceIn(0f, 1f),
          icon = Icons.Filled.Thermostat,
          accent = tempColor,
          modifier = Modifier.weight(1f).testTag("metric_card_temp"),
        )
      }
    }

    // 2. Detalhe de Memória RAM + Botão Otimizar Agora
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = PsNavySurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.4f)),
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Memória do Sistema",
                style = MaterialTheme.typography.titleLarge,
                color = PsIceWhite,
              )
              Text(
                text =
                  "Em uso: ${telemetry.ramUsedMb} MB de ${telemetry.ramTotalMb} MB • Livres: ${telemetry.ramAvailableMb} MB",
                style = MaterialTheme.typography.bodyMedium,
                color = PsSlateText,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          LinearProgressIndicator(
            progress = { (telemetry.ramUsagePercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
            color = PsCyanGlow,
            trackColor = PsNavyDeep,
            strokeCap = StrokeCap.Round,
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = onOptimizeNow,
            enabled = !isOptimizing,
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_optimize_now"),
            colors =
              ButtonDefaults.buttonColors(containerColor = PsCyanGlow, contentColor = PsNavyDeep),
            shape = RoundedCornerShape(16.dp),
          ) {
            if (isOptimizing) {
              CircularProgressIndicator(
                color = PsNavyDeep,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.5.dp,
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Otimizando memória...", fontWeight = FontWeight.Bold)
            } else {
              Icon(imageVector = Icons.Filled.RocketLaunch, contentDescription = null)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Otimizar Agora ($selectedCount app(s) selecionado(s))",
                fontWeight = FontWeight.Bold,
              )
            }
          }

          if (lastResult != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = PsSuccessGreen,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text =
                  "Última otimização: +${lastResult.freedMb} MB liberados, ${lastResult.killedCount} app(s) encerrado(s), ${lastResult.clearedCacheKb} KB de cache limpo.",
                style = MaterialTheme.typography.labelSmall,
                color = PsSuccessGreen,
              )
            }
          }
        }
      }
    }

    // 3. Banner de Permissão PACKAGE_USAGE_STATS quando não concedida
    if (!telemetry.hasUsageStatsPermission) {
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = PsWarningAmber.copy(alpha = 0.14f),
          border = BorderStroke(1.dp, PsWarningAmber.copy(alpha = 0.65f)),
          modifier = Modifier.testTag("usage_stats_permission_banner"),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = "Permissão de acesso de uso pendente",
                tint = PsWarningAmber,
              )
              Text(
                text = "Permissão de Acesso de Uso (PACKAGE_USAGE_STATS) não concedida",
                style = MaterialTheme.typography.titleMedium,
                color = PsIceWhite,
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text =
                "Sem essa permissão, o Android restringe a detecção de apps abertos recentemente em segundo plano. Conceda acesso de uso ao Game Hub nas configurações do sistema para listagem completa.",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
              onClick = onOpenUsageAccessSettings,
              border = BorderStroke(1.dp, PsWarningAmber),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = PsWarningAmber),
              modifier = Modifier.testTag("btn_grant_usage_stats"),
            ) {
              Text("Conceder Acesso de Uso", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Verificação automática configurável ao abrir jogo
    item {
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = PsNavyDeep.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, PsNavySurfaceElevated),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Sugerir otimização ao abrir jogo",
              style = MaterialTheme.typography.titleMedium,
              color = PsIceWhite,
            )
            Text(
              text =
                "Avisa se a RAM livre estiver abaixo de ${userPreferences.minFreeRamThresholdMb} MB (ajuste o limite em Configurações).",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
          Switch(
            checked = userPreferences.autoOptimizeCheckEnabled,
            onCheckedChange = onToggleAutoCheck,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = PsNavyDeep,
                checkedTrackColor = PsCyanGlow,
              ),
          )
        }
      }
    }

    // 5. Cabeçalho da lista de processos que serão encerrados
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Apps em Segundo Plano (${backgroundProcesses.size})",
            style = MaterialTheme.typography.titleLarge,
            color = PsIceWhite,
          )
          Text(
            text =
              "Desmarque os apps que você NÃO deseja fechar. O Game Hub e serviços do sistema nunca são encerrados.",
            style = MaterialTheme.typography.bodyMedium,
            color = PsSlateText,
          )
        }
        IconButton(onClick = onRefreshProcesses) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = "Atualizar processos",
            tint = PsCyanGlow,
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { onSelectAllProcesses(true) }) {
          Text("Marcar todos", color = PsCyanGlow)
        }
        TextButton(onClick = { onSelectAllProcesses(false) }) {
          Text("Desmarcar todos", color = PsMutedText)
        }
      }
    }

    if (backgroundProcesses.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = PsNavyDeep.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, PsNavySurfaceElevated),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Security,
              contentDescription = null,
              tint = PsSuccessGreen,
            )
            Text(
              text =
                "Nenhum aplicativo de usuário em segundo plano listado no momento. O cache interno ainda pode ser limpo pelo botão Otimizar Agora.",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
        }
      }
    } else {
      items(backgroundProcesses, key = { it.packageName }) { proc ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color =
            if (proc.isSelectedToKill) PsNavySurface.copy(alpha = 0.88f)
            else PsNavyDeep.copy(alpha = 0.65f),
          border =
            BorderStroke(
              1.dp,
              if (proc.isSelectedToKill) PsCyanGlow.copy(alpha = 0.45f)
              else PsNavySurfaceElevated.copy(alpha = 0.5f),
            ),
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .clickable { onToggleProcess(proc.packageName, !proc.isSelectedToKill) },
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (proc.icon != null) {
              Image(
                bitmap = proc.icon,
                contentDescription = proc.appName,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Fit,
              )
            } else {
              Box(
                modifier =
                  Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PsNavySurfaceElevated),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Filled.CleaningServices,
                  contentDescription = null,
                  tint = PsCyanGlow,
                  modifier = Modifier.size(20.dp),
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = proc.appName,
                style = MaterialTheme.typography.titleMedium,
                color = PsIceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${proc.sourceLabel} • ${proc.packageName}",
                style = MaterialTheme.typography.labelSmall,
                color = PsMutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            Checkbox(
              checked = proc.isSelectedToKill,
              onCheckedChange = { checked -> onToggleProcess(proc.packageName, checked) },
              colors =
                CheckboxDefaults.colors(
                  checkedColor = PsCyanGlow,
                  uncheckedColor = PsSlateText,
                  checkmarkColor = PsNavyDeep,
                ),
              modifier = Modifier.testTag("process_checkbox_${proc.packageName}"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TelemetryMetricCard(
  title: String,
  valueText: String,
  subtitle: String,
  progress: Float,
  icon: ImageVector,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    color = PsNavySurface.copy(alpha = 0.85f),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accent,
          modifier = Modifier.size(18.dp),
        )
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = PsSlateText)
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = valueText,
        style = MaterialTheme.typography.headlineMedium,
        color = PsIceWhite,
        maxLines = 1,
      )
      Spacer(modifier = Modifier.height(6.dp))
      LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
        color = accent,
        trackColor = PsNavyDeep,
        strokeCap = StrokeCap.Round,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = PsMutedText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
