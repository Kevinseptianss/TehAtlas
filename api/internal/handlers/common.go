package handlers

import (
	"net/http"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

// SearchItems searches for items across warehouse and outlet collections
func SearchItems(c *gin.Context) {
	query := c.Query("q")
	if query == "" {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Search query required",
		})
		return
	}

	outletID, exists := c.Get("outlet_id")
	var outletObjID *primitive.ObjectID
	if exists && outletID != nil {
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = &v
		case string:
			if oid, err := primitive.ObjectIDFromHex(v); err == nil {
				outletObjID = &oid
			}
		}
	}

	// Search products
	var collection *mongo.Collection
	if outletObjID != nil {
		collection = database.GetCollection("outlet_items")
	} else {
		collection = database.GetCollection("warehouse_items")
	}

	filter := bson.M{
		"location_id": outletObjID,
		"$or": []bson.M{
			{"name": bson.M{"$regex": query, "$options": "i"}},
			{"sku": bson.M{"$regex": query, "$options": "i"}},
		},
	}

	cursor, err := collection.Find(c, filter)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error searching products",
		})
		return
	}
	defer cursor.Close(c)

	var products []models.Product
	cursor.All(c, &products)

	result := map[string]interface{}{
		"items": products,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    result,
	})
}

// GetItemDetails returns details of a specific item
func GetItemDetails(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	// Check warehouse first
	warehouseCollection := database.GetCollection("warehouse_items")
	var product models.Product
	err = warehouseCollection.FindOne(c, bson.M{"_id": objID}).Decode(&product)
	if err == nil {
		c.JSON(http.StatusOK, models.APIResponse{
			Success: true,
			Data:    product,
		})
		return
	}

	// Then check outlets
	outletCollection := database.GetCollection("outlet_items")
	err = outletCollection.FindOne(c, bson.M{"_id": objID}).Decode(&product)
	if err == nil {
		c.JSON(http.StatusOK, models.APIResponse{
			Success: true,
			Data:    product,
		})
		return
	}

	c.JSON(http.StatusNotFound, models.APIResponse{
		Success: false,
		Message: "Item not found",
	})
}

// GetWarehouseStock returns current stock for a warehouse item
func GetWarehouseStock(c *gin.Context) {
	itemID := c.Param("item_id")
	objID, err := primitive.ObjectIDFromHex(itemID)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	// Aggregate stock from inventory transactions
	// This is a simplified version - in production, you'd use MongoDB aggregation
	collection := database.GetCollection("inventory_transactions")

	pipeline := []bson.M{
		{"$match": bson.M{
			"item_id":   objID,
			"item_type": "warehouse",
		}},
		{"$group": bson.M{
			"_id":         nil,
			"total_stock": bson.M{"$sum": "$quantity"},
		}},
	}

	cursor, err := collection.Aggregate(c, pipeline)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error calculating stock",
		})
		return
	}
	defer cursor.Close(c)

	var result []bson.M
	cursor.All(c, &result)

	stock := 0
	if len(result) > 0 {
		if totalStock, ok := result[0]["total_stock"].(int32); ok {
			stock = int(totalStock)
		}
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data: map[string]interface{}{
			"item_id": itemID,
			"stock":   stock,
		},
	})
}

// GetOutletStock returns current stock for an outlet item
func GetOutletStock(c *gin.Context) {
	itemID := c.Param("item_id")
	objID, err := primitive.ObjectIDFromHex(itemID)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	// Get outlet ID from context (assuming outlet user)
	outletID, exists := c.Get("outlet_id")
	if !exists {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Outlet ID required",
		})
		return
	}

	outletObjID, ok := outletID.(primitive.ObjectID)
	if !ok {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid outlet ID",
		})
		return
	}

	// Aggregate stock from inventory transactions
	collection := database.GetCollection("inventory_transactions")

	pipeline := []bson.M{
		{"$match": bson.M{
			"item_id":   objID,
			"item_type": "outlet",
			"outlet_id": outletObjID,
		}},
		{"$group": bson.M{
			"_id":         nil,
			"total_stock": bson.M{"$sum": "$quantity"},
		}},
	}

	cursor, err := collection.Aggregate(c, pipeline)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error calculating stock",
		})
		return
	}
	defer cursor.Close(c)

	var result []bson.M
	cursor.All(c, &result)

	stock := 0
	if len(result) > 0 {
		if totalStock, ok := result[0]["total_stock"].(int32); ok {
			stock = int(totalStock)
		}
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data: map[string]interface{}{
			"item_id":   itemID,
			"outlet_id": outletObjID.Hex(),
			"stock":     stock,
		},
	})
}

// GetInventoryTransactions returns inventory transaction history
func GetInventoryTransactions(c *gin.Context) {
	collection := database.GetCollection("inventory_transactions")

	cursor, err := collection.Find(c, bson.M{})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching transactions",
		})
		return
	}
	defer cursor.Close(c)

	var transactions []models.InventoryTransaction
	if err = cursor.All(c, &transactions); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding transactions",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    transactions,
	})
}

// HealthCheck returns the health status of the API and database
func HealthCheck(c *gin.Context) {
	// Check database connectivity
	if database.Client == nil {
		c.JSON(http.StatusServiceUnavailable, models.APIResponse{
			Success: false,
			Message: "Database connection failed",
		})
		return
	}

	// Ping the database
	if err := database.Client.Ping(c, nil); err != nil {
		c.JSON(http.StatusServiceUnavailable, models.APIResponse{
			Success: false,
			Message: "Database ping failed",
		})
		return
	}

	// Health response with version info
	healthResponse := map[string]interface{}{
		"status":    "healthy",
		"service":   "business-management-api",
		"version":   "1.0.0",
		"timestamp": "2026-02-02T00:00:00Z", // Current date
		"database": map[string]string{
			"status": "connected",
		},
		"endpoints": map[string]string{
			"version": "1.0.0",
		},
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Service is healthy",
		Data:    healthResponse,
	})
}
