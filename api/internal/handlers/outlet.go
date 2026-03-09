package handlers

import (
	"log"
	"net/http"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GetOutletDashboard returns outlet dashboard data
func GetOutletDashboard(c *gin.Context) {
	var outletObjID primitive.ObjectID
	var err error

	// Check if user is admin and outlet_id is provided as query parameter
	userRole, roleExists := c.Get("role")
	outletIDParam := c.Query("outlet_id")
	if roleExists && userRole == "admin" && outletIDParam != "" {
		// Admin can specify outlet_id as query parameter
		outletObjID, err = primitive.ObjectIDFromHex(outletIDParam)
		if err != nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Invalid outlet ID format",
			})
			return
		}
	} else {
		// Get outlet ID from context for regular users
		outletID, exists := c.Get("outlet_id")
		if !exists || outletID == nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Outlet ID required",
			})
			return
		}

		// Handle outlet_id as string (from JWT) or ObjectID
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = v
		case string:
			if v == "" {
				c.JSON(http.StatusBadRequest, models.APIResponse{
					Success: false,
					Message: "Outlet ID required",
				})
				return
			}
			var parseErr error
			outletObjID, parseErr = primitive.ObjectIDFromHex(v)
			if parseErr != nil {
				c.JSON(http.StatusBadRequest, models.APIResponse{
					Success: false,
					Message: "Invalid outlet ID format",
				})
				return
			}
		default:
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Invalid outlet ID",
			})
			return
		}
	}

	// Get today's sales for this outlet
	salesCollection := database.GetCollection("sales")
	today := time.Now().Truncate(24 * time.Hour)
	tomorrow := today.Add(24 * time.Hour)

	todaySalesPipeline := mongo.Pipeline{
		bson.D{{Key: "$match", Value: bson.M{
			"outlet_id": outletObjID,
			"sale_date": bson.M{"$gte": today, "$lt": tomorrow},
		}}},
		bson.D{{Key: "$group", Value: bson.M{
			"_id":         nil,
			"today_sales": bson.M{"$sum": "$total_amount"},
			"sale_count":  bson.M{"$sum": 1},
		}}},
	}

	cursor, err := salesCollection.Aggregate(c, todaySalesPipeline)
	todaySales := 0.0
	todaySaleCount := 0

	if err == nil {
		var result struct {
			TodaySales float64 `bson:"today_sales"`
			SaleCount  int     `bson:"sale_count"`
		}
		if cursor.Next(c) {
			cursor.Decode(&result)
			todaySales = result.TodaySales
			todaySaleCount = result.SaleCount
		}
		cursor.Close(c)
	}

	// Get current inventory count (total outlet items)
	outletItemsCollection := database.GetCollection("outlet_items")
	currentInventory, err := outletItemsCollection.CountDocuments(c, bson.M{})
	if err != nil {
		currentInventory = 0
	}

	// Get pending purchases for this outlet
	outletPurchasesCollection := database.GetCollection("outlet_purchases")
	pendingPurchases, err := outletPurchasesCollection.CountDocuments(c, bson.M{
		"outlet_id": outletObjID,
		"status":    "pending",
	})
	if err != nil {
		pendingPurchases = 0
	}

	// Get total sales for this outlet (all time)
	totalSalesPipeline := mongo.Pipeline{
		{{"$match", bson.M{"outlet_id": outletObjID}}},
		{{"$group", bson.M{
			"_id":         nil,
			"total_sales": bson.M{"$sum": "$total_amount"},
			"total_count": bson.M{"$sum": 1},
		}}},
	}

	cursor, err = salesCollection.Aggregate(c, totalSalesPipeline)
	totalSales := 0.0
	totalSaleCount := 0

	if err == nil {
		var result struct {
			TotalSales float64 `bson:"total_sales"`
			TotalCount int     `bson:"total_count"`
		}
		if cursor.Next(c) {
			cursor.Decode(&result)
			totalSales = result.TotalSales
			totalSaleCount = result.TotalCount
		}
		cursor.Close(c)
	}

	dashboard := map[string]interface{}{
		"today_sales":       todaySales,
		"today_sale_count":  todaySaleCount,
		"total_sales":       totalSales,
		"total_sale_count":  totalSaleCount,
		"current_inventory": currentInventory,
		"pending_purchases": pendingPurchases,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    dashboard,
	})
}

