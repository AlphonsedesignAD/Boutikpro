package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.ui.components.BarcodePreviewView
import com.example.ui.components.categoryIconFor
import com.example.ui.components.parseHexColor
import com.example.ui.theme.AmberLow
import com.example.ui.theme.AmberSoft
import com.example.ui.theme.CobaltPrimary
import com.example.ui.theme.CrimsonOut
import com.example.ui.theme.CrimsonSoft
import com.example.ui.theme.DeepNavyInk
import com.example.ui.theme.DeepSapphire
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
import com.example.ui.viewmodel.UiState
import com.example.util.BarcodeUtils

enum class AdminTab(val label: String) {
  INVENTORY("Stocks & Produits"),
  BARCODES("Codes-Barres"),
}

@Composable
fun AdminScreen(
  selectedTab: AdminTab,
  categoriesState: UiState<List<Category>>,
  productsState: UiState<List<Product>>,
  onCreateCategory: (name: String, colorHex: String, iconKey: String) -> Unit,
  onUpdateCategory: (Category) -> Unit,
  onDeleteCategory: (String) -> Unit,
  onCreateProduct: (categoryId: String, categoryName: String, name: String, barcode: String, price: Double, stock: Long, minStockAlert: Long) -> Unit,
  onUpdateProduct: (Product) -> Unit,
  onAdjustStock: (Product, Long) -> Unit,
  onDeleteProduct: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showCategoryDialog by remember { mutableStateOf(false) }
  var editingCategory by remember { mutableStateOf<Category?>(null) }
  var showProductDialog by remember { mutableStateOf(false) }
  var editingProduct by remember { mutableStateOf<Product?>(null) }

  val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()

  if (showCategoryDialog) {
    CategoryEditorDialog(
      existing = editingCategory,
      onDismiss = {
        showCategoryDialog = false
        editingCategory = null
      },
      onSave = { name, colorHex, iconKey ->
        val current = editingCategory
        if (current == null) {
          onCreateCategory(name, colorHex, iconKey)
        } else {
          onUpdateCategory(current.copy(name = name, colorHex = colorHex, iconKey = iconKey))
        }
        showCategoryDialog = false
        editingCategory = null
      },
      onDelete = editingCategory?.let { cat ->
        {
          onDeleteCategory(cat.id)
          showCategoryDialog = false
          editingCategory = null
        }
      },
    )
  }

  if (showProductDialog) {
    ProductEditorDialog(
      existing = editingProduct,
      categories = categories,
      onDismiss = {
        showProductDialog = false
        editingProduct = null
      },
      onOpenNewCategory = {
        showCategoryDialog = true
      },
      onSave = { cat, name, barcode, price, stock, minAlert ->
        val current = editingProduct
        if (current == null) {
          onCreateProduct(cat.id, cat.name, name, barcode, price, stock, minAlert)
        } else {
          onUpdateProduct(
            current.copy(
              categoryId = cat.id,
              categoryName = cat.name,
              name = name,
              barcode = barcode,
              price = price,
              stock = stock,
              minStockAlert = minAlert,
            )
          )
        }
        showProductDialog = false
        editingProduct = null
      },
    )
  }

  AnimatedContent(
    targetState = selectedTab,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "admin_tab_transition",
    modifier = modifier.fillMaxSize(),
  ) { tab ->
    when (tab) {
      AdminTab.INVENTORY -> AdminInventoryView(
        categoriesState = categoriesState,
        productsState = productsState,
        onNewCategoryClick = {
          editingCategory = null
          showCategoryDialog = true
        },
        onEditCategoryClick = { cat ->
          editingCategory = cat
          showCategoryDialog = true
        },
        onNewProductClick = {
          if (categories.isEmpty()) {
            editingCategory = null
            showCategoryDialog = true
          } else {
            editingProduct = null
            showProductDialog = true
          }
        },
        onEditProductClick = { prod ->
          editingProduct = prod
          showProductDialog = true
        },
        onAdjustStock = onAdjustStock,
        onDeleteProduct = onDeleteProduct,
      )
      AdminTab.BARCODES -> AdminBarcodePrintView(
        categoriesState = categoriesState,
        productsState = productsState,
      )
    }
  }
}

