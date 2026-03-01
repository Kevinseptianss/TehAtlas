# Business Management API Documentation

## Overview
This API provides comprehensive business management functionality including POS (Point of Sale) operations, inventory management, and analytics for retail outlets.

**Base URL**: `http://localhost` (for development) or your production server URL
**API Base Path**: `/api`

## Authentication

### Login
Authenticate users and receive JWT token for subsequent requests.

**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "username": "string",
  "password": "string"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt_token_string",
    "user": {
      "id": "string",
      "username": "string",
      "role": "admin|warehouse|cashier",
      "outlet_id": "string (optional)",
      "created_at": "datetime",
      "updated_at": "datetime"
    }
  }
}
```

**Example Request**:
```bash
curl -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Authentication Headers
Include the JWT token in all subsequent requests:
```
Authorization: Bearer <jwt_token>
```

## POS (Point of Sale) Endpoints

### Get Product Catalog
Retrieve available products for sale at the outlet.

**Endpoint**: `GET /api/outlet/items`

**Authorization**: Required (admin, cashier roles)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "sku": "string",
      "category": "string",
      "unit_price": "number",
      "created_at": "datetime",
      "updated_at": "datetime"
    }
  ]
}
```

### Create Sale Transaction
Process a POS sale transaction.

**Endpoint**: `POST /api/outlet/sales`

**Authorization**: Required (admin, cashier roles)

**Request Body**:
```json
{
  "outlet_id": "string (required for admin users)",
  "customer_name": "string (optional)",
  "customer_phone": "string (optional)",
  "items": [
    {
      "outlet_item_id": "string",
      "quantity": "integer",
      "unit_price": "number",
      "total_price": "number"
    }
  ],
  "subtotal": "number",
  "tax_amount": "number",
  "discount_amount": "number",
  "total_amount": "number",
  "payment_method": "string"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Sale created successfully",
  "data": {
    "id": "string",
    "receipt_number": "string",
    "outlet_id": "string",
    "customer_name": "string (optional)",
    "customer_phone": "string (optional)",
    "items": [
      {
        "outlet_item_id": "string",
        "quantity": "integer",
        "unit_price": "number",
        "total_price": "number"
      }
    ],
    "subtotal": "number",
    "tax_amount": "number",
    "discount_amount": "number",
    "total_amount": "number",
    "payment_method": "string",
    "sale_date": "datetime",
    "created_by": "string"
  }
}
```

**Example Request**:
```bash
curl -X POST http://localhost/api/outlet/sales \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "outlet_id": "69802f36ecbbc029ab284d0d",
    "customer_name": "John Doe",
    "items": [
      {
        "outlet_item_id": "69802f36ecbbc029ab284d10",
        "quantity": 2,
        "unit_price": 15.00,
        "total_price": 30.00
      }
    ],
    "subtotal": 30.00,
    "tax_amount": 3.00,
    "discount_amount": 0.00,
    "total_amount": 33.00,
    "payment_method": "cash"
  }'
```

### Get Sales Transactions
Retrieve list of sales transactions.

**Endpoint**: `GET /api/outlet/sales`

**Authorization**: Required (admin, cashier roles)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "receipt_number": "string",
      "outlet_id": "string",
      "customer_name": "string (optional)",
      "customer_phone": "string (optional)",
      "items": [
        {
          "outlet_item_id": "string",
          "quantity": "integer",
          "unit_price": "number",
          "total_price": "number"
        }
      ],
      "subtotal": "number",
      "tax_amount": "number",
      "discount_amount": "number",
      "total_amount": "number",
      "payment_method": "string",
      "sale_date": "datetime",
      "created_by": "string"
    }
  ]
}
```

### Get Sale Receipt
Retrieve specific sale transaction details.

**Endpoint**: `GET /api/outlet/sales/{sale_id}`

**Authorization**: Required (admin, cashier roles)

**Parameters**:
- `sale_id`: MongoDB ObjectId of the sale

