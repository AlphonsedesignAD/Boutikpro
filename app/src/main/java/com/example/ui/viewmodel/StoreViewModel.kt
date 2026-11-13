package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class AppRole(val label: String) {
  SELLER("Vendeur"),
  ADMIN("Admin"),
}

sealed interface ScanFeedback {
  data class Added(val product: Product, val newCartQty: Long) : ScanFeedback
  data class NotFound(val barcode: String) : ScanFeedback
  data class OutOfStock(val product: Product) : ScanFeedback
  data class StockLimitReached(val product: Product) : ScanFeedback
}

class StoreViewModel(
  private val repository: StoreRepository,
  val currentUserId: String,
) : ViewModel() {

  val categoriesState: StateFlow<UiState<List<Category>>> = repository.observeCategories(currentUserId)
    .map<List<Category>, UiState<List<Category>>> { UiState.Success(it) }
    .catch { error ->
      val root = unwrapFirestore(error) ?: error
      if (root is CancellationException) throw root
      Log.w(TAG, "Error observing categories", root)
      emit(UiState.Error(root.message ?: "Impossible de charger les catégories"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val productsState: StateFlow<UiState<List<Product>>> = repository.observeProducts(currentUserId)
    .map<List<Product>, UiState<List<Product>>> { UiState.Success(it) }
    .catch { error ->
      val root = unwrapFirestore(error) ?: error
      if (root is CancellationException) throw root
      Log.w(TAG, "Error observing products", root)
      emit(UiState.Error(root.message ?: "Impossible de charger les produits"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val salesState: StateFlow<UiState<List<Sale>>> = repository.observeSales(currentUserId)
    .map<List<Sale>, UiState<List<Sale>>> { UiState.Success(it) }
    .catch { error ->
      val root = unwrapFirestore(error) ?: error
      if (root is CancellationException) throw root
      Log.w(TAG, "Error observing sales", root)
      emit(UiState.Error(root.message ?: "Impossible de charger l'historique des ventes"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  private val _activeRole = MutableStateFlow(AppRole.SELLER)
  val activeRole: StateFlow<AppRole> = _activeRole.asStateFlow()

  private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
  val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

  private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CASH)
  val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

  private val _isCheckingOut = MutableStateFlow(false)
  val isCheckingOut: StateFlow<Boolean> = _isCheckingOut.asStateFlow()

  private val _lastConfirmedSale = MutableStateFlow<Sale?>(null)
  val lastConfirmedSale: StateFlow<Sale?> = _lastConfirmedSale.asStateFlow()

  private val _scanFeedback = MutableStateFlow<ScanFeedback?>(null)
  val scanFeedback: StateFlow<ScanFeedback?> = _scanFeedback.asStateFlow()

  private val _actionMessage = MutableStateFlow<String?>(null)
  val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

  private var lastScannedBarcode: String? = null
  private var lastScannedTimeMs: Long = 0L

  fun switchRole(role: AppRole) {
    _activeRole.value = role
  }

  fun selectPaymentMethod(method: PaymentMethod) {
    _selectedPaymentMethod.value = method
  }

  fun clearScanFeedback() {
    _scanFeedback.value = null
  }

  fun clearActionMessage() {
    _actionMessage.value = null
  }

  fun dismissConfirmedSale() {
    _lastConfirmedSale.value = null
  }

  fun onCameraBarcodeDetected(rawCode: String) {
    val cleanCode = rawCode.trim()
    if (cleanCode.isBlank()) return
    val now = System.currentTimeMillis()
    if (cleanCode == lastScannedBarcode && (now - lastScannedTimeMs) < SCAN_DEBOUNCE_MS) {
      return
    }
    lastScannedBarcode = cleanCode
    lastScannedTimeMs = now
    addProductByBarcode(cleanCode)
  }

  fun addProductByBarcode(rawCode: String) {
    val cleanCode = rawCode.trim()
    if (cleanCode.isBlank()) return
    val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
    val product = products.firstOrNull { it.barcode.equals(cleanCode, ignoreCase = true) }
    if (product == null) {
      _scanFeedback.value = ScanFeedback.NotFound(cleanCode)
      return
    }
    addProductToCart(product)
  }

  fun addProductToCart(product: Product) {
    if (product.stock <= 0L) {
      _scanFeedback.value = ScanFeedback.OutOfStock(product)
      return
    }
    val existing = _cartItems.value.firstOrNull { it.product.id == product.id }
    val currentQty = existing?.quantity ?: 0L
    if (currentQty >= product.stock) {
      _scanFeedback.value = ScanFeedback.StockLimitReached(product)
      return
    }
    val nextQty = currentQty + 1L
    _cartItems.update { list ->
      if (existing == null) {
        listOf(CartItem(product = product, quantity = 1L)) + list
      } else {
        list.map { item ->
          if (item.product.id == product.id) item.copy(product = product, quantity = nextQty)
          else item
        }
      }
    }
    _scanFeedback.value = ScanFeedback.Added(product, nextQty)
  }

  fun incrementCartItem(productId: String) {
    val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
    val liveProduct = products.firstOrNull { it.id == productId }
    _cartItems.update { list ->
      list.map { item ->
        if (item.product.id == productId) {
          val latest = liveProduct ?: item.product
          if (item.quantity < latest.stock) {
            item.copy(product = latest, quantity = item.quantity + 1L)
          } else {
            _scanFeedback.value = ScanFeedback.StockLimitReached(latest)
            item
          }
        } else {
          item
        }
      }
    }
  }

  fun decrementCartItem(productId: String) {
    _cartItems.update { list ->
      list.mapNotNull { item ->
        if (item.product.id == productId) {
          if (item.quantity <= 1L) null else item.copy(quantity = item.quantity - 1L)
        } else {
          item
        }
      }
    }
  }

  fun removeCartItem(productId: String) {
    _cartItems.update { list -> list.filterNot { it.product.id == productId } }
  }

  fun clearCart() {
    _cartItems.value = emptyList()
  }

  fun confirmPayment() {
    val items = _cartItems.value
    if (items.isEmpty() || _isCheckingOut.value) return
    viewModelScope.launch {
      _isCheckingOut.value = true
      val result = repository.confirmSale(items, _selectedPaymentMethod.value)
      _isCheckingOut.value = false
      result.fold(
        onSuccess = { sale ->
          _cartItems.value = emptyList()
          _scanFeedback.value = null
          _lastConfirmedSale.value = sale
        },
        onFailure = { error ->
          _actionMessage.value = error.localizedMessage ?: "Erreur lors de la confirmation du paiement"
        },
      )
    }
  }

  fun createCategory(name: String, colorHex: String, iconKey: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      repository.createCategory(name, colorHex, iconKey).fold(
        onSuccess = { _actionMessage.value = "Catégorie « ${name.trim()} » créée" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur création catégorie" },
      )
    }
  }

  fun updateCategory(category: Category) {
    if (category.name.isBlank()) return
    viewModelScope.launch {
      repository.updateCategory(category).fold(
        onSuccess = { _actionMessage.value = "Catégorie mise à jour" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur mise à jour catégorie" },
      )
    }
  }

  fun deleteCategory(categoryId: String) {
    viewModelScope.launch {
      repository.deleteCategory(categoryId).fold(
        onSuccess = { _actionMessage.value = "Catégorie supprimée" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur suppression catégorie" },
      )
    }
  }

  fun createProduct(
    categoryId: String,
    categoryName: String,
    name: String,
    barcode: String,
    price: Double,
    stock: Long,
    minStockAlert: Long,
  ) {
    viewModelScope.launch {
      repository.createProduct(
        categoryId = categoryId,
        categoryName = categoryName,
        name = name,
        barcode = barcode,
        price = price,
        stock = stock,
        minStockAlert = minStockAlert,
      ).fold(
        onSuccess = { _actionMessage.value = "Produit « ${name.trim()} » ajouté au stock" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur création produit" },
      )
    }
  }

  fun updateProduct(product: Product) {
    viewModelScope.launch {
      repository.updateProduct(product).fold(
        onSuccess = { _actionMessage.value = "Produit « ${product.name} » mis à jour" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur mise à jour produit" },
      )
    }
  }

  fun adjustStock(product: Product, delta: Long) {
    val nextStock = (product.stock + delta).coerceAtLeast(0L)
    viewModelScope.launch {
      repository.adjustProductStock(product.id, nextStock).fold(
        onSuccess = { _actionMessage.value = "Stock ${product.name} : $nextStock" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur mise à jour stock" },
      )
    }
  }

  fun deleteProduct(productId: String) {
    viewModelScope.launch {
      repository.deleteProduct(productId).fold(
        onSuccess = { _actionMessage.value = "Produit supprimé" },
        onFailure = { _actionMessage.value = it.localizedMessage ?: "Erreur suppression produit" },
      )
    }
  }

  private fun unwrapFirestore(error: Throwable): Throwable? {
    val fsError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (fsError != null) return fsError
    return if (error is CancellationException) error else error
  }

  private companion object {
    const val TAG = "StoreVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
    const val SCAN_DEBOUNCE_MS = 1600L
  }
}
