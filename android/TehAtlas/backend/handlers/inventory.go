package handlers

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"time"

	"github.com/google/uuid"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"github.com/kevinseptian/tehatlas-backend/database"
	"github.com/kevinseptian/tehatlas-backend/models"
)

// AddOutletStock - Add stock to outlet inventory (cashier access)
func AddOutletStock(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.AddStockRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Get user info from context (set by middleware)
	userID := r.Context().Value("user_id").(string)
	outletID := r.Context().Value("outlet_id").(string)

	// Create inventory transaction
	transaction := models.InventoryTransaction{
		ItemID:          req.ItemID,
		ItemName:        "Coffee Beans", // Mock item name
		TransactionType: "add_stock",
		Quantity:        req.Quantity,
		PreviousStock:   0, // Would need to query current stock
		NewStock:        req.Quantity,
		Supplier:        &req.Supplier,
		UnitCost:        &req.UnitCost,
		UserID:          userID,
		TransactionDate: time.Now(),
	}

	if outletID != "" {
		transaction.OutletID = &outletID
	}

	// Save to database if available
	var transactionID string
	if collection := database.GetCollection("inventory_transactions"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, transaction)
		if err != nil {
			log.Printf("Failed to save inventory transaction: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save transaction"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Inventory transaction saved: %+v", transaction)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			transactionID = oid.Hex()
		}
	}

	response := models.APIResponse{
		Success: true,
		Message: "Stock added successfully",
		Data: map[string]interface{}{
			"transaction_id": transactionID,
			"item_id":        req.ItemID,
			"quantity":       req.Quantity,
			"batch_id":       req.BatchID,
			"supplier":       req.Supplier,
			"unit_cost":      req.UnitCost,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// AdjustOutletStock - Adjust stock levels (cashier access)
func AdjustOutletStock(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.AdjustStockRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Get user info from context (set by middleware)
	userID := r.Context().Value("user_id").(string)
	outletID := r.Context().Value("outlet_id").(string)

	// Create inventory transaction
	transaction := models.InventoryTransaction{
		ItemID:          req.ItemID,
		ItemName:        "Coffee Beans", // Mock item name
		TransactionType: "adjustment",
		Quantity:        req.Adjustment,
		PreviousStock:   0, // Would need to query current stock
		NewStock:        req.Adjustment,
		Reason:          &req.Reason,
		UserID:          userID,
		TransactionDate: time.Now(),
	}

	if outletID != "" {
		transaction.OutletID = &outletID
	}

	// Save to database if available
	var transactionID string
	if collection := database.GetCollection("inventory_transactions"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, transaction)
		if err != nil {
			log.Printf("Failed to save inventory adjustment: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save adjustment"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Inventory adjustment saved: %+v", transaction)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			transactionID = oid.Hex()
		}
	}

	// Mock response for now
	response := models.APIResponse{
		Success: true,
		Message: "Stock adjusted successfully",
		Data: map[string]interface{}{
			"transaction_id": transactionID,
			"item_id":        req.ItemID,
			"adjustment":     req.Adjustment,
			"reason":         req.Reason,
			"previous_stock": 100, // mock
			"new_stock":      100 + req.Adjustment,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// CreatePurchaseOrder - Create purchase order (cashier access)
func CreatePurchaseOrder(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	var req models.PurchaseOrderRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, `{"success": false, "message": "Invalid request format"}`, http.StatusBadRequest)
		return
	}

	// Get user info from context (set by middleware)
	userID := r.Context().Value("user_id").(string)
	outletID := r.Context().Value("outlet_id").(string)

	// Create purchase order
	order := models.PurchaseOrder{
		SupplierName: req.SupplierName,
		OrderNumber:  req.OrderNumber,
		Items:        req.Items,
		TotalAmount:  req.TotalAmount,
		OrderDate:    time.Now(),
		ExpectedDate: req.ExpectedDate,
		Status:       "pending",
		UserID:       userID,
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}

	if outletID != "" {
		order.OutletID = &outletID
	}

	// Save to database if available
	var orderID string
	if collection := database.GetCollection("purchase_orders"); collection != nil {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()

		result, err := collection.InsertOne(ctx, order)
		if err != nil {
			log.Printf("Failed to save purchase order: %v", err)
			http.Error(w, `{"success": false, "message": "Failed to save purchase order"}`, http.StatusInternalServerError)
			return
		}
		log.Printf("✅ Purchase order saved: %+v", order)
		// Extract the inserted ID
		if oid, ok := result.InsertedID.(primitive.ObjectID); ok {
			orderID = oid.Hex()
		}
	} else {
		// Fallback to mock ID
		orderID = uuid.New().String()
	}

	response := models.APIResponse{
		Success: true,
		Message: "Purchase order created successfully",
		Data: map[string]interface{}{
			"order_id":     orderID,
			"supplier_name": req.SupplierName,
			"order_number": req.OrderNumber,
			"items":        req.Items,
			"total_amount": req.TotalAmount,
			"status":       "pending",
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetOutletInventoryTransactions - Get inventory transactions for outlet (cashier access)
func GetOutletInventoryTransactions(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock data
	transactions := []models.InventoryTransaction{
		{
			ItemID:          "mock-item-1",
			ItemName:        "Mock Item 1",
			TransactionType: "add_stock",
			Quantity:        50,
			PreviousStock:   20,
			NewStock:        70,
			BatchID:         stringPtr("BATCH001"),
			Supplier:        stringPtr("Mock Supplier"),
			UnitCost:        floatPtr(10.50),
			TotalCost:       floatPtr(525.00),
			OutletID:        stringPtr("outlet-1"),
			UserID:          "user-1",
			TransactionDate: time.Now().Add(-24 * time.Hour),
		},
		{
			ItemID:          "mock-item-2",
			ItemName:        "Mock Item 2",
			TransactionType: "adjust_stock",
			Quantity:        -5,
			PreviousStock:   30,
			NewStock:        25,
			Reason:          stringPtr("Damaged goods"),
			OutletID:        stringPtr("outlet-1"),
			UserID:          "user-1",
			TransactionDate: time.Now().Add(-12 * time.Hour),
		},
	}

	response := models.APIResponse{
		Success: true,
		Message: "Transactions retrieved successfully (mock)",
		Data:    transactions,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetOutletInventoryItems - Get inventory items for outlet (cashier access)
func GetOutletInventoryItems(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"success": false, "message": "Method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}

	// Mock data
	items := []models.OutletItem{
		{
			ItemID:    "mock-item-1",
			Name:      "Mock Item 1",
			Stock:     70,
			MinStock:  10,
			Unit:      "pcs",
			Category:  "Mock Category",
			UpdatedAt: time.Now(),
		},
		{
			ItemID:    "mock-item-2",
			Name:      "Mock Item 2",
			Stock:     25,
			MinStock:  5,
			Unit:      "kg",
			Category:  "Mock Category 2",
			UpdatedAt: time.Now(),
		},
	}

	response := models.APIResponse{
		Success: true,
		Message: "Inventory items retrieved successfully (mock)",
		Data:    items,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// Helper functions for mock data
func stringPtr(s string) *string {
	return &s
}

func floatPtr(f float64) *float64 {
	return &f
}