@Composable
private fun AdminInventoryView(
  categoriesState: UiState<List<Category>>,
  productsState: UiState<List<Product>>,
  onNewCategoryClick: () -> Unit,
  onEditCategoryClick: (Category) -> Unit,
  onNewProductClick: () -> Unit,
  onEditProductClick: (Product) -> Unit,
  onAdjustStock: (Product, Long) -> Unit,
  onDeleteProduct: (String) -> Unit,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryId by remember { mutableStateOf<String?>(null) }
  var productToDelete by remember { mutableStateOf<Product?>(null) }

  if (productToDelete != null) {
    AlertDialog(
      onDismissRequest = { productToDelete = null },
      title = { Text("Supprimer ce produit ?") },
      text = { Text("« ${productToDelete?.name} » sera retiré de l'inventaire.") },
      confirmButton = {
        TextButton(
          onClick = {
            productToDelete?.let { onDeleteProduct(it.id) }
            productToDelete = null
          }
        ) {
          Text("Supprimer", color = CrimsonOut)
        }
      },
      dismissButton = {
        TextButton(onClick = { productToDelete = null }) {
          Text("Annuler")
        }
      },
    )
  }

  val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  val isLoading = categoriesState is UiState.Loading || productsState is UiState.Loading

  val filteredProducts = remember(products, selectedCategoryId, searchQuery) {
    products.filter { prod ->
      val matchesCat = selectedCategoryId == null || prod.categoryId == selectedCategoryId
      val q = searchQuery.trim()
      val matchesQuery = q.isEmpty() ||
        prod.name.contains(q, ignoreCase = true) ||
        prod.barcode.contains(q, ignoreCase = true) ||
        prod.categoryName.contains(q, ignoreCase = true)
      matchesCat && matchesQuery
    }
  }

  val totalStockValue = remember(products) { products.sumOf { it.price * it.stock } }
  val lowStockCount = remember(products) { products.count { it.stock <= it.minStockAlert } }

  Scaffold(
    containerColor = IceBackground,
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onNewProductClick,
        containerColor = CobaltPrimary,
        contentColor = PureWhite,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.testTag("add_product_fab"),
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = {
          Text(
            text = if (categories.isEmpty()) "Créer catégorie" else "Nouveau produit",
            style = MaterialTheme.typography.labelLarge,
          )
        },
      )
    },
  ) { innerPadding ->
    if (isLoading) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator(color = CobaltPrimary)
      }
      return@Scaffold
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          AdminKpiCard(
            title = "Produits",
            value = products.size.toString(),
            subtitle = "${categories.size} catégorie${if (categories.size > 1) "s" else ""}",
            accentColor = CobaltPrimary,
            modifier = Modifier.weight(1f),
          )
          AdminKpiCard(
            title = "Valeur stock",
            value = BarcodeUtils.formatPrice(totalStockValue),
            subtitle = if (lowStockCount > 0) "$lowStockCount alerte(s)" else "Stock sain",
            accentColor = if (lowStockCount > 0) AmberLow else EmeraldPaid,
            modifier = Modifier.weight(1f),
          )
        }
      }

      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Rechercher produit ou code-barres...") },
          leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = CobaltPrimary)
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = PureWhite,
            unfocusedContainerColor = PureWhite,
            focusedBorderColor = CobaltPrimary,
            unfocusedBorderColor = OutlineBlue,
          ),
          modifier = Modifier.fillMaxWidth(),
        )
      }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Catégories",
              style = MaterialTheme.typography.titleSmall,
              color = DeepNavyInk,
            )
            TextButton(
              onClick = onNewCategoryClick,
              modifier = Modifier.testTag("add_category_button"),
            ) {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Catégorie")
            }
          }

          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              FilterChip(
                selected = selectedCategoryId == null,
                onClick = { selectedCategoryId = null },
                label = { Text("Toutes (${products.size})") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CobaltPrimary,
                  selectedLabelColor = PureWhite,
                ),
              )
            }
            items(categories, key = { it.id }) { cat ->
              val isSelected = selectedCategoryId == cat.id
              val count = products.count { it.categoryId == cat.id }
              FilterChip(
                selected = isSelected,
                onClick = {
                  selectedCategoryId = if (isSelected) null else cat.id
                },
                leadingIcon = {
                  Icon(
                    imageVector = categoryIconFor(cat.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                },
                trailingIcon = if (isSelected) {
                  {
                    Icon(
                      imageVector = Icons.Filled.Edit,
                      contentDescription = "Modifier catégorie",
                      modifier = Modifier
                        .size(16.dp)
                        .clickable { onEditCategoryClick(cat) },
                    )
                  }
                } else null,
                label = { Text("${cat.name} ($count)") },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = parseHexColor(cat.colorHex, CobaltPrimary),
                  selectedLabelColor = PureWhite,
                  selectedLeadingIconColor = PureWhite,
                  selectedTrailingIconColor = PureWhite,
                ),
              )
            }
          }
        }
      }

      if (filteredProducts.isEmpty()) {
        item {
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, IceContainer),
          ) {
            Column(
              modifier = Modifier.padding(26.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                tint = SkyAccent,
                modifier = Modifier.size(42.dp),
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = if (categories.isEmpty()) {
                  "Commencez par créer une catégorie"
                } else {
                  "Aucun produit dans cette vue"
                },
                style = MaterialTheme.typography.titleMedium,
                color = DeepNavyInk,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = if (categories.isEmpty()) {
                  "Exemples : Vêtements, Chaussures, Accessoires, Électronique..."
                } else {
                  "Appuyez sur « + Nouveau produit » pour ajouter votre stock et générer un code-barres."
                },
                style = MaterialTheme.typography.bodySmall,
                color = SlateInk,
              )
            }
          }
        }
      } else {
        items(filteredProducts, key = { it.id }) { product ->
          AdminProductCard(
            product = product,
            onAdjustStock = { delta -> onAdjustStock(product, delta) },
            onEdit = { onEditProductClick(product) },
            onDelete = { productToDelete = product },
          )
        }
      }
    }
  }
}

