# Android POS Integration Quick Start

## Prerequisites
- Android Studio Arctic Fox or later
- Minimum SDK: API 21 (Android 5.0)
- Dependencies: Retrofit, OkHttp, Gson

## 1. Add Dependencies (build.gradle)

```gradle
dependencies {
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.10.0'
}
```

## 2. Configure Retrofit

```kotlin
object ApiClient {
    private const val BASE_URL = "http://your-server-url/api/"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}

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

## 3. Data Models

```kotlin
// API Response Wrapper
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

// Authentication
data class LoginRequest(val username: String, val password: String)
data class LoginData(val token: String, val user: User)
data class User(
    val id: String,
    val username: String,
    val role: String,
    val outlet_id: String? = null
)

// Products
data class OutletItem(
    val id: String,
    val name: String,
    val description: String,
    val sku: String,
    val category: String,
    val unit_price: Double
)

// Sales
data class SaleRequest(
    val outlet_id: String? = null,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val items: List<SaleItem>,
    val subtotal: Double,
    val tax_amount: Double,
    val discount_amount: Double,
    val total_amount: Double,
    val payment_method: String
)

data class SaleItem(
    val outlet_item_id: String,
    val quantity: Int,
    val unit_price: Double,
    val total_price: Double
)

data class Sale(
    val id: String,
    val receipt_number: String,
    val outlet_id: String,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val items: List<SaleItem>,
    val subtotal: Double,
    val tax_amount: Double,
    val discount_amount: Double,
    val total_amount: Double,
    val payment_method: String,
    val sale_date: String,
    val created_by: String
)

// Dashboard
data class DashboardData(
    val today_sales: Double,
    val today_sale_count: Int,
    val total_sales: Double,
    val total_sale_count: Int,
    val current_inventory: Int,
    val pending_purchases: Int
)
```

## 4. Usage Examples

### Authentication
```kotlin
class AuthRepository(private val apiService: ApiService) {
    suspend fun login(username: String, password: String): Result<String> {
        return try {
            val response = apiService.login(LoginRequest(username, password))
            if (response.success && response.data != null) {
                // Store token securely
                tokenStorage.saveToken(response.data.token)
                Result.success(response.data.token)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### POS Operations
```kotlin
class PosRepository(private val apiService: ApiService) {

    suspend fun getProducts(token: String): Result<List<OutletItem>> {
        return try {
            val response = apiService.getOutletItems("Bearer $token")
            if (response.success) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSale(token: String, saleRequest: SaleRequest): Result<Sale> {
        return try {
            val response = apiService.createSale("Bearer $token", saleRequest)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSalesHistory(token: String): Result<List<Sale>> {
        return try {
            val response = apiService.getSales("Bearer $token")
            if (response.success) {
                Result.success(response.data ?: emptyList())
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSaleReceipt(token: String, saleId: String): Result<Sale> {
        return try {
            val response = apiService.getSaleReceipt("Bearer $token", saleId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboard(token: String, outletId: String? = null): Result<DashboardData> {
        return try {
            val response = apiService.getOutletDashboard("Bearer $token", outletId)
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### ViewModel Example
```kotlin
class PosViewModel(private val posRepository: PosRepository) : ViewModel() {

    private val _products = MutableLiveData<List<OutletItem>>()
    val products: LiveData<List<OutletItem>> = _products

    private val _currentSale = MutableLiveData<SaleRequest>()
    val currentSale: LiveData<SaleRequest> = _currentSale

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        _currentSale.value = SaleRequest(
            items = mutableListOf(),
            subtotal = 0.0,
            tax_amount = 0.0,
            discount_amount = 0.0,
            total_amount = 0.0,
            payment_method = "cash"
        )
    }

    fun loadProducts(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = posRepository.getProducts(token)) {
                is Result.Success -> _products.value = result.data
                is Result.Failure -> {
                    // Handle error
                    Log.e("PosViewModel", "Failed to load products", result.exception)
                }
            }
            _isLoading.value = false
        }
    }

    fun addItemToSale(product: OutletItem, quantity: Int) {
        val currentSale = _currentSale.value ?: return
        val existingItem = currentSale.items.find { it.outlet_item_id == product.id }

        val updatedItems = currentSale.items.toMutableList()
        if (existingItem != null) {
            // Update existing item
            val index = updatedItems.indexOf(existingItem)
            val newQuantity = existingItem.quantity + quantity
            val newTotalPrice = newQuantity * existingItem.unit_price
            updatedItems[index] = existingItem.copy(
                quantity = newQuantity,
                total_price = newTotalPrice
            )
        } else {
            // Add new item
            updatedItems.add(SaleItem(
                outlet_item_id = product.id,
                quantity = quantity,
                unit_price = product.unit_price,
                total_price = quantity * product.unit_price
            ))
        }

        // Recalculate totals
        val subtotal = updatedItems.sumOf { it.total_price }
        val taxAmount = subtotal * 0.1 // 10% tax example
        val totalAmount = subtotal + taxAmount

        _currentSale.value = currentSale.copy(
            items = updatedItems,
            subtotal = subtotal,
            tax_amount = taxAmount,
            total_amount = totalAmount
        )
    }

    fun processSale(token: String, outletId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val saleRequest = _currentSale.value?.copy(outlet_id = outletId) ?: return@launch

            when (val result = posRepository.createSale(token, saleRequest)) {
                is Result.Success -> {
                    // Sale successful, clear current sale
                    _currentSale.value = SaleRequest(
                        items = mutableListOf(),
                        subtotal = 0.0,
                        tax_amount = 0.0,
                        discount_amount = 0.0,
                        total_amount = 0.0,
                        payment_method = "cash"
                    )
                    // Show success message or navigate to receipt
                }
                is Result.Failure -> {
                    // Handle error
                    Log.e("PosViewModel", "Failed to process sale", result.exception)
                }
            }
            _isLoading.value = false
        }
    }
}
```

## 5. Error Handling Strategy

```kotlin
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
}

