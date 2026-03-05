package com.blackcode.tehatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.blackcode.tehatlas.network.*
import com.blackcode.tehatlas.ui.theme.*
import com.blackcode.tehatlas.utils.formatRp
import com.blackcode.tehatlas.utils.AppUpdater
import com.blackcode.tehatlas.ui.UpdateDialog
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

// Cashier Navigation
enum class CashierScreen { POS, PRODUCTS, INVENTORY, HISTORY, SETTINGS, STOCK_HISTORY, PRODUCT_DETAILS, EXPENSES }

// Local UI cart model
data class CartItem(
    val product: ProductDto,
    val quantity: Int,
    val unitPrice: Double,
    val discount: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierDashboard(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(CashierScreen.POS) }
    var selectedItemForHistory by remember { mutableStateOf<ProductDto?>(null) }
    var selectedProductForDetails by remember { mutableStateOf<ProductDto?>(null) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp), drawerContainerColor = Surface) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (sessionManager.getUsername()?.firstOrNull()?.uppercase() ?: "C"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                sessionManager.getUsername() ?: "Cashier",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text("Kasir", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(16.dp))

                listOf(
                    Triple(CashierScreen.POS, Icons.Filled.PointOfSale, "Kasir (POS)"),
                    Triple(CashierScreen.PRODUCTS, Icons.Filled.Category, "Daftar Produk"),
                    Triple(CashierScreen.INVENTORY, Icons.Filled.Inventory, "Inventaris & Stok"),
                    Triple(CashierScreen.HISTORY, Icons.Filled.History, "Riwayat Penjualan"),
                    Triple(CashierScreen.EXPENSES, Icons.Filled.AccountBalanceWallet, "Pengeluaran"),
                    Triple(CashierScreen.SETTINGS, Icons.Filled.Settings, "Pengaturan")
                ).forEach { (screen, icon, label) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, null, tint = if (currentScreen == screen) Primary else TextSecondary) },
                        label = { Text(label, color = if (currentScreen == screen) Primary else TextPrimary) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Primary.copy(alpha = 0.06f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Error) },
                    label = { Text("Keluar", color = Error) },
                    selected = false,
                    onClick = {
                        sessionManager.clearSession()
                        RetrofitClient.refresh()
                        onLogout()
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentScreen != CashierScreen.STOCK_HISTORY) {
                    TopAppBar(
                        title = {
                            Text(
                                when (currentScreen) {
                                    CashierScreen.POS -> "Kasir (POS)"
                                    CashierScreen.PRODUCTS -> "Daftar Produk"
                                    CashierScreen.PRODUCT_DETAILS -> "Detail Produk"
                                    CashierScreen.INVENTORY -> "Inventaris & Stok"
                                    CashierScreen.HISTORY -> "Riwayat Penjualan"
                                    CashierScreen.EXPENSES -> "Pengeluaran"
                                    CashierScreen.SETTINGS -> "Pengaturan"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, null)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Surface,
                            titleContentColor = TextPrimary,
                            navigationIconContentColor = TextPrimary
                        )
                    )
                }
            },
            modifier = modifier,
            containerColor = Background
        ) { padding ->
            when (currentScreen) {
                CashierScreen.POS -> CashierPOSScreen(padding)
                CashierScreen.PRODUCTS -> CashierProductsScreen(
                    padding = padding,
                    onShowDetails = { product ->
                        selectedProductForDetails = product
                        currentScreen = CashierScreen.PRODUCT_DETAILS
                    }
                )
                CashierScreen.PRODUCT_DETAILS -> {
                    selectedProductForDetails?.let { product ->
                        CashierProductDetailsScreen(
                            product = product,
                            onBack = { currentScreen = CashierScreen.PRODUCTS }
                        )
                    }
                }
                CashierScreen.INVENTORY -> CashierInventoryScreen(
                    padding = padding, 
                    onShowHistory = { item ->
                        selectedItemForHistory = item
                        currentScreen = CashierScreen.STOCK_HISTORY
                    }
                )
                CashierScreen.HISTORY -> CashierHistoryScreen(padding)
                CashierScreen.EXPENSES -> CashierExpensesScreen(padding)
                CashierScreen.SETTINGS -> CashierSettingsScreen(padding, onLogout)
                CashierScreen.STOCK_HISTORY -> {
                    selectedItemForHistory?.let { item ->
                        CashierStockHistoryScreen(
                            product = item,
                            onBack = { currentScreen = CashierScreen.INVENTORY }
                        )
                    }
                }
            }
        }
    }
}

