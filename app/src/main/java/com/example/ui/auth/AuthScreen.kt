package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.IceBackground
import com.example.ui.theme.IceContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyAccent

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showContent by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    showContent = true
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  val infiniteTransition = rememberInfiniteTransition(label = "auth_pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "badge_pulse",
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          colors = listOf(DeepSapphire, CobaltPrimary, IceBackground, PureWhite)
        )
      )
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 560.dp)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { -32 },
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Surface(
            modifier = Modifier
              .size(72.dp)
              .scale(pulseScale),
            shape = CircleShape,
            color = PureWhite.copy(alpha = 0.16f),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = "Logo BoutikPro",
                tint = PureWhite,
                modifier = Modifier.size(38.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "BoutikPro",
            style = MaterialTheme.typography.displaySmall,
            color = PureWhite,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Gestion de boutique, stocks, codes-barres et caisse vendeur en temps réel",
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(tween(550, delayMillis = 120)) + slideInVertically(tween(550)) { 40 },
      ) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_boutik_hero),
              contentDescription = "Aperçu tableau de bord BoutikPro",
              modifier = Modifier
                .fillMaxWidth()
                .height(165.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, IceContainer, RoundedCornerShape(18.dp)),
              contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.height(18.dp))

            FeaturePillRow(
              icon = Icons.Filled.Inventory2,
              title = "Admin Stocks & Catégories",
              subtitle = "Créez vos produits, gérez les seuils et ajustez les quantités",
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeaturePillRow(
              icon = Icons.Filled.Print,
              title = "Impression Codes-Barres",
              subtitle = "Générez et imprimez les étiquettes PDF par catégorie",
            )
            Spacer(modifier = Modifier.height(10.dp))
            FeaturePillRow(
              icon = Icons.Filled.QrCodeScanner,
              title = "Caisse & Scan Vendeur",
              subtitle = "Scannez un article, panier instantané et paiement confirmé",
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (errorMessage != null) {
              Surface(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer,
              ) {
                Text(
                  text = errorMessage ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(12.dp),
                )
              }
            }

            Button(
              onClick = {
                errorMessage = null
                isLoading = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isLoading = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = { isLoading = false },
                )
              },
              enabled = !isLoading,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_sign_in_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = RoyalBlue,
                contentColor = PureWhite,
              ),
            ) {
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  color = PureWhite,
                  strokeWidth = 2.5.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = "Connexion en cours...",
                  style = MaterialTheme.typography.labelLarge,
                )
              } else {
                Text(
                  text = "Continuer avec Google",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Synchronisation cloud sécurisée pour votre boutique",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun FeaturePillRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(IceBackground)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = IceContainer,
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = CobaltPrimary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