@Composable
private fun AdminKpiCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = SlateInk,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = value,
        fontFamily = JetBrainsMonoFamily,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = accentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = SlateInk,
      )
    }
  }
}

@Composable
private fun AdminProductCard(
  product: Product,
  onAdjustStock: (Long) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val (stockBg, stockFg, stockText) = when {
    product.isOutOfStock -> Triple(CrimsonSoft, CrimsonOut, "Rupture")
    product.isLowStock -> Triple(AmberSoft, AmberLow, "Stock faible (${product.stock})")
    else -> Triple(EmeraldSoft, EmeraldPaid, "En stock (${product.stock})")
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = PureWhite),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = IceContainer,
            ) {
              Text(
                text = product.categoryName,
                style = MaterialTheme.typography.labelSmall,
                color = CobaltPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = stockBg,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (product.isLowStock || product.isOutOfStock) {
                  Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = stockFg,
                    modifier = Modifier.size(12.dp),
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                  text = stockText,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = stockFg,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium,
            color = DeepNavyInk,
          )
          Text(
            text = "Code : ${product.barcode}",
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            color = SlateInk,
          )
        }

        Text(
          text = BarcodeUtils.formatPrice(product.price),
          fontFamily = JetBrainsMonoFamily,
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = CobaltPrimary,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = IceSurfaceVariant,
          ) {
            IconButton(
              onClick = { onAdjustStock(-1L) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Remove,
                contentDescription = "Retirer 1 du stock",
                tint = CobaltPrimary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Surface(
            shape = CircleShape,
            color = CobaltPrimary,
          ) {
            IconButton(
              onClick = { onAdjustStock(1L) },
              modifier = Modifier.size(36.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Ajouter 1 au stock",
                tint = PureWhite,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          OutlinedButton(
            onClick = { onAdjustStock(5L) },
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp),
          ) {
            Text("+5", style = MaterialTheme.typography.labelMedium)
          }
        }

        Row {
          IconButton(onClick = onEdit) {
            Icon(
              imageVector = Icons.Filled.Edit,
              contentDescription = "Modifier produit",
              tint = CobaltPrimary,
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = "Supprimer produit",
              tint = CrimsonOut,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AdminBarcodePrintView(
  categoriesState: UiState<List<Category>>,
  productsState: UiState<List<Product>>,
) {
  val context = LocalContext.current
  val categories = (categoriesState as? UiState.Success)?.data ?: emptyList()
  val products = (productsState as? UiState.Success)?.data ?: emptyList()
  var selectedCategoryId by remember { mutableStateOf<String?>(null) }

  val selectedCategoryName = remember(selectedCategoryId, categories) {
    categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Toutes les catégories"
  }

  val filteredProducts = remember(products, selectedCategoryId) {
    if (selectedCategoryId == null) products
    else products.filter { it.categoryId == selectedCategoryId }
  }

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
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(DeepSapphire, CobaltPrimary)))
            .padding(18.dp),
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Print,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Impression Codes-Barres",
              style = MaterialTheme.typography.titleLarge,
              color = PureWhite,
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Choisissez une catégorie pour générer et imprimer la planche PDF des étiquettes produits.",
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite.copy(alpha = 0.88f),
          )
          Spacer(modifier = Modifier.height(14.dp))
          Button(
            onClick = {
              BarcodeUtils.printCategoryBarcodes(
                context = context,
                categoryName = selectedCategoryName,
                products = filteredProducts,
              )
            },
            enabled = filteredProducts.isNotEmpty(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = PureWhite,
              contentColor = CobaltPrimary,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("print_category_barcodes_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Print,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Imprimer « $selectedCategoryName » (${filteredProducts.size})",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            )
          }
        }
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedCategoryId == null,
            onClick = { selectedCategoryId = null },
            label = { Text("Toutes (${products.size})") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = CobaltPrimary,
              selectedLabelColor = PureWhite,
            ),
          )
        }
        items(categories, key = { it.id }) { cat ->
          val count = products.count { it.categoryId == cat.id }
          FilterChip(
            selected = selectedCategoryId == cat.id,
            onClick = { selectedCategoryId = cat.id },
            leadingIcon = {
              Icon(
                imageVector = categoryIconFor(cat.iconKey),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            },
            label = { Text("${cat.name} ($count)") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = parseHexColor(cat.colorHex, CobaltPrimary),
              selectedLabelColor = PureWhite,
              selectedLeadingIconColor = PureWhite,
            ),
          )
        }
      }
    }

    if (filteredProducts.isEmpty()) {
      item {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          color = PureWhite,
          border = BorderStroke(1.dp, IceContainer),
        ) {
          Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Filled.QrCode2,
              contentDescription = null,
              tint = SkyAccent,
              modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Aucun code-barres à imprimer",
              style = MaterialTheme.typography.titleMedium,
              color = DeepNavyInk,
            )
            Text(
              text = "Ajoutez des produits dans cette catégorie pour prévisualiser et imprimer leurs codes-barres.",
              style = MaterialTheme.typography.bodySmall,
              color = SlateInk,
            )
          }
        }
      }
    } else {
      items(filteredProducts, key = { it.id }) { product ->
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = PureWhite),
          elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = product.categoryName.uppercase(),
                  style = MaterialTheme.typography.labelSmall,
                  color = CobaltPrimary,
                )
                Text(
                  text = product.name,
                  style = MaterialTheme.typography.titleMedium,
                  color = DeepNavyInk,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = BarcodeUtils.formatPrice(product.price),
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = CobaltPrimary,
              )
            }

            BarcodePreviewView(
              barcode = product.barcode,
              heightDp = 64,
              modifier = Modifier.fillMaxWidth(),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              OutlinedButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("Barcode", product.barcode))
                  Toast.makeText(context, "Code ${product.barcode} copié", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Filled.ContentCopy,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copier code")
              }

              Button(
                onClick = {
                  BarcodeUtils.printCategoryBarcodes(
                    context = context,
                    categoryName = product.name,
                    products = listOf(product),
                  )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Filled.Print,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Imprimer")
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryEditorDialog(
  existing: Category?,
  onDismiss: () -> Unit,
  onSave: (name: String, colorHex: String, iconKey: String) -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
  var selectedColor by remember(existing) { mutableStateOf(existing?.colorHex ?: "#1565C0") }
  var selectedIcon by remember(existing) { mutableStateOf(existing?.iconKey ?: "checkroom") }

  val colorOptions = listOf("#1565C0", "#0284C7", "#1D4ED8", "#0D9488", "#4338CA")
  val iconOptions = listOf("checkroom", "watch", "devices", "spa", "local_cafe", "shopping_bag", "category")

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = if (existing == null) "Nouvelle Catégorie" else "Modifier Catégorie",
          style = MaterialTheme.typography.titleLarge,
          color = DeepNavyInk,
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nom de la catégorie") },
          placeholder = { Text("Ex: Vêtements, Chaussures...") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("category_name_input"),
        )

        Text("Couleur du badge", style = MaterialTheme.typography.labelLarge, color = SlateInk)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          colorOptions.forEach { hex ->
            val color = parseHexColor(hex)
            val selected = selectedColor == hex
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (selected) 3.dp else 0.dp,
                  color = if (selected) DeepNavyInk else Color.Transparent,
                  shape = CircleShape,
                )
                .clickable { selectedColor = hex },
            )
          }
        }

        Text("Icône", style = MaterialTheme.typography.labelLarge, color = SlateInk)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(iconOptions) { key ->
            val selected = selectedIcon == key
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) CobaltPrimary else IceSurfaceVariant,
              modifier = Modifier
                .size(44.dp)
                .clickable { selectedIcon = key },
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = categoryIconFor(key),
                  contentDescription = key,
                  tint = if (selected) PureWhite else CobaltPrimary,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (onDelete != null) {
            TextButton(onClick = onDelete) {
              Text("Supprimer", color = CrimsonOut)
            }
            Spacer(modifier = Modifier.weight(1f))
          }
          TextButton(onClick = onDismiss) {
            Text("Annuler")
          }
          Spacer(modifier = Modifier.width(6.dp))
          Button(
            onClick = { onSave(name, selectedColor, selectedIcon) },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
            modifier = Modifier.testTag("save_category_button"),
          ) {
            Text("Enregistrer")
          }
        }
      }
    }
  }
}

