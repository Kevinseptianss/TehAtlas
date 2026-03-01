package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"golang.org/x/crypto/bcrypt"
	"github.com/kevinseptian/tehatlas-backend/config"
	"github.com/kevinseptian/tehatlas-backend/database"
	"github.com/kevinseptian/tehatlas-backend/models"
)

// Login - Authenticate user and return JWT token
func Login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Check database for user
	var user models.User
	found := false
	if collection := database.GetCollection("users"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		err := collection.FindOne(ctx, map[string]interface{}{
			"username": req.Username,
		}).Decode(&user)

		if err == nil {
			// Verify password using bcrypt
			err = bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(req.Password))
			if err == nil {
				found = true
			}
		}
	}

	if !found {
		// Fallback to mock authentication for demo or if database is empty
		if req.Username == "" || req.Password == "" {
			http.Error(w, `{"success": false, "message": "Invalid credentials"}`, http.StatusUnauthorized)
			return
		}

		// Mock user data - role based on username
		var role string
		var outletID *string

		switch req.Username {
		case "admin":
			role = "admin"
			outletID = nil
		case "warehouse":
			role = "warehouse"
			outletID = nil
		case "outlet1", "cashier":
			role = "cashier"
			outletID = stringPtr("outlet-1")
		default:
			role = "cashier" // Default role
			outletID = stringPtr("mock-outlet-1")
		}

		// Hash the password for mock users too
		hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
		if err != nil {
			http.Error(w, `{"success": false, "message": "Failed to hash password"}`, http.StatusInternalServerError)
			return
		}

		user = models.User{
			Username:  req.Username,
			Password:  string(hashedPassword),
			Role:      role,
			OutletID:  outletID,
			CreatedAt: time.Now(),
			UpdatedAt: time.Now(),
		}

		// Save mock user to database if collection exists
		if collection := database.GetCollection("users"); collection != nil {
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()

			result, err := collection.InsertOne(ctx, user)
			if err != nil {
				log.Printf("Failed to save mock user: %v", err)
				// Continue anyway for demo
			} else {
				log.Printf("✅ Mock user saved: %+v", user)
				// Set the ID from the inserted document
				if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
					user.ID = oid.Hex()
				}
			}
		} else {
			// If no database, generate a mock ID
			user.ID = uuid.New().String()
		}
	}

	// Generate JWT token
	outletIDStr := ""
	if user.OutletID != nil {
		outletIDStr = *user.OutletID
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, models.Claims{
		UserID:   user.ID,
		Username: user.Username,
		Role:     user.Role,
		OutletID: outletIDStr,
		RegisteredClaims: jwt.RegisteredClaims{
			ExpiresAt: jwt.NewNumericDate(time.Now().Add(24 * time.Hour)),
			IssuedAt:  jwt.NewNumericDate(time.Now()),
		},
	})

	tokenString, err := token.SignedString([]byte(config.AppConfig.JWTSecret))
	if err != nil {
		http.Error(w, `{"success": false, "message": "Failed to generate token"}`, http.StatusInternalServerError)
		return
	}

	response := models.LoginResponse{
		Token: tokenString,
		User: models.User{
			ID:       user.ID,
			Username: user.Username,
			Role:     user.Role,
			OutletID: user.OutletID,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Login successful",
		Data:    response,
	})
}

// GetOutletItems - Get items available at outlet (cashier access)
func GetOutletItems(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock data
	items := []models.OutletItem{
		{
			ItemID:    "item-1",
			Name:      "Mock Coffee Beans",
			Stock:     50,
			MinStock:  10,
			Unit:      "kg",
			Category:  "Beverages",
			UpdatedAt: time.Now(),
		},
		{
			ItemID:    "item-2",
			Name:      "Mock Tea Bags",
			Stock:     100,
			MinStock:  20,
			Unit:      "pcs",
			Category:  "Beverages",
			UpdatedAt: time.Now(),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Items retrieved successfully (mock)",
		Data:    items,
	})
}

// GetOutletDashboard - Get outlet dashboard data (cashier access)
func GetOutletDashboard(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock dashboard data
	dashboard := models.OutletDashboard{
		TotalItems:       25,
		LowStockItems:    3,
		TotalSales:       1500.50,
		TodaySales:       250.75,
		RecentTransactions: []models.Sale{
			{
				Total: 45.00,
				Date:  time.Now().Add(-1 * time.Hour),
			},
			{
				Total: 32.50,
				Date:  time.Now().Add(-2 * time.Hour),
			},
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Dashboard data retrieved successfully (mock)",
		Data:    dashboard,
	})
}

