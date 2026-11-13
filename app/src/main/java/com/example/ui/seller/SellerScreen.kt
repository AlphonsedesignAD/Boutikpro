package com.example.ui.seller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CartItem
import com.example.data.model.PaymentMethod
import com.example.data.model.Product
import com.example.data.model.Sale
import com.example.ui.components.AnimatedPriceDisplay
import com.example.ui.theme.AmberLow
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonOut
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.DeepNavyInk
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.IceBackground
import com.example.ui.theme.IceContainer
import com.example.ui.theme.IceSurfaceVariant
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.OutlineBlue
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SlateInk
import com.example.ui.viewmodel.ScanFeedback
import com.example.ui.viewmodel.UiState
import com.example.util.BarcodeUtils
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay

enum class SellerTab(val label: String) {
  POS("Caisse & Scan"),
  HISTORY("Ventes"),
}

@Composable
fun SellerScreen(
  selectedTab: SellerTab,
  productsState: UiState<List<Product>>,
  salesState: UiState<List<Sale>>,
  cartItems: List<CartItem>,
  selectedPaymentMethod: PaymentMethod,
  isCheckingOut: Boolean,
  scanFeedback: ScanFeedback?,
  lastConfirmedSale: Sale?,
  onCameraBarcodeDetected: (String) -> Unit,
  onManualBarcodeSubmit: (String) -> Unit,
  onProductQuickAdd: (Product) -> Unit,
  onIncrementCart: (String) -> Unit,
  onDecrementCart: (String) -> Unit,
  onRemoveCart: (String) -> Unit,
  onClearCart: () -> Unit,
  onSelectPaymentMethod: (PaymentMethod) -> Unit,
  onConfirmPayment: () -> Unit,
  onDismissConfirmedSale: () -> Unit,
  onClearScanFeedback: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(scanFeedback) {
    if (scanFeedback != null) {
      delay(3200)
      onClearScanFeedback()
    }
  }

  if (lastConfirmedSale != null) {
    SaleReceiptDialog(
      sale = lastConfirmedSale,
      onDismiss = onDismissConfirmedSale,
    )
  }

  AnimatedContent(
    targetState = selectedTab,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "seller_tab_transition",
    modifier = modifier.fillMaxSize(),
  ) { tab ->
    when (tab) {
      SellerTab.POS -> SellerPosView(
        productsState = productsState,
        cartItems = cartItems,
        selectedPaymentMethod = selectedPaymentMethod,
        isCheckingOut = isCheckingOut,
        scanFeedback = scanFeedback,
        onCameraBarcodeDetected = onCameraBarcodeDetected,
        onManualBarcodeSubmit = onManualBarcodeSubmit,
        onProductQuickAdd = onProductQuickAdd,
        onIncrementCart = onIncrementCart,
        onDecrementCart = onDecrementCart,
        onRemoveCart = onRemoveCart,
        onClearCart = onClearCart,
        onSelectPaymentMethod = onSelectPaymentMethod,
        onConfirmPayment = onConfirmPayment,
      )
      SellerTab.HISTORY -> SalesHistoryView(salesState = salesState)
    }
  }
}