// GetOutletItems returns list of outlet items
func GetOutletItems(c *gin.Context) {
	// Get outlet ID from context
	outletID, exists := c.Get("outlet_id")
	if !exists || outletID == nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Outlet ID required",
		})
		return
	}

	var outletObjID primitive.ObjectID
	switch v := outletID.(type) {
	case primitive.ObjectID:
		outletObjID = v
	case string:
		outletObjID, _ = primitive.ObjectIDFromHex(v)
	}

	collection := database.GetCollection("outlet_items")
	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{"location_id": outletObjID}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching items",
		})
		return
	}
	defer cursor.Close(c)

	items := []models.Product{}
	if err = cursor.All(c, &items); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding items",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    items,
	})
}

// CreateOutletItem creates a new outlet item
func CreateOutletItem(c *gin.Context) {
	var req struct {
		Name        string  `json:"name" binding:"required"`
		Description string  `json:"description"`
		SKU         string  `json:"sku"`
		Category    string  `json:"category"`
		UnitPrice   float64 `json:"unit_price" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request: " + err.Error(),
		})
		return
	}

	outletID, _ := c.Get("outlet_id")
	var outletObjID primitive.ObjectID
	if outletID != nil {
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = v
		case string:
			outletObjID, _ = primitive.ObjectIDFromHex(v)
		}
	}

	item := models.Product{
		ID:          primitive.NewObjectID(),
		LocationID:  &outletObjID,
		Name:        req.Name,
		Description: req.Description,
		SKU:         req.SKU,
		Category:    req.Category,
		UnitPrice:   req.UnitPrice,
		CreatedAt:   time.Now(),
		UpdatedAt:   time.Now(),
	}

	collection := database.GetCollection("outlet_items")
	_, err := collection.InsertOne(c, item)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating item",
		})
		return
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Item created successfully",
		Data:    item,
	})
}

// DeleteOutletItem deletes an outlet item
func DeleteOutletItem(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	collection := database.GetCollection("outlet_items")
	result, err := collection.DeleteOne(c, bson.M{"_id": objID})
	if err != nil || result.DeletedCount == 0 {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Item not found",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Item deleted successfully",
	})
}

// UpdateOutletItem updates an outlet item and handles stock adjustments
func UpdateOutletItem(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	var req struct {
		Name        string  `json:"name"`
		Description string  `json:"description"`
		SKU         string  `json:"sku"`
		Category    string  `json:"category"`
		UnitPrice   float64 `json:"unit_price"`
		Stock       *int    `json:"stock"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	outletID, exists := c.Get("outlet_id")
	var outletObjID primitive.ObjectID
	if exists && outletID != nil {
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = v
		case string:
			outletObjID, _ = primitive.ObjectIDFromHex(v)
		}
	}

	collection := database.GetCollection("outlet_items")
	var product models.Product
	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&product)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Product not found",
		})
		return
	}

	update := bson.M{
		"name":        req.Name,
		"description": req.Description,
		"sku":         req.SKU,
		"category":    req.Category,
		"unit_price":  req.UnitPrice,
		"updated_at":  time.Now(),
	}

	// Handle Stock Adjustment if provided
	if req.Stock != nil {
		newStock := *req.Stock
		changeQty := newStock - product.Stock

		if changeQty != 0 {
			update["stock"] = newStock

			// Log adjustment to stock_history
			historyCollection := database.GetCollection("stock_history")
			history := models.StockHistory{
				ID:         primitive.NewObjectID(),
				LocationID: &outletObjID,
				ProductID:  objID,
				Type:       "adjustment",
				ChangeQty:  changeQty,
				Balance:    newStock,
				CostPrice:  product.CostPrice,
				CreatedAt:  time.Now(),
			}
			historyCollection.InsertOne(c, history)
		}
	}

	_, err = collection.UpdateOne(c, bson.M{"_id": objID}, bson.M{"$set": update})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error updating product",
		})
		return
	}

	// Fetch updated product to return
	var updatedProduct models.Product
	collection.FindOne(c, bson.M{"_id": objID}).Decode(&updatedProduct)

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Product updated successfully",
		Data:    updatedProduct,
	})
}