// CreateSale - Create a new sale (cashier access)
func CreateSale(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.SaleRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Get user info from context (set by middleware)
	userID := r.Context().Value("user_id").(string)
	outletID := r.Context().Value("outlet_id").(string)

	// Create sale record
	sale := models.Sale{
		Items: req.Items,
		Total: req.Total,
		Date:  time.Now(),
		UserID: userID,
	}

	if outletID != "" {
		sale.OutletID = &outletID
	}

	// Save to database if available
	var saleID string
	if collection := database.GetCollection("sales"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, sale)
		if err != nil {
			log.Printf("Failed to save sale: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save sale"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Sale saved: %+v", sale)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			saleID = oid.Hex()
		}
	} else {
		// Fallback to mock ID
		saleID = uuid.New().String()
	}

	saleReceipt := models.SaleReceipt{
		SaleID: saleID,
		Total:  req.Total,
		Date:   time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Sale created successfully",
		Data:    saleReceipt,
	})
}

// GetSales - Get sales history or specific sale receipt (cashier access)
func GetSales(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Check if this is a request for a specific sale receipt
	path := r.URL.Path
	if len(path) > len("/api/outlet/sales/") {
		// This is a request for a specific sale receipt
		saleID := path[len("/api/outlet/sales/"):]

		// Mock receipt data
		receipt := models.SaleReceipt{
			SaleID: saleID,
			Total:  45.00,
			Date:   time.Now().Add(-1 * time.Hour),
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "Receipt retrieved successfully (mock)",
			Data:    receipt,
		})
		return
	}

	// This is a request for sales history
	// Mock sales data
	sales := []models.Sale{
		{
			Total: 45.00,
			Date:  time.Now().Add(-1 * time.Hour),
		},
		{
			Total: 32.50,
			Date:  time.Now().Add(-2 * time.Hour),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Sales retrieved successfully (mock)",
		Data:    sales,
	})
}

// GetSaleReceipt - Get sale receipt (cashier access)
func GetSaleReceipt(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	saleID := r.URL.Path[len("/api/outlet/sales/"):]

	// Mock receipt data
	receipt := models.SaleReceipt{
		SaleID: saleID,
		Total:  45.00,
		Date:   time.Now().Add(-1 * time.Hour),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Receipt retrieved successfully (mock)",
		Data:    receipt,
	})
}

// GetWarehouseItems - Get items in warehouse (warehouse access)
func GetWarehouseItems(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Check if items exist in database, if not create them
	if collection := database.GetCollection("warehouse_items"); collection != nil {
		// Try to find existing items
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		cursor, err := collection.Find(ctx, map[string]interface{}{})
		if err == nil {
			var items []models.WarehouseItem
			if err := cursor.All(ctx, &items); err == nil && len(items) > 0 {
				// Return existing items
				w.Header().Set("Content-Type", "application/json")
				json.NewEncoder(w).Encode(models.APIResponse{
					Success: true,
					Message: "Warehouse items retrieved successfully",
					Data:    items,
				})
				return
			}
		}

		// Create default items if none exist
		defaultItems := []models.WarehouseItem{
			{
				ItemID:   primitive.NewObjectID().Hex(),
				Name:     "Warehouse Coffee Beans",
				Stock:    500,
				Unit:     "kg",
				Category: "Beverages",
			},
			{
				ItemID:   primitive.NewObjectID().Hex(),
				Name:     "Green Tea Leaves",
				Stock:    300,
				Unit:     "kg",
				Category: "Beverages",
			},
		}

		// Insert items into database
		var itemsToInsert []interface{}
		for _, item := range defaultItems {
			itemsToInsert = append(itemsToInsert, item)
		}

		_, err = collection.InsertMany(ctx, itemsToInsert)
		if err != nil {
			log.Printf("Failed to create warehouse items: %v", err)
			// Fall back to returning items without saving
		} else {
			log.Printf("✅ Created %d warehouse items", len(defaultItems))
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "Warehouse items retrieved successfully",
			Data:    defaultItems,
		})
		return
	}

	// Fallback to mock items if database not available
	items := []models.WarehouseItem{
		{
			ItemID:   primitive.NewObjectID().Hex(),
			Name:     "Warehouse Coffee Beans",
			Stock:    500,
			Unit:     "kg",
			Category: "Beverages",
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Warehouse items retrieved successfully (fallback)",
		Data:    items,
	})
}

