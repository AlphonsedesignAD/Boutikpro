package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Category(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val colorHex: String = "#1565C0",
  val iconKey: String = "category",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val userId: String = "",
  val categoryId: String = "",
  val categoryName: String = "",
  val name: String = "",
  val barcode: String = "",
  val price: Double = 0.0,
  val stock: Long = 0L,
  val minStockAlert: Long = 5L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isLowStock: Boolean
    get() = stock in 1..minStockAlert

  val isOutOfStock: Boolean
    get() = stock <= 0L
}

@IgnoreExtraProperties
data class SaleItem(
  val productId: String = "",
  val productName: String = "",
  val barcode: String = "",
  val categoryName: String = "",
  val unitPrice: Double = 0.0,
  val quantity: Long = 1L,
  val lineTotal: Double = 0.0,
)

@IgnoreExtraProperties
data class Sale(
  @DocumentId val id: String = "",
  val userId: String = "",
  val receiptNumber: String = "",
  val totalAmount: Double = 0.0,
  val itemCount: Long = 0L,
  val paymentMethod: String = "cash",
  val status: String = "paid",
  val items: List<SaleItem> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CartItem(
  val product: Product,
  val quantity: Long,
) {
  val lineTotal: Double
    get() = product.price * quantity
}

enum class PaymentMethod(val code: String, val label: String) {
  CASH("cash", "Espèces"),
  CARD("card", "Carte Bancaire"),
  MOBILE("mobile", "Mobile Pay");

  companion object {
    fun fromCode(code: String): PaymentMethod =
      entries.firstOrNull { it.code == code } ?: CASH
  }
}