// CreateOutletPurchase creates a purchase from warehouse
func CreateOutletPurchase(c *gin.Context) {
	var purchase models.OutletPurchase
	if err := c.ShouldBindJSON(&purchase); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format: " + err.Error(),
		})
		return
	}

	outletID, exists := c.Get("outlet_id")
	if exists && outletID != nil {
		if oid, ok := outletID.(string); ok && oid != "" {
			if parsedOID, err := primitive.ObjectIDFromHex(oid); err == nil {
				purchase.OutletID = parsedOID
			}
		} else if oid, ok := outletID.(primitive.ObjectID); ok {
			purchase.OutletID = oid
		}
	}

	purchase.ID = primitive.NewObjectID()
	purchase.PurchaseDate = time.Now()
	purchase.Status = "pending"

	var total float64
	for i := range purchase.Items {
		itemTotal := purchase.Items[i].UnitPrice * float64(purchase.Items[i].Quantity)
		purchase.Items[i].TotalPrice = itemTotal
		total += itemTotal
	}
	purchase.TotalAmount = total

	collection := database.GetCollection("outlet_purchases")
	_, err := collection.InsertOne(c, purchase)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating purchase",
		})
		return
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Purchase created successfully",
		Data:    purchase,
	})
}

// GetOutletPurchases returns list of outlet purchases
func GetOutletPurchases(c *gin.Context) {
	collection := database.GetCollection("outlet_purchases")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching purchases",
		})
		return
	}
	defer cursor.Close(c)

	purchases := []models.OutletPurchase{}
	if err = cursor.All(c, &purchases); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding purchases",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    purchases,
	})
}

// ReceivePurchase marks purchase as received
func ReceivePurchase(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid purchase ID",
		})
		return
	}

	collection := database.GetCollection("outlet_purchases")

	// Fetch the purchase order
	var purchase models.OutletPurchase
	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&purchase)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Purchase not found",
		})
		return
	}

	if purchase.Status == "received" {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Purchase already received",
		})
		return
	}

	// Update purchase status
	_, err = collection.UpdateOne(c, bson.M{"_id": objID}, bson.M{"$set": bson.M{"status": "received"}})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error updating purchase status",
		})
		return
	}

	// Increment stock and log history
	itemsCollection := database.GetCollection("outlet_items")
	historyCollection := database.GetCollection("stock_history")

	for _, item := range purchase.Items {
		// Find or Create this specific product for this outlet
		var outletProduct models.Product

		// 1. Try to find by SKU for this specific outlet
		// Using the "item.ProductID" as the reference to the Warehouse Product (Template)
		err := itemsCollection.FindOne(c, bson.M{
			"location_id": purchase.OutletID,
			"sku":         item.ProductID.Hex(), // Old logic kept for compatibility
		}).Decode(&outletProduct)

		// 2. If not found, try finding by actual SKU if available in the future or via some other link
		if err != nil {
			// Let's check if the product name and outlet match as a fallback
			err = itemsCollection.FindOne(c, bson.M{
				"location_id": purchase.OutletID,
				"name":        item.ProductName,
			}).Decode(&outletProduct)
		}

		if err == mongo.ErrNoDocuments {
			// Create new product for this outlet
			outletProduct = models.Product{
				ID:         primitive.NewObjectID(),
				LocationID: &purchase.OutletID,
				Name:       item.ProductName,
				SKU:        item.ProductID.Hex(), // Link to warehouse product ID
				Stock:      0,
				CostPrice:  item.UnitPrice,
				UnitPrice:  item.UnitPrice * 1.5, // Default markup
				CreatedAt:  time.Now(),
				UpdatedAt:  time.Now(),
			}
			_, insertErr := itemsCollection.InsertOne(c, outletProduct)
			if insertErr != nil {
				log.Printf("Failed to create outlet product during receive: %v", insertErr)
				continue
			}
		}

		oldStock := float64(outletProduct.Stock)
		newStock := outletProduct.Stock + item.Quantity
		oldCost := outletProduct.CostPrice

		var newCost float64
		if float64(newStock) > 0 {
			// Weighted average cost: (old_total_cost + new_total_cost) / total_quantity
			newCost = ((oldStock * oldCost) + (float64(item.Quantity) * item.UnitPrice)) / float64(newStock)
		} else {
			newCost = item.UnitPrice
		}

		itemsCollection.UpdateOne(
			c,
			bson.M{"_id": outletProduct.ID},
			bson.M{
				"$set": bson.M{
					"stock":      newStock,
					"cost_price": newCost,
					"updated_at": time.Now(),
				},
			},
		)

		// Log history
		history := models.StockHistory{
			ID:          primitive.NewObjectID(),
			LocationID:  &purchase.OutletID,
			ProductID:   outletProduct.ID,
			Type:        "purchase",
			ChangeQty:   item.Quantity,
			Balance:     newStock,
			CostPrice:   item.UnitPrice,
			ReferenceID: purchase.ID,
			CreatedAt:   time.Now(),
		}
		historyCollection.InsertOne(c, history)
	}

	purchase.Status = "received"

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Purchase marked as received",
		Data:    purchase,
	})
}