**Response**: Same as individual sale object in the sales list.

### Get Outlet Dashboard
Get outlet performance metrics and analytics.

**Endpoint**: `GET /api/outlet/dashboard?outlet_id={outlet_id}`

**Authorization**: Required (admin, cashier roles)

**Query Parameters**:
- `outlet_id`: Required for admin users (optional for cashier users with outlet_id in token)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": {
    "today_sales": "number",
    "today_sale_count": "integer",
    "total_sales": "number",
    "total_sale_count": "integer",
    "current_inventory": "integer",
    "pending_purchases": "integer"
  }
}
```

## Admin Endpoints

### Get Outlets
Retrieve list of all outlets.

**Endpoint**: `GET /api/admin/outlets`

**Authorization**: Required (admin role only)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "name": "string",
      "address": "string",
      "phone": "string",
      "created_at": "datetime",
      "updated_at": "datetime"
    }
  ]
}
```

### Get Admin Dashboard
Get system-wide analytics and metrics.

**Endpoint**: `GET /api/admin/dashboard`

**Authorization**: Required (admin role only)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": {
    "total_discounts": "number",
    "total_outlets": "integer",
    "total_profit": "number",
    "total_revenue": "number",
    "total_sales": "number",
    "total_taxes": "number"
  }
}
```

## Warehouse Endpoints

### Get Warehouse Items
Retrieve warehouse inventory.

**Endpoint**: `GET /api/warehouse/items`

**Authorization**: Required (admin, warehouse roles)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "sku": "string",
      "category": "string",
      "cost_price": "number",
      "warehouse_id": "string",
      "created_at": "datetime",
      "updated_at": "datetime"
    }
  ]
}
```

### Get Warehouse Dashboard
Get warehouse performance metrics.

**Endpoint**: `GET /api/warehouse/dashboard`

**Authorization**: Required (admin, warehouse roles)

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": {
    "low_stock_alerts": "integer",
    "pending_invoices": "integer",
    "pending_purchases": "integer",
    "today_invoices": "integer",
    "today_sales": "integer",
    "total_items": "integer"
  }
}
```

## Common Endpoints

### Search Items
Search across all items.

**Endpoint**: `GET /api/items/search?q={query}`

**Authorization**: Required

**Query Parameters**:
- `q`: Search query string

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "name": "string",
      "type": "warehouse|outlet",
      "sku": "string",
      "category": "string",
      "price": "number"
    }
  ]
}
```

### Get Inventory Transactions
Get inventory movement history.

**Endpoint**: `GET /api/stock/transactions`

**Authorization**: Required

**Response**:
```json
{
  "success": true,
  "message": "",
  "data": [
    {
      "id": "string",
      "item_id": "string",
      "item_type": "warehouse|outlet",
      "transaction_type": "purchase|sale|transfer_in|transfer_out",
      "quantity": "integer",
      "reference_id": "string",
      "reference_type": "purchase|invoice|sale",
      "warehouse_id": "string (optional)",
      "outlet_id": "string (optional)",
      "transaction_date": "datetime"
    }
  ]
}
```

## Error Handling

All API responses follow a consistent error format:

**Success Response**:
```json
{
  "success": true,
  "message": "Optional success message",
  "data": { ... } // or [ ... ] for arrays
}
```

**Error Response**:
```json
{
  "success": false,
  "message": "Error description"
}
```

### Common HTTP Status Codes
- `200`: Success
- `201`: Created
- `400`: Bad Request (invalid data)
- `401`: Unauthorized (missing/invalid token)
- `403`: Forbidden (insufficient permissions)
- `404`: Not Found
- `500`: Internal Server Error

### Common Error Messages
- `"Authorization header required"`
- `"Invalid token"`
- `"Invalid credentials"`
- `"Invalid request format"`
- `"Outlet ID required"`
- `"Item not found in inventory"`
- `"Insufficient permissions"`

