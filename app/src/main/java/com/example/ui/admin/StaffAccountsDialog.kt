package com.example.ui.admin

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class StaffAccount(
  val uid: String,
  val username: String,
  val active: Boolean,
)

private fun secondaryAuth(context: Context): FirebaseAuth {
  val app = try {
    FirebaseApp.getInstance("BoutikProStaffCreator")
  } catch (_: IllegalStateException) {
    FirebaseApp.initializeApp(
      context,
      FirebaseApp.getInstance().options,
      "BoutikProStaffCreator",
    ) ?: FirebaseApp.getInstance("BoutikProStaffCreator")
  }
  return FirebaseAuth.getInstance(app)
}

@Composable
fun StaffAccountsDialog(onDismiss: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val db = remember(context) {
    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
  }
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
        val snapshot = db.collection("staff").get().await()
        accounts = snapshot.documents.mapNotNull { doc ->
          val name = doc.getString("username") ?: return@mapNotNull null
          StaffAccount(doc.id, name, doc.getBoolean("active") == true)
        }.sortedBy { it.username.lowercase() }
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
          onValueChange = {
            username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch in "._-" }.take(30)
            message = null
          },
          label = { Text("Nouvel identifiant") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = password,
          onValueChange = { password = it; message = null },
          label = { Text("Mot de passe") },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Button(
          onClick = {
            saving = true
            message = null
            scope.launch {
              val auth = secondaryAuth(context)
              try {
                val email = "$username@boutikpro.app"
                val credential = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = credential.user?.uid ?: error("Compte Firebase non créé.")
                try {
                  db.collection("staff").document(uid).set(
                    mapOf(
                      "uid" to uid,
                      "username" to username,
                      "active" to true,
                      "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    )
                  ).await()
                } catch (e: Exception) {
                  credential.user?.delete()?.await()
                  throw e
                }
                auth.signOut()
                username = ""
                password = ""
                message = "Compte vendeur créé."
                refresh()
              } catch (e: Exception) {
                auth.signOut()
                message = e.localizedMessage ?: "Création impossible."
              } finally {
                saving = false
              }
            }
          },
          enabled = !saving && username.length >= 3 && password.length >= 6,
          modifier = Modifier.fillMaxWidth(),
        ) {
          if (saving) CircularProgressIndicator()
          else Text("Créer le compte vendeur")
        }
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
                          db.collection("staff").document(account.uid)
                            .update("active", !account.active).await()
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
        Spacer(Modifier.height(2.dp))
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } },
  )
}