@Composable
private fun SellerPosView(
  productsState: UiState<List<Product>>,
  cartItems: List<CartItem>,
  selectedPaymentMethod: PaymentMethod,
  isCheckingOut: Boolean,
  scanFeedback: ScanFeedback?,
  onCameraBarcodeDetected: (String) -> Unit,
  onManualBarcodeSubmit: (String) -> Unit,
  onProductQuickAdd: (Product) -> Unit,
  onIncrementCart: (String) -> Unit,
  onDecrementCart: (String) -> Unit,
  onRemoveCart: (String) -> Unit,
  onClearCart: () -> Unit,
  onSelectPaymentMethod: (PaymentMethod) -> Unit,
  onConfirmPayment: () -> Unit,
) {
  var barcodeInput by remember { mutableStateOf("") }
  var cashReceivedInput by remember { mutableStateOf("") }
  val keyboardController = LocalSoftwareKeyboardController.current

  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  val totalAmount = remember(cartItems) { cartItems.sumOf { it.lineTotal } }
  val totalUnits = remember(cartItems) { cartItems.sumOf { it.quantity } }
  val cashGiven = cashReceivedInput.replace(",", ".").toDoubleOrNull()
  val changeDue = if (cashGiven != null && cashGiven >= totalAmount) cashGiven - totalAmount else null

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(IceBackground)
  ) {
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        BarcodeScannerCard(onBarcodeDetected = onCameraBarcodeDetected)
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = barcodeInput,
            onValueChange = { barcodeInput = it },
            placeholder = { Text("Saisir un code-barres (ex: 370...)") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Filled.QrCode2,
                contentDescription = null,
                tint = CobaltPrimary,
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = PureWhite,
              unfocusedContainerColor = PureWhite,
              focusedBorderColor = CobaltPrimary,
              unfocusedBorderColor = OutlineBlue,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
              onSearch = {
                if (barcodeInput.isNotBlank()) {
                  onManualBarcodeSubmit(barcodeInput)
                  barcodeInput = ""
                  keyboardController?.hide()
                }
              }
            ),
            modifier = Modifier
              .weight(1f)
              .testTag("manual_barcode_input"),
          )

          Button(
            onClick = {
              if (barcodeInput.isNotBlank()) {
                onManualBarcodeSubmit(barcodeInput)
                barcodeInput = ""
                keyboardController?.hide()
              }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
            modifier = Modifier
              .height(54.dp)
              .testTag("manual_barcode_submit"),
          ) {
            Icon(
              imageVector = Icons.Filled.QrCodeScanner,
              contentDescription = "Ajouter par code-barres",
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      item {
        AnimatedVisibility(
          visible = scanFeedback != null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically(),
        ) {
          scanFeedback?.let { feedback ->
            ScanFeedbackBanner(feedback = feedback)
          }
        }
      }

      if (products.isNotEmpty()) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Ajout rapide / Catalogue en stock",
              style = MaterialTheme.typography.labelLarge,
              color = SlateInk,
            )
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              items(products.take(15), key = { it.id }) { prod ->
                QuickProductChip(
                  product = prod,
                  onClick = { onProductQuickAdd(prod) },
                )
              }
            }
          }
        }
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.ShoppingCart,
              contentDescription = null,
              tint = CobaltPrimary,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Panier ($totalUnits article${if (totalUnits > 1) "s" else ""})",
              style = MaterialTheme.typography.titleMedium,
              color = DeepNavyInk,
            )
          }
          if (cartItems.isNotEmpty()) {
            TextButton(onClick = onClearCart) {
              Text("Vider", color = CrimsonOut)
            }
          }
        }
      }

      if (cartItems.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, IceContainer),
          ) {
            Column(
              modifier = Modifier.padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = SkyAccent,
                modifier = Modifier.size(38.dp),
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Panier vide",
                style = MaterialTheme.typography.titleMedium,
                color = DeepNavyInk,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (products.isEmpty()) {
                  "Passez sur l'Espace Admin pour créer vos premiers produits et codes-barres."
                } else {
                  "Scannez un code-barres ou sélectionnez un article ci-dessus."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SlateInk,
              )
            }
          }
        }
      } else {
        items(cartItems, key = { it.product.id }) { item ->
          CartItemCard(
            item = item,
            onIncrement = { onIncrementCart(item.product.id) },
            onDecrement = { onDecrementCart(item.product.id) },
            onRemove = { onRemoveCart(item.product.id) },
          )
        }
      }
    }

    // Checkout Payment Dock
    AnimatedVisibility(
      visible = cartItems.isNotEmpty(),
      enter = slideInVertically { it } + fadeIn(),
      exit = fadeOut(),
    ) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PureWhite,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            PaymentMethod.entries.forEach { method ->
              val isSelected = selectedPaymentMethod == method
              FilterChip(
                selected = isSelected,
                onClick = { onSelectPaymentMethod(method) },
                label = {
                  Text(
                    text = method.label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = when (method) {
                      PaymentMethod.CASH -> Icons.Filled.Payments
                      PaymentMethod.CARD -> Icons.Filled.CreditCard
                      PaymentMethod.MOBILE -> Icons.Filled.Smartphone
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CobaltPrimary,
                  selectedLabelColor = PureWhite,
                  selectedLeadingIconColor = PureWhite,
                ),
                modifier = Modifier.weight(1f),
              )
            }
          }

          if (selectedPaymentMethod == PaymentMethod.CASH) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = cashReceivedInput,
                onValueChange = { cashReceivedInput = it },
                label = { Text("Montant reçu (€)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              )
              if (changeDue != null) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = EmeraldSoft,
                ) {
                  Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                      text = "Monnaie à rendre",
                      style = MaterialTheme.typography.labelSmall,
                      color = EmeraldPaid,
                    )
                    Text(
                      text = BarcodeUtils.formatPrice(changeDue),
                      fontFamily = JetBrainsMonoFamily,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = EmeraldPaid,
                    )
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text(
                text = "Total à payer",
                style = MaterialTheme.typography.labelMedium,
                color = SlateInk,
              )
              AnimatedPriceDisplay(
                amount = totalAmount,
                style = MaterialTheme.typography.headlineMedium,
                color = CobaltPrimary,
              )
            }

            Button(
              onClick = {
                cashReceivedInput = ""
                onConfirmPayment()
              },
              enabled = !isCheckingOut,
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPaid,
                contentColor = PureWhite,
              ),
              modifier = Modifier
                .height(52.dp)
                .testTag("confirm_payment_button"),
            ) {
              if (isCheckingOut) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = PureWhite,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isCheckingOut) "Validation..." else "Confirmer payé",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ScanFeedbackBanner(feedback: ScanFeedback) {
  val (bgColor, fgColor, icon, message) = when (feedback) {
    is ScanFeedback.Added -> Quad(
      EmeraldSoft,
      EmeraldPaid,
      Icons.Filled.CheckCircle,
      "+1 ${feedback.product.name} (${BarcodeUtils.formatPrice(feedback.product.price)}) • Qté panier : ${feedback.newCartQty}",
    )
    is ScanFeedback.NotFound -> Quad(
      CrimsonSoft,
      CrimsonOut,
      Icons.Filled.ErrorOutline,
      "Code-barres inconnu : ${feedback.barcode}",
    )
    is ScanFeedback.OutOfStock -> Quad(
      CrimsonSoft,
      CrimsonOut,
      Icons.Filled.WarningAmber,
      "Rupture de stock pour ${feedback.product.name}",
    )
    is ScanFeedback.StockLimitReached -> Quad(
      AmberSoft,
      AmberLow,
      Icons.Filled.WarningAmber,
      "Stock maximal atteint pour ${feedback.product.name} (${feedback.product.stock} dispo)",
    )
  }

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(14.dp),
    color = bgColor,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = fgColor,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(10.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.labelLarge,
        color = fgColor,
      )
    }
  }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun QuickProductChip(
  product: Product,
  onClick: () -> Unit,
) {
  val inStock = product.stock > 0L
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(enabled = inStock, onClick = onClick),
    shape = RoundedCornerShape(14.dp),
    color = if (inStock) PureWhite else IceSurfaceVariant.copy(alpha = 0.6f),
    border = BorderStroke(1.dp, if (inStock) OutlineBlue else Color.LightGray),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
      Text(
        text = product.name,
        style = MaterialTheme.typography.labelLarge,
        color = if (inStock) DeepNavyInk else SlateInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Text(
          text = BarcodeUtils.formatPrice(product.price),
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = CobaltPrimary,
        )
        Text(
          text = "• Stock ${product.stock}",
          style = MaterialTheme.typography.bodySmall,
          color = if (inStock) SlateInk else CrimsonOut,
        )
      }
    }
  }
}

