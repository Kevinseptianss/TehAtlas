package handlers

import (
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GetWarehouseDashboard returns warehouse dashboard data
func GetWarehouseDashboard(c *gin.Context) {
	// Get pending invoices count
	invoicesCollection := database.GetCollection("invoices")
	pendingInvoices, err := invoicesCollection.CountDocuments(c, bson.M{"status": "pending"})
	if err != nil {
		pendingInvoices = 0
	}

	// Get today's sales from invoices (assuming invoices represent sales to outlets)
	today := time.Now().Truncate(24 * time.Hour)
	tomorrow := today.Add(24 * time.Hour)

	todaySalesPipeline := mongo.Pipeline{
		{{"$match", bson.M{
			"invoice_date": bson.M{"$gte": today, "$lt": tomorrow},
			"status":       bson.M{"$ne": "cancelled"},
		}}},
		{{"$group", bson.M{
			"_id":           nil,
			"today_sales":   bson.M{"$sum": "$total_amount"},
			"invoice_count": bson.M{"$sum": 1},
		}}},
	}

	cursor, err := invoicesCollection.Aggregate(c, todaySalesPipeline)
	todaySales := 0.0
	todayInvoiceCount := 0

	if err == nil {
		var result struct {
			TodaySales   float64 `bson:"today_sales"`
			InvoiceCount int     `bson:"invoice_count"`
		}
		if cursor.Next(c) {
			cursor.Decode(&result)
			todaySales = result.TodaySales
			todayInvoiceCount = result.InvoiceCount
		}
		cursor.Close(c)
	}

	// Get low stock alerts (items with quantity < 10, for example)
	// For now, we'll check products and assume low stock is < 10
	// In a real implementation, this would be configurable
	productsCollection := database.GetCollection("products")
	lowStockCount, err := productsCollection.CountDocuments(c, bson.M{"warehouse_stock": bson.M{"$lt": 10}})
	if err != nil {
		lowStockCount = 0
	}

	// Get total products count
	totalItems, err := productsCollection.CountDocuments(c, bson.M{})
	if err != nil {
		totalItems = 0
	}

	// Get pending purchases count
	purchasesCollection := database.GetCollection("purchases")
	pendingPurchases, err := purchasesCollection.CountDocuments(c, bson.M{"status": "pending"})
	if err != nil {
		pendingPurchases = 0
	}

	dashboard := map[string]interface{}{
		"today_sales":       todaySales,
		"today_invoices":    todayInvoiceCount,
		"pending_invoices":  pendingInvoices,
		"pending_purchases": pendingPurchases,
		"low_stock_alerts":  lowStockCount,
		"total_items":       totalItems,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    dashboard,
	})
}

// GetWarehouseItems returns list of warehouse items
func GetWarehouseItems(c *gin.Context) {
	collection := database.GetCollection("products")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{}, opts)
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

// CreateWarehouseItem creates a new warehouse item
func CreateWarehouseItem(c *gin.Context) {
	var item models.Product
	if err := c.ShouldBindJSON(&item); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	item.ID = primitive.NewObjectID()
	item.CreatedAt = time.Now()
	item.UpdatedAt = time.Now()
	if item.OutletStock == nil {
		item.OutletStock = make(map[string]int)
	}

	collection := database.GetCollection("products")
	_, err := collection.InsertOne(c, item)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating item",
		})
		return
	}

	// Log initial stock history
	if item.WarehouseStock > 0 {
		historyCollection := database.GetCollection("stock_history")
		history := models.StockHistory{
			ID:        primitive.NewObjectID(),
			ProductID: item.ID,
			Type:      "adjustment",
			ChangeQty: item.WarehouseStock,
			Balance:   item.WarehouseStock,
			CostPrice: item.CostPrice,
			CreatedAt: time.Now(),
		}
		historyCollection.InsertOne(c, history)
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Item created successfully",
		Data:    item,
	})
}

// UpdateWarehouseItem updates a warehouse item
func UpdateWarehouseItem(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	var updateData map[string]interface{}
	if err := c.ShouldBindJSON(&updateData); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	collection := database.GetCollection("products") // Fixed collection name

	// Check if warehouse_stock is being updated to log history
	if newStockVal, ok := updateData["warehouse_stock"]; ok {
		var product models.Product
		err := collection.FindOne(c, bson.M{"_id": objID}).Decode(&product)
		if err == nil {
			var newStock int
			switch v := newStockVal.(type) {
			case float64:
				newStock = int(v)
			case int32:
				newStock = int(v)
			case int:
				newStock = v
			}

			changeQty := newStock - product.WarehouseStock
			if changeQty != 0 {
				historyCollection := database.GetCollection("stock_history")
				history := models.StockHistory{
					ID:        primitive.NewObjectID(),
					ProductID: objID,
					Type:      "adjustment",
					ChangeQty: changeQty,
					Balance:   newStock,
					CostPrice: product.CostPrice,
					CreatedAt: time.Now(),
				}
				historyCollection.InsertOne(c, history)
			}
		}
	}

	updateData["updated_at"] = time.Now()
	_, err = collection.UpdateOne(c, bson.M{"_id": objID}, bson.M{"$set": updateData})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error updating item",
		})
		return
	}

	var product models.Product
	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&product)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching updated item",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Item updated successfully",
		Data:    product,
	})
}

