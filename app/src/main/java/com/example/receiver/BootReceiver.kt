package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.PreferencesRepository

/**
 * BroadcastReceiver registrado para BOOT_COMPLETED (e QUICKBOOT_POWERON). Quando o dispositivo é
 * ligado, verifica se a inicialização automática está ativa e abre o Game Hub automaticamente.
 *
 * Observação importante: fabricantes como Xiaomi (MIUI/HyperOS), Samsung (One UI) e Motorola
 * podem exigir que o usuário autorize "Iniciar automaticamente" / "Sem restrições de bateria"
 * manualmente nas configurações do sistema.
 */
class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    if (
      action == Intent.ACTION_BOOT_COMPLETED ||
        action == "android.intent.action.QUICKBOOT_POWERON" ||
        action == "com.htc.intent.action.QUICKBOOT_POWERON"
    ) {
      if (PreferencesRepository.isAutoBootEnabledSync(context)) {
        val launchIntent =
          Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("launched_from_boot", true)
          }
        runCatching { context.startActivity(launchIntent) }
      }
    }
  }
}