// GetWarehouseDashboard - Get warehouse dashboard (warehouse access)
func GetWarehouseDashboard(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock dashboard
	dashboard := models.WarehouseDashboard{
		TotalItems:        150,
		LowStockItems:     5,
		PendingTransfers:  3,
		TotalStockValue:   25000.00,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Warehouse dashboard retrieved successfully (mock)",
		Data:    dashboard,
	})
}

// CreateTransfer - Create item transfer (warehouse access)
func CreateTransfer(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.TransferRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Get user info from context (set by middleware)
	userID := r.Context().Value("user_id").(string)

	// Create transfer record
	transfer := models.Transfer{
		FromOutletID: req.FromOutletID,
		ToOutletID:   req.ToOutletID,
		Status:       "pending",
		Items:        req.Items,
		UserID:       userID,
		CreatedAt:    time.Now(),
	}

	// Save to database if available
	var transferID string
	if collection := database.GetCollection("transfers"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, transfer)
		if err != nil {
			log.Printf("Failed to save transfer: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save transfer"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Transfer saved: %+v", transfer)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			transferID = oid.Hex()
		}
	} else {
		// Fallback to mock ID
		transferID = uuid.New().String()
	}

	responseData := map[string]interface{}{
		"transfer_id": transferID,
		"from_outlet_id": req.FromOutletID,
		"to_outlet_id":   req.ToOutletID,
		"status":         "pending",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Transfer created successfully",
		Data:    responseData,
	})
}

// GetTransfers - Get transfer history or receive specific transfer (warehouse/cashier access)
func GetTransfers(w http.ResponseWriter, r *http.Request) {
	path := r.URL.Path

	if r.Method == http.MethodPut {
		// This is a request to receive a transfer
		if len(path) <= len("/api/warehouse/transfers/") {
			http.Error(w, `{"success": false, "message": "Transfer ID required"}`, http.StatusBadRequest)
			return
		}

		transferID := path[len("/api/warehouse/transfers/"):]

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "Transfer received successfully (mock)",
			Data: map[string]string{
				"transfer_id": transferID,
				"status":      "received",
			},
		})
		return
	}

	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// This is a request for transfer history
	// Mock transfers
	transfers := []models.Transfer{
		{
			FromOutletID: "outlet-1",
			ToOutletID:   "outlet-2",
			Status:       "completed",
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Transfers retrieved successfully (mock)",
		Data:    transfers,
	})
}

