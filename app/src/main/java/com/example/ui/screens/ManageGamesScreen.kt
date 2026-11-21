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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.InstalledAppInfo
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsMutedText
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText

/**
 * Aba "Gerenciar Jogos": lista aplicativos instalados no aparelho via PackageManager com checkboxes
 * para o usuário escolher quais aparecem na Biblioteca (persistido no DataStore).
 */
@Composable
fun ManageGamesScreen(
  apps: List<InstalledAppInfo>,
  selectedPackages: Set<String>,
  searchQuery: String,
  onlyGamesFilter: Boolean,
  focusedIndex: Int,
  onSearchQueryChange: (String) -> Unit,
  onFilterOnlyGamesChange: (Boolean) -> Unit,
  onToggleGame: (String, Boolean) -> Unit,
  onAutoDetectGames: () -> Unit,
  onRefreshApps: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(focusedIndex, apps.size) {
    if (apps.isNotEmpty() && focusedIndex in apps.indices) {
      listState.animateScrollToItem(focusedIndex)
    }
  }

  Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Gerenciar Jogos da Biblioteca",
          style = MaterialTheme.typography.titleLarge,
          color = PsIceWhite,
        )
        Text(
          text = "${selectedPackages.size} selecionado(s) • salvo automaticamente via DataStore",
          style = MaterialTheme.typography.bodyMedium,
          color = PsSlateText,
        )
      }
      IconButton(
        onClick = onRefreshApps,
        modifier = Modifier.testTag("btn_refresh_installed_apps"),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Atualizar lista de apps instalados",
          tint = PsCyanGlow,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Barra de busca
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      placeholder = { Text("Buscar app ou pacote...", color = PsMutedText) },
      leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = PsCyanGlow) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors =
        OutlinedTextFieldDefaults.colors(
          focusedBorderColor = PsCyanGlow,
          unfocusedBorderColor = PsNavySurfaceElevated,
          focusedTextColor = PsIceWhite,
          unfocusedTextColor = PsIceWhite,
          focusedContainerColor = PsNavyDeep.copy(alpha = 0.8f),
          unfocusedContainerColor = PsNavyDeep.copy(alpha = 0.6f),
        ),
      modifier = Modifier.fillMaxWidth().testTag("manage_search_input"),
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Filtros rápidos
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = !onlyGamesFilter,
        onClick = { onFilterOnlyGamesChange(false) },
        label = { Text("Todos os Apps") },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = PsCyanGlow.copy(alpha = 0.22f),
            selectedLabelColor = PsIceWhite,
            labelColor = PsSlateText,
          ),
      )
      FilterChip(
        selected = onlyGamesFilter,
        onClick = { onFilterOnlyGamesChange(true) },
        label = { Text("Somente Jogos") },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = PsCyanGlow.copy(alpha = 0.22f),
            selectedLabelColor = PsIceWhite,
            labelColor = PsSlateText,
          ),
      )
      Spacer(modifier = Modifier.weight(1f))
      OutlinedButton(
        onClick = onAutoDetectGames,
        border = BorderStroke(1.dp, PsCyanGlow.copy(alpha = 0.55f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.AutoAwesome,
          contentDescription = null,
          tint = PsCyanGlow,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Auto", color = PsCyanGlow)
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (apps.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          text = "Nenhum aplicativo encontrado com esse filtro.",
          style = MaterialTheme.typography.bodyLarge,
          color = PsSlateText,
        )
      }
    } else {
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("manage_games_list"),
      ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
          val isChecked = app.packageName in selectedPackages
          val isFocused = index == focusedIndex

          Surface(
            shape = RoundedCornerShape(16.dp),
            color =
              when {
                isFocused -> PsNavySurfaceElevated
                isChecked -> PsNavySurface.copy(alpha = 0.92f)
                else -> PsNavyDeep.copy(alpha = 0.72f)
              },
            border =
              BorderStroke(
                width = if (isFocused) 2.dp else 1.dp,
                color =
                  when {
                    isFocused -> PsIceWhite
                    isChecked -> PsCyanGlow.copy(alpha = 0.55f)
                    else -> PsNavySurfaceElevated.copy(alpha = 0.6f)
                  },
              ),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onToggleGame(app.packageName, !isChecked) },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              if (app.icon != null) {
                Image(
                  bitmap = app.icon,
                  contentDescription = app.appName,
                  modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)),
                  contentScale = ContentScale.Fit,
                )
              } else {
                Box(
                  modifier =
                    Modifier.size(46.dp)
                      .clip(RoundedCornerShape(12.dp))
                      .background(PsNavySurfaceElevated),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Filled.SportsEsports,
                    contentDescription = null,
                    tint = PsCyanGlow,
                  )
                }
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = PsIceWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                  )
                  if (app.isGame) {
                    Box(
                      modifier =
                        Modifier.clip(RoundedCornerShape(6.dp))
                          .background(PsCyanGlow.copy(alpha = 0.18f))
                          .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                      Text(
                        text = "JOGO",
                        style = MaterialTheme.typography.labelSmall,
                        color = PsCyanGlow,
                      )
                    }
                  }
                }
                Text(
                  text = app.packageName,
                  style = MaterialTheme.typography.labelSmall,
                  color = PsMutedText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Checkbox(
                checked = isChecked,
                onCheckedChange = { checked -> onToggleGame(app.packageName, checked) },
                colors =
                  CheckboxDefaults.colors(
                    checkedColor = PsCyanGlow,
                    uncheckedColor = PsSlateText,
                    checkmarkColor = PsNavyDeep,
                  ),
                modifier = Modifier.testTag("manage_checkbox_${app.packageName}"),
              )
            }
          }
        }
      }
    }
  }
}
