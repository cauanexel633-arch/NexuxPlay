package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório que consulta o PackageManager do Android para listar aplicativos lançáveis
 * instalados no dispositivo, carregar suas capas/ícones e gerar Intents de inicialização.
 */
class InstalledAppsRepository(private val context: Context) {

  private val packageManager: PackageManager = context.packageManager

  /**
   * Busca todos os aplicativos com ícone no launcher (excluindo o próprio Game Hub). O uso de
   * `<queries>` para MAIN/LAUNCHER no AndroidManifest garante compatibilidade no Android 11+.
   */
  suspend fun loadInstalledLaunchableApps(): List<InstalledAppInfo> =
    withContext(Dispatchers.IO) {
      val mainIntent =
        Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolveInfos =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
          )
        } else {
          @Suppress("DEPRECATION")
          packageManager.queryIntentActivities(mainIntent, 0)
        }

      val seenPackages = HashSet<String>()
      val results = mutableListOf<InstalledAppInfo>()

      for (resolveInfo in resolveInfos) {
        val activityInfo = resolveInfo.activityInfo ?: continue
        val packageName = activityInfo.packageName ?: continue

        // Nunca exibir o próprio Game Hub na lista de seleção de jogos
        if (packageName == context.packageName) continue
        if (!seenPackages.add(packageName)) continue

        val appInfo = activityInfo.applicationInfo
        val label =
          runCatching { resolveInfo.loadLabel(packageManager).toString() }.getOrDefault(packageName)

        val isGame = isGameApplication(appInfo)

        val versionName =
          runCatching {
              val pkgInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                  @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0)
                }
              pkgInfo.versionName ?: "1.0"
            }
            .getOrDefault("1.0")

        val iconBitmap =
          runCatching {
              val drawable = packageManager.getApplicationIcon(appInfo)
              drawableToImageBitmap(drawable, targetSizePx = 224)
            }
            .getOrNull()

        results.add(
          InstalledAppInfo(
            packageName = packageName,
            appName = label,
            isGame = isGame,
            versionName = versionName,
            icon = iconBitmap,
          )
        )
      }

      // Ordena colocando jogos detectados primeiro e depois em ordem alfabética
      results.sortedWith(
        compareByDescending<InstalledAppInfo> { it.isGame }.thenBy { it.appName.lowercase() }
      )
    }

  /** Verifica se o aplicativo é classificado pelo sistema como jogo (API 26+ CATEGORY_GAME). */
  @Suppress("DEPRECATION")
  private fun isGameApplication(appInfo: ApplicationInfo): Boolean {
    if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return true
    return (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
  }

  /**
   * Obtém a Intent oficial para iniciar o jogo escolhido pelo pacote. Retorna null caso o pacote
   * tenha sido desinstalado ou não possua atividade inicializável.
   */
  fun getLaunchIntent(packageName: String): Intent? {
    return packageManager.getLaunchIntentForPackage(packageName)?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }
  }

  /** Converte qualquer Drawable (incluindo AdaptiveIconDrawable) para ImageBitmap nítido. */
  fun drawableToImageBitmap(drawable: Drawable, targetSizePx: Int = 192): ImageBitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      val src = drawable.bitmap
      if (src.width >= targetSizePx && src.height >= targetSizePx) {
        return src.asImageBitmap()
      }
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSizePx
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSizePx
    val finalW = width.coerceIn(96, targetSizePx)
    val finalH = height.coerceIn(96, targetSizePx)
    val bitmap = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
  }
}