@Composable
private fun CartItemCard(
  item: CartItem,
  onIncrement: () -> Unit,
  onDecrement: () -> Unit,
  onRemove: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.product.name,
          style = MaterialTheme.typography.titleMedium,
          color = DeepNavyInk,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = IceContainer,
          ) {
            Text(
              text = item.product.categoryName,
              style = MaterialTheme.typography.labelSmall,
              color = CobaltPrimary,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
          Text(
            text = "${BarcodeUtils.formatPrice(item.product.price)} / u",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            color = SlateInk,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Sous-total : ${BarcodeUtils.formatPrice(item.lineTotal)}",
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = CobaltPrimary,
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = IceSurfaceVariant,
        ) {
          IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Remove,
              contentDescription = "Diminuer quantité",
              tint = CobaltPrimary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Text(
          text = item.quantity.toString(),
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = DeepNavyInk,
          modifier = Modifier.padding(horizontal = 8.dp),
        )

        Surface(
          shape = CircleShape,
          color = CobaltPrimary,
        ) {
          IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(36.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Add,
              contentDescription = "Augmenter quantité",
              tint = PureWhite,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        IconButton(onClick = onRemove) {
          Icon(
            imageVector = Icons.Filled.DeleteOutline,
            contentDescription = "Retirer du panier",
            tint = CrimsonOut,
          )
        }
      }
    }
  }
}

@Composable
private fun SaleReceiptDialog(
  sale: Sale,
  onDismiss: () -> Unit,
) {
  val checkScale by animateFloatAsState(
    targetValue = 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "receipt_check_scale",
  )

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Surface(
          modifier = Modifier
            .size(68.dp)
            .scale(checkScale),
          shape = CircleShape,
          color = EmeraldSoft,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = "Paiement confirmé",
              tint = EmeraldPaid,
              modifier = Modifier.size(40.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Paiement Confirmé !",
          style = MaterialTheme.typography.headlineSmall,
          color = DeepNavyInk,
        )
        Text(
          text = "Ticket ${sale.receiptNumber} • ${PaymentMethod.fromCode(sale.paymentMethod).label}",
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.bodySmall,
          color = SlateInk,
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = IceContainer)
        Spacer(modifier = Modifier.height(10.dp))

        sale.items.forEach { line ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(
              text = "${line.quantity}x ${line.productName}",
              style = MaterialTheme.typography.bodyMedium,
              color = DeepNavyInk,
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = BarcodeUtils.formatPrice(line.lineTotal),
              fontFamily = JetBrainsMonoFamily,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = DeepNavyInk,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = IceContainer)
        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "TOTAL PAYÉ",
            style = MaterialTheme.typography.titleSmall,
            color = SlateInk,
          )
          Text(
            text = BarcodeUtils.formatPrice(sale.totalAmount),
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldPaid,
          )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("new_sale_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
        ) {
          Text(
            text = "Nouvelle vente",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          )
        }
      }
    }
  }
}

