package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.StoreRepository
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.AdminTab
import com.example.ui.admin.StaffAccountsDialog
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.BoutikTopBar
import com.example.ui.seller.SellerScreen
import com.example.ui.seller.SellerTab
import com.example.ui.theme.BoutikProTheme
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.IceBackground
import com.example.ui.theme.IceContainer
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateInk
import com.example.ui.viewmodel.AppRole
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      BoutikProTheme {
        AppNavigation()
      }
    }
  }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val user by auth.currentUserFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var isAdmin by remember(user?.uid) { mutableStateOf(false) }
  var checking by remember(user?.uid) { mutableStateOf(user != null) }
  val context = LocalContext.current
  val db = remember(context) {
    FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
  }

  LaunchedEffect(user?.uid) {
    val current = user
    if (current == null) {
      isAdmin = false
      checking = false
    } else {
      checking = true
      try {
        current.reload().await()
        val email = current.email.orEmpty().lowercase()
        isAdmin = email == "admin@boutikpro.app"
        if (!isAdmin) {
          val profile = db.collection("staff").document(current.uid).get().await()
          if (!profile.exists() || profile.getBoolean("active") != true) {
            auth.signOut()
          }
        }
      } catch (_: Exception) {
        auth.signOut()
      } finally {
        checking = false
      }
    }
  }

  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else if (checking) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = CobaltPrimary)
    }
  } else {
    key(user!!.uid) {
      MainStoreScreen(
        currentUserId = "boutikpro-store",
        userEmail = if (isAdmin) "Administrateur" else user!!.email?.substringBefore("@") ?: "Vendeur",
        isAdmin = isAdmin,
      )
    }
  }
}

