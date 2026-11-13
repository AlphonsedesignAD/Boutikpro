package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createCategoryAndProduct_authenticatedOwner_succeedsAndEmitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = StoreRepository(firestore, auth)

    val catId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createCategory("Chemises", "#1565C0", "checkroom").getOrThrow()
    }
    assertTrue(catId.isNotBlank())

    val prodId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.createProduct(
        categoryId = catId,
        categoryName = "Chemises",
        name = "Chemise Oxford Bleu",
        barcode = "3700001122334",
        price = 49.90,
        stock = 10L,
        minStockAlert = 3L,
      ).getOrThrow()
    }
    assertTrue(prodId.isNotBlank())

    val products = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProducts(uid).first { list -> list.any { it.id == prodId } }
    }
    val saved = products.first { it.id == prodId }
    assertEquals("Chemise Oxford Bleu", saved.name)
    assertEquals(10L, saved.stock)

    val saleResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.confirmSale(
        cartItems = listOf(CartItem(product = saved, quantity = 2L)),
        paymentMethod = PaymentMethod.CARD,
      )
    }
    assertTrue(saleResult.isSuccess)

    val updatedProducts = withTimeout(FLOW_TIMEOUT_MS) {
      repo.observeProducts(uid).first { list ->
        list.firstOrNull { it.id == prodId }?.stock == 8L
      }
    }
    assertEquals(8L, updatedProducts.first { it.id == prodId }.stock)
  }

  @Test
  fun observeProducts_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = StoreRepository(firestore, auth)
    val catId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createCategory("Montres", "#0284C7", "watch").getOrThrow()
    }
    assertTrue(catId.isNotBlank())

    signInTestUser(BOB_EMAIL)
    val bobRepo = StoreRepository(firestore, auth)
    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeProducts(aliceUid).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun observeCategories_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    auth.signOut()
    val repo = StoreRepository(firestore, auth)

    var caught: Exception? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeCategories(aliceUid).first()
      }
    } catch (e: Exception) {
      caught = e
    }
    val firestoreException = caught?.unwrapFirestoreException()
    assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "boutique_alice@test.com"
    const val BOB_EMAIL = "boutique_bob@test.com"
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
