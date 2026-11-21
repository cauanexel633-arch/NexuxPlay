package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SettingsApplications
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.model.GamepadState
import com.example.model.SystemTelemetry
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsMutedText
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText
import com.example.ui.theme.PsSuccessGreen
import com.example.ui.theme.PsWarningAmber
import kotlin.math.roundToInt

/**
 * Aba "Configurações": gerencia verificação automática de RAM, inicialização automática no boot,
 * guia de fabricantes (Xiaomi/Samsung/Motorola), permissões e mapeamento de controles.
 */
@Composable
fun SettingsScreen(
  userPreferences: UserPreferences,
  telemetry: SystemTelemetry,
  gamepadState: GamepadState,
  onToggleAutoOptimizeCheck: (Boolean) -> Unit,
  onChangeMinFreeRamMb: (Int) -> Unit,
  onToggleAutoBoot: (Boolean) -> Unit,
  onRestoreHomeGuide: () -> Unit,
  onOpenUsageAccessSettings: () -> Unit,
  onOpenAppDetailsSettings: () -> Unit,
  onOpenBatterySettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
    contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Verificação Automática de RAM ao Abrir Jogos
    item {
      SettingsSectionCard(
        title = "Otimização Automática ao Iniciar Jogos",
        icon = Icons.Filled.Memory,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Verificar RAM livre antes de abrir",
              style = MaterialTheme.typography.titleMedium,
              color = PsIceWhite,
            )
            Text(
              text = "Sugere otimização caso a memória livre esteja abaixo do limite.",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
          Switch(
            checked = userPreferences.autoOptimizeCheckEnabled,
            onCheckedChange = onToggleAutoOptimizeCheck,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = PsNavyDeep,
                checkedTrackColor = PsCyanGlow,
              ),
            modifier = Modifier.testTag("switch_auto_optimize_check"),
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Limite mínimo de RAM livre: ${userPreferences.minFreeRamThresholdMb} MB",
          style = MaterialTheme.typography.labelLarge,
          color = PsCyanGlow,
        )

        Slider(
          value = userPreferences.minFreeRamThresholdMb.toFloat(),
          onValueChange = { raw ->
            val snapped = (raw / 128f).roundToInt() * 128
            onChangeMinFreeRamMb(snapped)
          },
          valueRange = 512f..2560f,
          enabled = userPreferences.autoOptimizeCheckEnabled,
          colors =
            SliderDefaults.colors(
              thumbColor = PsCyanGlow,
              activeTrackColor = PsCyanGlow,
              inactiveTrackColor = PsNavyDeep,
            ),
          modifier = Modifier.testTag("slider_min_ram"),
        )
      }
    }

    // 2. Inicialização Automática (BOOT_COMPLETED) & Guia de Fabricantes
    item {
      SettingsSectionCard(
        title = "Inicialização Automática (BOOT_COMPLETED)",
        icon = Icons.Filled.PowerSettingsNew,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
              text = "Abrir Game Hub ao ligar o celular",
              style = MaterialTheme.typography.titleMedium,
              color = PsIceWhite,
            )
            Text(
              text = "Ativa o BootReceiver para iniciar o console launcher após o boot.",
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
            )
          }
          Switch(
            checked = userPreferences.autoBootEnabled,
            onCheckedChange = onToggleAutoBoot,
            colors =
              SwitchDefaults.colors(
                checkedThumbColor = PsNavyDeep,
                checkedTrackColor = PsCyanGlow,
              ),
            modifier = Modifier.testTag("switch_auto_boot"),
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text =
            "Como liberar em aparelhos que bloqueiam por padrão:\n" +
              "• Xiaomi (MIUI/HyperOS): Configurações → Apps → Gerenciar apps → Game Hub → Início automático (ON) e Economia de bateria = Sem restrições.\n" +
              "• Samsung (One UI): Configurações → Aplicativos → Game Hub → Bateria → Não restrito.\n" +
              "• Motorola: Configurações → Apps → Game Hub → Uso da bateria do app → Irrestrito.",
          style = MaterialTheme.typography.bodyMedium,
          color = PsSlateText,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(
            onClick = onOpenAppDetailsSettings,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PsCyanGlow),
          ) {
            Icon(
              imageVector = Icons.Filled.SettingsApplications,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Info do App")
          }

          OutlinedButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PsCyanGlow),
          ) {
            Icon(
              imageVector = Icons.Filled.BatteryChargingFull,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bateria")
          }
        }

        if (!userPreferences.showAutoStartGuideOnHome) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = onRestoreHomeGuide,
            border = BorderStroke(1.dp, PsNavySurfaceElevated),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PsSlateText),
          ) {
            Text("Exibir guia novamente na tela inicial")
          }
        }
      }
    }

    // 3. Status das Permissões
    item {
      SettingsSectionCard(
        title = "Permissões Necessárias do Sistema",
        icon = Icons.Filled.VerifiedUser,
      ) {
        PermissionStatusRow(
          name = "PACKAGE_USAGE_STATS",
          description = "Identifica apps recentes em segundo plano para otimização.",
          granted = telemetry.hasUsageStatsPermission,
        )
        if (!telemetry.hasUsageStatsPermission) {
          Spacer(modifier = Modifier.height(8.dp))
          Button(
            onClick = onOpenUsageAccessSettings,
            colors =
              ButtonDefaults.buttonColors(containerColor = PsWarningAmber, contentColor = PsNavyDeep),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("Abrir Configurações de Acesso de Uso", fontWeight = FontWeight.Bold)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        PermissionStatusRow(
          name = "RECEIVE_BOOT_COMPLETED",
          description = "Permite iniciar automaticamente quando o sistema liga.",
          granted = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionStatusRow(
          name = "KILL_BACKGROUND_PROCESSES",
          description = "Encerra processos em segundo plano selecionados sem root.",
          granted = true,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PermissionStatusRow(
          name = "ACCESS_NETWORK_STATE",
          description = "Exibe o status real de Wi-Fi / Dados Móveis na barra superior.",
          granted = true,
        )
      }
    }

    // 4. Mapeamento de Controle (Gamepad Bluetooth/USB)
    item {
      SettingsSectionCard(
        title = "Controle Bluetooth / USB (Xbox & PlayStation)",
        icon = Icons.Filled.SportsEsports,
      ) {
        Text(
          text =
            if (gamepadState.isConnected)
              "Conectado agora: ${gamepadState.deviceName} (${gamepadState.controllerType.displayLabel})"
            else
              "Nenhum gamepad conectado no momento. Conecte via Bluetooth ou cabo USB-OTG.",
          style = MaterialTheme.typography.bodyMedium,
          color = if (gamepadState.isConnected) PsSuccessGreen else PsSlateText,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GamepadMappingRow("D-Pad / Analógico", "Navegar entre abas, jogos e listas")
        GamepadMappingRow("Botão A / ✕ (Cross)", "Selecionar item ou iniciar jogo")
        GamepadMappingRow("Botão B / ◯ (Circle)", "Voltar para Biblioteca ou fechar diálogo")
        GamepadMappingRow("Start / Options", "Abrir Configurações")
        GamepadMappingRow("Select / Share", "Ir direto para aba Desempenho")
        GamepadMappingRow("L1 / R1 (LB / RB)", "Alternar entre as abas rapidamente")
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  content: @Composable () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = PsNavySurface.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, PsNavySurfaceElevated),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = PsCyanGlow,
          modifier = Modifier.size(20.dp),
        )
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = PsIceWhite)
      }
      Spacer(modifier = Modifier.height(12.dp))
      content()
    }
  }
}

@Composable
private fun PermissionStatusRow(name: String, description: String, granted: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
      contentDescription = if (granted) "Concedida" else "Pendente",
      tint = if (granted) PsSuccessGreen else PsWarningAmber,
      modifier = Modifier.size(18.dp),
    )
    Column(modifier = Modifier.weight(1f)) {
      Text(text = name, style = MaterialTheme.typography.labelMedium, color = PsIceWhite)
      Text(text = description, style = MaterialTheme.typography.bodyMedium, color = PsMutedText)
    }
    Text(
      text = if (granted) "Ativa" else "Pendente",
      style = MaterialTheme.typography.labelSmall,
      color = if (granted) PsSuccessGreen else PsWarningAmber,
    )
  }
}

@Composable
private fun GamepadMappingRow(button: String, action: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(text = button, style = MaterialTheme.typography.labelMedium, color = PsCyanGlow)
    Text(text = action, style = MaterialTheme.typography.bodyMedium, color = PsSlateText)
  }
}