@Composable
private fun ProductEditorDialog(
  existing: Product?,
  categories: List<Category>,
  onDismiss: () -> Unit,
  onOpenNewCategory: () -> Unit,
  onSave: (
    category: Category,
    name: String,
    barcode: String,
    price: Double,
    stock: Long,
    minAlert: Long,
  ) -> Unit,
) {
  var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
  var selectedCategoryId by remember(existing, categories) {
    mutableStateOf(existing?.categoryId ?: categories.firstOrNull()?.id ?: "")
  }
  var priceStr by remember(existing) {
    mutableStateOf(existing?.price?.toString() ?: "")
  }
  var stockStr by remember(existing) {
    mutableStateOf(existing?.stock?.toString() ?: "10")
  }
  var minAlertStr by remember(existing) {
    mutableStateOf(existing?.minStockAlert?.toString() ?: "5")
  }
  var barcode by remember(existing) {
    mutableStateOf(existing?.barcode ?: BarcodeUtils.generateEan13Barcode())
  }

  val selectedCategory = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.firstOrNull()
  val parsedPrice = priceStr.replace(",", ".").toDoubleOrNull()
  val parsedStock = stockStr.toLongOrNull()
  val parsedAlert = minAlertStr.toLongOrNull() ?: 5L
  val isValid = selectedCategory != null &&
    name.isNotBlank() &&
    barcode.trim().length >= 3 &&
    parsedPrice != null && parsedPrice >= 0.0 &&
    parsedStock != null && parsedStock >= 0L

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = PureWhite),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = if (existing == null) "Nouveau Produit" else "Modifier Produit",
          style = MaterialTheme.typography.titleLarge,
          color = DeepNavyInk,
        )

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nom du produit") },
          placeholder = { Text("Ex: Chemise Oxford Bleue") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("product_name_input"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text("Catégorie", style = MaterialTheme.typography.labelLarge, color = SlateInk)
            TextButton(onClick = onOpenNewCategory) {
              Text("+ Créer")
            }
          }
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories, key = { it.id }) { cat ->
              FilterChip(
                selected = selectedCategory?.id == cat.id,
                onClick = { selectedCategoryId = cat.id },
                label = { Text(cat.name) },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = CobaltPrimary,
                  selectedLabelColor = PureWhite,
                ),
              )
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = priceStr,
            onValueChange = { priceStr = it },
            label = { Text("Prix (€)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("product_price_input"),
          )
          OutlinedTextField(
            value = stockStr,
            onValueChange = { stockStr = it },
            label = { Text("Stock") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("product_stock_input"),
          )
        }

        OutlinedTextField(
          value = minAlertStr,
          onValueChange = { minAlertStr = it },
          label = { Text("Seuil d'alerte stock faible") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = barcode,
            onValueChange = { barcode = it },
            label = { Text("Code-barres") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("product_barcode_input"),
          )
          OutlinedButton(
            onClick = { barcode = BarcodeUtils.generateEan13Barcode() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(54.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.AutoAwesome,
              contentDescription = "Générer code-barres",
              modifier = Modifier.size(18.dp),
            )
          }
        }

        if (barcode.trim().length >= 3) {
          BarcodePreviewView(
            barcode = barcode.trim(),
            heightDp = 50,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(onClick = onDismiss) {
            Text("Annuler")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (selectedCategory != null && parsedPrice != null && parsedStock != null) {
                onSave(
                  selectedCategory,
                  name,
                  barcode.trim(),
                  parsedPrice,
                  parsedStock,
                  parsedAlert,
                )
              }
            },
            enabled = isValid,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CobaltPrimary),
            modifier = Modifier.testTag("save_product_button"),
          ) {
            Text("Enregistrer")
          }
        }
      }
    }
  }
}
