package com.blackcode.tehatlas

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.util.Locale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.blackcode.tehatlas.network.*
import com.blackcode.tehatlas.ui.theme.*
import com.blackcode.tehatlas.utils.formatRp
import kotlinx.coroutines.launch
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState

// Navigation items
enum class AdminScreen {
    DASHBOARD, OUTLETS, SETTINGS, WAREHOUSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AdminScreen.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Surface,
                contentColor = TextPrimary
            ) {
                val items = listOf(
                    Triple(AdminScreen.DASHBOARD, Icons.Filled.Dashboard, "Dashboard"),
                    Triple(AdminScreen.OUTLETS, Icons.Filled.Store, "Outlet"),
                    Triple(AdminScreen.WAREHOUSE, Icons.Filled.Inventory, "Gudang"),
                    Triple(AdminScreen.SETTINGS, Icons.Filled.Settings, "Pengaturan")
                )

                items.forEach { (screen, icon, label) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
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
        },
        modifier = modifier
    ) { padding ->
        when (currentScreen) {
            AdminScreen.DASHBOARD -> DashboardScreen(padding)
            AdminScreen.OUTLETS -> OutletsManagementScreen(padding)
            AdminScreen.SETTINGS -> SettingsScreen(padding, onLogout)
            AdminScreen.WAREHOUSE -> WarehouseScreen(padding)
        }
    }
}

