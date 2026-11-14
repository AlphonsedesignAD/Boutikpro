package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class StaffAccount(
  val uid: String,
  val username: String,
  val active: Boolean,
)

@Composable
fun StaffAccountsDialog(onDismiss: () -> Unit) {
  val functions = remember { FirebaseFunctions.getInstance() }
  val scope = rememberCoroutineScope()
  var accounts by remember { mutableStateOf<List<StaffAccount>>(emptyList()) }
  var username by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var loading by remember { mutableStateOf(false) }
  var saving by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf<String?>(null) }

  fun refresh() {
    scope.launch {
      loading = true
      try {
        val result = functions.getHttpsCallable("listStaffAccounts").call().await()
        val data = result.data as? Map<*, *>
        val rows = data?.get("accounts") as? List<*> ?: emptyList<Any>()
        accounts = rows.mapNotNull { row ->
          val item = row as? Map<*, *> ?: return@mapNotNull null
          StaffAccount(
            uid = item["uid"] as? String ?: return@mapNotNull null,
            username = item["username"] as? String ?: return@mapNotNull null,
            active = item["active"] as? Boolean ?: false,
          )
        }
        message = null
      } catch (e: Exception) {
        message = e.localizedMessage ?: "Impossible de charger les comptes."
      } finally {
        loading = false
      }
    }
  }

  LaunchedEffect(Unit) { refresh() }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Comptes vendeurs") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = username,
          onValueChange = { username = it.lowercase().filter { ch -> ch.isLetterOrDigit() || ch in "._-" }.take(30) },
          label = { Text("Nouvel identifiant") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text("Mot de passe") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            saving = true
            scope.launch {
              try {
                functions.getHttpsCallable("createStaffAccount")
                  .call(mapOf("username" to username, "password" to password)).await()
                username = ""
                password = ""
                message = "Compte créé."
                refresh()
              } catch (e: Exception) {
                message = e.localizedMessage ?: "Création impossible."
              } finally {
                saving = false
              }
            }
          },
          enabled = !saving && username.length >= 3 && password.length >= 4,
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (saving) CircularProgressIndicator()
          else Text("Créer le compte vendeur")
        }
        Spacer(Modifier.height(4.dp))
        Text("Comptes existants", style = MaterialTheme.typography.titleSmall)
        if (loading) {
          CircularProgressIndicator()
        } else if (accounts.isEmpty()) {
          Text("Aucun compte vendeur.")
        } else {
          LazyColumn(modifier = Modifier.height(180.dp)) {
            items(accounts, key = { it.uid }) { account ->
              Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column {
                    Text(account.username, style = MaterialTheme.typography.bodyMedium)
                    Text(if (account.active) "Actif" else "Désactivé", style = MaterialTheme.typography.bodySmall)
                  }
                  TextButton(
                    onClick = {
                      scope.launch {
                        try {
                          functions.getHttpsCallable("setStaffAccountActive")
                            .call(mapOf("uid" to account.uid, "active" to !account.active)).await()
                          refresh()
                        } catch (e: Exception) {
                          message = e.localizedMessage ?: "Modification impossible."
                        }
                      }
                    }
                  ) {
                    Text(if (account.active) "Désactiver" else "Activer")
                  }
                }
              }
            }
          }
        }
        if (message != null) {
          Text(message ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
  )
}