// CreateSale creates a new sale transaction
func CreateSale(c *gin.Context) {
	var sale models.Sale
	if err := c.ShouldBindJSON(&sale); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	var outletObjID primitive.ObjectID
	var err error

	// Check if user is admin and outlet_id is provided in request
	userRole, roleExists := c.Get("role")
	if roleExists && userRole == "admin" && !sale.OutletID.IsZero() {
		// Admin can specify outlet_id in the request
		outletObjID = sale.OutletID
	} else {
		// Get outlet ID from context for regular users
		outletID, exists := c.Get("outlet_id")
		if !exists || outletID == nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Outlet ID required",
			})
			return
		}

		// Handle outlet_id as string (from JWT) or ObjectID
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = v
		case string:
			if v == "" {
				c.JSON(http.StatusBadRequest, models.APIResponse{
					Success: false,
					Message: "Outlet ID required",
				})
				return
			}
			outletObjID, err = primitive.ObjectIDFromHex(v)
			if err != nil {
				c.JSON(http.StatusBadRequest, models.APIResponse{
					Success: false,
					Message: "Invalid outlet ID format",
				})
				return
			}
		default:
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Invalid outlet ID",
			})
			return
		}
	}

	sale.ID = primitive.NewObjectID()
	sale.SaleDate = time.Now()
	sale.OutletID = outletObjID

	// Get user ID from context
	userID, exists := c.Get("user_id")
	if exists {
		if userObjID, ok := userID.(primitive.ObjectID); ok {
			sale.CreatedBy = userObjID
		} else if userIDStr, ok := userID.(string); ok {
			if id, err := primitive.ObjectIDFromHex(userIDStr); err == nil {
				sale.CreatedBy = id
			}
		}
	}

	// Generate receipt number
	sale.ReceiptNumber = generateReceiptNumber()

	// Validate and update inventory for each item
	productsCollection := database.GetCollection("outlet_items")
	historyCollection := database.GetCollection("stock_history")

	for i, item := range sale.Items {
		// Find this specific product for this outlet
		var product models.Product
		err := productsCollection.FindOne(c, bson.M{
			"location_id": outletObjID,
			"_id":         item.ProductID,
		}).Decode(&product)

		if err != nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Item not found in inventory: " + item.ProductID.Hex(),
			})
			return
		}

		sale.Items[i].ProductName = product.Name
		sale.Items[i].CostPrice = product.CostPrice

		// Decrement stock and get updated balance
		newStock := product.Stock - item.Quantity
		_, err = productsCollection.UpdateOne(
			c,
			bson.M{"_id": product.ID},
			bson.M{
				"$set": bson.M{
					"stock":      newStock,
					"updated_at": time.Now(),
				},
			},
		)

		if err == nil {
			// Log history
			history := models.StockHistory{
				ID:          primitive.NewObjectID(),
				LocationID:  &outletObjID,
				ProductID:   product.ID,
				Type:        "sale",
				ChangeQty:   -item.Quantity,
				Balance:     newStock,
				CostPrice:   product.CostPrice,
				ReferenceID: sale.ID,
				CreatedAt:   time.Now(),
			}
			historyCollection.InsertOne(c, history)
		}
	}

	// Save the sale
	salesCollection := database.GetCollection("sales")
	_, err = salesCollection.InsertOne(c, sale)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating sale",
		})
		return
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Sale created successfully",
		Data:    sale,
	})
}