private fun FirebaseAuth.currentUserFlow() = kotlinx.coroutines.flow.callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun MainStoreScreen(
  currentUserId: String,
  userEmail: String?,
  isAdmin: Boolean,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: StoreViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        StoreViewModel(StoreRepository(db, ownerId = currentUserId), currentUserId)
      }
    },
  )

  val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
  val categoriesState by viewModel.categoriesState.collectAsStateWithLifecycle()
  val productsState by viewModel.productsState.collectAsStateWithLifecycle()
  val salesState by viewModel.salesState.collectAsStateWithLifecycle()
  val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
  val selectedPaymentMethod by viewModel.selectedPaymentMethod.collectAsStateWithLifecycle()
  val isCheckingOut by viewModel.isCheckingOut.collectAsStateWithLifecycle()
  val scanFeedback by viewModel.scanFeedback.collectAsStateWithLifecycle()
  val lastConfirmedSale by viewModel.lastConfirmedSale.collectAsStateWithLifecycle()
  val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

  var sellerTab by rememberSaveable { mutableStateOf(SellerTab.POS) }
  var adminTab by rememberSaveable { mutableStateOf(AdminTab.INVENTORY) }
  val snackbarHostState = remember { SnackbarHostState() }
  var showStaffAccounts by remember { mutableStateOf(false) }

  LaunchedEffect(isAdmin) {
    viewModel.switchRole(if (isAdmin) AppRole.ADMIN else AppRole.SELLER)
  }

  if (showStaffAccounts && isAdmin) {
    StaffAccountsDialog(onDismiss = { showStaffAccounts = false })
  }

  LaunchedEffect(actionMessage) {
    actionMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.clearActionMessage()
    }
  }

  androidx.compose.foundation.layout.BoxWithConstraints(
    modifier = Modifier.fillMaxSize()
  ) {
    val isExpandedScreen = maxWidth >= 700.dp

    Scaffold(
      containerColor = IceBackground,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      topBar = {
        BoutikTopBar(
          activeRole = activeRole,
          onRoleSelected = viewModel::switchRole,
          userEmail = userEmail,
          canManageAccounts = isAdmin,
          onManageAccounts = { showStaffAccounts = true },
          canSwitchRole = isAdmin,
          onSignOut = {
            signOut(
              credentialManager = credentialManager,
              scope = scope,
            )
          },
        )
      },
      bottomBar = {
        if (!isExpandedScreen) {
          BoutikBottomNavigation(
            activeRole = activeRole,
            sellerTab = sellerTab,
            adminTab = adminTab,
            onSellerTabSelected = { sellerTab = it },
            onAdminTabSelected = { adminTab = it },
          )
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (isExpandedScreen) {
          BoutikNavigationRail(
            activeRole = activeRole,
            sellerTab = sellerTab,
            adminTab = adminTab,
            onSellerTabSelected = { sellerTab = it },
            onAdminTabSelected = { adminTab = it },
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
          contentAlignment = Alignment.TopCenter,
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .widthIn(max = 840.dp)
          ) {
            when (activeRole) {
              AppRole.SELLER -> SellerScreen(
                selectedTab = sellerTab,
                productsState = productsState,
                salesState = salesState,
                cartItems = cartItems,
                selectedPaymentMethod = selectedPaymentMethod,
                isCheckingOut = isCheckingOut,
                scanFeedback = scanFeedback,
                lastConfirmedSale = lastConfirmedSale,
                onCameraBarcodeDetected = viewModel::onCameraBarcodeDetected,
                onManualBarcodeSubmit = viewModel::addProductByBarcode,
                onProductQuickAdd = viewModel::addProductToCart,
                onIncrementCart = viewModel::incrementCartItem,
                onDecrementCart = viewModel::decrementCartItem,
                onRemoveCart = viewModel::removeCartItem,
                onClearCart = viewModel::clearCart,
                onSelectPaymentMethod = viewModel::selectPaymentMethod,
                onConfirmPayment = viewModel::confirmPayment,
                onDismissConfirmedSale = viewModel::dismissConfirmedSale,
                onClearScanFeedback = viewModel::clearScanFeedback,
              )
              AppRole.ADMIN -> AdminScreen(
                selectedTab = adminTab,
                categoriesState = categoriesState,
                productsState = productsState,
                onCreateCategory = viewModel::createCategory,
                onUpdateCategory = viewModel::updateCategory,
                onDeleteCategory = viewModel::deleteCategory,
                onCreateProduct = viewModel::createProduct,
                onUpdateProduct = viewModel::updateProduct,
                onAdjustStock = viewModel::adjustStock,
                onDeleteProduct = viewModel::deleteProduct,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun BoutikBottomNavigation(
  activeRole: AppRole,
  sellerTab: SellerTab,
  adminTab: AdminTab,
  onSellerTabSelected: (SellerTab) -> Unit,
  onAdminTabSelected: (AdminTab) -> Unit,
) {
  NavigationBar(
    containerColor = PureWhite,
    windowInsets = WindowInsets.navigationBars,
  ) {
    val itemColors = NavigationBarItemDefaults.colors(
      selectedIconColor = CobaltPrimary,
      selectedTextColor = CobaltPrimary,
      indicatorColor = IceContainer,
      unselectedIconColor = SlateInk,
      unselectedTextColor = SlateInk,
    )
    if (activeRole == AppRole.SELLER) {
      NavigationBarItem(
        selected = sellerTab == SellerTab.POS,
        onClick = { onSellerTabSelected(SellerTab.POS) },
        icon = {
          Icon(
            imageVector = if (sellerTab == SellerTab.POS) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
            contentDescription = SellerTab.POS.label,
          )
        },
        label = { Text(SellerTab.POS.label, style = MaterialTheme.typography.labelMedium) },
        colors = itemColors,
      )
      NavigationBarItem(
        selected = sellerTab == SellerTab.HISTORY,
        onClick = { onSellerTabSelected(SellerTab.HISTORY) },
        icon = {
          Icon(
            imageVector = if (sellerTab == SellerTab.HISTORY) Icons.AutoMirrored.Filled.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = SellerTab.HISTORY.label,
          )
        },
        label = { Text(SellerTab.HISTORY.label, style = MaterialTheme.typography.labelMedium) },
        colors = itemColors,
      )
    } else {
      NavigationBarItem(
        selected = adminTab == AdminTab.INVENTORY,
        onClick = { onAdminTabSelected(AdminTab.INVENTORY) },
        icon = {
          Icon(
            imageVector = if (adminTab == AdminTab.INVENTORY) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
            contentDescription = AdminTab.INVENTORY.label,
          )
        },
        label = { Text(AdminTab.INVENTORY.label, style = MaterialTheme.typography.labelMedium) },
        colors = itemColors,
      )
      NavigationBarItem(
        selected = adminTab == AdminTab.BARCODES,
        onClick = { onAdminTabSelected(AdminTab.BARCODES) },
        icon = {
          Icon(
            imageVector = if (adminTab == AdminTab.BARCODES) Icons.Filled.Print else Icons.Outlined.Print,
            contentDescription = AdminTab.BARCODES.label,
          )
        },
        label = { Text(AdminTab.BARCODES.label, style = MaterialTheme.typography.labelMedium) },
        colors = itemColors,
      )
    }
  }
}

@Composable
private fun BoutikNavigationRail(
  activeRole: AppRole,
  sellerTab: SellerTab,
  adminTab: AdminTab,
  onSellerTabSelected: (SellerTab) -> Unit,
  onAdminTabSelected: (AdminTab) -> Unit,
) {
  NavigationRail(containerColor = PureWhite) {
    if (activeRole == AppRole.SELLER) {
      NavigationRailItem(
        selected = sellerTab == SellerTab.POS,
        onClick = { onSellerTabSelected(SellerTab.POS) },
        icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = SellerTab.POS.label) },
        label = { Text(SellerTab.POS.label) },
      )
      NavigationRailItem(
        selected = sellerTab == SellerTab.HISTORY,
        onClick = { onSellerTabSelected(SellerTab.HISTORY) },
        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = SellerTab.HISTORY.label) },
        label = { Text(SellerTab.HISTORY.label) },
      )
    } else {
      NavigationRailItem(
        selected = adminTab == AdminTab.INVENTORY,
        onClick = { onAdminTabSelected(AdminTab.INVENTORY) },
        icon = { Icon(Icons.Filled.Inventory2, contentDescription = AdminTab.INVENTORY.label) },
        label = { Text(AdminTab.INVENTORY.label) },
      )
      NavigationRailItem(
        selected = adminTab == AdminTab.BARCODES,
        onClick = { onAdminTabSelected(AdminTab.BARCODES) },
        icon = { Icon(Icons.Filled.Print, contentDescription = AdminTab.BARCODES.label) },
        label = { Text(AdminTab.BARCODES.label) },
      )
    }
  }
}