## Data Types

### Primitive Types
- `string`: Text data
- `number`: Floating point numbers (use double in Android)
- `integer`: Whole numbers (use int/long in Android)
- `boolean`: true/false values
- `datetime`: ISO 8601 formatted dates (e.g., "2026-02-02T06:34:02.535Z")

### ObjectId
MongoDB ObjectIds are represented as 24-character hexadecimal strings.

## Android Integration Notes

### 1. Authentication Flow
```kotlin
// 1. Login to get token
val loginRequest = LoginRequest("cashier_username", "password")
val loginResponse = apiService.login(loginRequest)

// 2. Store token for subsequent requests
val token = loginResponse.data.token
val authHeader = "Bearer $token"

// 3. Use token in all API calls
val products = apiService.getOutletItems(authHeader)
```

### 2. POS Transaction Flow
```kotlin
// 1. Get product catalog
val products = apiService.getOutletItems(authHeader)

// 2. Create sale transaction
val saleRequest = SaleRequest(
    outletId = "69802f36ecbbc029ab284d0d",
    customerName = "John Doe",
    items = listOf(
        SaleItem(
            outletItemId = "69802f36ecbbc029ab284d10",
            quantity = 2,
            unitPrice = 15.00,
            totalPrice = 30.00
        )
    ),
    subtotal = 30.00,
    taxAmount = 3.00,
    discountAmount = 0.00,
    totalAmount = 33.00,
    paymentMethod = "cash"
)

val saleResponse = apiService.createSale(authHeader, saleRequest)

// 3. Get receipt
val receipt = apiService.getSaleReceipt(authHeader, saleResponse.data.id)
```

### 3. Error Handling
```kotlin
try {
    val response = apiService.someApiCall(authHeader)
    if (response.success) {
        // Handle success
        val data = response.data
    } else {
        // Handle API error
        showError(response.message)
    }
} catch (e: HttpException) {
    // Handle HTTP errors (401, 403, 404, etc.)
    when (e.code()) {
        401 -> redirectToLogin()
        403 -> showPermissionError()
        else -> showGenericError()
    }
} catch (e: IOException) {
    // Handle network errors
    showNetworkError()
}
```

### 4. Data Models
Create corresponding Kotlin data classes for all API models:

```kotlin
// Login models
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String, val data: LoginData)
data class LoginData(val token: String, val user: User)

// Sale models
data class SaleRequest(
    val outletId: String? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val items: List<SaleItem>,
    val subtotal: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val totalAmount: Double,
    val paymentMethod: String
)

data class SaleItem(
    val outletItemId: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

// Generic API response wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)
```

### 5. Retrofit Configuration
```kotlin
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<LoginData>

    @GET("outlet/items")
    suspend fun getOutletItems(@Header("Authorization") token: String): ApiResponse<List<OutletItem>>

    @POST("outlet/sales")
    suspend fun createSale(@Header("Authorization") token: String, @Body sale: SaleRequest): ApiResponse<Sale>

    @GET("outlet/sales")
    suspend fun getSales(@Header("Authorization") token: String): ApiResponse<List<Sale>>

    @GET("outlet/sales/{id}")
    suspend fun getSaleReceipt(@Header("Authorization") token: String, @Path("id") saleId: String): ApiResponse<Sale>

    @GET("outlet/dashboard")
    suspend fun getOutletDashboard(
        @Header("Authorization") token: String,
        @Query("outlet_id") outletId: String? = null
    ): ApiResponse<DashboardData>
}
```

## Testing

Use the provided test script to validate API functionality:

```bash
cd /path/to/api
./test-api.sh
```

This will run comprehensive tests on all endpoints and report any issues.

## Support

For issues or questions about API integration, check:
1. This documentation
2. API test results
3. Server logs for detailed error information</content>
<parameter name="filePath">/Users/kevinseptian/Documents/Teh Atlas/api/documentation/api-documentation.md