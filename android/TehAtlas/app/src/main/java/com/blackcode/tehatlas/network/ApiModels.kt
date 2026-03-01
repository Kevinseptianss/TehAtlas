package com.blackcode.tehatlas.network

import com.google.gson.annotations.SerializedName

// ─── Generic API Response ─────────────────────────────────────────────
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)

// ─── Auth ──────────────────────────────────────────────────────────────
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class CreateUserRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("role") val role: String,
    @SerializedName("outlet_id") val outletId: String? = null
)

data class LoginData(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserDto
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("role") val role: String,
    @SerializedName("outlet_id") val outletId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

// ─── Outlet ────────────────────────────────────────────────────────────
data class OutletDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateOutletRequest(
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("cashier_username") val cashierUsername: String,
    @SerializedName("cashier_password") val cashierPassword: String
)

data class OutletStatsDto(
    @SerializedName("outlet_id") val outletId: String,
    @SerializedName("revenue") val revenue: Double,
    @SerializedName("cogs") val cogs: Double,
    @SerializedName("gross_profit") val grossProfit: Double,
    @SerializedName("expenses") val expenses: Double,
    @SerializedName("net_profit") val netProfit: Double,
    @SerializedName("selling_margin") val sellingMargin: Double,
    @SerializedName("daily_revenue") val dailyRevenue: List<DailyRevenueDto>? = null,
    @SerializedName("total_sales") val totalSales: Int = 0,
    @SerializedName("outlet_items") val outletItems: List<ProductDto>? = null
)

data class DailyRevenueDto(
    @SerializedName("date") val date: String,
    @SerializedName("revenue") val revenue: Double
)

// ─── Products ────────────────────────────────────────────────────────
data class ProductDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("sku") val sku: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("warehouse_stock") val warehouseStock: Int = 0,
    @SerializedName("outlet_stock") val outletStock: Map<String, Int> = emptyMap(),
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class CreateProductRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String = "",
    @SerializedName("sku") val sku: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("cost_price") val costPrice: Double = 0.0,
    @SerializedName("unit_price") val unitPrice: Double = 0.0,
    @SerializedName("stock") val stock: Int? = null
)

// ─── Purchase (Supplier → Warehouse) ──────────────────────────────────
data class PurchaseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("supplier_name") val supplierName: String,
    @SerializedName("supplier_id") val supplierId: String? = null,
    @SerializedName("items") val items: List<PurchaseItemDto>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("warehouse_id") val warehouseId: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("status") val status: String = "pending"
)

data class PurchaseItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_cost") val unitCost: Double,
    @SerializedName("total_cost") val totalCost: Double
)

data class CreatePurchaseRequest(
    @SerializedName("supplier_name") val supplierName: String,
    @SerializedName("items") val items: List<PurchaseItemDto>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("status") val status: String = "pending"
)

data class UpdatePurchaseStatusRequest(
    @SerializedName("status") val status: String
)

// ─── Invoice (Warehouse → Outlet) ─────────────────────────────────────
data class InvoiceDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("outlet_id") val outletId: String,
    @SerializedName("warehouse_id") val warehouseId: String? = null,
    @SerializedName("items") val items: List<InvoiceItemDto>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("invoice_date") val invoiceDate: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("created_by") val createdBy: String? = null
)

data class InvoiceItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class CreateInvoiceRequest(
    @SerializedName("invoice_number") val invoiceNumber: String,
    @SerializedName("outlet_id") val outletId: String,
    @SerializedName("items") val items: List<InvoiceItemDto>,
    @SerializedName("total_amount") val totalAmount: Double
)

// ─── Outlet Purchase (Outlet ← Warehouse) ─────────────────────────────
data class OutletPurchaseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("invoice_id") val invoiceId: String,
    @SerializedName("outlet_id") val outletId: String,
    @SerializedName("items") val items: List<OutletPurchaseItemDto>,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("purchase_date") val purchaseDate: String? = null,
    @SerializedName("status") val status: String = "pending",
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("received_by") val receivedBy: String? = null
)

data class OutletPurchaseItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class CreateOutletPurchaseRequest(
    @SerializedName("invoice_id") val invoiceId: String? = null,
    @SerializedName("outlet_id") val outletId: String? = null,
    @SerializedName("items") val items: List<OutletPurchaseItemDto>,
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("notes") val notes: String? = null
)

// ─── Sale ──────────────────────────────────────────────────────────────
data class SaleDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("receipt_number") val receiptNumber: String? = null,
    @SerializedName("outlet_id") val outletId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("items") val items: List<SaleItemDto>,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("sale_date") val saleDate: String? = null,
    @SerializedName("created_by") val createdBy: String? = null
)