// ─── Dashboard Screen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val sessionManager = remember { SessionManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var dashboardData by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Date filter state
    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    // Metric Detail state
    var selectedMetric by remember { mutableStateOf<MetricDetailType?>(null) }

    fun fetchData() {
        scope.launch {
            isLoading = true
            when (val result = repository.getAdminDashboard(startDate, endDate)) {
                is Resource.Success -> {
                    dashboardData = result.data
                    errorMessage = null
                }
                is Resource.Error -> errorMessage = result.message
                is Resource.Loading -> {}
            }
            isLoading = false
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

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

    LaunchedEffect(startDate, endDate) {
        fetchData()
    }

    // Date picker modal
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        startDate = sdf.format(java.util.Date(start))
                        endDate = sdf.format(java.util.Date(end))
                    }
                    showDatePicker = false
                }) { Text("Terapkan", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    startDate = null
                    endDate = null
                    showDatePicker = false
                }) { Text("Bersihkan", color = TextSecondary) }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = { Text("Pilih Rentang Tanggal", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    if (errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(errorMessage ?: "Kesalahan tidak diketahui", color = TextSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { fetchData() },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Coba Lagi") }
            }
        }
        return
    }

    val data = dashboardData ?: return

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Selamat Datang, ${sessionManager.getUsername() ?: "Admin"}",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Berikut adalah ringkasan bisnis Anda",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }

        // Date Filter Button
        item {
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (startDate != null && endDate != null) "$startDate  →  $endDate" else "Semua Waktu — Ketuk untuk filter tanggal",
                    color = if (startDate != null) Primary else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Key Metric Cards - Row 1
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Total Outlet", 
                    value = data.totalOutlets.toString(), 
                    icon = Icons.Filled.Store, 
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Total Produk", 
                    value = data.totalProducts.toString(), 
                    icon = Icons.Filled.Inventory, 
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Key Metric Cards - Row 2
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Total Pendapatan", 
                    value = data.totalRevenue.formatRp(), 
                    icon = Icons.Filled.AttachMoney, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.REVENUE }
                )
                AdminMetricCard(
                    title = "HPP (COGS)", 
                    value = data.totalCogs.formatRp(), 
                    icon = Icons.Filled.ShoppingCart, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.COGS }
                )
            }
        }

        // Key Metric Cards - Row 3
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Laba Kotor", 
                    value = data.grossProfit.formatRp(), 
                    icon = Icons.Filled.AccountBalanceWallet, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.GROSS_PROFIT }
                )
                AdminMetricCard(
                    title = "Laba Bersih", 
                    value = data.netProfit.formatRp(), 
                    icon = Icons.Filled.TrendingUp, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.NET_PROFIT }
                )
            }
        }

        // Key Metric Cards - Row 4
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Margin Penjualan", 
                    value = String.format(Locale.ROOT, "%.1f%%", data.sellingMargin), 
                    icon = Icons.Filled.Percent, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.MARGIN }
                )
                AdminMetricCard(
                    title = "Beban Pengeluaran", 
                    value = data.expenses.formatRp(), 
                    icon = Icons.Filled.MoneyOff, 
                    modifier = Modifier.weight(1f),
                    onClick = { selectedMetric = MetricDetailType.EXPENSES }
                )
            }
        }

        // Key Metric Cards - Row 5
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdminMetricCard(
                    title = "Tagihan", 
                    value = "${data.totalInvoices} (${data.pendingInvoices} pending)", 
                    icon = Icons.Filled.Receipt, 
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Revenue Chart
        val chartData = data.dailyRevenue.orEmpty().sortedBy { it.date }
        if (chartData.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Tren Pendapatan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RevenueChart(chartData)
                    }
                }
            }
        }

        // Alerts
        if (data.pendingInvoices > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Warning.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Warning)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "${data.pendingInvoices} tagihan pending memerlukan perhatian",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Outlets Overview
        val outlets = data.outlets.orEmpty()
        if (outlets.isNotEmpty()) {
            item {
                Text("Outlet", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            }
            items(outlets.take(4)) { outlet ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Primary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Store, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp)) }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(outlet.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                            Text(outlet.address ?: "Alamat tidak tersedia", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        if (outlet.phone != null) {
                            Text(outlet.phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Recent Sales
        val recentSales = data.recentSales.orEmpty()
        if (recentSales.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Penjualan Terakhir", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            }
            items(recentSales.take(5)) { sale ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Success.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = Success, modifier = Modifier.size(18.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sale.receiptNumber ?: "Penjualan", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
                            Text("${sale.customerName ?: "Pelanggan Umum"} | ${sale.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        Text(sale.totalAmount.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Success)
                    }
                }
            }
        }

        // Recent Purchases
        val recentPurchases = data.recentPurchases.orEmpty()
        if (recentPurchases.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Pembelian Terakhir", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary))
            }
            items(recentPurchases.take(5)) { purchase ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Secondary, modifier = Modifier.size(18.dp)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(purchase.supplierName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${purchase.items.size} item(s)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(when (purchase.status) { "received" -> Success.copy(alpha = 0.15f); "cancelled" -> Error.copy(alpha = 0.15f); else -> Warning.copy(alpha = 0.15f) })
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        purchase.status.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = when (purchase.status) { "received" -> Success; "cancelled" -> Error; else -> Warning }
                                    )
                                }
                            }
                        }
                        Text(purchase.totalAmount.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                    }
                }
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

    // Full-screen Metric Detail
    selectedMetric?.let { metricType ->
        MetricDetailScreen(
            type = metricType,
            data = data,
            onBack = { selectedMetric = null }
        )
    }
}

enum class MetricDetailType {
    REVENUE, COGS, GROSS_PROFIT, EXPENSES, NET_PROFIT, MARGIN
}