// GetSales returns list of sales
func GetSales(c *gin.Context) {
	collection := database.GetCollection("sales")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching sales",
		})
		return
	}
	defer cursor.Close(c)

	var sales []models.Sale
	if err = cursor.All(c, &sales); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding sales",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    sales,
	})
}

// GetSaleReceipt returns sale receipt details
func GetSaleReceipt(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid sale ID",
		})
		return
	}

	collection := database.GetCollection("sales")
	var sale models.Sale

	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&sale)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Sale not found",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    sale,
	})
}

// GetOutletSalesReport returns outlet sales report
func GetOutletSalesReport(c *gin.Context) {
	// Generate sales report
	report := map[string]interface{}{
		"total_sales": 0,
		"period":      "daily",
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    report,
	})
}

// GetOutletProfitReport returns outlet profit report
func GetOutletProfitReport(c *gin.Context) {
	// Generate profit report
	report := map[string]interface{}{
		"total_profit": 0,
		"period":       "daily",
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    report,
	})
}

// generateReceiptNumber generates a unique receipt number
func generateReceiptNumber() string {
	return "RCP-" + time.Now().Format("20060102150405")
}

// GetStockHistory returns the stock history for an outlet item
func GetStockHistory(c *gin.Context) {
	itemIDStr := c.Param("id")
	itemID, err := primitive.ObjectIDFromHex(itemIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	collection := database.GetCollection("stock_history")

	// Get outlet ID from context to filter history for this specific outlet
	outletID, exists := c.Get("outlet_id")
	filter := bson.M{"product_id": itemID}

	if exists && outletID != nil {
		var outletObjID primitive.ObjectID
		switch v := outletID.(type) {
		case primitive.ObjectID:
			outletObjID = v
		case string:
			outletObjID, _ = primitive.ObjectIDFromHex(v)
		}
		if !outletObjID.IsZero() {
			filter["location_id"] = outletObjID
		}
	}

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, filter, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching stock history",
		})
		return
	}
	defer cursor.Close(c)

	history := []models.StockHistory{}
	if err = cursor.All(c, &history); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding history",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    history,
	})
}

// CreateExpense records a new store expense
func CreateExpense(c *gin.Context) {
	var expense models.Expense
	if err := c.ShouldBindJSON(&expense); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format: " + err.Error(),
		})
		return
	}

	outletID, exists := c.Get("outlet_id")
	if !exists || outletID == nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Outlet ID required",
		})
		return
	}

	var outletObjID primitive.ObjectID
	switch v := outletID.(type) {
	case primitive.ObjectID:
		outletObjID = v
	case string:
		var err error
		outletObjID, err = primitive.ObjectIDFromHex(v)
		if err != nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Invalid outlet ID format",
			})
			return
		}
	}

	expense.ID = primitive.NewObjectID()
	expense.OutletID = outletObjID
	expense.CreatedAt = time.Now()
	if expense.ExpenseDate.IsZero() {
		expense.ExpenseDate = time.Now()
	}

	// Get user ID from context
	userID, exists := c.Get("user_id")
	if exists {
		if userObjID, ok := userID.(primitive.ObjectID); ok {
			expense.CreatedBy = userObjID
		}
	}

	collection := database.GetCollection("expenses")
	_, err := collection.InsertOne(c, expense)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error recording expense",
		})
		return
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Expense recorded successfully",
		Data:    expense,
	})
}

// GetExpenses returns list of expenses for an outlet
func GetExpenses(c *gin.Context) {
	outletID, exists := c.Get("outlet_id")
	if !exists || outletID == nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Outlet ID required",
		})
		return
	}

	var outletObjID primitive.ObjectID
	switch v := outletID.(type) {
	case primitive.ObjectID:
		outletObjID = v
	case string:
		var err error
		outletObjID, err = primitive.ObjectIDFromHex(v)
		if err != nil {
			c.JSON(http.StatusBadRequest, models.APIResponse{
				Success: false,
				Message: "Invalid outlet ID format",
			})
			return
		}
	}

	collection := database.GetCollection("expenses")
	opts := options.Find().SetSort(bson.M{"expense_date": -1})
	cursor, err := collection.Find(c, bson.M{"outlet_id": outletObjID}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching expenses",
		})
		return
	}
	defer cursor.Close(c)

	expenses := []models.Expense{}
	if err = cursor.All(c, &expenses); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding expenses",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    expenses,
	})
}
