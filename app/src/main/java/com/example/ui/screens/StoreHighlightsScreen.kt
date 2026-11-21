package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StoreHighlight
import com.example.ui.theme.PsCyanGlow
import com.example.ui.theme.PsIceWhite
import com.example.ui.theme.PsNavyDeep
import com.example.ui.theme.PsNavySurface
import com.example.ui.theme.PsNavySurfaceElevated
import com.example.ui.theme.PsSlateText

/**
 * Aba "Loja/Destaques" com banner principal 16:9 e vitrine horizontal de categorias que abrem
 * buscas reais na Google Play Store.
 */
@Composable
fun StoreHighlightsScreen(
  highlights: List<StoreHighlight>,
  focusedIndex: Int,
  onSelectHighlight: (StoreHighlight) -> Unit,
  onRefreshLibrary: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val hero = highlights.firstOrNull { it.isHero } ?: highlights.firstOrNull()
  val secondaryCards = highlights.filter { it != hero }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 20.dp)
  ) {
    Spacer(modifier = Modifier.height(10.dp))

    // Banner principal 16:9
    if (hero != null) {
      val heroFocused = focusedIndex == 0
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = PsNavySurface,
        border =
          BorderStroke(
            width = if (heroFocused) 2.dp else 1.dp,
            color = if (heroFocused) PsIceWhite else PsCyanGlow.copy(alpha = 0.4f),
          ),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onSelectHighlight(hero) }
            .testTag("store_hero_card"),
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = hero.imageRes),
            contentDescription = hero.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, PsNavyDeep.copy(alpha = 0.92f))
                  )
                )
          )
          Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)
          ) {
            Text(
              text = hero.genre,
              style = MaterialTheme.typography.labelSmall,
              color = PsCyanGlow,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = hero.title,
              style = MaterialTheme.typography.headlineMedium,
              color = PsIceWhite,
            )
            Text(
              text = hero.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = PsSlateText,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { onSelectHighlight(hero) },
              colors =
                ButtonDefaults.buttonColors(containerColor = PsCyanGlow, contentColor = PsNavyDeep),
              shape = RoundedCornerShape(20.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Explorar na Play Store", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Coleções em Destaque",
        style = MaterialTheme.typography.titleLarge,
        color = PsIceWhite,
      )
      OutlinedButton(
        onClick = onRefreshLibrary,
        border = BorderStroke(1.dp, PsNavySurfaceElevated),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PsCyanGlow),
      ) {
        Icon(
          imageVector = Icons.Filled.Refresh,
          contentDescription = "Atualizar apps instalados",
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Atualizar Apps")
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(
      contentPadding = PaddingValues(horizontal = 20.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      itemsIndexed(secondaryCards, key = { _, item -> item.id }) { idx, item ->
        val isFocused = (idx + 1) == focusedIndex
        val scale by
          animateFloatAsState(
            targetValue = if (isFocused) 1.04f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "store_card_scale",
          )

        Surface(
          shape = RoundedCornerShape(20.dp),
          color = PsNavySurface,
          border =
            BorderStroke(
              width = if (isFocused) 2.dp else 1.dp,
              color = if (isFocused) PsIceWhite else PsNavySurfaceElevated,
            ),
          modifier =
            Modifier.width(268.dp)
              .scale(scale)
              .clip(RoundedCornerShape(20.dp))
              .clickable { onSelectHighlight(item) }
              .testTag("store_card_${item.id}"),
        ) {
          Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
              Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
              )
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .background(
                      Brush.verticalGradient(
                        listOf(Color.Transparent, PsNavyDeep.copy(alpha = 0.7f))
                      )
                    )
              )
            }
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = item.genre,
                style = MaterialTheme.typography.labelSmall,
                color = PsCyanGlow,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = PsIceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = PsSlateText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = "Ver na Play Store",
                  style = MaterialTheme.typography.labelLarge,
                  color = PsCyanGlow,
                )
                Icon(
                  imageVector = Icons.Filled.OpenInNew,
                  contentDescription = null,
                  tint = PsCyanGlow,
                  modifier = Modifier.size(15.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
