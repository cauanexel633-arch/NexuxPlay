package com.example.data

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Process
import com.example.model.BackgroundProcessInfo
import com.example.model.NetworkConnectionType
import com.example.model.OptimizationResult
import com.example.model.SystemTelemetry
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório responsável pela telemetria em tempo real (CPU, RAM, temperatura, bateria e rede),
 * verificação da permissão PACKAGE_USAGE_STATS, listagem segura de apps em segundo plano e
 * otimização via limpeza de cache + ActivityManager.killBackgroundProcesses.
 */
class SystemPerformanceRepository(
  private val context: Context,
  private val installedAppsRepository: InstalledAppsRepository,
) {

  private val activityManager =
    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  private val packageManager = context.packageManager

  private var prevCpuTotal: Long = 0L
  private var prevCpuIdle: Long = 0L
  private var prevProcessCpuMs: Long = Process.getElapsedCpuTime()
  private var prevWallClockMs: Long = System.currentTimeMillis()

  private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

  /** Coleta instantaneamente os dados reais do sistema operacional e hardware. */
  suspend fun readTelemetry(): SystemTelemetry =
    withContext(Dispatchers.IO) {
      val memInfo = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(memInfo)

      val totalMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(1L)
      val availMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
      val usedMb = (totalMb - availMb).coerceAtLeast(0L)
      val ramPercent = ((usedMb.toFloat() / totalMb.toFloat()) * 100f).toInt().coerceIn(0, 100)

      val (cpuPercent, cpuSource) = readRealCpuUsage()
      val (batteryPct, isCharging, tempCelsius) = readBatteryAndTemperature()
      val networkType = readNetworkStatus()
      val hasUsagePerm = hasUsageStatsPermission()

      SystemTelemetry(
        ramTotalMb = totalMb,
        ramAvailableMb = availMb,
        ramUsedMb = usedMb,
        ramUsagePercent = ramPercent,
        cpuUsagePercent = cpuPercent,
        cpuMetricSource = cpuSource,
        temperatureCelsius = tempCelsius,
        batteryPercent = batteryPct,
        isCharging = isCharging,
        networkType = networkType,
        hasUsageStatsPermission = hasUsagePerm,
        currentTimeFormatted = timeFormatter.format(Date()),
      )
    }

  /** Verifica se a permissão especial PACKAGE_USAGE_STATS foi concedida pelo usuário. */
  fun hasUsageStatsPermission(): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(
          AppOpsManager.OPSTR_GET_USAGE_STATS,
          Process.myUid(),
          context.packageName,
        )
      }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  /**
   * Lista processos/apps não essenciais em segundo plano que podem ser encerrados com segurança.
   * Nunca inclui o próprio Game Hub nem serviços ou aplicativos do sistema.
   */
  suspend fun loadKillableBackgroundApps(
    previousSelections: Map<String, Boolean> = emptyMap()
  ): List<BackgroundProcessInfo> =
    withContext(Dispatchers.IO) {
      val candidates = linkedMapOf<String, Pair<Long, String>>()
      val now = System.currentTimeMillis()

      // 1) Se PACKAGE_USAGE_STATS estiver concedida, consulta apps usados recentemente
      if (hasUsageStatsPermission()) {
        val usageStatsManager =
          context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager != null) {
          val stats =
            usageStatsManager.queryUsageStats(
              UsageStatsManager.INTERVAL_DAILY,
              now - 6L * 60L * 60L * 1000L, // últimas 6 horas
              now,
            )
          stats
            ?.filter { it.lastTimeUsed > 0L }
            ?.sortedByDescending { it.lastTimeUsed }
            ?.forEach { stat ->
              if (isSafeNonSystemUserApp(stat.packageName)) {
                candidates.putIfAbsent(stat.packageName, stat.lastTimeUsed to "Uso recente")
              }
            }
        }
      }

      // 2) Complementa com runningAppProcesses (apps ativos no escopo visível ao processo)
      @Suppress("DEPRECATION")
      activityManager.runningAppProcesses?.forEach { proc ->
        proc.pkgList?.forEach { pkg ->
          if (isSafeNonSystemUserApp(pkg)) {
            candidates.putIfAbsent(pkg, now to "Processo ativo")
          }
        }
      }

      // 3) Também inclui apps de usuário lançáveis instalados (marcados como elegíveis ao
      // encerramento)
      // caso o Android restrinja a lista de runningAppProcesses no nível de API atual.
      if (candidates.isEmpty()) {
        installedAppsRepository.loadInstalledLaunchableApps().forEach { app ->
          if (isSafeNonSystemUserApp(app.packageName)) {
            candidates.putIfAbsent(app.packageName, 0L to "App de usuário em espera")
          }
        }
      }

      candidates.entries
        .take(25)
        .mapNotNull { (pkg, meta) ->
          runCatching {
              val appInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                  @Suppress("DEPRECATION") packageManager.getApplicationInfo(pkg, 0)
                }
              val label = packageManager.getApplicationLabel(appInfo).toString()
              val icon =
                installedAppsRepository.drawableToImageBitmap(
                  packageManager.getApplicationIcon(appInfo),
                  128,
                )
              BackgroundProcessInfo(
                packageName = pkg,
                appName = label,
                icon = icon,
                lastTimeUsedMs = meta.first,
                sourceLabel = meta.second,
                isSelectedToKill = previousSelections[pkg] ?: true,
              )
            }
            .getOrNull()
        }
    }

  /**
   * Garante estritamente que o próprio aplicativo e serviços/pacotes do sistema nunca sejam
   * encerrados.
   */
  private fun isSafeNonSystemUserApp(packageName: String): Boolean {
    if (packageName == context.packageName) return false
    if (packageName == "android" || packageName.startsWith("com.android.systemui")) return false

    return runCatching {
        val appInfo =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
          } else {
            @Suppress("DEPRECATION") packageManager.getApplicationInfo(packageName, 0)
          }
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        // Permite apenas aplicativos instalados pelo usuário (não sistema)
        !isSystem && !isUpdatedSystem
      }
      .getOrDefault(false)
  }

  /**
   * Executa a otimização permitida sem root:
   * 1. Limpa arquivos temporários de cache do próprio app.
   * 2. Encerra apenas os processos em segundo plano selecionados via killBackgroundProcesses.
   * 3. Solicita coleta de lixo da VM e mede a RAM recuperada.
   */
  suspend fun runOptimization(packagesToKill: List<String>): OptimizationResult =
    withContext(Dispatchers.IO) {
      val memBefore = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(memBefore)

      val clearedCacheBytes = clearOwnCacheSafely()

      var killedCount = 0
      for (pkg in packagesToKill) {
        if (isSafeNonSystemUserApp(pkg)) {
          runCatching {
            activityManager.killBackgroundProcesses(pkg)
            killedCount++
          }
        }
      }

      System.gc()

      val memAfter = ActivityManager.MemoryInfo()
      activityManager.getMemoryInfo(memAfter)
      val freedBytes = (memAfter.availMem - memBefore.availMem).coerceAtLeast(0L)
      val freedMb = freedBytes / (1024L * 1024L)

      OptimizationResult(
        freedMb = freedMb,
        killedCount = killedCount,
        clearedCacheKb = clearedCacheBytes / 1024L,
        timestampMs = System.currentTimeMillis(),
      )
    }

  /** Limpa apenas diretórios de cache sem tocar em DataStore ou SharedPreferences. */
  private fun clearOwnCacheSafely(): Long {
    var totalFreed = 0L
    listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
      totalFreed += deleteChildrenRecursively(dir)
    }
    return totalFreed
  }

  private fun deleteChildrenRecursively(dir: File): Long {
    if (!dir.exists() || !dir.isDirectory) return 0L
    var freed = 0L
    dir.listFiles()?.forEach { child ->
      freed +=
        if (child.isDirectory) {
          val sub = deleteChildrenRecursively(child)
          child.delete()
          sub
        } else {
          val size = child.length()
          if (child.delete()) size else 0L
        }
    }
    return freed
  }

  /**
   * Tenta ler uso global de CPU em /proc/stat; se bloqueado pelo SELinux (Android 8+), utiliza a
   * razão de frequência dos núcleos (/sys/devices/system/cpu) ou carga do processo.
   */
  private fun readRealCpuUsage(): Pair<Int, String> {
    // Tentativa 1: /proc/stat (delta total vs ocioso)
    runCatching {
        RandomAccessFile("/proc/stat", "r").use { reader ->
          val line = reader.readLine()
          if (line != null && line.startsWith("cpu ")) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 5) {
              val user = parts[1].toLong()
              val nice = parts[2].toLong()
              val system = parts[3].toLong()
              val idle = parts[4].toLong()
              val iowait = parts.getOrNull(5)?.toLongOrNull() ?: 0L
              val irq = parts.getOrNull(6)?.toLongOrNull() ?: 0L
              val softIrq = parts.getOrNull(7)?.toLongOrNull() ?: 0L
              val total = user + nice + system + idle + iowait + irq + softIrq

              val diffTotal = total - prevCpuTotal
              val diffIdle = (idle + iowait) - prevCpuIdle
              prevCpuTotal = total
              prevCpuIdle = idle + iowait

              if (diffTotal > 0L) {
                val pct = (((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()) * 100f).toInt()
                return pct.coerceIn(1, 100) to "Kernel /proc/stat"
              }
            }
          }
        }
      }

    // Tentativa 2: Frequência atual vs máxima dos núcleos da CPU (cpufreq)
    runCatching {
      val cpuDir = File("/sys/devices/system/cpu")
      val cores =
        cpuDir.listFiles { f -> f.isDirectory && f.name.matches(Regex("cpu\\d+")) } ?: emptyArray()
      var sumRatio = 0f
      var measuredCores = 0
      for (core in cores) {
        val curFile = File(core, "cpufreq/scaling_cur_freq")
        val maxFile = File(core, "cpufreq/cpuinfo_max_freq")
        if (curFile.canRead() && maxFile.canRead()) {
          val cur = curFile.readText().trim().toLongOrNull() ?: 0L
          val max = maxFile.readText().trim().toLongOrNull() ?: 0L
          if (cur > 0L && max > 0L) {
            sumRatio += (cur.toFloat() / max.toFloat()).coerceIn(0f, 1f)
            measuredCores++
          }
        }
      }
      if (measuredCores > 0) {
        val avgPct = ((sumRatio / measuredCores) * 100f).toInt().coerceIn(5, 100)
        return avgPct to "Clock médio ($measuredCores núcleos)"
      }
    }

    // Tentativa 3: Uso de CPU do processo via android.os.Process (sempre disponível)
    val nowWall = System.currentTimeMillis()
    val nowProc = Process.getElapsedCpuTime()
    val wallDelta = (nowWall - prevWallClockMs).coerceAtLeast(1L)
    val procDelta = (nowProc - prevProcessCpuMs).coerceAtLeast(0L)
    prevWallClockMs = nowWall
    prevProcessCpuMs = nowProc
    val coresCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    val procPct = ((procDelta.toFloat() / (wallDelta * coresCount).toFloat()) * 100f).toInt()
    return procPct.coerceIn(1, 100) to "Carga de CPU do processo"
  }

  /** Lê porcentagem, status de carregamento e temperatura da bateria (ou sensor térmico). */
  private fun readBatteryAndTemperature(): Triple<Int, Boolean, Float> {
    val batteryIntent =
      context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct =
      if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100

    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging =
      status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL

    // Temperatura da bateria vem em décimos de grau Celsius (ex: 345 = 34.5 °C)
    val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val tempC =
      if (rawTemp > 0) {
        rawTemp / 10f
      } else {
        readThermalZoneFallback()
      }

    return Triple(pct, isCharging, tempC)
  }

  private fun readThermalZoneFallback(): Float {
    return runCatching {
        val zone0 = File("/sys/class/thermal/thermal_zone0/temp")
        if (zone0.canRead()) {
          val raw = zone0.readText().trim().toFloatOrNull() ?: 0f
          if (raw > 1000f) raw / 1000f else raw
        } else {
          0f
        }
      }
      .getOrDefault(0f)
  }

  /** Lê o estado real de conectividade de rede (Wi-Fi, Dados Móveis, Ethernet ou Offline). */
  private fun readNetworkStatus(): NetworkConnectionType {
    val activeNetwork = connectivityManager.activeNetwork ?: return NetworkConnectionType.OFFLINE
    val caps =
      connectivityManager.getNetworkCapabilities(activeNetwork)
        ?: return NetworkConnectionType.OFFLINE
    return when {
      caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkConnectionType.WIFI
      caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkConnectionType.ETHERNET
      caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkConnectionType.CELLULAR
      else -> NetworkConnectionType.OFFLINE
    }
  }
}
