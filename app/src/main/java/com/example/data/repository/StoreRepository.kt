package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.data.model.SaleItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    db = FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    auth = Firebase.auth,
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("L'utilisateur doit être connecté avec Google pour accéder à la boutique.")
  }

  fun observeCategories(userId: String): Flow<List<Category>> = flow {
    val path = "users/$userId/categories"
    emitAll(
      db.collection("users").document(userId).collection("categories")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            Category::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  fun observeProducts(userId: String): Flow<List<Product>> = flow {
    val path = "users/$userId/products"
    emitAll(
      db.collection("users").document(userId).collection("products")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            Product::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedBy { it.name.lowercase(Locale.getDefault()) }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  fun observeSales(userId: String): Flow<List<Sale>> = flow {
    val path = "users/$userId/sales"
    emitAll(
      db.collection("users").document(userId).collection("sales")
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            Sale::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
          ).sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
        .catch { error ->
          logFlowError(error, OperationType.LIST, path, userId)
          throw error
        }
    )
  }

  suspend fun createCategory(
    name: String,
    colorHex: String,
    iconKey: String,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("categories").document()
    val payload = mapOf(
      "userId" to uid,
      "name" to name.trim(),
      "colorHex" to colorHex,
      "iconKey" to iconKey,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateCategory(category: Category): Result<Unit> {
    val uid = requireUserId()
    require(category.id.isNotBlank()) { "ID catégorie requis" }
    val docRef = db.collection("users").document(uid).collection("categories").document(category.id)
    val updates = mapOf(
      "name" to category.name.trim(),
      "colorHex" to category.colorHex,
      "iconKey" to category.iconKey,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteCategory(categoryId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("categories").document(categoryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun createProduct(
    categoryId: String,
    categoryName: String,
    name: String,
    barcode: String,
    price: Double,
    stock: Long,
    minStockAlert: Long,
  ): Result<String> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("products").document()
    val payload = mapOf(
      "userId" to uid,
      "categoryId" to categoryId,
      "categoryName" to categoryName.trim(),
      "name" to name.trim(),
      "barcode" to barcode.trim(),
      "price" to price,
      "stock" to stock.coerceAtLeast(0L),
      "minStockAlert" to minStockAlert.coerceAtLeast(0L),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateProduct(product: Product): Result<Unit> {
    val uid = requireUserId()
    require(product.id.isNotBlank()) { "ID produit requis" }
    val docRef = db.collection("users").document(uid).collection("products").document(product.id)
    val updates = mapOf(
      "categoryId" to product.categoryId,
      "categoryName" to product.categoryName.trim(),
      "name" to product.name.trim(),
      "barcode" to product.barcode.trim(),
      "price" to product.price,
      "stock" to product.stock.coerceAtLeast(0L),
      "minStockAlert" to product.minStockAlert.coerceAtLeast(0L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun adjustProductStock(productId: String, newStock: Long): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("products").document(productId)
    val updates = mapOf(
      "stock" to newStock.coerceAtLeast(0L),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(productId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("products").document(productId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun confirmSale(
    cartItems: List<CartItem>,
    paymentMethod: PaymentMethod,
  ): Result<Sale> {
    val uid = requireUserId()
    if (cartItems.isEmpty()) {
      return Result.failure(IllegalArgumentException("Le panier est vide."))
    }
    val userDoc = db.collection("users").document(uid)
    val saleRef = userDoc.collection("sales").document()
    val receiptNum = buildReceiptNumber()
    val saleItems = cartItems.map { item ->
      SaleItem(
        productId = item.product.id,
        productName = item.product.name,
        barcode = item.product.barcode,
        categoryName = item.product.categoryName,
        unitPrice = item.product.price,
        quantity = item.quantity,
        lineTotal = item.lineTotal,
      )
    }
    val totalAmount = saleItems.sumOf { it.lineTotal }
    val itemCount = saleItems.sumOf { it.quantity }

    return try {
      db.runTransaction { transaction ->
        val productRefs = cartItems.map { item ->
          userDoc.collection("products").document(item.product.id) to item
        }
        val currentSnapshots = productRefs.map { (ref, item) ->
          val snap = transaction.get(ref)
          if (!snap.exists()) {
            throw IllegalStateException("Produit introuvable : ${item.product.name}")
          }
          val currentStock = snap.getLong("stock") ?: 0L
          if (currentStock < item.quantity) {
            throw IllegalStateException(
              "Stock insuffisant pour ${item.product.name} ($currentStock restant)"
            )
          }
          Triple(ref, currentStock - item.quantity, item)
        }

        currentSnapshots.forEach { (ref, updatedStock, _) ->
          transaction.update(
            ref,
            mapOf(
              "stock" to updatedStock,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
        }

        val itemMaps = saleItems.map { s ->
          mapOf(
            "productId" to s.productId,
            "productName" to s.productName,
            "barcode" to s.barcode,
            "categoryName" to s.categoryName,
            "unitPrice" to s.unitPrice,
            "quantity" to s.quantity,
            "lineTotal" to s.lineTotal,
          )
        }
        val salePayload = mapOf(
          "userId" to uid,
          "receiptNumber" to receiptNum,
          "totalAmount" to totalAmount,
          "itemCount" to itemCount,
          "paymentMethod" to paymentMethod.code,
          "status" to "paid",
          "items" to itemMaps,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        transaction.set(saleRef, salePayload)
      }.await()

      Result.success(
        Sale(
          id = saleRef.id,
          userId = uid,
          receiptNumber = receiptNum,
          totalAmount = totalAmount,
          itemCount = itemCount,
          paymentMethod = paymentMethod.code,
          status = "paid",
          items = saleItems,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, saleRef.path, auth)
      Result.failure(e)
    }
  }

  private fun buildReceiptNumber(): String {
    val stamp = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date())
    return "TK-$stamp"
  }

  private fun logFlowError(
    error: Throwable,
    operationType: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) return
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, operationType, path, auth)
    }
  }
}