// ─── Outlets Management Screen ─────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OutletsManagementScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var outlets by remember { mutableStateOf<List<OutletDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Add Outlet dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newCashierUsername by remember { mutableStateOf("") }
    var newCashierPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Analytics Stats Modal
    var showStatsOutlet by remember { mutableStateOf<OutletDto?>(null) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
        when (val result = repository.getOutlets()) {
            is Resource.Success -> { outlets = result.data; errorMessage = null }
            is Resource.Error -> errorMessage = result.message
            is Resource.Loading -> {}
        }
        isLoading = false
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                when (val result = repository.getOutlets()) {
                    is Resource.Success -> { outlets = result.data; errorMessage = null }
                    is Resource.Error -> errorMessage = result.message
                    is Resource.Loading -> {}
                }
                isRefreshing = false
            }
        }
    )

    // Add Outlet Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("Add New Outlet", fontWeight = FontWeight.Bold, color = Primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Outlet Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = newCashierUsername,
                        onValueChange = { newCashierUsername = it },
                        label = { Text("Cashier Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCashierPassword,
                        onValueChange = { newCashierPassword = it },
                        label = { Text("Cashier Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newCashierUsername.isNotBlank() && newCashierPassword.isNotBlank()) {
                            isSaving = true
                            scope.launch {
                                val result = repository.createOutlet(
                                    CreateOutletRequest(
                                        name = newName, 
                                        address = newAddress, 
                                        phone = newPhone,
                                        cashierUsername = newCashierUsername,
                                        cashierPassword = newCashierPassword
                                    )
                                )
                                isSaving = false
                                if (result is Resource.Success) {
                                    showAddDialog = false
                                    newName = ""; newAddress = ""; newPhone = ""
                                    newCashierUsername = ""; newCashierPassword = ""
                                    refreshTrigger++
                                }
                            }
                        }
                    },
                    enabled = newName.isNotBlank() && newCashierUsername.isNotBlank() && newCashierPassword.isNotBlank() && !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Surface, strokeWidth = 2.dp)
                    else Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSaving) { Text("Batal", color = TextSecondary) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(padding)
            .pullRefresh(pullRefreshState)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage ?: "Kesalahan tidak diketahui", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { refreshTrigger++ }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Coba Lagi")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Kelola Outlet",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${outlets.size} outlet terdaftar",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }

                if (outlets.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.StoreMallDirectory, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No outlets yet", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                                Text("Tap + to add your first outlet", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                            }
                        }
                    }
                }

                items(outlets) { outlet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStatsOutlet = outlet },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Store, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            outlet.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        if (outlet.address != null) {
                                            Text(
                                                outlet.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextTertiary
                                            )
                                        }
                                    }
                                }
                            }

                            if (outlet.phone != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Phone, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(outlet.phone, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary,
            contentColor = Surface
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Outlet")
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            contentColor = Primary
        )
    }

    // Full-screen Analytics
    showStatsOutlet?.let { outlet ->
        OutletAnalyticsScreen(
            outlet = outlet,
            repository = repository,
            onBack = { showStatsOutlet = null }
        )
    }
}

