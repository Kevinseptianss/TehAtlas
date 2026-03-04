package com.blackcode.tehatlas.network

import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ──────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginData>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Any>>

    @GET("auth/me")
    suspend fun getMe(): Response<ApiResponse<UserDto>>

    // ─── Admin Dashboard ───────────────────────────────────────────────
    @GET("admin/dashboard")
    suspend fun getAdminDashboard(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ApiResponse<AdminDashboardDto>>

    // ─── Admin Outlets ─────────────────────────────────────────────────
    @GET("outlets")
    suspend fun getOutlets(): Response<ApiResponse<List<OutletDto>>>

    @POST("admin/outlets")
    suspend fun createOutlet(@Body request: CreateOutletRequest): Response<ApiResponse<OutletDto>>

    @PUT("admin/outlets/{id}")
    suspend fun updateOutlet(@Path("id") id: String, @Body request: Map<String, String>): Response<ApiResponse<OutletDto>>

    @DELETE("admin/outlets/{id}")
    suspend fun deleteOutlet(@Path("id") id: String): Response<ApiResponse<Any>>

    // ─── Admin Stats ───────────────────────────────────────────────────
    @GET("admin/warehouse/stats")
    suspend fun getWarehouseStats(): Response<ApiResponse<Any>>

    @GET("admin/outlets/{id}/stats")
    suspend fun getOutletStats(
        @Path("id") id: String,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<ApiResponse<OutletStatsDto>>

    @GET("admin/reports/sales")
    suspend fun getSalesReports(): Response<ApiResponse<Any>>

    @GET("admin/reports/profits")
    suspend fun getProfitReports(): Response<ApiResponse<Any>>

    // ─── Admin Users ───────────────────────────────────────────────────
    @GET("admin/users")
    suspend fun getUsers(): Response<ApiResponse<List<UserDto>>>

    @POST("admin/users")
    suspend fun createUser(@Body request: CreateUserRequest): Response<ApiResponse<UserDto>>


    // ─── Warehouse Dashboard ───────────────────────────────────────────
    @GET("warehouse/dashboard")
    suspend fun getWarehouseDashboard(): Response<ApiResponse<WarehouseDashboardDto>>

    // ─── Warehouse Items ───────────────────────────────────────────────
    @GET("warehouse/items")
    suspend fun getWarehouseItems(): Response<ApiResponse<List<ProductDto>>>

    @POST("warehouse/items")
    suspend fun createWarehouseItem(@Body request: CreateProductRequest): Response<ApiResponse<ProductDto>>

    @PUT("warehouse/items/{id}")
    suspend fun updateWarehouseItem(@Path("id") id: String, @Body request: Map<String, Any>): Response<ApiResponse<ProductDto>>

    @DELETE("warehouse/items/{id}")
    suspend fun deleteWarehouseItem(@Path("id") id: String): Response<ApiResponse<Any>>

    // ─── Warehouse Purchases ───────────────────────────────────────────
    @GET("warehouse/purchases")
    suspend fun getPurchases(): Response<ApiResponse<List<PurchaseDto>>>

    @POST("warehouse/purchases")
    suspend fun createPurchase(@Body request: CreatePurchaseRequest): Response<ApiResponse<PurchaseDto>>

    @PUT("warehouse/purchases/{id}/status")
    suspend fun updatePurchaseStatus(@Path("id") id: String, @Body request: UpdatePurchaseStatusRequest): Response<ApiResponse<PurchaseDto>>

    // ─── Warehouse Invoices ────────────────────────────────────────────
    @GET("warehouse/invoices")
    suspend fun getInvoices(): Response<ApiResponse<List<InvoiceDto>>>

    @POST("warehouse/invoices")
    suspend fun createInvoice(@Body request: CreateInvoiceRequest): Response<ApiResponse<InvoiceDto>>

    @GET("warehouse/invoices/{id}")
    suspend fun getInvoiceDetails(@Path("id") id: String): Response<ApiResponse<InvoiceDto>>

    // ─── Outlet Dashboard ──────────────────────────────────────────────
    @GET("outlet/dashboard")
    suspend fun getOutletDashboard(@Query("outlet_id") outletId: String? = null): Response<ApiResponse<OutletDashboardDto>>

    // ─── Outlet Items ──────────────────────────────────────────────────
    @GET("outlet/items")
    suspend fun getOutletItems(): Response<ApiResponse<List<ProductDto>>>

    @POST("outlet/items")
    suspend fun createOutletItem(@Body request: CreateProductRequest): Response<ApiResponse<ProductDto>>

    @DELETE("outlet/items/{id}")
    suspend fun deleteOutletItem(@Path("id") id: String): Response<ApiResponse<Any>>

    @PUT("outlet/items/{id}")
    suspend fun updateOutletItem(@Path("id") id: String, @Body request: CreateProductRequest): Response<ApiResponse<ProductDto>>

    // ─── Outlet Purchases ──────────────────────────────────────────────
    @POST("outlet/purchases")
    suspend fun createOutletPurchase(@Body request: CreateOutletPurchaseRequest): Response<ApiResponse<OutletPurchaseDto>>

    @GET("outlet/purchases")
    suspend fun getOutletPurchases(): Response<ApiResponse<List<OutletPurchaseDto>>>

    @PUT("outlet/purchases/{id}/receive")
    suspend fun receivePurchase(@Path("id") id: String): Response<ApiResponse<OutletPurchaseDto>>

    // ─── Outlet Sales ──────────────────────────────────────────────────
    @POST("outlet/sales")
    suspend fun createSale(@Body request: CreateSaleRequest): Response<ApiResponse<SaleDto>>

    @GET("outlet/sales")
    suspend fun getSales(): Response<ApiResponse<List<SaleDto>>>

    @GET("outlet/sales/{id}")
    suspend fun getSaleReceipt(@Path("id") id: String): Response<ApiResponse<SaleDto>>

    // ─── Items Search & Stock ──────────────────────────────────────────
    @GET("items/search")
    suspend fun searchItems(@Query("q") query: String): Response<ApiResponse<List<Any>>>

    @GET("stock/warehouse/{item_id}")
    suspend fun getWarehouseStock(@Path("item_id") itemId: String): Response<ApiResponse<StockDto>>

    @GET("stock/outlet/{item_id}")
    suspend fun getOutletStock(@Path("item_id") itemId: String): Response<ApiResponse<StockDto>>

    @GET("stock/transactions")
    suspend fun getInventoryTransactions(): Response<ApiResponse<List<InventoryTransactionDto>>>

    @GET("warehouse/next-invoice-number")
    suspend fun getNextInvoiceNumber(): Response<ApiResponse<String>>

    @GET("warehouse/stock-history/{id}")
    suspend fun getWarehouseStockHistory(@Path("id") itemId: String): Response<ApiResponse<List<StockHistoryDto>>>

    @GET("outlet/stock-history/{id}")
    suspend fun getOutletStockHistory(@Path("id") itemId: String): Response<ApiResponse<List<StockHistoryDto>>>

    @GET("admin/stock-history/{id}")
    suspend fun getAdminStockHistory(@Path("id") itemId: String): Response<ApiResponse<List<StockHistoryDto>>>

    // ─── Outlet Expenses ───────────────────────────────────────────────
    @POST("outlet/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): Response<ApiResponse<ExpenseDto>>

    @GET("outlet/expenses")
    suspend fun getExpenses(): Response<ApiResponse<List<ExpenseDto>>>

    // ─── App Version ──────────────────────────────────────────────────────
    @GET("app/version")
    suspend fun checkAppVersion(): Response<ApiResponse<AppVersionDto>>
}