@Composable
private fun SalesHistoryView(
  salesState: UiState<List<Sale>>,
) {
  when (salesState) {
    UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator(color = CobaltPrimary)
    }
    is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
      Text(salesState.message, color = CrimsonOut)
    }
    is UiState.Success -> {
      val sales = salesState.data
      val totalRevenue = sales.sumOf { it.totalAmount }
      val totalUnits = sales.sumOf { it.itemCount }
      val dateFmt = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.FRANCE) }

      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(IceBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        item {
          Surface(
            shape = RoundedCornerShape(22.dp),
            color = PureWhite,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(CobaltPrimary, RoyalBlue)))
                .padding(18.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Column {
                Text(
                  text = "Chiffre d'affaires",
                  style = MaterialTheme.typography.labelMedium,
                  color = PureWhite.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = BarcodeUtils.formatPrice(totalRevenue),
                  fontFamily = JetBrainsMonoFamily,
                  style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                  color = PureWhite,
                )
              }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${sales.size} vente${if (sales.size > 1) "s" else ""}",
                  style = MaterialTheme.typography.titleMedium,
                  color = PureWhite,
                )
                Text(
                  text = "$totalUnits article${if (totalUnits > 1) "s" else ""} vendus",
                  style = MaterialTheme.typography.bodySmall,
                  color = PureWhite.copy(alpha = 0.85f),
                )
              }
            }
          }
        }

        if (sales.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(18.dp),
              color = PureWhite,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                  contentDescription = null,
                  tint = SkyAccent,
                  modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Aucune vente enregistrée",
                  style = MaterialTheme.typography.titleMedium,
                  color = DeepNavyInk,
                )
                Text(
                  text = "Les ventes confirmées depuis la caisse apparaîtront ici en temps réel.",
                  style = MaterialTheme.typography.bodySmall,
                  color = SlateInk,
                )
              }
            }
          }
        } else {
          items(sales, key = { it.id }) { sale ->
            var expanded by remember { mutableStateOf(false) }
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickable { expanded = !expanded },
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.cardColors(containerColor = PureWhite),
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text(
                      text = sale.receiptNumber,
                      fontFamily = JetBrainsMonoFamily,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = DeepNavyInk,
                    )
                    val dateStr = sale.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "À l'instant"
                    Text(
                      text = "$dateStr • ${PaymentMethod.fromCode(sale.paymentMethod).label}",
                      style = MaterialTheme.typography.bodySmall,
                      color = SlateInk,
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(
                      text = BarcodeUtils.formatPrice(sale.totalAmount),
                      fontFamily = JetBrainsMonoFamily,
                      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                      color = EmeraldPaid,
                    )
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = EmeraldSoft,
                    ) {
                      Text(
                        text = "PAYÉ • ${sale.itemCount} art.",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPaid,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                      )
                    }
                  }
                }

                if (expanded) {
                  Spacer(modifier = Modifier.height(10.dp))
                  HorizontalDivider(color = IceContainer)
                  Spacer(modifier = Modifier.height(8.dp))
                  sale.items.forEach { item ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(
                        text = "${item.quantity}x ${item.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeepNavyInk,
                      )
                      Text(
                        text = BarcodeUtils.formatPrice(item.lineTotal),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateInk,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
