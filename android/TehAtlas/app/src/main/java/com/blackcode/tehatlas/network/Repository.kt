package com.blackcode.tehatlas.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Sealed class representing API call results.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val code: Int? = null) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

/**
 * Central repository for all API calls.
 */
class TehAtlasRepository(private val sessionManager: SessionManager) {

    private val api: ApiService get() = RetrofitClient.getApiService()

    // ─── Helper ────────────────────────────────────────────────────────
    private suspend fun <T> safeApiCall(call: suspend () -> Response<ApiResponse<T>>): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                val response = call()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        Resource.Success(body.data)
                    } else {
                        Resource.Error(body?.message ?: "Unknown error", response.code())
                    }
                } else {
                    val errorMsg = try {
                        response.errorBody()?.string() ?: "Error ${response.code()}"
                    } catch (e: Exception) {
                        "Error ${response.code()}"
                    }
                    Resource.Error(errorMsg, response.code())
                }
            } catch (e: Exception) {
                Resource.Error(e.localizedMessage ?: "Network error. Check your connection.")
            }
        }
    }

    // ─── Auth ──────────────────────────────────────────────────────────
    suspend fun login(username: String, password: String): Resource<LoginData> {
        val result = safeApiCall { api.login(LoginRequest(username, password)) }
        if (result is Resource.Success) {
            sessionManager.saveSession(result.data.token, result.data.user)
            RetrofitClient.refresh()
        }
        return result
    }

    suspend fun logout(): Resource<Any> {
        val result = safeApiCall { api.logout() }
        sessionManager.clearSession()
        RetrofitClient.refresh()
        return result
    }

    suspend fun getMe(): Resource<UserDto> = safeApiCall { api.getMe() }

    // ─── Admin Users ───────────────────────────────────────────────────
    suspend fun getUsers(): Resource<List<UserDto>> = safeApiCall { api.getUsers() }
    suspend fun createUser(request: CreateUserRequest): Resource<UserDto> = safeApiCall { api.createUser(request) }

    // ─── Admin Dashboard ───────────────────────────────────────────────
    suspend fun getAdminDashboard(startDate: String? = null, endDate: String? = null): Resource<AdminDashboardDto> = safeApiCall { api.getAdminDashboard(startDate, endDate) }

    // ─── Admin Outlets ─────────────────────────────────────────────────
    suspend fun getOutlets(): Resource<List<OutletDto>> = safeApiCall { api.getOutlets() }
    suspend fun createOutlet(request: CreateOutletRequest): Resource<OutletDto> = safeApiCall { api.createOutlet(request) }
    suspend fun updateOutlet(id: String, fields: Map<String, String>): Resource<OutletDto> = safeApiCall { api.updateOutlet(id, fields) }
    suspend fun deleteOutlet(id: String): Resource<Any> = safeApiCall { api.deleteOutlet(id) }
    suspend fun getOutletStats(id: String, startDate: String? = null, endDate: String? = null): Resource<OutletStatsDto> = safeApiCall { api.getOutletStats(id, startDate, endDate) }

    // ─── Warehouse Dashboard ───────────────────────────────────────────
    suspend fun getWarehouseDashboard(): Resource<WarehouseDashboardDto> = safeApiCall { api.getWarehouseDashboard() }

    // ─── Warehouse Items ───────────────────────────────────────────────
    suspend fun getWarehouseItems(): Resource<List<ProductDto>> = safeApiCall { api.getWarehouseItems() }
    suspend fun createWarehouseItem(request: CreateProductRequest): Resource<ProductDto> = safeApiCall { api.createWarehouseItem(request) }
    suspend fun updateWarehouseItem(id: String, fields: Map<String, Any>): Resource<ProductDto> = safeApiCall { api.updateWarehouseItem(id, fields) }
    suspend fun deleteWarehouseItem(id: String): Resource<Any> = safeApiCall { api.deleteWarehouseItem(id) }

    // ─── Warehouse Purchases ───────────────────────────────────────────
    suspend fun getPurchases(): Resource<List<PurchaseDto>> = safeApiCall { api.getPurchases() }
    suspend fun createPurchase(request: CreatePurchaseRequest): Resource<PurchaseDto> = safeApiCall { api.createPurchase(request) }
    suspend fun updatePurchaseStatus(id: String, status: String): Resource<PurchaseDto> = safeApiCall { api.updatePurchaseStatus(id, UpdatePurchaseStatusRequest(status)) }

    // ─── Warehouse Invoices ────────────────────────────────────────────
    suspend fun getInvoices(): Resource<List<InvoiceDto>> = safeApiCall { api.getInvoices() }
    suspend fun createInvoice(request: CreateInvoiceRequest): Resource<InvoiceDto> = safeApiCall { api.createInvoice(request) }
    suspend fun getInvoiceDetails(id: String): Resource<InvoiceDto> = safeApiCall { api.getInvoiceDetails(id) }

    // ─── Outlet Dashboard ──────────────────────────────────────────────
    suspend fun getOutletDashboard(outletId: String? = null): Resource<OutletDashboardDto> = safeApiCall { api.getOutletDashboard(outletId) }

    // ─── Outlet Items ──────────────────────────────────────────────────
    suspend fun getOutletItems(): Resource<List<ProductDto>> = safeApiCall { api.getOutletItems() }
    suspend fun createOutletItem(request: CreateProductRequest): Resource<ProductDto> = safeApiCall { api.createOutletItem(request) }
    suspend fun deleteOutletItem(id: String): Resource<Any> = safeApiCall { api.deleteOutletItem(id) }
    suspend fun updateProduct(productId: String, product: CreateProductRequest): Resource<ProductDto> = safeApiCall { api.updateOutletItem(productId, product) }

    // ─── Outlet Purchases ──────────────────────────────────────────────
    suspend fun createOutletPurchase(request: CreateOutletPurchaseRequest): Resource<OutletPurchaseDto> = safeApiCall { api.createOutletPurchase(request) }
    suspend fun getOutletPurchases(): Resource<List<OutletPurchaseDto>> = safeApiCall { api.getOutletPurchases() }
    suspend fun receivePurchase(id: String): Resource<OutletPurchaseDto> = safeApiCall { api.receivePurchase(id) }

    // ─── Outlet Sales ──────────────────────────────────────────────────
    suspend fun createSale(request: CreateSaleRequest): Resource<SaleDto> = safeApiCall { api.createSale(request) }
    suspend fun getSales(): Resource<List<SaleDto>> = safeApiCall { api.getSales() }
    suspend fun getSaleReceipt(id: String): Resource<SaleDto> = safeApiCall { api.getSaleReceipt(id) }

    // ─── Stock ─────────────────────────────────────────────────────────
    suspend fun getWarehouseStock(itemId: String): Resource<StockDto> = safeApiCall { api.getWarehouseStock(itemId) }
    suspend fun getOutletStock(itemId: String): Resource<StockDto> = safeApiCall { api.getOutletStock(itemId) }
    suspend fun getInventoryTransactions(): Resource<List<InventoryTransactionDto>> = safeApiCall { api.getInventoryTransactions() }
    suspend fun getNextInvoiceNumber(): Resource<String> = safeApiCall { api.getNextInvoiceNumber() }
    suspend fun getWarehouseStockHistory(id: String): Resource<List<StockHistoryDto>> = safeApiCall { api.getWarehouseStockHistory(id) }
    suspend fun getOutletStockHistory(itemId: String): Resource<List<StockHistoryDto>> = safeApiCall { api.getOutletStockHistory(itemId) }
    suspend fun getAdminStockHistory(itemId: String): Resource<List<StockHistoryDto>> = safeApiCall { api.getAdminStockHistory(itemId) }
}