fun ProductDto.getStock(context: android.content.Context): Int {
    val outletId = SessionManager.getInstance(context).getOutletId()
    return this.outletStock[outletId] ?: 0
}

// ─── Modern Split-View POS Screen ──────────────────────────────────────
@Composable
fun CashierPOSScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showCheckoutSheet by remember { mutableStateOf(false) }

    // Checkout state
    var customerName by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var isProcessing by remember { mutableStateOf(false) }

    fun refreshItems() {
        coroutineScope.launch {
            isLoading = true
            val result = repository.getOutletItems()
            when (result) {
                is Resource.Success -> products = result.data
                is Resource.Error -> errorMsg = result.message
                is Resource.Loading -> {}
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { refreshItems() }

    // Category state
    val categories = remember(products) { listOf("All") + products.mapNotNull { it.category }.distinct().sorted() }
    var selectedCategory by remember { mutableStateOf("All") }

    val filtered = products.filter { item ->
        val matchesSearch = if (searchQuery.isBlank()) true else item.name.contains(searchQuery, ignoreCase = true) || (item.category ?: "").contains(searchQuery, ignoreCase = true)
        val matchesCategory = if (selectedCategory == "All") true else item.category == selectedCategory
        matchesSearch && matchesCategory
    }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val total = subtotal

    var showMobileCart by remember { mutableStateOf(false) }

    @Composable
    fun CartPanel(modifier: Modifier = Modifier) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cart header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pesanan Saat Ini", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = { cartItems = emptyList() }) {
                            Text("Hapus Semua", color = Error, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 24.dp))

                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(Background),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.ShoppingCart, null, tint = TextTertiary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Keranjang Anda kosong", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Ketuk produk untuk menambah", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                        }
                    }
                } else {
                    // Cart items list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cartItems.size) { idx ->
                            val item = cartItems[idx]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Item icon thumbnail
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.LocalDrink, null, tint = Primary, modifier = Modifier.size(20.dp)) }
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.unitPrice.formatRp(), style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                                }

                                // Quantity controls inside cart
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Background).padding(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            cartItems = cartItems.toMutableList().also {
                                                if (item.quantity <= 1) it.removeAt(idx) else it[idx] = it[idx].copy(quantity = it[idx].quantity - 1)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Filled.Remove, null, tint = TextSecondary, modifier = Modifier.size(16.dp)) }

                                    Text("${item.quantity}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(horizontal = 8.dp))

                                    IconButton(
                                        onClick = { cartItems = cartItems.toMutableList().also { it[idx] = it[idx].copy(quantity = it[idx].quantity + 1) } },
                                        modifier = Modifier.size(24.dp)
                                    ) { Icon(Icons.Filled.Add, null, tint = Primary, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                }

                // Cart footer / totals (Receipt fold)
                if (cartItems.isNotEmpty()) {
                    // Dashed divider indicating receipt fold
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 24.dp)) {
                        drawLine(
                            color = DividerColor,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                    
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                            Text(subtotal.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(total.formatRp(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showMobileCart = false; showCheckoutSheet = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize()
                                    .background(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Primary, Secondary))),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Lanjut ke Pembayaran", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Background Container
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Background).padding(padding)) {
        val isLargeScreen = maxWidth >= 800.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize().padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // ═══ LEFT: Products Panel (Floating) ═══
            Card(
                modifier = if (isLargeScreen) Modifier.weight(0.6f).fillMaxHeight() else Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Premium Search Bar
                    Box(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari produk...", color = TextTertiary) },
                            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = DividerColor,
                                cursorColor = Primary,
                                focusedContainerColor = Background,
                                unfocusedContainerColor = Background,
                                focusedPlaceholderColor = TextTertiary,
                                unfocusedPlaceholderColor = TextTertiary
                            ),
                            singleLine = true
                        )
                    }

                    // Category Chips
                    if (categories.size > 1) {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = category == selectedCategory
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) Primary else Background)
                                        .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.Transparent else DividerColor, RoundedCornerShape(20.dp))
                                        .clickable { selectedCategory = category }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 24.dp))

                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Primary)
                        }
                } else if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.PointOfSale, null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Produk tidak ditemukan", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered) { item ->
                            val inCart = cartItems.firstOrNull { it.product.id == item.id }
                            val outOfStock = item.getStock(context) <= 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (inCart != null) Primary.copy(alpha = 0.06f) else Surface
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (inCart != null) 1.dp else 0.dp,
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 1.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            val existing = cartItems.indexOfFirst { it.product.id == item.id }
                                            if (existing >= 0) {
                                                cartItems = cartItems.toMutableList().also {
                                                    it[existing] = it[existing].copy(quantity = it[existing].quantity + 1)
                                                }
                                            } else {
                                                cartItems = cartItems + CartItem(product = item, quantity = 1, unitPrice = item.unitPrice)
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.width(4.dp).height(72.dp)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Primary, Secondary)),
                                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Icon
                                    Box(
                                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))
                                            .background(Primary.copy(0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.LocalDrink, null,
                                            tint = Primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Text info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!item.category.isNullOrBlank()) {
                                            Text(item.category, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                item.unitPrice.formatRp(),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Primary
                                            )
                                            Box(
                                                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                .background(Success.copy(0.12f))
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            val stockCount = item.getStock(context)
                                            Text(
                                                "Stok $stockCount",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Success
                                            )
                                        }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Add / Cart indicator
                                    if (inCart != null) {
                                        // In cart — show stepper
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Primary.copy(0.1f)).padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val idx = cartItems.indexOfFirst { it.product.id == item.id }
                                                    if (idx >= 0) {
                                                        cartItems = cartItems.toMutableList().also {
                                                            if (it[idx].quantity <= 1) it.removeAt(idx)
                                                            else it[idx] = it[idx].copy(quantity = it[idx].quantity - 1)
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) { Icon(Icons.Filled.Remove, null, tint = Primary, modifier = Modifier.size(14.dp)) }
                                            Text(
                                                "${inCart.quantity}",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Primary,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = {
                                                    val idx = cartItems.indexOfFirst { it.product.id == item.id }
                                                    if (idx >= 0) cartItems = cartItems.toMutableList().also { it[idx] = it[idx].copy(quantity = it[idx].quantity + 1) }
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) { Icon(Icons.Filled.Add, null, tint = Primary, modifier = Modifier.size(14.dp)) }
                                        }
                                    } else {
                                        // Not in cart — show + button
                                        Box(
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                                .background(brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Primary, Secondary))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                        }
                    }
                }
            }
            }
            
            // ═══ RIGHT: Cart / Receipt Panel (DESKTOP) ═══
            if (isLargeScreen) {
                CartPanel(modifier = Modifier.weight(0.4f).fillMaxHeight())
            }
        }
        }
        
        // ═══ MOBILE FLOATING EXCLUSIVE ═══
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            androidx.compose.animation.AnimatedVisibility(
                visible = !isLargeScreen && cartItems.isNotEmpty(),
                modifier = Modifier.padding(bottom = 24.dp),
                enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
            ) {
                androidx.compose.material3.ExtendedFloatingActionButton(
                    onClick = { showMobileCart = true },
                    containerColor = Primary,
                    contentColor = Color.White,
                    expanded = true,
                    icon = { Icon(Icons.Filled.ShoppingCart, null) },
                    text = { Text("Lihat Keranjang (${cartItems.size}) • ${subtotal.formatRp()}", fontWeight = FontWeight.Bold) },
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                )
            }
        }
    }
    
    // ── Mobile Cart Dialog ─────────────────────────────────────────────
    if (showMobileCart) {
        Dialog(
            onDismissRequest = { showMobileCart = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)) {
                CartPanel(modifier = Modifier.fillMaxSize())
                
                // Floating close button
                IconButton(
                    onClick = { showMobileCart = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(36.dp)
                        .background(Background, CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = TextPrimary)
                }
            }
        }
    }

    // ── Checkout bottom sheet ──────────────────────────────────────────
    if (showCheckoutSheet) {
        Dialog(onDismissRequest = { showCheckoutSheet = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    // Header gradient
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Primary, Primary.copy(alpha = 0.75f))
                                ),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.ShoppingCartCheckout, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Selesaikan Penjualan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text("${cartItems.size} produk", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Total", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                                Text(total.formatRp(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            }
                        }
                    }

                    // Body
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Nama Pelanggan (opsional)") },
                            leadingIcon = { Icon(Icons.Filled.Person, null, tint = TextTertiary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = Primary,
                                unfocusedBorderColor = DividerColor,
                                cursorColor = Primary,
                                focusedLabelColor = Primary,
                                focusedPlaceholderColor = TextTertiary,
                                unfocusedPlaceholderColor = TextTertiary
                            ),
                            singleLine = true
                        )

                        Text("Metode Pembayaran", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("cash" to "Tunai", "card" to "Kartu", "e-wallet" to "Dompet Digital").forEach { (value, label) ->
                                val isSelected = paymentMethod == value
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Primary else Background)
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = if (isSelected) Primary else DividerColor,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { paymentMethod = value }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showCheckoutSheet = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                            ) { Text("Batal", color = TextSecondary, style = MaterialTheme.typography.titleSmall) }

                            Button(
                                onClick = {
                                    isProcessing = true
                                    val saleItems = cartItems.map {
                                        SaleItemDto(productId = it.product.id ?: "", quantity = it.quantity, unitPrice = it.unitPrice, totalPrice = it.unitPrice * it.quantity)
                                    }
                                    coroutineScope.launch {
                                        val result = repository.createSale(
                                            CreateSaleRequest(
                                                customerName = customerName.ifBlank { null }, customerPhone = null,
                                                items = saleItems, subtotal = subtotal, taxAmount = 0.0, discountAmount = 0.0,
                                                totalAmount = total, paymentMethod = paymentMethod
                                            )
                                        )
                                        if (result is Resource.Success) {
                                            cartItems = emptyList()
                                            customerName = ""
                                            paymentMethod = "cash"
                                            showCheckoutSheet = false
                                            refreshItems()
                                        }
                                        isProcessing = false
                                    }
                                },
                                enabled = !isProcessing,
                                modifier = Modifier.weight(2f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                if (isProcessing) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Selesaikan Penjualan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Products Management Screen ────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CashierProductsScreen(
    padding: PaddingValues,
    onShowDetails: (ProductDto) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val result = repository.getOutletItems()
        if (result is Resource.Success) products = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                loadData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        loadData()
        isLoading = false
    }

    // Add Product Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var sku by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var price by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    // Header
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Secondary, Primary)),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.AddShoppingCart, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Tambah Produk Baru", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("Isi detail produk di bawah ini", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                        focusedPlaceholderColor = TextTertiary,
                        unfocusedPlaceholderColor = TextTertiary
                    )
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = name, onValueChange = { name = it },
                            label = { Text("Nama Produk *") },
                            leadingIcon = { Icon(Icons.Filled.LocalDrink, null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = fieldColors, singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = category, onValueChange = { category = it },
                                label = { Text("Kategori") },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                                colors = fieldColors, singleLine = true)
                            OutlinedTextField(value = sku, onValueChange = { sku = it },
                                label = { Text("SKU") },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                                colors = fieldColors, singleLine = true)
                        }
                        OutlinedTextField(value = price, onValueChange = { price = it },
                            label = { Text("Harga Jual (Rp) *") },
                            leadingIcon = { Icon(Icons.Filled.AttachMoney, null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = fieldColors, singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it },
                            label = { Text("Deskripsi") },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = fieldColors, singleLine = true)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                            ) { Text("Batal", color = TextSecondary) }

                            Button(
                                onClick = {
                                    val p = price.toDoubleOrNull() ?: return@Button
                                    isSaving = true
                                    scope.launch {
                                        val result = repository.createOutletItem(CreateProductRequest(name = name, description = description, sku = sku, category = category, unitPrice = p))
                                        if (result is Resource.Success) { showAddDialog = false; refreshTrigger++ }
                                        isSaving = false
                                    }
                                },
                                enabled = name.isNotBlank() && price.toDoubleOrNull() != null && !isSaving,
                                modifier = Modifier.weight(2f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tambah Produk", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background).padding(padding).pullRefresh(pullRefreshState)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Daftar Produk", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp), color = TextPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("${products.size} produk", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (products.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Category, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Belum ada produk", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                Text("Ketuk + untuk menambah produk pertama Anda", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                        }
                    }
                }

                items(products) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onShowDetails(product) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.LocalDrink, null, tint = Primary, modifier = Modifier.size(22.dp)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                Text("${product.category ?: "-"} • SKU: ${product.sku ?: "-"}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Primary.copy(alpha = 0.08f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text(product.unitPrice.formatRp(), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    }
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(
                                        if (product.getStock(context) <= 0) Error.copy(alpha = 0.08f) else Success.copy(alpha = 0.08f)
                                    ).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                        Text("Stok: ${product.getStock(context)}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (product.getStock(context) <= 0) Error else Success)
                                    }
                                }
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextTertiary, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Primary, contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Filled.Add, "Tambah Produk") }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierProductDetailsScreen(
    product: ProductDto,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(product.name) }
    var description by remember { mutableStateOf(product.description ?: "") }
    var sku by remember { mutableStateOf(product.sku ?: "") }
    var category by remember { mutableStateOf(product.category ?: "") }
    var sellingPrice by remember { mutableStateOf(if (product.unitPrice > 0) product.unitPrice.toLong().toString() else "") }
    var stock by remember { mutableStateOf(product.getStock(context).toString()) }
    
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Produk") },
            text = { Text("Apakah Anda yakin ingin menghapus \"${product.name}\"? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        product.id?.let { repository.deleteOutletItem(it) }
                        onBack()
                    }
                }) { Text("Hapus", color = Error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Produk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, null, tint = Error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = Primary,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = Surface,
                unfocusedContainerColor = Surface,
                focusedPlaceholderColor = TextTertiary,
                unfocusedPlaceholderColor = TextTertiary
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Produk") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Kategori") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = if (product.costPrice > 0) product.costPrice.toLong().toString() else "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Harga Beli (Info)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DividerColor,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                        focusedPlaceholderColor = TextTertiary,
                        unfocusedPlaceholderColor = TextTertiary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { if (it.all { char -> char.isDigit() }) sellingPrice = it },
                    label = { Text("Harga Jual (Rp)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = stock,
                onValueChange = { if (it.all { char -> char.isDigit() }) stock = it },
                label = { Text("Stok Saat Ini") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                minLines = 3
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val sp = sellingPrice.toDoubleOrNull() ?: 0.0
                    val s = stock.toIntOrNull() ?: 0
                    isSaving = true
                    scope.launch {
                        val result = repository.updateProduct(
                            productId = product.id ?: "",
                            product = CreateProductRequest(
                                name = name,
                                description = description,
                                sku = sku,
                                category = category,
                                costPrice = product.costPrice,
                                unitPrice = sp,
                                stock = s
                            )
                        )
                        if (result is Resource.Success) {
                            android.widget.Toast.makeText(context, "Produk berhasil diperbarui", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        } else if (result is Resource.Error) {
                            android.widget.Toast.makeText(context, result.message, android.widget.Toast.LENGTH_LONG).show()
                        }
                        isSaving = false
                    }
                },
                enabled = name.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Simpan Perubahan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

// ─── Inventory Screen with Purchase ────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CashierInventoryScreen(
    padding: PaddingValues,
    onShowHistory: (ProductDto) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var purchases by remember { mutableStateOf<List<OutletPurchaseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var selectedPurchase by remember { mutableStateOf<OutletPurchaseDto?>(null) }
    // Removed selectedStockItem dialog state

    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val itemResult = repository.getOutletItems()
        if (itemResult is Resource.Success) products = itemResult.data
        val purchaseResult = repository.getOutletPurchases()
        if (purchaseResult is Resource.Success) {
            purchases = purchaseResult.data.sortedByDescending { it.purchaseDate ?: it.id ?: "" }
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                loadData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        loadData()
        isLoading = false
    }

    // Purchase dialog — multi-item builder
    if (showPurchaseDialog && products.isNotEmpty()) {
        var orderNotes by remember { mutableStateOf("") }
        var addedItems by remember { mutableStateOf<List<OutletPurchaseItemDto>>(emptyList()) }
        
        var selectedItemIdx by remember { mutableIntStateOf(0) }
        var qty by remember { mutableStateOf("1") }
        var cost by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }
        var expanded by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showPurchaseDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Primary, Secondary)),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ).padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.LocalShipping, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Buat Pesanan Pembelian", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("Tambah item ke pesanan Anda", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }

                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                        focusedPlaceholderColor = TextTertiary,
                        unfocusedPlaceholderColor = TextTertiary
                    )
                    Column(modifier = Modifier.padding(20.dp).heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(value = orderNotes, onValueChange = { orderNotes = it }, label = { Text("Judul / Catatan Pesanan") },
                            leadingIcon = { Icon(Icons.Filled.Notes, null, tint = TextTertiary, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true, colors = fieldColors)

                        if (addedItems.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Item Ditambahkan (${addedItems.size})", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                val orderTotal = addedItems.sumOf { it.totalPrice }
                                Text(orderTotal.formatRp(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                            }
                            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 160.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(addedItems) { added ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Background).padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(added.productName ?: "Item", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                            Text("${added.quantity} × ${added.unitPrice.formatRp()}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                        Text(added.totalPrice.formatRp(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier.size(26.dp).clip(CircleShape).background(Error.copy(alpha = 0.1f)).clickable { addedItems = addedItems.filter { it != added } },
                                            contentAlignment = Alignment.Center
                                        ) { Icon(Icons.Filled.Close, null, tint = Error, modifier = Modifier.size(14.dp)) }
                                    }
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                        }

                        Text("Tambah Item", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = products.getOrNull(selectedItemIdx)?.name ?: "",
                                onValueChange = {}, readOnly = true, label = { Text("Pilih Produk") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(14.dp), colors = fieldColors
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                products.forEachIndexed { idx, item ->
                                    DropdownMenuItem(text = { Text(item.name) }, onClick = { 
                                        selectedItemIdx = idx
                                        cost = if (item.costPrice > 0) item.costPrice.toLong().toString() else ""
                                        expanded = false 
                                    })
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Jml") },
                                modifier = Modifier.weight(0.35f), shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fieldColors)
                            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Harga Satuan (Rp)") },
                                modifier = Modifier.weight(0.65f), shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = fieldColors)
                        }
                        Button(
                            onClick = {
                                val q = qty.toIntOrNull() ?: return@Button
                                val c = cost.toDoubleOrNull() ?: return@Button
                                val item = products.getOrNull(selectedItemIdx) ?: return@Button
                                addedItems = addedItems + OutletPurchaseItemDto(productId = item.id ?: "", productName = item.name, quantity = q, unitPrice = c, totalPrice = c * q)
                                qty = "1"; cost = ""
                            },
                            enabled = qty.toIntOrNull() != null && cost.toDoubleOrNull() != null,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary), shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah ke Pesanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showPurchaseDialog = false }, modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                            ) { Text("Batal", color = TextSecondary) }
                            Button(
                                onClick = {
                                    isSaving = true
                                    val orderTotal = addedItems.sumOf { it.totalPrice }
                                    scope.launch {
                                        val result = repository.createOutletPurchase(CreateOutletPurchaseRequest(
                                            invoiceId = "", outletId = "", items = addedItems, totalAmount = orderTotal, notes = orderNotes.ifBlank { "Pembelian Kasir" }
                                        ))
                                        if (result is Resource.Success) { showPurchaseDialog = false; refreshTrigger++ }
                                        isSaving = false
                                    }
                                },
                                enabled = addedItems.isNotEmpty() && !isSaving,
                                modifier = Modifier.weight(2f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Primary), shape = RoundedCornerShape(14.dp)
                            ) {
                                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Filled.Send, null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Kirim Pesanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Purchase Details & Receive dialog
    selectedPurchase?.let { purchase ->
        var isReceiving by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { selectedPurchase = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column {
                    val statusColor = when (purchase.status) { "received" -> Success; "pending" -> Warning; else -> TextTertiary }
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .background(brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(statusColor.copy(alpha = 0.85f), statusColor)), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.LocalShipping, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(purchase.notes ?: "Order #${purchase.id?.takeLast(6)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    Text(purchase.purchaseDate ?: "", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Status", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
                                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.25f)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    Text(when(purchase.status) { "received" -> "Diterima"; "pending" -> "Menunggu"; else -> purchase.status.replaceFirstChar { it.uppercase() } }, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${purchase.items.size} produk", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = TextSecondary)
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(purchase.items) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Background).padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName ?: "Item", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        Text("${item.quantity} × ${item.unitPrice.formatRp()}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    }
                                    Text(item.totalPrice.formatRp(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                }
                            }
                        }
                        HorizontalDivider(color = DividerColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(purchase.totalAmount.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { selectedPurchase = null },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
                            ) { Text("Tutup", color = TextSecondary) }
                            if (purchase.status == "pending") {
                                Button(
                                    onClick = {
                                        isReceiving = true
                                        scope.launch {
                                            val result = repository.receivePurchase(purchase.id ?: "")
                                            if (result is Resource.Success) { selectedPurchase = null; refreshTrigger++ }
                                            isReceiving = false
                                        }
                                    },
                                    enabled = !isReceiving,
                                    modifier = Modifier.weight(2f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Success), shape = RoundedCornerShape(14.dp)
                                ) {
                                    if (isReceiving) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    else {
                                        Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tandai Telah Diterima", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize().background(Background).padding(padding).pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("Inventaris", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp), color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Tingkat stok & pesanan pembelian", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary,
                modifier = Modifier.padding(horizontal = 20.dp).clip(RoundedCornerShape(12.dp))
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Stok") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Pembelian") })
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else when (selectedTab) {
                0 -> { // Stock tab
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stats row
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Item", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${products.size}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    }
                                }
                                Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Kategori", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("${products.mapNotNull { it.category }.distinct().size}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Secondary)
                                    }
                                }
                            }
                        }

                        if (products.isEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Inventory, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Tidak ada item di inventaris", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        items(products) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onShowHistory(item) }, 
                                shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                                        Text("${item.getStock(context)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        Text("${item.category ?: "-"} | SKU: ${item.sku ?: "-"}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                        Text("Harga Beli: ${item.costPrice.formatRp()}", style = MaterialTheme.typography.labelSmall, color = Primary)
                                    }
                                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextTertiary, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
                1 -> { // Purchases tab
                    if (purchases.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.LocalShipping, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Tidak ada pesanan pembelian", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                Text("Ketuk + untuk membuat pesanan", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(purchases) { purchase ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable { selectedPurchase = purchase }, 
                                    shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)
                                ) {
                                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Secondary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.LocalShipping, null, tint = Secondary, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(purchase.notes ?: "Order #${purchase.id?.takeLast(6) ?: "-"}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                            Text("${purchase.items.size} produk", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                        }
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(
                                                when (purchase.status) { "received" -> Success.copy(alpha = 0.1f); "pending" -> Warning.copy(alpha = 0.1f); else -> TextTertiary.copy(alpha = 0.1f) }
                                            ).padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                when(purchase.status) {
                                                    "received" -> "Diterima"
                                                    "pending" -> "Menunggu"
                                                    else -> purchase.status.replaceFirstChar { it.uppercase() }
                                                },
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = when (purchase.status) { "received" -> Success; "pending" -> Warning; else -> TextTertiary }
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
            }
        }

        // FAB for purchase order
        FloatingActionButton(
            onClick = { showPurchaseDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Primary, contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) { Icon(Icons.Filled.Add, "Pembelian Baru") }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

// ─── Sales History Screen ──────────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CashierHistoryScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }

    var sales by remember { mutableStateOf<List<SaleDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val loadData = suspend {
        val result = repository.getSales()
        if (result is Resource.Success) sales = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                loadData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        loadData()
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Background).padding(padding).pullRefresh(pullRefreshState)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp)) {
            Text(
                "Riwayat Penjualan",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Transaksi terbaru", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (sales.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belum ada penjualan", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    Text("Penjualan Anda akan muncul di sini", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sales) { sale ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Receipt, null, tint = Primary, modifier = Modifier.size(18.dp)) }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sale.receiptNumber ?: "Penjualan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                val customer = sale.customerName ?: "Pelanggan Umum"
                                Text(customer, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Primary.copy(alpha = 0.08f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(sale.paymentMethod.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = Primary)
                                    }
                                    Text("${sale.items.size} produk", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                            }
                            Text(sale.totalAmount.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

// ─── Settings Screen ───────────────────────────────────────────────────
@Composable
fun CashierSettingsScreen(padding: PaddingValues, onLogout: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Pengaturan", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp), color = TextPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Akun dan pengaturan aplikasi", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary), contentAlignment = Alignment.Center) {
                            Text((sessionManager.getUsername()?.firstOrNull()?.uppercase() ?: "C"), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(sessionManager.getUsername() ?: "Cashier", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                            Text("Peran: Kasir", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Update App Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { 
                                scope.launch {
                                    AppUpdater.checkForUpdate(context, showToastIfUpToDate = true)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SystemUpdate, null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Perbarui Aplikasi", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                            Text("Cek dan instal versi terbaru", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { sessionManager.clearSession(); RetrofitClient.refresh(); onLogout() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Error.copy(alpha = 0.1f), contentColor = Error),
                        shape = RoundedCornerShape(14.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Server", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("Terhubung ke: ${RetrofitClient.BASE_URL}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CashierStockHistoryScreen(
    product: ProductDto,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    var stockHistory by remember { mutableStateOf<List<StockHistoryDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(product.id) {
        isLoading = true
        val result = repository.getOutletStockHistory(product.id ?: "")
        if (result is Resource.Success) stockHistory = result.data
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Stok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(product.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (stockHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, null, tint = TextTertiary, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum ada riwayat", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Text("Transaksi akan muncul di sini", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Info Produk Saat Ini", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("SKU", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        Text(product.sku ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Harga Beli", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        Text(product.costPrice.formatRp(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Log Mutasi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    }

                    items(stockHistory) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (log.changeQty > 0) Success.copy(0.1f) else Warning.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(if (log.changeQty > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown, null,
                                        tint = if (log.changeQty > 0) Success else Warning, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        when(log.type) {
                                            "purchase" -> "Pembelian"
                                            "sale" -> "Penjualan"
                                            "adjustment" -> "Penyesuaian"
                                            else -> log.type.replaceFirstChar { it.uppercase() }
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(log.createdAt?.take(16)?.replace("T", " ") ?: "", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    val displayCost = if ((log.costPrice ?: 0.0) > 0) log.costPrice else product.costPrice
                                    if ((displayCost ?: 0.0) > 0) {
                                        Text("Harga: ${displayCost!!.formatRp()}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Secondary)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${if (log.changeQty > 0) "+" else ""}${log.changeQty}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (log.changeQty > 0) Success else Warning
                                    )
                                    Text("Saldo: ${log.balance}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CashierExpensesScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf<List<ExpenseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Add Expense Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categoryHistory = remember(expenses) {
        expenses.map { it.category }.distinct().sorted()
    }

    val filteredCategories = remember(category, categoryHistory) {
        categoryHistory.filter { it.contains(category, ignoreCase = true) }
    }

    fun fetchData() {
        scope.launch {
            isLoading = true
            when (val result = repository.getExpenses()) {
                is Resource.Success -> {
                    expenses = result.data ?: emptyList()
                    errorMessage = null
                }
                is Resource.Error -> errorMessage = result.message
                is Resource.Loading -> {}
            }
            isLoading = false
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                fetchData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(Unit) { fetchData() }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Tambah Pengeluaran", fontWeight = FontWeight.Bold, color = Primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Keterangan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                            focusedLabelColor = Primary,
                            focusedPlaceholderColor = TextTertiary,
                            unfocusedPlaceholderColor = TextTertiary
                        )
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                        label = { Text("Jumlah (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = DividerColor,
                            cursorColor = Primary,
                            focusedLabelColor = Primary,
                            focusedPlaceholderColor = TextTertiary,
                            unfocusedPlaceholderColor = TextTertiary
                        )
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { 
                                    category = it
                                    categoryExpanded = true 
                                },
                                label = { Text("Kategori") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            if (filteredCategories.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    filteredCategories.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption) },
                                            onClick = {
                                                category = selectionOption
                                                categoryExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountDouble = amount.toDoubleOrNull() ?: 0.0
                        if (description.isNotBlank() && amountDouble > 0 && category.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                val result = repository.createExpense(
                                    CreateExpenseRequest(
                                        description = description,
                                        amount = amountDouble,
                                        category = category
                                    )
                                )
                                isSaving = false
                                if (result is Resource.Success) {
                                    showAddDialog = false
                                    description = ""
                                    amount = ""
                                    fetchData()
                                }
                            }
                        }
                    },
                    enabled = description.isNotBlank() && amount.isNotBlank() && category.isNotBlank() && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSaving) {
                    Text("Batal", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Surface
        )
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
        if (isLoading && !isRefreshing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (errorMessage != null && expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, null, tint = Error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage ?: "Gagal memuat data", color = TextSecondary)
                    Button(onClick = { fetchData() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                if (expenses.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize().padding(bottom = 100.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.ReceiptLong, null, tint = TextTertiary.copy(alpha = 0.3f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Tidak ada pengeluaran", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                Text("Klik + untuk menambah pengeluaran baru", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                        }
                    }
                } else {
                    items(expenses) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Error.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.ArrowDownward, null, tint = Error, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(expense.description, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                    Text("${expense.category} • ${expense.expenseDate?.split("T")?.getOrNull(0) ?: ""}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                }
                                Text(expense.amount.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Error)
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = Primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Filled.Add, "Tambah Pengeluaran")
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

class CashierActivityClass : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager.getInstance(this)
        RetrofitClient.init(sessionManager)
        setContent {
            val context = LocalContext.current
            TehAtlasTheme(darkTheme = false) {
                LaunchedEffect(Unit) {
                    AppUpdater.checkForUpdate(context)
                }
                CashierDashboard(onLogout = {
                    sessionManager.clearSession()
                    RetrofitClient.refresh()
                    
                    val intent = android.content.Intent(this@CashierActivityClass, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                })
                AppUpdater.Component()
            }
        }
    }
}
