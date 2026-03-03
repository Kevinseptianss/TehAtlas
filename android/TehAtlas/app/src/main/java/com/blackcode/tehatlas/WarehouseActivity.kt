package com.blackcode.tehatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.blackcode.tehatlas.ui.theme.TehAtlasTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackcode.tehatlas.network.*
import com.blackcode.tehatlas.ui.theme.*
import com.blackcode.tehatlas.utils.formatRp
import com.blackcode.tehatlas.utils.AppUpdater
import com.blackcode.tehatlas.ui.UpdateDialog
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.pullrefresh.PullRefreshIndicator

// Warehouse Navigation
enum class WarehouseScreen { DASHBOARD, INVENTORY, PURCHASING, INVOICING, SETTINGS, STOCK_HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDashboard(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(WarehouseScreen.DASHBOARD) }
    var selectedItemForHistory by remember { mutableStateOf<ProductDto?>(null) }

    Scaffold(
        bottomBar = {
            if (currentScreen != WarehouseScreen.STOCK_HISTORY) {
                NavigationBar(containerColor = Surface, contentColor = TextPrimary, modifier = Modifier.height(80.dp)) {
                    listOf(
                        Triple(WarehouseScreen.DASHBOARD, Icons.Filled.Dashboard, "Dashboard"),
                        Triple(WarehouseScreen.INVENTORY, Icons.Filled.Inventory, "Stok"),
                        Triple(WarehouseScreen.PURCHASING, Icons.Filled.ShoppingCart, "Pembelian"),
                        Triple(WarehouseScreen.INVOICING, Icons.Filled.Receipt, "Tagihan"),
                        Triple(WarehouseScreen.SETTINGS, Icons.Filled.Settings, "Pengaturan")
                    ).forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, label, modifier = Modifier.size(20.dp)) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = Primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        },
        modifier = modifier,
        containerColor = Background
    ) { padding ->
        when (currentScreen) {
            WarehouseScreen.DASHBOARD -> WarehouseDashboardScreen(padding)
            WarehouseScreen.INVENTORY -> WarehouseInventoryScreen(
                padding = padding,
                onShowHistory = { item ->
                    selectedItemForHistory = item
                    currentScreen = WarehouseScreen.STOCK_HISTORY
                }
            )
            WarehouseScreen.PURCHASING -> WarehousePurchasingScreen(padding)
            WarehouseScreen.INVOICING -> WarehouseInvoicingScreen(padding)
            WarehouseScreen.SETTINGS -> WarehouseSettingsScreen(padding, onLogout)
            WarehouseScreen.STOCK_HISTORY -> {
                selectedItemForHistory?.let { item ->
                    WarehouseStockHistoryScreen(
                        product = item,
                        onBack = { currentScreen = WarehouseScreen.INVENTORY }
                    )
                }
            }
        }
    }
}

// ─── Dashboard Screen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WarehouseDashboardScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var dashboard by remember { mutableStateOf<WarehouseDashboardDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val result = repository.getWarehouseDashboard()
        if (result is Resource.Success) dashboard = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                loadData()
                isRefreshing = false
            }
        }
    )

    LaunchedEffect(Unit) {
        isLoading = true
        loadData()
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    val d = dashboard

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Dashboard Gudang",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Pantau operasional gudang", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        // Metrics
        item {
            Text("Ringkasan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WarehouseMetricCard(
                    title = "Total Produk",
                    value = (d?.totalItems ?: 0).toString(),
                    icon = Icons.Filled.Inventory,
                    modifier = Modifier.weight(1f)
                )
                WarehouseMetricCard(
                    title = "Tagihan Menunggu",
                    value = (d?.pendingInvoices ?: 0).toString(),
                    icon = Icons.Filled.Receipt,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WarehouseMetricCard(
                    title = "Stok Rendah",
                    value = (d?.lowStockItems ?: 0).toString(),
                    icon = Icons.Filled.Warning,
                    modifier = Modifier.weight(1f)
                )
                WarehouseMetricCard(
                    title = "Nilai Stok",
                    value = "${(d?.totalStockValue ?: 0.0).formatRp()}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Purchases
        val recentPurchases = d?.recentPurchases ?: emptyList()
        if (recentPurchases.isNotEmpty()) {
            item {
                Text(
                    "Pembelian Terbaru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
            items(recentPurchases.take(3)) { purchase ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.ShoppingCart, null, tint = Secondary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                purchase.supplierName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                "${purchase.items.size} produk - ${when(purchase.status) { "received" -> "Diterima"; "pending" -> "Menunggu"; else -> purchase.status }}",
                                style = MaterialTheme.typography.bodySmall, color = TextTertiary
                            )
                        }
                        Text(
                            purchase.totalAmount.formatRp(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }
            }
        }

        // Recent Invoices
        val recentInvoices = d?.recentInvoices ?: emptyList()
        if (recentInvoices.isNotEmpty()) {
            item {
                Text(
                    "Tagihan Terbaru",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
            items(recentInvoices.take(3)) { invoice ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Primary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Receipt, null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                invoice.invoiceNumber,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                "${when(invoice.status) { "paid" -> "Lunas"; "pending" -> "Menunggu"; else -> invoice.status }} - ${invoice.items.size} produk",
                                style = MaterialTheme.typography.bodySmall, color = TextTertiary
                            )
                        }
                        Text(
                            invoice.totalAmount.formatRp(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

// ─── Inventory Screen ──────────────────────────────────────────────────
@Composable
fun WarehouseInventoryScreen(
    padding: PaddingValues,
    onShowHistory: (ProductDto) -> Unit
) {
    var showAddProduct by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    if (showAddProduct) {
        WarehouseAddProductScreen(
            onBack = { showAddProduct = false },
            onProductCreated = {
                showAddProduct = false
                refreshTrigger++
            }
        )
    } else {
        WarehouseInventoryListScreen(
            padding = padding,
            onAddProduct = { showAddProduct = true },
            onShowHistory = onShowHistory,
            refreshTrigger = refreshTrigger
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WarehouseInventoryListScreen(
    padding: PaddingValues,
    onAddProduct: () -> Unit,
    onShowHistory: (ProductDto) -> Unit,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedHistoryItem by remember { mutableStateOf<ProductDto?>(null) }
    var historyData by remember { mutableStateOf<List<StockHistoryDto>?>(null) }
    var isHistoryLoading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedHistoryItem) {
        if (selectedHistoryItem != null) {
            isHistoryLoading = true
            val res = repository.getWarehouseStockHistory(selectedHistoryItem!!.id ?: "")
            if (res is Resource.Success) {
                historyData = res.data
            }
            isHistoryLoading = false
        } else {
            historyData = null
        }
    }

    val loadData = suspend {
        val result = repository.getWarehouseItems()
        if (result is Resource.Success) products = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Background),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Inventaris",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Kelola stok gudang", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                FloatingActionButton(
                    onClick = onAddProduct,
                    containerColor = Primary, contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, "Tambah Produk")
                }
            }
        }

        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Produk", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            products.size.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kategori", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            products.mapNotNull { it.category }.distinct().size.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Secondary
                        )
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Inventory, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada produk di gudang", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        Text("Tambah produk untuk memulai", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                }
            }
        }

        items(products) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onShowHistory(item) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                            .background(Primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Inventory, null, tint = Primary, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Text(
                            "${item.category ?: "-"} | SKU: ${item.sku ?: "-"}",
                            style = MaterialTheme.typography.bodySmall, color = TextTertiary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            item.costPrice.formatRp(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                        Text("Harga Beli", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun WarehouseStockHistoryScreen(
    product: ProductDto,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var historyData by remember { mutableStateOf<List<StockHistoryDto>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadHistory = suspend {
        val res = repository.getWarehouseStockHistory(product.id ?: "")
        if (res is Resource.Success) {
            historyData = res.data
        }
    }

    LaunchedEffect(product.id) {
        isLoading = true
        loadHistory()
        isLoading = false
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
                isRefreshing = true
                loadHistory()
                isRefreshing = false
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Stok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(product.name, style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (historyData.isNullOrEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.History, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Riwayat stok tidak ditemukan", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Info Produk Saat Ini", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("SKU", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        Text(product.sku ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Harga Beli Saat Ini", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        Text(product.costPrice.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Log Mutasi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                    }

                    items(historyData!!) { history ->
                        val dateStr = runCatching {
                            val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                            val formatter = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                            formatter.format(parser.parse(history.createdAt ?: "") ?: java.util.Date())
                        }.getOrElse { history.createdAt ?: "Unknown" }

                        val amountColor = if (history.changeQty > 0) Success else if (history.changeQty < 0) Error else TextPrimary
                        val amountPrefix = if (history.changeQty > 0) "+" else ""

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            when(history.type) {
                                                "initial" -> "Stok Awal"
                                                "purchase" -> "Pembelian Diterima"
                                                "invoice" -> "Tagihan Dikirim"
                                                "adjustment" -> "Penyesuaian Stok"
                                                else -> history.type.replaceFirstChar { it.uppercase() }
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "$amountPrefix${history.changeQty}",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = amountColor
                                        )
                                        Text("Saldo: ${history.balance}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    }
                                }
                                
                                val displayCost = if (history.costPrice != null && history.costPrice > 0) history.costPrice else product.costPrice
                                
                                if (displayCost > 0) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Background, thickness = 1.dp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Harga Beli", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                                        Text(
                                            displayCost.formatRp(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(isRefreshing, pullRefreshState, Modifier.align(Alignment.TopCenter), contentColor = Primary)
        }
    }
}

// ─── Add Product Screen ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseAddProductScreen(onBack: () -> Unit, onProductCreated: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var productName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var existingCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        when (val result = repository.getWarehouseItems()) {
            is Resource.Success -> {
                existingCategories = result.data?.mapNotNull { it.category }
                    ?.filter { it.isNotBlank() }
                    ?.distinct()
                    ?.sorted() ?: emptyList()
            }
            else -> {}
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary)
                }
                Text(
                    "Tambah Produk Baru",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Informasi Produk",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = productName, onValueChange = { productName = it },
                        label = { Text("Nama Produk") },
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
                        )
                    )

                    OutlinedTextField(
                        value = sku, onValueChange = { sku = it },
                        label = { Text("SKU") },
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
                        )
                    )

                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = category, onValueChange = { category = it },
                            label = { Text("Kategori (Pilih atau Ketik Baru)") },
                            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = "Category", tint = Primary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                            )
                        )
                        if (existingCategories.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.background(Surface)
                            ) {
                                existingCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                                        onClick = { category = cat; categoryExpanded = false },
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Deskripsi (Opsional)") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
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
                        )
                    )
                }
            }
        }

        if (errorMsg != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))
                ) {
                    Text(
                        errorMsg ?: "",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium, color = Error
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    isCreating = true
                    errorMsg = null
                    coroutineScope.launch {
                        val result = repository.createWarehouseItem(
                            CreateProductRequest(
                                name = productName,
                                sku = sku,
                                category = category,
                                costPrice = 0.0,
                                description = description
                            )
                        )
                        when (result) {
                            is Resource.Success -> onProductCreated()
                            is Resource.Error -> { errorMsg = result.message; isCreating = false }
                            is Resource.Loading -> {}
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = productName.isNotEmpty() && sku.isNotEmpty() && category.isNotEmpty()
                         && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                } else {
                    Text("Tambah Produk", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Purchasing Screen ─────────────────────────────────────────────────
@Composable
fun WarehousePurchasingScreen(padding: PaddingValues) {
    var showAddPurchase by remember { mutableStateOf(false) }
    var selectedPurchase by remember { mutableStateOf<PurchaseDto?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    if (showAddPurchase) {
        WarehouseAddPurchaseScreen(
            onBack = { showAddPurchase = false },
            onPurchaseCreated = { showAddPurchase = false; refreshTrigger++ }
        )
    } else if (selectedPurchase != null) {
        WarehousePurchaseDetailScreen(
            purchase = selectedPurchase!!,
            onBack = { selectedPurchase = null },
            onStatusUpdated = { selectedPurchase = null; refreshTrigger++ }
        )
    } else {
        WarehousePurchasesListScreen(
            padding = padding,
            onAddPurchase = { showAddPurchase = true },
            onPurchaseClick = { selectedPurchase = it },
            refreshTrigger = refreshTrigger
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WarehousePurchasesListScreen(
    padding: PaddingValues,
    onAddPurchase: () -> Unit,
    onPurchaseClick: (PurchaseDto) -> Unit,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var purchases by remember { mutableStateOf<List<PurchaseDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val result = repository.getPurchases()
        if (result is Resource.Success) purchases = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Pesanan Pembelian",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Kelola pesanan ke pemasok", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                FloatingActionButton(
                    onClick = onAddPurchase,
                    containerColor = Primary, contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, "Pembelian Baru")
                }
            }
        }

        if (purchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada pembelian", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    }
                }
            }
        }

        items(purchases) { purchase ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onPurchaseClick(purchase) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            purchase.supplierName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (purchase.status) {
                                        "pending" -> Warning.copy(alpha = 0.1f)
                                        "received" -> Success.copy(alpha = 0.1f)
                                        "cancelled" -> Error.copy(alpha = 0.1f)
                                        else -> DividerColor
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                when (purchase.status) { "pending" -> "Menunggu"; "received" -> "Diterima"; "cancelled" -> "Dibatalkan"; else -> purchase.status.replaceFirstChar { it.uppercase() } },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (purchase.status) {
                                    "pending" -> Warning
                                    "received" -> Success
                                    "cancelled" -> Error
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${purchase.items.size} produk",
                        style = MaterialTheme.typography.bodySmall, color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        purchase.totalAmount.formatRp(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
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
fun WarehousePurchaseDetailScreen(
    purchase: PurchaseDto,
    onBack: () -> Unit,
    onStatusUpdated: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()
    var isUpdating by remember { mutableStateOf(false) }
    
    // Fetch products to map names to IDs since purchase item only has ID
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        val result = repository.getWarehouseItems()
        if (result is Resource.Success) products = result.data
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary)
                }
                Text(
                    "Detail Pembelian",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Pemasok", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                            Text(purchase.supplierName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (purchase.status) {
                                        "pending" -> Warning.copy(alpha = 0.1f)
                                        "received" -> Success.copy(alpha = 0.1f)
                                        "cancelled" -> Error.copy(alpha = 0.1f)
                                        else -> DividerColor
                                    }
                                ).padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                when (purchase.status) { "pending" -> "Menunggu"; "received" -> "Diterima"; "cancelled" -> "Dibatalkan"; else -> purchase.status.replaceFirstChar { it.uppercase() } },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = when (purchase.status) {
                                    "pending" -> Warning
                                    "received" -> Success
                                    "cancelled" -> Error
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                    HorizontalDivider(color = DividerColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Tanggal", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                            Text(purchase.purchaseDate?.take(10) ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Tagihan", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
                            Text(purchase.totalAmount.formatRp(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                    }
                }
            }
        }

        item {
            Text("Item Pesanan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }

        items(purchase.items) { item ->
            val product = products.find { it.id == item.productId }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Inventory, null, tint = Primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product?.name ?: "Loading...", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        Text("${item.quantity} x ${item.unitCost.formatRp()}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                    Text(item.totalCost.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
            }
        }

        if (purchase.status == "pending") {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isUpdating = true
                        coroutineScope.launch {
                            val statusRes = repository.updatePurchaseStatus(purchase.id ?: "", "received")
                            isUpdating = false
                            if (statusRes is Resource.Success) {
                                android.widget.Toast.makeText(context, "Pembelian berhasil diterima", android.widget.Toast.LENGTH_SHORT).show()
                                onStatusUpdated()
                            } else {
                                android.widget.Toast.makeText(context, "Gagal memperbarui status", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Success),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Filled.CheckCircle, "Terima", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tandai Telah Diterima", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }
    }
}

// ─── Add Purchase Screen ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseAddPurchaseScreen(onBack: () -> Unit, onPurchaseCreated: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var supplierName by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var purchaseItems by remember { mutableStateOf<List<PurchaseItemDto>>(emptyList()) }
    var showItemDialog by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = repository.getWarehouseItems()
        if (result is Resource.Success) products = result.data
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary)
                }
                Text(
                    "Buat Pesanan Pembelian",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Info Pemasok", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    OutlinedTextField(
                        value = supplierName, onValueChange = { supplierName = it },
                        label = { Text("Nama Pemasok") },
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
                        )
                    )
                }
            }
        }

        // Items header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item Pesanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Button(
                    onClick = { showItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Item")
                }
            }
        }

        items(purchaseItems) { item ->
            val product = products.find { it.id == item.productId }
            val itemName = product?.name ?: item.productId
            val stockInfo = product?.warehouseStock?.toString() ?: "0"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(itemName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        Text("Jml: ${item.quantity} x ${item.unitCost.formatRp()}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Text("Stok Saat Ini: $stockInfo", style = MaterialTheme.typography.labelSmall, color = Primary)
                    }
                    Text(item.totalCost.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                }
            }
        }

        // Total & Submit
        if (purchaseItems.isNotEmpty()) {
            item {
                val total = purchaseItems.sumOf { it.totalCost }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Text(total.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    isCreating = true
                    coroutineScope.launch {
                        val total = purchaseItems.sumOf { it.totalCost }
                        val result = repository.createPurchase(
                            CreatePurchaseRequest(
                                supplierName = supplierName,
                                items = purchaseItems,
                                totalAmount = total
                            )
                        )
                        if (result is Resource.Success) onPurchaseCreated()
                        isCreating = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = supplierName.isNotEmpty() && purchaseItems.isNotEmpty() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                else Text("Buat Pesanan Pembelian", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Item Dialog
    if (showItemDialog) {
        var selectedItem by remember { mutableStateOf<ProductDto?>(null) }
        var quantity by remember { mutableStateOf("") }
        var unitCost by remember { mutableStateOf("") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showItemDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Tambah Item Pembelian", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        var itemExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = itemExpanded, onExpandedChange = { itemExpanded = !itemExpanded }) {
                            OutlinedTextField(
                                value = selectedItem?.name ?: "",
                                onValueChange = {},
                                label = { Text("Pilih Item") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                readOnly = true,
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
                                )
                            )
                            ExposedDropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                                products.forEach { item ->
                                    key(item.id) {
                                        DropdownMenuItem(
                                            text = { Text("${item.name} (${item.category ?: ""})") },
                                            onClick = { 
                                                selectedItem = item
                                                unitCost = if (item.costPrice > 0) item.costPrice.toLong().toString() else ""
                                                itemExpanded = false 
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = quantity, onValueChange = { quantity = it },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
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
                            value = unitCost, onValueChange = { unitCost = it },
                            label = { Text("Harga Satuan (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
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
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showItemDialog = false }) {
                            Text("Batal", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val sel = selectedItem ?: return@Button
                                val qty = quantity.toIntOrNull() ?: 0
                                val cost = unitCost.toDoubleOrNull() ?: 0.0
                                purchaseItems = purchaseItems + PurchaseItemDto(
                                    productId = sel.id ?: "", quantity = qty, unitCost = cost, totalCost = qty * cost
                                )
                                showItemDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tambah Item", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Invoicing Screen ──────────────────────────────────────────────────
@Composable
fun WarehouseInvoicingScreen(padding: PaddingValues) {
    var showAddInvoice by remember { mutableStateOf(false) }
    var selectedInvoice by remember { mutableStateOf<InvoiceDto?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    if (showAddInvoice) {
        WarehouseAddInvoiceScreen(
            onBack = { showAddInvoice = false },
            onInvoiceCreated = { showAddInvoice = false; refreshTrigger++ }
        )
    } else if (selectedInvoice != null) {
        WarehouseInvoiceDetailsScreen(
            invoice = selectedInvoice!!,
            onBack = { selectedInvoice = null }
        )
    } else {
        WarehouseInvoicesListScreen(
            padding = padding,
            onAddInvoice = { showAddInvoice = true },
            onInvoiceClick = { selectedInvoice = it },
            refreshTrigger = refreshTrigger
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WarehouseInvoicesListScreen(
    padding: PaddingValues,
    onAddInvoice: () -> Unit,
    onInvoiceClick: (InvoiceDto) -> Unit,
    refreshTrigger: Int = 0
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var invoices by remember { mutableStateOf<List<InvoiceDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val result = repository.getInvoices()
        if (result is Resource.Success) invoices = result.data
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            coroutineScope.launch {
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tagihan",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tagihan outlet", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                FloatingActionButton(
                    onClick = onAddInvoice,
                    containerColor = Primary, contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Add, "Tagihan Baru")
                }
            }
        }

        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            invoices.size.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Menunggu", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            invoices.count { it.status == "pending" }.toString(),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Warning
                        )
                    }
                }
            }
        }

        if (invoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Receipt, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada tagihan", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    }
                }
            }
        }

        items(invoices) { invoice ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onInvoiceClick(invoice) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            invoice.invoiceNumber,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (invoice.status) {
                                        "paid" -> Success.copy(alpha = 0.1f)
                                        "pending" -> Warning.copy(alpha = 0.1f)
                                        "overdue" -> Error.copy(alpha = 0.1f)
                                        else -> DividerColor
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                when (invoice.status) { "paid" -> "Lunas"; "pending" -> "Menunggu"; "overdue" -> "Terlambat"; else -> invoice.status.replaceFirstChar { it.uppercase() } },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (invoice.status) {
                                    "paid" -> Success
                                    "pending" -> Warning
                                    "overdue" -> Error
                                    else -> TextSecondary
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${invoice.items.size} produk", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        invoice.totalAmount.formatRp(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }
}

// ─── Add Invoice Screen ────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseAddInvoiceScreen(onBack: () -> Unit, onInvoiceCreated: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val coroutineScope = rememberCoroutineScope()

    var invoiceNumber by remember { mutableStateOf("") }
    var selectedOutletId by remember { mutableStateOf("") }
    var selectedOutletName by remember { mutableStateOf("") }
    var outlets by remember { mutableStateOf<List<OutletDto>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var invoiceItems by remember { mutableStateOf<List<InvoiceItemDto>>(emptyList()) }
    var showItemDialog by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val nextNumResult = repository.getNextInvoiceNumber()
        if (nextNumResult is Resource.Success) invoiceNumber = nextNumResult.data
        val outletResult = repository.getOutlets()
        if (outletResult is Resource.Success) outlets = outletResult.data
        val itemResult = repository.getWarehouseItems()
        if (itemResult is Resource.Success) products = itemResult.data
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali", tint = TextPrimary)
                }
                Text("Buat Tagihan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Detail Tagihan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)

                    OutlinedTextField(
                        value = invoiceNumber, onValueChange = { invoiceNumber = it },
                        label = { Text("Nomor Tagihan") },
                        placeholder = { Text("e.g., INV-001") },
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
                        )
                    )

                    var outletExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = outletExpanded, onExpandedChange = { outletExpanded = !outletExpanded }) {
                        OutlinedTextField(
                            value = selectedOutletName, onValueChange = {},
                            label = { Text("Pilih Outlet") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outletExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            readOnly = true,
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
                            )
                        )
                        ExposedDropdownMenu(expanded = outletExpanded, onDismissRequest = { outletExpanded = false }) {
                            outlets.forEach { outlet ->
                                DropdownMenuItem(
                                    text = { Text(outlet.name) },
                                    onClick = {
                                        selectedOutletId = outlet.id ?: ""
                                        selectedOutletName = outlet.name
                                        outletExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Items header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item Tagihan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Button(
                    onClick = { showItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Item")
                }
            }
        }

        items(invoiceItems) { item ->
            val itemName = products.find { it.id == item.productId }?.name ?: item.productId
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(itemName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                        Text("Jml: ${item.quantity} x ${item.unitPrice.formatRp()}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    }
                    Text(item.totalPrice.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                }
            }
        }

        // Total & Submit
        if (invoiceItems.isNotEmpty()) {
            item {
                val total = invoiceItems.sumOf { it.totalPrice }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                        Text(total.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    isCreating = true
                    coroutineScope.launch {
                        val total = invoiceItems.sumOf { it.totalPrice }
                        val result = repository.createInvoice(
                            CreateInvoiceRequest(
                                invoiceNumber = invoiceNumber,
                                outletId = selectedOutletId,
                                items = invoiceItems,
                                totalAmount = total
                            )
                        )
                        if (result is Resource.Success) onInvoiceCreated()
                        isCreating = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = invoiceNumber.isNotEmpty() && selectedOutletId.isNotEmpty()
                        && invoiceItems.isNotEmpty() && !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = Primary, disabledContainerColor = Primary.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (isCreating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.5.dp)
                else Text("Buat Tagihan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Item Dialog
    if (showItemDialog) {
        var selectedItem by remember { mutableStateOf<ProductDto?>(null) }
        var quantity by remember { mutableStateOf("") }
        var unitPrice by remember { mutableStateOf("") }

        androidx.compose.ui.window.Dialog(onDismissRequest = { showItemDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Surface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Tambah Item Tagihan", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        var itemExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = itemExpanded, onExpandedChange = { itemExpanded = !itemExpanded }) {
                            OutlinedTextField(
                                value = selectedItem?.name ?: "",
                                onValueChange = {},
                                label = { Text("Pilih Item") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                readOnly = true, shape = RoundedCornerShape(14.dp),
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
                            ExposedDropdownMenu(expanded = itemExpanded, onDismissRequest = { itemExpanded = false }) {
                                products.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.name} (${item.category ?: ""})") },
                                        onClick = { selectedItem = item; unitPrice = if (item.costPrice > 0) item.costPrice.toLong().toString() else ""; itemExpanded = false }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = quantity, onValueChange = { quantity = it },
                            label = { Text("Jumlah") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary, unfocusedBorderColor = DividerColor,
                                cursorColor = Primary, focusedLabelColor = Primary
                            )
                        )
                        OutlinedTextField(
                            value = unitPrice, onValueChange = { unitPrice = it },
                            label = { Text("Harga Satuan (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
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
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showItemDialog = false }) {
                            Text("Batal", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val sel = selectedItem ?: return@Button
                                val qty = quantity.toIntOrNull() ?: 0
                                val price = unitPrice.toDoubleOrNull() ?: 0.0
                                invoiceItems = invoiceItems + InvoiceItemDto(
                                    productId = sel.id ?: "", quantity = qty, unitPrice = price, totalPrice = qty * price
                                )
                                showItemDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Tambah Item", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ─── Settings Screen ───────────────────────────────────────────────────
@Composable
fun WarehouseSettingsScreen(padding: PaddingValues, onLogout: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Pengaturan",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Akun dan pengaturan aplikasi", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (sessionManager.getUsername()?.firstOrNull()?.uppercase() ?: "W"),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                sessionManager.getUsername() ?: "Pengguna Gudang",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                "Peran: ${sessionManager.getRole() ?: "gudang"}",
                                style = MaterialTheme.typography.bodySmall, color = TextTertiary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Update App Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { AppUpdater.showAlert() }
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
                        onClick = {
                            sessionManager.clearSession()
                            RetrofitClient.refresh()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Error.copy(alpha = 0.1f), contentColor = Error
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluar", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Server", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("Terhubung ke: ${RetrofitClient.BASE_URL}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
        }
    }
}

// ─── Shared Components ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseInvoiceDetailsScreen(
    invoice: InvoiceDto,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    
    var outletName by remember { mutableStateOf<String?>(null) }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(invoice.id) {
        isLoading = true
        // Fetch outlets to get name
        val outletsRes = repository.getOutlets()
        if (outletsRes is Resource.Success) {
            outletName = outletsRes.data.find { it.id == invoice.outletId }?.name
        }
        
        // Fetch products to show item names
        val productsRes = repository.getWarehouseItems()
        if (productsRes is Resource.Success) {
            products = productsRes.data
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Detail Tagihan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(invoice.invoiceNumber, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Status", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (invoice.status) {
                                                    "paid" -> Success.copy(alpha = 0.1f)
                                                    "pending" -> Warning.copy(alpha = 0.1f)
                                                    "overdue" -> Error.copy(alpha = 0.1f)
                                                    else -> DividerColor
                                                }
                                            )
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            when (invoice.status) { "paid" -> "Lunas"; "pending" -> "Menunggu"; "overdue" -> "Terlambat"; else -> invoice.status.replaceFirstChar { it.uppercase() } },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = when (invoice.status) {
                                                "paid" -> Success
                                                "pending" -> Warning
                                                "overdue" -> Error
                                                else -> TextSecondary
                                            }
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Tanggal", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                    Text(invoice.invoiceDate?.take(10) ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                }
                            }
                            
                            HorizontalDivider(color = Background, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                            
                            Column {
                                Text("Outlet Tujuan", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(outletName ?: "Outlet Tidak Diketahui", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                            }
                        }
                    }
                }

                item {
                    Text("Item Pesanan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }

                items(invoice.items) { item ->
                    val productName = products.find { it.id == item.productId }?.name ?: "Produk Tidak Diketahui"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(productName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${item.quantity} unit @ ${item.unitPrice.formatRp()}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                            Text(item.totalPrice.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.05f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total Tagihan", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(invoice.totalAmount.formatRp(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun WarehouseMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Icon(icon, null, tint = Primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(title, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
        }
    }
}

class WarehouseActivityClass : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sessionManager = SessionManager.getInstance(this)
        RetrofitClient.init(sessionManager)
        setContent {
            TehAtlasTheme(darkTheme = false) {
                WarehouseDashboard(
                    onLogout = {
                        sessionManager.clearSession()
                        RetrofitClient.refresh()
                        val intent = android.content.Intent(this@WarehouseActivityClass, MainActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                AppUpdater.Component()
            }
        }
    }
}
