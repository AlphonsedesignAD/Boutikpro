package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.DeepSapphire
import com.example.ui.theme.IceBackground
import com.example.ui.theme.PureWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var loading by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(DeepSapphire, CobaltPrimary, IceBackground))),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      shape = RoundedCornerShape(28.dp),
      color = PureWhite,
      shadowElevation = 12.dp,
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(
          imageVector = Icons.Filled.Storefront,
          contentDescription = "BoutikPro",
          tint = CobaltPrimary,
        )
        Text(
          text = "BoutikPro",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = DeepSapphire,
        )
        Text(
          text = "Connectez-vous avec votre identifiant.",
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
          value = username,
          onValueChange = { username = it.trimStart(); error = null },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Identifiant") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
          shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it; error = null },
          modifier = Modifier.fillMaxWidth(),
          label = { Text("Mot de passe") },
          singleLine = true,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
          shape = RoundedCornerShape(14.dp),
        )
        if (error != null) {
          Text(
            text = error ?: "",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
          )
        }
        Button(
          onClick = {
            if (username.isBlank() || password.isBlank()) {
              error = "Saisissez votre identifiant et votre mot de passe."
              return@Button
            }
            loading = true
            error = null
            scope.launch {
              try {
                val response = FirebaseFunctions.getInstance()
                  .getHttpsCallable("loginWithUsername")
                  .call(mapOf("username" to username.trim(), "password" to password))
                  .await()
                val data = response.data as? Map<*, *>
                  ?: throw IllegalStateException("Réponse du serveur invalide.")
                val token = data["token"] as? String
                  ?: throw IllegalStateException("Jeton de connexion absent.")
                Firebase.auth.signInWithCustomToken(token).await()
                onAuthSuccess()
              } catch (e: Exception) {
                error = e.localizedMessage ?: "Connexion impossible. Vérifiez vos identifiants."
              } finally {
                loading = false
              }
            }
          },
          enabled = !loading,
          modifier = Modifier.fillMaxWidth().height(54.dp),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
        ) {
          if (loading) CircularProgressIndicator(color = PureWhite)
          else Text("Se connecter", fontWeight = FontWeight.Bold)
        }
        Text(
          text = "Les comptes sont créés par l'administrateur.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      }
    }
  }
}