// ─── Warehouse Screen ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(padding: PaddingValues) {
    val context = LocalContext.current
    val repository = remember { TehAtlasRepository(SessionManager.getInstance(context)) }
    val scope = rememberCoroutineScope()

    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var warehouseUsers by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var selectedStockItem by remember { mutableStateOf<ProductDto?>(null) }
    var stockHistory by remember { mutableStateOf<List<StockHistoryDto>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isSavingUser by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val loadData = suspend {
        val itemsResult = repository.getWarehouseItems()
        if (itemsResult is Resource.Success) products = itemsResult.data
        val usersResult = repository.getUsers()
        if (usersResult is Resource.Success) {
            warehouseUsers = usersResult.data.filter { it.role == "warehouse" }
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

    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingUser) showAddUserDialog = false },
            title = { Text("Add Warehouse User", fontWeight = FontWeight.Bold, color = Primary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Create credentials for warehouse staff.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                            isSavingUser = true
                            scope.launch {
                                val result = repository.createUser(
                                    CreateUserRequest(username = newUsername, password = newPassword, role = "warehouse")
                                )
                                isSavingUser = false
                                if (result is Resource.Success) {
                                    showAddUserDialog = false
                                    newUsername = ""; newPassword = ""
                                    refreshTrigger++
                                }
                            }
                        }
                    },
                    enabled = newUsername.isNotBlank() && newPassword.isNotBlank() && !isSavingUser,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSavingUser) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Surface, strokeWidth = 2.dp)
                    else Text("Buat", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }, enabled = !isSavingUser) { Text("Batal", color = TextSecondary) }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Surface
        )
    }


    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    // Full-screen Stock History
    selectedStockItem?.let { item ->
        LaunchedEffect(item.id) {
            isLoadingHistory = true
            val result = repository.getAdminStockHistory(item.id ?: "")
            if (result is Resource.Success) stockHistory = result.data
            isLoadingHistory = false
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Riwayat Stok (Admin)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(item.name, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedStockItem = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
                )
            },
            containerColor = Background
        ) { hPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(hPadding)) {
                if (isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (stockHistory.isEmpty()) {
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
                                    Text("Informasi Produk Saat Ini", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("SKU", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                            Text(item.sku ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Harga Beli Saat Ini", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                            Text(item.costPrice.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Log Pergerakan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                        }

                        items(stockHistory) { log ->
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
                                                when(log.type) { 
                                                    "receive" -> "Terima Pembelian"
                                                    "sale" -> "Penjualan" 
                                                    "purchase" -> "Terima Pembelian"
                                                    else -> when(log.type) {
                                                        "adjustment" -> "Penyesuaian"
                                                        else -> log.type.replaceFirstChar { it.uppercase() }
                                                    }
                                                },
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), 
                                                color = TextPrimary
                                            )
                                            Text(log.createdAt ?: "", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${if (log.changeQty > 0) "+" else ""}${log.changeQty}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (log.changeQty > 0) Success else if (log.changeQty < 0) Error else TextPrimary
                                            )
                                            Text("Sisa: ${log.balance}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }
                                    
                                    val displayCost = if (log.costPrice != null && log.costPrice > 0) log.costPrice else item.costPrice
                                    
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
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Manajemen Gudang",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pantau level inventaris dan kelola stok",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }

        // Warehouse Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total Item", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                        Text(
                            products.size.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Primary
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Kategori", style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
                        Text(
                            products.mapNotNull { it.category }.distinct().size.toString(),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Secondary
                        )
                    }
                }
            }
        }

        if (warehouseUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showAddUserDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).background(Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = Surface, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tidak Ada Pengguna Gudang", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                            Text("Ketuk di sini untuk membuat kredensial login staf gudang.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Inventory, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Belum ada item gudang", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                    }
                }
            }
        }

        items(products) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { selectedStockItem = item },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Inventory,
                            contentDescription = "Item",
                            tint = Primary,
                            modifier = Modifier.size(22.dp)
                        )
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
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
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

        item {
            Spacer(modifier = Modifier.height(16.dp))
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
fun SettingsScreen(padding: PaddingValues, onLogout: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Pengaturan",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Kelola pengaturan dan preferensi aplikasi",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }

        // User Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Akun", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Primary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                sessionManager.getUsername() ?: "Admin",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                "Peran: ${sessionManager.getRole() ?: "admin"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }

        // Server Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Server", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            RetrofitClient.BASE_URL,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Logout
        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Reusable Metric Card ──────────────────────────────────────────────
@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = Primary, modifier = Modifier.size(18.dp))
                }
                
                if (onClick != null) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = TextTertiary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }
    }
}