// ReceiveTransfer - Receive transfer at outlet (cashier access)
func ReceiveTransfer(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPut {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	transferID := r.URL.Path[len("/api/warehouse/transfers/"):]
	userID := r.Context().Value("user_id").(string)

	// Update transfer status in database if available
	if collection := database.GetCollection("transfers"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		filter := map[string]interface{}{"transfer_id": transferID}
		update := map[string]interface{}{
			"$set": map[string]interface{}{
				"status":       "received",
				"received_at":  time.Now(),
				"received_by":  userID,
			},
		}

		_, err := collection.UpdateOne(ctx, filter, update)
		if err != nil {
			log.Printf("Failed to update transfer status: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to update transfer"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Transfer %s status updated to received", transferID)
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Transfer received successfully",
		Data: map[string]string{
			"transfer_id": transferID,
			"status":      "received",
		},
	})
}

// GetAllInventoryTransactions - Admin access to all inventory transactions
func GetAllInventoryTransactions(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var transactions []models.InventoryTransaction

	// Fetch from database if available
	if collection := database.GetCollection("inventory_transactions"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		cursor, err := collection.Find(ctx, map[string]interface{}{})
		if err != nil {
			log.Printf("Failed to fetch inventory transactions: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to fetch transactions"}`, http.StatusInternalServerError)
			return
		}
		defer cursor.Close(ctx)

		for cursor.Next(ctx) {
			var transaction models.InventoryTransaction
			if err := cursor.Decode(&transaction); err != nil {
				log.Printf("Failed to decode transaction: %v", err)
				continue
			}
			transactions = append(transactions, transaction)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Inventory transactions retrieved successfully",
		Data:    transactions,
	})
}

// GetAllSales - Admin access to all sales
func GetAllSales(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var sales []map[string]interface{}

	// Fetch from database if available
	if collection := database.GetCollection("sales"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		cursor, err := collection.Find(ctx, map[string]interface{}{})
		if err != nil {
			log.Printf("Failed to fetch sales: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to fetch sales"}`, http.StatusInternalServerError)
			return
		}
		defer cursor.Close(ctx)

		for cursor.Next(ctx) {
			var sale map[string]interface{}
			if err := cursor.Decode(&sale); err != nil {
				log.Printf("Failed to decode sale: %v", err)
				continue
			}
			sales = append(sales, sale)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Sales retrieved successfully",
		Data:    sales,
	})
}

// GetAllTransfers - Admin access to all transfers
func GetAllTransfers(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var transfers []map[string]interface{}

	// Fetch from database if available
	if collection := database.GetCollection("transfers"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		cursor, err := collection.Find(ctx, map[string]interface{}{})
		if err != nil {
			log.Printf("Failed to fetch transfers: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to fetch transfers"}`, http.StatusInternalServerError)
			return
		}
		defer cursor.Close(ctx)

		for cursor.Next(ctx) {
			var transfer map[string]interface{}
			if err := cursor.Decode(&transfer); err != nil {
				log.Printf("Failed to decode transfer: %v", err)
				continue
			}
			transfers = append(transfers, transfer)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Transfers retrieved successfully",
		Data:    transfers,
	})
}

// GetOutlets - Get all outlets or create new outlet (admin access)
func GetOutlets(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodPost {
		// Create new outlet
		var req models.OutletRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
			return
		}

		outlet := models.Outlet{
			Name:      req.Name,
			Address:   req.Address,
			CreatedAt: time.Now(),
		}

		// Save to database if available
		var outletID string
		if collection := database.GetCollection("outlets"); collection != nil {
			ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
			defer cancel()

			result, err := collection.InsertOne(ctx, outlet)
			if err != nil {
				log.Printf("Failed to save outlet: %v", err)
				http.Error(w, `{"success": false, "message": "Failed to save outlet"}`, http.StatusInternalServerError)
				return
			}
			log.Printf("✅ Outlet saved: %+v", outlet)
			// Extract the inserted ID
			if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
				outletID = oid.Hex()
			}
		} else {
			// Fallback to mock ID
			outletID = primitive.NewObjectID().Hex()
		}

		outlet.ID = outletID

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "Outlet created successfully",
			Data:    outlet,
		})
		return
	}

	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Get all outlets from database
	if collection := database.GetCollection("outlets"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		cursor, err := collection.Find(ctx, map[string]interface{}{})
		if err != nil {
			log.Printf("Failed to get outlets: %v", err)
			// Fall back to mock data
		} else {
			var outlets []models.Outlet
			if err := cursor.All(ctx, &outlets); err == nil {
				w.Header().Set("Content-Type", "application/json")
				json.NewEncoder(w).Encode(models.APIResponse{
					Success: true,
					Message: "Outlets retrieved successfully",
					Data:    outlets,
				})
				return
			}
		}
	}

	// Fallback to mock outlets if database not available or empty
	outlets := []models.Outlet{
		{
			ID:      primitive.NewObjectID().Hex(),
			Name:    "Main Outlet",
			Address: "123 Main St",
			CreatedAt: time.Now(),
		},
		{
			ID:      primitive.NewObjectID().Hex(),
			Name:    "Branch Outlet",
			Address: "456 Branch Ave",
			CreatedAt: time.Now(),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Outlets retrieved successfully (fallback)",
		Data:    outlets,
	})
}

// CreateOutlet - Create new outlet (admin access)
func CreateOutlet(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.OutletRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	outlet := models.Outlet{
		Name:      req.Name,
		Address:   req.Address,
		CreatedAt: time.Now(),
	}

	// Save to database if available
	var outletID string
	if collection := database.GetCollection("outlets"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, outlet)
		if err != nil {
			log.Printf("Failed to save outlet: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save outlet"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Outlet saved: %+v", outlet)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			outletID = oid.Hex()
		}
	} else {
		// Fallback to mock ID
		outletID = primitive.NewObjectID().Hex()
	}

	outlet.ID = outletID

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Outlet created successfully",
		Data:    outlet,
	})
}

// GetAdminDashboard - Get admin dashboard (admin access)
func GetAdminDashboard(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock admin dashboard
	dashboard := models.AdminDashboard{
		TotalOutlets:     5,
		TotalUsers:       25,
		TotalSales:       50000.00,
		MonthlyRevenue:   15000.00,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Admin dashboard retrieved successfully (mock)",
		Data:    dashboard,
	})
}

// CreateUser - Create new user (admin access or initial setup)
func CreateUser(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.UserRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Check if this is initial setup (no users exist) - allow creating first admin without auth
	allowUnauthenticated := false
	if collection := database.GetCollection("users"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		count, err := collection.CountDocuments(ctx, map[string]interface{}{})
		if err == nil && count == 0 && req.Role == "admin" {
			allowUnauthenticated = true
		}
	}

	// Check authentication unless this is initial admin setup
	if !allowUnauthenticated {
		// Verify the request is from an admin (this will be checked by middleware)
		// The middleware should handle this
	}

	// Hash password
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, `{"success": false, "message": "Failed to hash password"}`, http.StatusInternalServerError)
		return
	}

	// Create user with hashed password
	user := models.User{
		Username:  req.Username,
		Password:  string(hashedPassword),
		Role:      req.Role,
		OutletID:  req.OutletID,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	// Save to database if available
	if collection := database.GetCollection("users"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, user)
		if err != nil {
			log.Printf("Failed to create user: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to create user"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ User created: %+v", user)
		// Set the ID from the inserted document
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			user.ID = oid.Hex()
		}
	}

	response := models.APIResponse{
		Success: true,
		Message: "User created successfully",
		Data: map[string]interface{}{
			"user_id":  user.ID,
			"username": user.Username,
			"role":     user.Role,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetUsers - Get all users or create new user (admin access)
func GetUsers(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodPost {
		// Create new user
		var req models.UserRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
			return
		}

		// Mock user creation
		user := models.User{
			Username: req.Username,
			Role:     req.Role,
			OutletID: req.OutletID,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "User created successfully (mock)",
			Data:    user,
		})
		return
	}

	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Get all users
	// Mock users
	users := []models.User{
		{
			Username: "admin",
			Role:     "admin",
		},
		{
			Username: "cashier1",
			Role:     "cashier",
			OutletID: stringPtr("outlet-1"),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Users retrieved successfully (mock)",
		Data:    users,
	})
}

// CreateSupplier - Create new supplier (admin access)
func CreateSupplier(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.SupplierRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Mock supplier creation
	supplier := models.Supplier{
		Name:    req.Name,
		Contact: req.Contact,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Supplier created successfully (mock)",
		Data:    supplier,
	})
}

// GetSuppliers - Get all suppliers (admin access)
// GetSuppliers - Get all suppliers or create new supplier (admin access)
func GetSuppliers(w http.ResponseWriter, r *http.Request) {
	if r.Method == http.MethodPost {
		// Create new supplier
		var req models.SupplierRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
			return
		}

		// Mock supplier creation
		supplier := models.Supplier{
			Name:    req.Name,
			Contact: req.Contact,
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(models.APIResponse{
			Success: true,
			Message: "Supplier created successfully (mock)",
			Data:    supplier,
		})
		return
	}

	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Get all suppliers
	// Mock suppliers
	suppliers := []models.Supplier{
		{
			Name:    "Coffee Supplier Inc",
			Contact: "supplier@coffee.com",
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Suppliers retrieved successfully (mock)",
		Data:    suppliers,
	})
}

// SearchItems - Search items across system (all roles)
func SearchItems(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	query := r.URL.Query().Get("q")

	// Mock search results
	items := []models.Item{
		{
			Name:     "Coffee Beans",
			Category: "Beverages",
		},
	}

	if query != "" {
		// Filter by query (mock)
		filteredItems := make([]models.Item, 0)
		for _, item := range items {
			if item.Name == query {
				filteredItems = append(filteredItems, item)
			}
		}
		items = filteredItems
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Items searched successfully (mock)",
		Data:    items,
	})
}

// GetStockTransactions - Get stock transaction history (all roles)
func GetStockTransactions(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock transactions
	transactions := []models.StockTransaction{
		{
			ItemName: "Coffee Beans",
			Type:     "in",
			Quantity: 50,
			Date:     time.Now().Add(-24 * time.Hour),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Stock transactions retrieved successfully (mock)",
		Data:    transactions,
	})
}

// ClearDatabase - Clear all data from database (admin only)
func ClearDatabase(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	collections := []string{
		"users", "transfers", "sales", "inventory_transactions",
		"outlet_items", "suppliers", "outlets", "purchase_orders",
		"warehouse_items",
	}

	for _, collectionName := range collections {
		if collection := database.GetCollection(collectionName); collection != nil {
			ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
			_, err := collection.DeleteMany(ctx, map[string]interface{}{})
			cancel()
			if err != nil {
				log.Printf("Failed to clear collection %s: %v", collectionName, err)
				http.Error(w, fmt.Sprintf(`{"success": false, "message": "Failed to clear collection %s"}`, collectionName), http.StatusInternalServerError)
				return
			}
		}
	}

	log.Println("✅ Database cleared successfully")
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(models.APIResponse{
		Success: true,
		Message: "Database cleared successfully",
	})
}