class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

fun handleApiError(throwable: Throwable): String {
    return when (throwable) {
        is HttpException -> {
            when (throwable.code()) {
                401 -> "Authentication required. Please login again."
                403 -> "You don't have permission to perform this action."
                404 -> "The requested resource was not found."
                500 -> "Server error. Please try again later."
                else -> "An error occurred: ${throwable.message()}"
            }
        }
        is IOException -> "Network error. Please check your connection."
        is ApiException -> throwable.message ?: "Unknown API error"
        else -> "An unexpected error occurred."
    }
}
```

## 6. Testing the Integration

```kotlin
// Example test for POS functionality
@RunWith(AndroidJUnit4::class)
class PosIntegrationTest {

    private lateinit var apiService: ApiService

    @Before
    fun setup() {
        // Create test API service
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:8080/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    @Test
    fun testLoginAndGetProducts() = runBlocking {
        // Test login
        val loginResponse = apiService.login(LoginRequest("admin", "admin123"))
        assertTrue(loginResponse.success)
        assertNotNull(loginResponse.data?.token)

        val token = loginResponse.data!!.token

        // Test get products
        val productsResponse = apiService.getOutletItems("Bearer $token")
        assertTrue(productsResponse.success)
        assertNotNull(productsResponse.data)
        assertTrue(productsResponse.data!!.isNotEmpty())
    }

    @Test
    fun testCreateSale() = runBlocking {
        // Login first
        val loginResponse = apiService.login(LoginRequest("admin", "admin123"))
        val token = loginResponse.data!!.token

        // Create sale
        val saleRequest = SaleRequest(
            outlet_id = "69802f36ecbbc029ab284d0d",
            items = listOf(
                SaleItem(
                    outlet_item_id = "69802f36ecbbc029ab284d10",
                    quantity = 1,
                    unit_price = 15.00,
                    total_price = 15.00
                )
            ),
            subtotal = 15.00,
            tax_amount = 1.50,
            discount_amount = 0.00,
            total_amount = 16.50,
            payment_method = "cash"
        )

        val saleResponse = apiService.createSale("Bearer $token", saleRequest)
        assertTrue(saleResponse.success)
        assertNotNull(saleResponse.data)
        assertNotNull(saleResponse.data?.receipt_number)
    }
}
```

## 7. Best Practices

1. **Token Management**: Store JWT tokens securely using EncryptedSharedPreferences
2. **Error Handling**: Always check `response.success` before accessing `response.data`
3. **Network Calls**: Use coroutines and handle exceptions properly
4. **Data Validation**: Validate sale data before sending to API
5. **Offline Support**: Consider implementing local caching for products and pending sales
6. **Security**: Never log sensitive data like tokens or passwords

## 8. Common Issues & Solutions

### Issue: 401 Unauthorized
**Solution**: Ensure token is valid and properly formatted in Authorization header

### Issue: 400 Bad Request
**Solution**: Check request payload format matches API specification

### Issue: Network Timeout
**Solution**: Implement retry logic and proper timeout configuration

### Issue: Invalid Outlet ID
**Solution**: For admin users, always include `outlet_id` in sale requests

## 9. Production Configuration

```kotlin
object ApiConfig {
    const val BASE_URL = "https://your-production-api.com/api/"
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}
```

This quick start guide provides everything needed to integrate the POS API into an Android application. For detailed API specifications, refer to the full API documentation.</content>
<parameter name="filePath">/Users/kevinseptian/Documents/Teh Atlas/api/documentation/android-pos-integration.md