// ─── Revenue Chart Composable ──────────────────────────────────────────
@Composable
fun RevenueChart(data: List<DailyRevenueDto>) {
    if (data.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada data pendapatan untuk grafik", color = TextTertiary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val maxRevenue = data.maxOfOrNull { it.revenue } ?: 1.0

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(vertical = 8.dp)
    ) {
        val chartWidth = size.width
        val chartHeight = size.height - 40f
        val barCount = data.size
        val barWidth = (chartWidth / barCount) * 0.6f
        val gapWidth = (chartWidth / barCount) * 0.4f

        // Draw bars
        data.forEachIndexed { index, item ->
            val barHeight = (item.revenue / maxRevenue * chartHeight).toFloat()
            val x = index * (barWidth + gapWidth) + gapWidth / 2

            drawRoundRect(
                color = Primary,
                topLeft = androidx.compose.ui.geometry.Offset(x, chartHeight - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )
        }

        // Draw bottom line
        drawLine(
            color = TextTertiary.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(0f, chartHeight),
            end = androidx.compose.ui.geometry.Offset(chartWidth, chartHeight),
            strokeWidth = 1f
        )
    }

    // Date labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (data.size <= 7) {
            data.forEach { item ->
                Text(
                    item.date.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        } else {
            Text(data.first().date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(data.last().date.takeLast(5), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
    }
}

// ─── Full-Screen Outlet Analytics ──────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletAnalyticsScreen(
    outlet: OutletDto,
    repository: TehAtlasRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<OutletStatsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedStockItem by remember { mutableStateOf<ProductDto?>(null) }
    var stockHistory by remember { mutableStateOf<List<StockHistoryDto>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var selectedMetric by remember { mutableStateOf<MetricDetailType?>(null) }

    fun fetchStats() {
        scope.launch {
            val id = outlet.id ?: return@launch
            isLoading = true
            val result = repository.getOutletStats(id, startDate, endDate)
            if (result is Resource.Success) {
                stats = result.data
                errorMessage = null
            } else if (result is Resource.Error) {
                errorMessage = result.message
            }
            isLoading = false
        }
    }

    LaunchedEffect(startDate, endDate) {
        fetchStats()
    }

    // Full-screen Stock History
    selectedStockItem?.let { item ->
        LaunchedEffect(item.id) {
            isLoadingHistory = true
            val result = repository.getAdminStockHistory(item.id ?: "")
            if (result is Resource.Success) stockHistory = result.data
            isLoadingHistory = false
        }
        
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Riwayat Stok (Admin)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text(item.name, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedStockItem = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
                )
            },
            containerColor = Background
        ) { hPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(hPadding)) {
                if (isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else if (stockHistory.isEmpty()) {
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
                                    Text("Informasi Produk Saat Ini", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("SKU", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                            Text(item.sku ?: "-", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Harga Beli Saat Ini", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                            Text(item.costPrice.formatRp(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Text("Log Pergerakan", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                        }

                        items(stockHistory) { log ->
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
                                                when(log.type) { 
                                                    "receive" -> "Terima Pembelian"
                                                    "sale" -> "Penjualan" 
                                                    "purchase" -> "Terima Pembelian"
                                                    else -> when(log.type) {
                                                        "adjustment" -> "Penyesuaian"
                                                        else -> log.type.replaceFirstChar { it.uppercase() }
                                                    }
                                                },
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), 
                                                color = TextPrimary
                                            )
                                            Text(log.createdAt ?: "", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "${if (log.changeQty > 0) "+" else ""}${log.changeQty}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (log.changeQty > 0) Success else if (log.changeQty < 0) Error else TextPrimary
                                            )
                                            Text("Sisa: ${log.balance}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }
                                    
                                    val displayCost = if (log.costPrice != null && log.costPrice > 0) log.costPrice else item.costPrice
                                    
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
            }
        }
        return@OutletAnalyticsScreen
    }

    // Date picker modal
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                        startDate = sdf.format(java.util.Date(start))
                        endDate = sdf.format(java.util.Date(end))
                    }
                    showDatePicker = false
                }) { Text("Apply", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    startDate = null
                    endDate = null
                    showDatePicker = false
                }) { Text("Clear", color = TextSecondary) }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.height(500.dp),
                title = { Text("Pilih Rentang Tanggal", modifier = Modifier.padding(16.dp)) }
            )
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Outlet") },
            text = { Text("Apakah Anda yakin ingin menghapus \"${outlet.name}\"? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isDeleting = true
                            val id = outlet.id ?: return@launch
                            val result = repository.deleteOutlet(id)
                            if (result is Resource.Success) {
                                onBack()
                            }
                            isDeleting = false
                            showDeleteConfirm = false
                        }
                    },
                    enabled = !isDeleting
                ) { Text("Hapus", color = Error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analitik", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(outlet.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Hapus Outlet", tint = Error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return@Scaffold
        }

        if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMessage ?: "Unknown error", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { fetchStats() }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Coba Lagi")
                    }
                }
            }
            return@Scaffold
        }

        val validStats = stats ?: return@Scaffold

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date filter
            item {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (startDate != null && endDate != null) "$startDate  →  $endDate" else "Semua Waktu — Ketuk untuk filter",
                        color = if (startDate != null) Primary else TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.06f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Pendapatan", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(validStats.revenue.formatRp(), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${validStats.totalSales} penjualan", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(
                                String.format(Locale.ROOT, "%.1f%% margin", validStats.sellingMargin),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (validStats.sellingMargin >= 0) Success else Error
                            )
                        }
                    }
                }
            }

            // Metric cards
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard(title = "Laba Kotor", value = validStats.grossProfit.formatRp(), icon = Icons.Filled.AccountBalanceWallet, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.GROSS_PROFIT })
                    AdminMetricCard(title = "Laba Bersih", value = validStats.netProfit.formatRp(), icon = Icons.Filled.TrendingUp, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.NET_PROFIT })
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard(title = "HPP (COGS)", value = validStats.cogs.formatRp(), icon = Icons.Filled.ShoppingCart, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.COGS })
                    AdminMetricCard(title = "Pengeluaran", value = validStats.expenses.formatRp(), icon = Icons.Filled.MoneyOff, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.EXPENSES })
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdminMetricCard(title = "Pendapatan", value = validStats.revenue.formatRp(), icon = Icons.Filled.AttachMoney, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.REVENUE })
                    AdminMetricCard(title = "Margin Penjualan", value = String.format(Locale.ROOT, "%.2f%%", validStats.sellingMargin), icon = Icons.Filled.Percent, modifier = Modifier.weight(1f), onClick = { selectedMetric = MetricDetailType.MARGIN })
                }
            }

            // Revenue Chart
            val chartData = validStats.dailyRevenue.orEmpty().sortedBy { it.date }
            if (chartData.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Pendapatan Harian", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            RevenueChart(chartData)
                        }
                    }
                }
            }
            
            // Stock list
            val itemsData = validStats.outletItems.orEmpty()
            if (itemsData.isNotEmpty()) {
                item {
                    Text("Stok Inventaris", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                items(itemsData) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedStockItem = item }, 
                        shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Primary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                                Text("${item.outletStock[validStats.outletId] ?: 0}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                                Text("${item.category ?: "-"} | SKU: ${item.sku ?: "-"}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                            }
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    item.costPrice.formatRp(),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Primary
                                )
                                Text("Harga Beli", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        selectedMetric?.let { metricType ->
            MetricDetailScreen(
                type = metricType,
                data = AdminDashboardDto(
                    totalRevenue = validStats.revenue,
                    totalCogs = validStats.cogs,
                    grossProfit = validStats.grossProfit,
                    expenses = validStats.expenses,
                    netProfit = validStats.netProfit,
                    sellingMargin = validStats.sellingMargin,
                    recentSales = validStats.salesList,
                    expensesList = validStats.expensesList,
                    totalSales = validStats.revenue // workaround
                ),
                onBack = { selectedMetric = null }
            )
        }
    }
}
// ─── Metric Detail Screen (Full Screen) ────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailScreen(
    type: MetricDetailType,
    data: AdminDashboardDto,
    onBack: () -> Unit
) {
    val title = when (type) {
        MetricDetailType.REVENUE -> "Detail Pendapatan"
        MetricDetailType.COGS -> "Detail HPP (COGS)"
        MetricDetailType.GROSS_PROFIT -> "Detail Laba Kotor"
        MetricDetailType.EXPENSES -> "Detail Pengeluaran"
        MetricDetailType.NET_PROFIT -> "Detail Laba Bersih"
        MetricDetailType.MARGIN -> "Detail Margin Penjualan"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        var selectedSaleByDialog by remember { mutableStateOf<SaleDto?>(null) }
        var selectedExpenseByDialog by remember { mutableStateOf<ExpenseDto?>(null) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (type) {
                                MetricDetailType.REVENUE -> "Total Pendapatan"
                                MetricDetailType.COGS -> "Total HPP"
                                MetricDetailType.GROSS_PROFIT -> "Total Laba Kotor"
                                MetricDetailType.EXPENSES -> "Total Pengeluaran"
                                MetricDetailType.NET_PROFIT -> "Total Laba Bersih"
                                MetricDetailType.MARGIN -> "Margin Penjualan"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            when (type) {
                                MetricDetailType.REVENUE -> data.totalRevenue.formatRp()
                                MetricDetailType.COGS -> data.totalCogs.formatRp()
                                MetricDetailType.GROSS_PROFIT -> data.grossProfit.formatRp()
                                MetricDetailType.EXPENSES -> data.expenses.formatRp()
                                MetricDetailType.NET_PROFIT -> data.netProfit.formatRp()
                                MetricDetailType.MARGIN -> String.format(Locale.ROOT, "%.1f%%", data.sellingMargin)
                            },
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            // Breakdown Sections
            item {
                Text(
                    when(type) {
                        MetricDetailType.REVENUE -> "Daftar Transaksi Penjualan"
                        MetricDetailType.COGS -> "Daftar Modal per Item Terjual"
                        MetricDetailType.EXPENSES -> "Daftar Biaya Operasional"
                        else -> "Rincian Perhitungan"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            when (type) {
                MetricDetailType.REVENUE -> {
                    val sales = data.recentSales.orEmpty()
                    if (sales.isEmpty()) {
                        item { EmptyDetailState("Tidak ada transaksi penjualan") }
                    } else {
                        items(sales) { sale ->
                            SaleDetailRow(sale, onClick = { selectedSaleByDialog = sale })
                        }
                    }
                }
                MetricDetailType.EXPENSES -> {
                    val expenses = data.expensesList.orEmpty()
                    if (expenses.isEmpty()) {
                        item { EmptyDetailState("Tidak ada catatan pengeluaran") }
                    } else {
                        items(expenses) { expense ->
                            ExpenseDetailRow(expense, onClick = { selectedExpenseByDialog = expense })
                        }
                    }
                }
                MetricDetailType.COGS -> {
                    val sales = data.recentSales.orEmpty()
                    if (sales.isEmpty()) {
                        item { EmptyDetailState("Tidak ada data HPP") }
                    } else {
                        items(sales) { sale ->
                            SaleCogsRow(sale, onClick = { selectedSaleByDialog = sale })
                        }
                    }
                }
                MetricDetailType.GROSS_PROFIT -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                BreakdownRow("Total Pendapatan", data.totalRevenue.formatRp())
                                BreakdownRow("Total HPP", "- ${data.totalCogs.formatRp()}", color = Error)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Background, thickness = 1.dp)
                                BreakdownRow("Laba Kotor", data.grossProfit.formatRp(), isBold = true)
                            }
                        }
                    }
                }
                MetricDetailType.NET_PROFIT -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                BreakdownRow("Laba Kotor", data.grossProfit.formatRp())
                                BreakdownRow("Beban Pengeluaran", "- ${data.expenses.formatRp()}", color = Error)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Background, thickness = 1.dp)
                                BreakdownRow("Laba Bersih", data.netProfit.formatRp(), isBold = true)
                            }
                        }
                    }
                }
                MetricDetailType.MARGIN -> {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Surface)) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                BreakdownRow("Laba Kotor", data.grossProfit.formatRp())
                                BreakdownRow("Total Pendapatan", data.totalRevenue.formatRp())
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Background, thickness = 1.dp)
                                BreakdownRow("Persentase Margin", String.format(Locale.ROOT, "%.2f%%", data.sellingMargin), isBold = true)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        selectedSaleByDialog?.let { sale ->
            TransactionDetailDialog(sale = sale, onDismiss = { selectedSaleByDialog = null })
        }
        selectedExpenseByDialog?.let { expense ->
            ExpenseDetailDialog(expense = expense, onDismiss = { selectedExpenseByDialog = null })
        }
    }
}