data class SaleItemDto(
    @SerializedName("product_id") val productId: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("total_price") val totalPrice: Double
)

data class CreateSaleRequest(
    @SerializedName("outlet_id") val outletId: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("customer_phone") val customerPhone: String? = null,
    @SerializedName("items") val items: List<SaleItemDto>,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("tax_amount") val taxAmount: Double = 0.0,
    @SerializedName("discount_amount") val discountAmount: Double = 0.0,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("payment_method") val paymentMethod: String
)

// ─── Inventory Transaction ─────────────────────────────────────────────
data class InventoryTransactionDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("item_id") val itemId: String,
    @SerializedName("item_type") val itemType: String,
    @SerializedName("transaction_type") val transactionType: String,
    @SerializedName("quantity") val quantity: Int,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("reference_type") val referenceType: String? = null,
    @SerializedName("warehouse_id") val warehouseId: String? = null,
    @SerializedName("outlet_id") val outletId: String? = null,
    @SerializedName("transaction_date") val transactionDate: String? = null,
    @SerializedName("created_by") val createdBy: String? = null
)

// ─── Stock ─────────────────────────────────────────────────────────────
data class StockDto(
    @SerializedName("item_id") val itemId: String? = null,
    @SerializedName("quantity") val quantity: Int = 0,
    @SerializedName("item") val item: Any? = null
)

// ─── Dashboard Responses ───────────────────────────────────────────────
data class AdminDashboardDto(
    @SerializedName("total_outlets") val totalOutlets: Int = 0,
    @SerializedName("total_products") val totalProducts: Int = 0,
    @SerializedName("total_sales") val totalSales: Double = 0.0,
    @SerializedName("total_revenue") val totalRevenue: Double = 0.0,
    @SerializedName("total_purchases") val totalPurchases: Double = 0.0,
    @SerializedName("total_invoices") val totalInvoices: Int = 0,
    @SerializedName("pending_invoices") val pendingInvoices: Int = 0,
    @SerializedName("total_cogs") val totalCogs: Double = 0.0,
    @SerializedName("gross_profit") val grossProfit: Double = 0.0,
    @SerializedName("net_profit") val netProfit: Double = 0.0,
    @SerializedName("selling_margin") val sellingMargin: Double = 0.0,
    @SerializedName("expenses") val expenses: Double = 0.0,
    @SerializedName("daily_revenue") val dailyRevenue: List<DailyRevenueDto>? = null,
    @SerializedName("recent_sales") val recentSales: List<SaleDto>? = null,
    @SerializedName("recent_purchases") val recentPurchases: List<PurchaseDto>? = null,
    @SerializedName("outlets") val outlets: List<OutletDto>? = null
)

data class WarehouseDashboardDto(
    @SerializedName("total_items") val totalItems: Int = 0,
    @SerializedName("total_stock_value") val totalStockValue: Double = 0.0,
    @SerializedName("pending_purchases") val pendingPurchases: Int = 0,
    @SerializedName("pending_invoices") val pendingInvoices: Int = 0,
    @SerializedName("recent_purchases") val recentPurchases: List<PurchaseDto>? = null,
    @SerializedName("recent_invoices") val recentInvoices: List<InvoiceDto>? = null,
    @SerializedName("low_stock_items") val lowStockItems: Int = 0,
    @SerializedName("total_sales") val totalSales: Double = 0.0,
    @SerializedName("monthly_revenue") val monthlyRevenue: Double = 0.0
)

data class OutletDashboardDto(
    @SerializedName("total_items") val totalItems: Int = 0,
    @SerializedName("total_sales") val totalSales: Double = 0.0,
    @SerializedName("today_sales") val todaySales: Double = 0.0,
    @SerializedName("pending_purchases") val pendingPurchases: Int = 0,
    @SerializedName("recent_sales") val recentSales: List<SaleDto>? = null,
    @SerializedName("recent_purchases") val recentPurchases: List<OutletPurchaseDto>? = null
)

// ─── Stock History ─────────────────────────────────────────────────────
data class StockHistoryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("location_id") val locationId: String? = null,
    @SerializedName("product_id") val productId: String,
    @SerializedName("type") val type: String,
    @SerializedName("change_qty") val changeQty: Int,
    @SerializedName("balance") val balance: Int,
    @SerializedName("cost_price") val costPrice: Double? = 0.0,
    @SerializedName("reference_id") val referenceId: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