// DeleteWarehouseItem deletes a warehouse item
func DeleteWarehouseItem(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	collection := database.GetCollection("warehouse_items")
	_, err = collection.DeleteOne(c, bson.M{"_id": objID})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error deleting item",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Item deleted successfully",
	})
}

// GetPurchases returns list of purchases
func GetPurchases(c *gin.Context) {
	collection := database.GetCollection("purchases")

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

	purchases := []models.Purchase{}
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

// CreatePurchase creates a new purchase order
func CreatePurchase(c *gin.Context) {
	var purchase models.Purchase
	if err := c.ShouldBindJSON(&purchase); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	purchase.ID = primitive.NewObjectID()
	purchase.PurchaseDate = time.Now()

	collection := database.GetCollection("purchases")
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

// UpdatePurchaseStatus updates purchase status
func UpdatePurchaseStatus(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid purchase ID",
		})
		return
	}

	var req struct {
		Status string `json:"status" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	collection := database.GetCollection("purchases")

	var purchase models.Purchase
	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&purchase)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Purchase not found",
		})
		return
	}

	if req.Status == "received" && purchase.Status == "pending" {
		productsCollection := database.GetCollection("products")
		historyCollection := database.GetCollection("stock_history")

		for _, item := range purchase.Items {
			var product models.Product
			err := productsCollection.FindOne(c, bson.M{"_id": item.ProductID}).Decode(&product)
			if err == nil {
				oldStock := float64(product.WarehouseStock)
				oldCost := product.CostPrice
				newAddedStock := float64(item.Quantity)
				newTotalCost := item.TotalCost

				newQuantity := product.WarehouseStock + item.Quantity

				var newCostPrice float64
				if oldStock+newAddedStock > 0 {
					newCostPrice = ((oldStock * oldCost) + newTotalCost) / (oldStock + newAddedStock)
				} else {
					newCostPrice = item.UnitCost
				}

				productsCollection.UpdateOne(
					c,
					bson.M{"_id": item.ProductID},
					bson.M{
						"$set": bson.M{
							"warehouse_stock": newQuantity,
							"cost_price":      newCostPrice,
							"updated_at":      time.Now(),
						},
					},
				)

				history := models.StockHistory{
					ID:          primitive.NewObjectID(),
					ProductID:   item.ProductID,
					Type:        "purchase",
					ChangeQty:   item.Quantity,
					Balance:     newQuantity,
					CostPrice:   item.UnitCost,
					ReferenceID: objID,
					CreatedAt:   time.Now(),
				}
				historyCollection.InsertOne(c, history)
			}
		}
	}

	_, err = collection.UpdateOne(c, bson.M{"_id": objID}, bson.M{"$set": bson.M{"status": req.Status}})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error updating purchase status",
		})
		return
	}

	var updatedPurchase models.Purchase
	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&updatedPurchase)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching updated purchase",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Purchase status updated successfully",
		Data:    updatedPurchase,
	})
}

// GetInvoices returns list of invoices
func GetInvoices(c *gin.Context) {
	collection := database.GetCollection("invoices")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching invoices",
		})
		return
	}
	defer cursor.Close(c)

	invoices := []models.Invoice{}
	if err = cursor.All(c, &invoices); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding invoices",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    invoices,
	})
}

// CreateInvoice creates a new invoice for outlet
func CreateInvoice(c *gin.Context) {
	var invoice models.Invoice
	if err := c.ShouldBindJSON(&invoice); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	invoice.ID = primitive.NewObjectID()
	invoice.InvoiceDate = time.Now()
	invoice.DueDate = time.Now().AddDate(0, 0, 30) // 30 days due
	invoice.Status = "pending"

	collection := database.GetCollection("invoices")
	_, err := collection.InsertOne(c, invoice)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating invoice",
		})
		return
	}

	// Deduct stock from products and construct OutletPurchase synced Items
	productsCollection := database.GetCollection("products")

	var outletPurchaseItems []models.OutletPurchaseItem

	for _, item := range invoice.Items {
		var product models.Product
		err := productsCollection.FindOne(c, bson.M{"_id": item.ProductID}).Decode(&product)
		if err == nil {
			newQuantity := product.WarehouseStock - item.Quantity
			if newQuantity < 0 {
				newQuantity = 0 // Prevent negative stock
			}

			productsCollection.UpdateOne(
				c,
				bson.M{"_id": item.ProductID},
				bson.M{
					"$set": bson.M{
						"warehouse_stock": newQuantity,
						"updated_at":      time.Now(),
					},
				},
			)

			// Log stock history for warehouse deduction
			historyCollection := database.GetCollection("stock_history")
			history := models.StockHistory{
				ID:          primitive.NewObjectID(),
				ProductID:   item.ProductID,
				Type:        "invoice",
				ChangeQty:   -item.Quantity,
				Balance:     newQuantity,
				CostPrice:   product.CostPrice,
				ReferenceID: invoice.ID,
				CreatedAt:   time.Now(),
			}
			historyCollection.InsertOne(c, history)

			// Append to the synced Outlet Purchase array
			outletPurchaseItems = append(outletPurchaseItems, models.OutletPurchaseItem{
				ProductID:   item.ProductID,
				ProductName: product.Name,
				Quantity:    item.Quantity,
				UnitPrice:   item.UnitPrice,
				TotalPrice:  item.TotalPrice,
			})
		}
	}

	// ----------------------------------------------------
	// AUTOMATIC OUTLET PURCHASE GENERATION
	// ----------------------------------------------------
	log.Printf("Automatically generating OutletPurchase for Invoice %v (Outlet: %v)", invoice.ID, invoice.OutletID)
	outletPurchasesCollection := database.GetCollection("outlet_purchases")
	outletPurchase := models.OutletPurchase{
		ID:           primitive.NewObjectID(),
		InvoiceID:    invoice.ID,
		OutletID:     invoice.OutletID,
		Items:        outletPurchaseItems,
		TotalAmount:  invoice.TotalAmount,
		PurchaseDate: invoice.InvoiceDate,
		Status:       "pending",
		Notes:        "Auto-generated from Warehouse Invoice",
	}
	_, err = outletPurchasesCollection.InsertOne(c, outletPurchase)
	if err != nil {
		log.Printf("Failed to generate OutletPurchase dynamically from CreateInvoice: %v", err)
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Invoice created successfully",
		Data:    invoice,
	})
}

// GetInvoiceDetails returns invoice details
func GetInvoiceDetails(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid invoice ID",
		})
		return
	}

	collection := database.GetCollection("invoices")
	var invoice models.Invoice

	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&invoice)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "Invoice not found",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    invoice,
	})
}

// GetWarehouseSalesReport returns warehouse sales report
func GetWarehouseSalesReport(c *gin.Context) {
	// Generate sales report
	report := map[string]interface{}{
		"total_sales": 0,
		"period":      "monthly",
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    report,
	})
}

// GetWarehouseProfitReport returns warehouse profit report
func GetWarehouseProfitReport(c *gin.Context) {
	// Generate profit report
	report := map[string]interface{}{
		"total_profit": 0,
		"period":       "monthly",
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    report,
	})
}

// GetWarehouseStockHistory returns the stock history for a warehouse item
func GetWarehouseStockHistory(c *gin.Context) {
	productIDStr := c.Param("id")
	productID, err := primitive.ObjectIDFromHex(productIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid product ID",
		})
		return
	}

	collection := database.GetCollection("stock_history")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{"product_id": productID}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching stock history",
		})
		return
	}
	defer cursor.Close(c)

	var history []models.StockHistory
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

// GetNextInvoiceNumber returns the next suggested invoice number
func GetNextInvoiceNumber(c *gin.Context) {
	collection := database.GetCollection("invoices")

	// Find the latest invoice by created_at
	opts := options.FindOne().SetSort(bson.M{"created_at": -1})
	var lastInvoice models.Invoice
	err := collection.FindOne(c, bson.M{}, opts).Decode(&lastInvoice)

	nextNumber := "INV-001"
	if err == nil && lastInvoice.InvoiceNumber != "" {
		// Try to parse the last number part
		// Format: INV-XXX
		parts := strings.Split(lastInvoice.InvoiceNumber, "-")
		if len(parts) >= 2 {
			prefix := parts[0]
			numStr := parts[len(parts)-1]
			num, err := strconv.Atoi(numStr)
			if err == nil {
				nextNumber = fmt.Sprintf("%s-%03d", prefix, num+1)
			}
		}
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    nextNumber,
	})
}