@Composable
fun EmptyDetailState(msg: String) {
    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodyMedium, color = TextTertiary)
    }
}

@Composable
fun SaleDetailRow(sale: SaleDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(sale.receiptNumber ?: "No Receipt", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Primary)
                    Text(sale.saleDate ?: "", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Text(sale.totalAmount.formatRp(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Success)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${sale.customerName ?: "Pelanggan Umum"} | ${sale.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
fun SaleCogsRow(sale: SaleDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(sale.receiptNumber ?: "No Receipt", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Primary)
            Text(sale.saleDate ?: "", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Background, thickness = 1.dp)
            sale.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName ?: "Item Terhapus", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text("${item.quantity} x ${item.costPrice?.formatRp() ?: "-"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    val totalItemCogs = (item.costPrice ?: 0.0) * item.quantity
                    Text(totalItemCogs.formatRp(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun ExpenseDetailRow(expense: ExpenseDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Error.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MoneyOff, null, tint = Error, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                Text("${expense.category} | ${expense.expenseDate ?: ""}", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Text(expense.amount.formatRp(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Error)
        }
    }
}

@Composable
fun TransactionDetailDialog(sale: SaleDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Primary) } },
        title = {
            Column {
                Text("Detail Transaksi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(sale.receiptNumber ?: "-", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailItemRow("Tanggal", sale.saleDate ?: "-")
                DetailItemRow("Pelanggan", sale.customerName ?: "Pelanggan Umum")
                DetailItemRow("Telepon", sale.customerPhone ?: "-")
                DetailItemRow("Metode Bayar", sale.paymentMethod)

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Background)

                Text("Daftar Item", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))

                sale.items.forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName ?: "Item Terhapus", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text("${item.quantity} x ${item.unitPrice.formatRp()}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Text(item.totalPrice.formatRp(), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Background)

                val showSubtotal = sale.taxAmount > 0 || sale.discountAmount > 0
                if (showSubtotal) {
                    DetailItemRow("Subtotal", sale.subtotal.formatRp())
                }
                if (sale.taxAmount > 0) {
                    DetailItemRow("Pajak", sale.taxAmount.formatRp())
                }
                if (sale.discountAmount > 0) {
                    DetailItemRow("Diskon", "- ${sale.discountAmount.formatRp()}", color = Error)
                }
                DetailItemRow("Total Akhir", sale.totalAmount.formatRp(), isBold = true)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface
    )
}

@Composable
fun ExpenseDetailDialog(expense: ExpenseDto, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup", color = Primary) } },
        title = { Text("Detail Pengeluaran", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DetailItemRow("Deskripsi", expense.description)
                DetailItemRow("Kategori", expense.category)
                DetailItemRow("Jumlah", expense.amount.formatRp(), isBold = true, valueColor = Error)
                DetailItemRow("Tanggal", expense.expenseDate ?: "-")
                DetailItemRow("Dibuat Oleh", expense.createdBy ?: "-")
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface
    )
}

@Composable
fun DetailItemRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = TextSecondary,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        Text(
            value,
            style = if (isBold) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodySmall,
            color = if (isBold && valueColor == TextPrimary) Primary else valueColor
        )
    }
}

@Composable
fun BreakdownRow(
    label: String,
    value: String,
    color: Color = TextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            value,
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (isBold && color == TextPrimary) Primary else color
        )
    }
}
