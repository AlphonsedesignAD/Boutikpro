package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.DeepNavyInk
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.IceContainer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyAccent
import com.example.ui.viewmodel.AppRole
import com.example.util.BarcodeUtils

@Composable
fun BoutikTopBar(
  activeRole: AppRole,
  onRoleSelected: (AppRole) -> Unit,
  userEmail: String?,
  onSignOut: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        Brush.horizontalGradient(
          colors = listOf(DeepSapphire, CobaltPrimary, RoyalBlue)
        )
      )
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = PureWhite.copy(alpha = 0.16f),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = "BoutikPro",
                tint = PureWhite,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "BoutikPro",
              style = MaterialTheme.typography.titleLarge,
              color = PureWhite,
            )
            if (!userEmail.isNullOrBlank()) {
              Text(
                text = userEmail,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        IconButton(
          onClick = onSignOut,
          modifier = Modifier.testTag("sign_out_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = "Déconnexion",
            tint = PureWhite,
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      AnimatedRoleSwitcher(
        activeRole = activeRole,
        onRoleSelected = onRoleSelected,
      )
    }
  }
}

@Composable
fun AnimatedRoleSwitcher(
  activeRole: AppRole,
  onRoleSelected: (AppRole) -> Unit,
) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxWidth()
      .height(48.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(PureWhite.copy(alpha = 0.14f))
      .padding(4.dp)
  ) {
    val pillWidth = maxWidth / 2
    val indicatorOffset by animateDpAsState(
      targetValue = if (activeRole == AppRole.SELLER) 0.dp else pillWidth,
      animationSpec = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
      ),
      label = "role_pill_offset",
    )

    Box(
      modifier = Modifier
        .offset(x = indicatorOffset)
        .width(pillWidth)
        .fillMaxHeight()
        .clip(RoundedCornerShape(12.dp))
        .background(PureWhite)
    )

    Row(modifier = Modifier.fillMaxSize()) {
      RoleTabOption(
        label = "Espace Vendeur",
        icon = Icons.Filled.PointOfSale,
        selected = activeRole == AppRole.SELLER,
        onClick = { onRoleSelected(AppRole.SELLER) },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .testTag("role_seller_tab"),
      )
      RoleTabOption(
        label = "Espace Admin",
        icon = Icons.Filled.AdminPanelSettings,
        selected = activeRole == AppRole.ADMIN,
        onClick = { onRoleSelected(AppRole.ADMIN) },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .testTag("role_admin_tab"),
      )
    }
  }
}

@Composable
private fun RoleTabOption(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val textColor by animateColorAsState(
    targetValue = if (selected) CobaltPrimary else PureWhite,
    label = "role_text_color",
  )

  Row(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = textColor,
      modifier = Modifier.size(18.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      ),
      color = textColor,
    )
  }
}

@Composable
fun BarcodePreviewView(
  barcode: String,
  modifier: Modifier = Modifier,
  heightDp: Int = 56,
) {
  val bitmap = remember(barcode) {
    BarcodeUtils.createBarcodeBitmap(barcode, width = 540, height = 150)
  }

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(PureWhite)
      .border(1.dp, IceContainer, RoundedCornerShape(12.dp))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Code-barres $barcode",
        modifier = Modifier
          .fillMaxWidth()
          .height(heightDp.dp),
        contentScale = ContentScale.FillBounds,
      )
      Spacer(modifier = Modifier.height(4.dp))
    }
    Text(
      text = barcode,
      fontFamily = JetBrainsMonoFamily,
      style = MaterialTheme.typography.labelSmall,
      color = DeepNavyInk,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
fun AnimatedPriceDisplay(
  amount: Double,
  style: androidx.compose.ui.text.TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val formatted = remember(amount) { BarcodeUtils.formatPrice(amount) }
  AnimatedContent(
    targetState = formatted,
    transitionSpec = {
      (fadeIn() + slideInVertically { -it / 3 }) togetherWith
        (fadeOut() + slideOutVertically { it / 3 })
    },
    label = "price_roll",
    modifier = modifier,
  ) { target ->
    Text(
      text = target,
      style = style.copy(fontFamily = JetBrainsMonoFamily, fontWeight = FontWeight.Bold),
      color = color,
    )
  }
}

fun categoryIconFor(key: String): ImageVector = when (key) {
  "checkroom" -> Icons.Filled.Checkroom
  "watch" -> Icons.Filled.Watch
  "devices" -> Icons.Filled.Devices
  "spa" -> Icons.Filled.Spa
  "local_cafe" -> Icons.Filled.LocalCafe
  "shopping_bag" -> Icons.Filled.ShoppingBag
  else -> Icons.Filled.Category
}

fun parseHexColor(hex: String, fallback: Color = SkyAccent): Color {
  return runCatching {
    Color(android.graphics.Color.parseColor(hex))
  }.getOrDefault(fallback)
}
