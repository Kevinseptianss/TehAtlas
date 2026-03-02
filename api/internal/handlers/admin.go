package handlers

import (
	"net/http"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GetAdminDashboard returns dashboard statistics with optional date filtering
func GetAdminDashboard(c *gin.Context) {
	startDateStr := c.Query("start_date")
	endDateStr := c.Query("end_date")

	dateFilter := bson.M{}
	if startDateStr != "" && endDateStr != "" {
		startDate, err1 := time.Parse("2006-01-02", startDateStr)
		endDate, err2 := time.Parse("2006-01-02", endDateStr)
		if err1 == nil && err2 == nil {
			endDate = endDate.Add(24*time.Hour - time.Second)
			dateFilter = bson.M{"sale_date": bson.M{"$gte": startDate, "$lte": endDate}}
		}
	}

	outletsCollection := database.GetCollection("outlets")
	outletsCount, _ := outletsCollection.CountDocuments(c, bson.M{})

	productsCollection := database.GetCollection("products")
	totalProductsCount, _ := productsCollection.CountDocuments(c, bson.M{})

	invoicesCollection := database.GetCollection("invoices")
	totalInvoices, _ := invoicesCollection.CountDocuments(c, bson.M{})
	pendingInvoices, _ := invoicesCollection.CountDocuments(c, bson.M{"status": "pending"})

	salesCollection := database.GetCollection("sales")
	var sales []models.Sale
	cursor, err := salesCollection.Find(c, dateFilter, options.Find().SetSort(bson.M{"sale_date": -1}))
	if err == nil {
		cursor.All(c, &sales)
	}

	var totalRevenue float64
	var totalCOGS float64
	productIDs := []primitive.ObjectID{}
	for _, sale := range sales {
		totalRevenue += sale.TotalAmount
		for _, item := range sale.Items {
			productIDs = append(productIDs, item.ProductID)
		}
	}

	cursor, _ = productsCollection.Find(c, bson.M{"_id": bson.M{"$in": productIDs}})
	var products []models.Product
	cursor.All(c, &products)

	costMap := make(map[primitive.ObjectID]float64)
	nameMap := make(map[primitive.ObjectID]string)
	for _, p := range products {
		costMap[p.ID] = p.CostPrice
		nameMap[p.ID] = p.Name
	}

	type EnrichedSaleItem struct {
		models.SaleItem
		ProductName string  `json:"product_name"`
		CostPrice   float64 `json:"cost_price"`
	}
	type EnrichedSale struct {
		models.Sale
		Items []EnrichedSaleItem `json:"items"`
	}

	enrichedSales := make([]EnrichedSale, len(sales))
	for i, sale := range sales {
		enrichedItems := make([]EnrichedSaleItem, len(sale.Items))
		for j, item := range sale.Items {
			itemCost := item.CostPrice
			itemName := item.ProductName
			if itemCost == 0 {
				itemCost = costMap[item.ProductID]
			}
			if itemName == "" {
				itemName = nameMap[item.ProductID]
				if itemName == "" {
					itemName = "Item Terhapus"
				}
			}
			totalCOGS += itemCost * float64(item.Quantity)
			enrichedItems[j] = EnrichedSaleItem{
				SaleItem:    item,
				ProductName: itemName,
				CostPrice:   itemCost,
			}
		}
		enrichedSales[i] = EnrichedSale{
			Sale:  sale,
			Items: enrichedItems,
		}
	}

	grossProfit := totalRevenue - totalCOGS

	// Fetch actual expenses
	expensesCollection := database.GetCollection("expenses")
	expensesCursor, _ := expensesCollection.Find(c, dateFilter)
	var allExpenses []models.Expense
	expensesCursor.All(c, &allExpenses)

	expenses := 0.0
	for _, e := range allExpenses {
		expenses += e.Amount
	}

	netProfit := grossProfit - expenses
	var sellingMargin float64
	if totalRevenue > 0 {
		sellingMargin = (grossProfit / totalRevenue) * 100
	}

	// Build daily revenue for chart
	dailyMap := make(map[string]float64)
	for _, sale := range sales {
		day := sale.SaleDate.Format("2006-01-02")
		dailyMap[day] += sale.TotalAmount
	}
	dailyRevenue := []map[string]interface{}{}
	for day, rev := range dailyMap {
		dailyRevenue = append(dailyRevenue, map[string]interface{}{"date": day, "revenue": rev})
	}

	// Recent sales / purchases / outlets for the list
	recentSales := sales
	if len(recentSales) > 5 {
		recentSales = recentSales[:5]
	}

	purchasesCollection := database.GetCollection("purchases")
	purchaseDateFilter := bson.M{}
	if startDateStr != "" && endDateStr != "" {
		startDate, err1 := time.Parse("2006-01-02", startDateStr)
		endDate, err2 := time.Parse("2006-01-02", endDateStr)
		if err1 == nil && err2 == nil {
			endDate = endDate.Add(24*time.Hour - time.Second)
			purchaseDateFilter = bson.M{"purchase_date": bson.M{"$gte": startDate, "$lte": endDate}}
		}
	}
	var recentPurchases []models.Purchase
	cursor, err = purchasesCollection.Find(c, purchaseDateFilter, options.Find().SetSort(bson.M{"purchase_date": -1}).SetLimit(5))
	if err == nil {
		cursor.All(c, &recentPurchases)
	}
	if recentPurchases == nil {
		recentPurchases = []models.Purchase{}
	}

	outletsList := []models.Outlet{}
	cursor, _ = outletsCollection.Find(c, bson.M{}, options.Find().SetSort(bson.M{"created_at": -1}).SetLimit(4))
	cursor.All(c, &outletsList)

	var totalPurchaseAmount float64
	cursor, _ = purchasesCollection.Find(c, purchaseDateFilter)
	var allPurchases []models.Purchase
	cursor.All(c, &allPurchases)
	for _, p := range allPurchases {
		totalPurchaseAmount += p.TotalAmount
	}

	stats := map[string]interface{}{
		"total_outlets":    outletsCount,
		"total_products":   totalProductsCount,
		"total_sales":      totalRevenue,
		"total_revenue":    totalRevenue,
		"total_purchases":  totalPurchaseAmount,
		"total_invoices":   totalInvoices,
		"pending_invoices": pendingInvoices,
		"total_cogs":       totalCOGS,
		"gross_profit":     grossProfit,
		"net_profit":       netProfit,
		"selling_margin":   sellingMargin,
		"expenses":         expenses,
		"daily_revenue":    dailyRevenue,
		"recent_sales":     enrichedSales, // Include enriched sales for breakdown
		"recent_purchases": recentPurchases,
		"outlets":          outletsList,
		"expenses_list":    allExpenses,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    stats,
	})
}

// GetOutlets returns list of all outlets
func GetOutlets(c *gin.Context) {
	collection := database.GetCollection("outlets")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{}, opts)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching outlets",
		})
		return
	}
	defer cursor.Close(c)

	outlets := []models.Outlet{}
	if err = cursor.All(c, &outlets); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding outlets",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    outlets,
	})
}

// CreateOutlet creates a new outlet
func CreateOutlet(c *gin.Context) {
	var req struct {
		Name            string `json:"name" binding:"required"`
		Address         string `json:"address"`
		Phone           string `json:"phone"`
		CashierUsername string `json:"cashier_username" binding:"required"`
		CashierPassword string `json:"cashier_password" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format or missing required fields",
		})
		return
	}

	usersCollection := database.GetCollection("users")
	count, _ := usersCollection.CountDocuments(c, bson.M{"username": req.CashierUsername})
	if count > 0 {
		c.JSON(http.StatusConflict, models.APIResponse{
			Success: false,
			Message: "Cashier username already exists",
		})
		return
	}

	outletID := primitive.NewObjectID()

	outlet := models.Outlet{
		ID:        outletID,
		Name:      req.Name,
		Address:   req.Address,
		Phone:     req.Phone,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	cashier := models.User{
		ID:        primitive.NewObjectID(),
		Username:  req.CashierUsername,
		Password:  req.CashierPassword,
		Role:      "cashier",
		OutletID:  &outletID,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	collection := database.GetCollection("outlets")
	_, err := collection.InsertOne(c, outlet)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating outlet",
		})
		return
	}

	_, err = usersCollection.InsertOne(c, cashier)
	if err != nil {
		collection.DeleteOne(c, bson.M{"_id": outletID})
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating cashier account",
		})
		return
	}

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Outlet and cashier created successfully",
		Data:    outlet,
	})
}

// UpdateOutlet updates an existing outlet
func UpdateOutlet(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid outlet ID",
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

	updateData["updated_at"] = time.Now()

	collection := database.GetCollection("outlets")
	_, err = collection.UpdateOne(c, bson.M{"_id": objID}, bson.M{"$set": updateData})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error updating outlet",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Outlet updated successfully",
	})
}

// DeleteOutlet deletes an outlet
func DeleteOutlet(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid outlet ID",
		})
		return
	}

	collection := database.GetCollection("outlets")
	_, err = collection.DeleteOne(c, bson.M{"_id": objID})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error deleting outlet",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Outlet deleted successfully",
	})
}

// GetWarehouseStats returns warehouse statistics
func GetWarehouseStats(c *gin.Context) {
	// Aggregate warehouse statistics
	stats := map[string]interface{}{
		"total_items":      0,
		"total_inventory":  0,
		"recent_purchases": 0,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    stats,
	})
}

// GetOutletStats returns statistics for a specific outlet
func GetOutletStats(c *gin.Context) {
	id := c.Param("id")
	objID, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid outlet ID",
		})
		return
	}

	startDateStr := c.Query("start_date")
	endDateStr := c.Query("end_date")

	salesFilter := bson.M{"outlet_id": objID}
	if startDateStr != "" && endDateStr != "" {
		startDate, err1 := time.Parse("2006-01-02", startDateStr)
		endDate, err2 := time.Parse("2006-01-02", endDateStr)
		if err1 == nil && err2 == nil {
			endDate = endDate.Add(24*time.Hour - time.Second)
			salesFilter["sale_date"] = bson.M{"$gte": startDate, "$lte": endDate}
		}
	}

	salesCollection := database.GetCollection("sales")
	cursor, err := salesCollection.Find(c, salesFilter, options.Find().SetSort(bson.M{"sale_date": -1}))
	var sales []models.Sale
	if err == nil {
		cursor.All(c, &sales)
	}

	var totalRevenue float64
	var totalCOGS float64

	productIDs := []primitive.ObjectID{}
	for _, sale := range sales {
		totalRevenue += sale.TotalAmount
		for _, item := range sale.Items {
			productIDs = append(productIDs, item.ProductID)
		}
	}

	productsCollection := database.GetCollection("products")
	cursor, _ = productsCollection.Find(c, bson.M{"_id": bson.M{"$in": productIDs}})
	var products []models.Product
	cursor.All(c, &products)

	costMap := make(map[primitive.ObjectID]float64)
	nameMap := make(map[primitive.ObjectID]string)
	for _, p := range products {
		costMap[p.ID] = p.CostPrice
		nameMap[p.ID] = p.Name
	}

	type EnrichedSaleItem struct {
		models.SaleItem
		ProductName string  `json:"product_name"`
		CostPrice   float64 `json:"cost_price"`
	}
	type EnrichedSale struct {
		models.Sale
		Items []EnrichedSaleItem `json:"items"`
	}

	enrichedSales := make([]EnrichedSale, len(sales))
	for i, sale := range sales {
		enrichedItems := make([]EnrichedSaleItem, len(sale.Items))
		for j, item := range sale.Items {
			itemCost := item.CostPrice
			itemName := item.ProductName
			if itemCost == 0 {
				itemCost = costMap[item.ProductID]
			}
			if itemName == "" {
				itemName = nameMap[item.ProductID]
				if itemName == "" {
					itemName = "Item Terhapus"
				}
			}
			totalCOGS += itemCost * float64(item.Quantity)
			enrichedItems[j] = EnrichedSaleItem{
				SaleItem:    item,
				ProductName: itemName,
				CostPrice:   itemCost,
			}
		}
		enrichedSales[i] = EnrichedSale{
			Sale:  sale,
			Items: enrichedItems,
		}
	}

	grossProfit := totalRevenue - totalCOGS

	// Fetch actual expenses for this outlet
	expensesCollection := database.GetCollection("expenses")
	expenseFilter := bson.M{"outlet_id": objID}
	if startDateStr != "" && endDateStr != "" {
		startDate, err1 := time.Parse("2006-01-02", startDateStr)
		endDate, err2 := time.Parse("2006-01-02", endDateStr)
		if err1 == nil && err2 == nil {
			endDate = endDate.Add(24*time.Hour - time.Second)
			expenseFilter["expense_date"] = bson.M{"$gte": startDate, "$lte": endDate}
		}
	}

	expensesCursor, _ := expensesCollection.Find(c, expenseFilter)
	var allExpenses []models.Expense
	expensesCursor.All(c, &allExpenses)

	expenses := 0.0
	for _, e := range allExpenses {
		expenses += e.Amount
	}

	netProfit := grossProfit - expenses

	var sellingMargin float64
	if totalRevenue > 0 {
		sellingMargin = (grossProfit / totalRevenue) * 100
	}

	dailyMap := make(map[string]float64)
	for _, sale := range sales {
		day := sale.SaleDate.Format("2006-01-02")
		dailyMap[day] += sale.TotalAmount
	}
	dailyRevenue := []map[string]interface{}{}
	for day, rev := range dailyMap {
		dailyRevenue = append(dailyRevenue, map[string]interface{}{"date": day, "revenue": rev})
	}

	outletIDStr := objID.Hex()
	stockUpdateKey := "outlet_stock." + outletIDStr
	cursor, _ = productsCollection.Find(c, bson.M{stockUpdateKey: bson.M{"$exists": true}})
	var allOutletItems []models.Product
	cursor.All(c, &allOutletItems)

	stats := map[string]interface{}{
		"outlet_id":      id,
		"revenue":        totalRevenue,
		"cogs":           totalCOGS,
		"gross_profit":   grossProfit,
		"expenses":       expenses,
		"net_profit":     netProfit,
		"selling_margin": sellingMargin,
		"daily_revenue":  dailyRevenue,
		"total_sales":    len(sales),
		"outlet_items":   allOutletItems,
		"sales_list":     enrichedSales,
		"expenses_list":  allExpenses,
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    stats,
	})
}

// GetSalesReports returns sales reports
func GetSalesReports(c *gin.Context) {
	// Generate sales reports
	reports := map[string]interface{}{
		"total_sales": 0,
		"period":      "monthly",
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    reports,
	})
}

// GetProfitReports returns profit reports
func GetProfitReports(c *gin.Context) {
	// Generate profit reports
	reports := map[string]interface{}{
		"total_profit": 0,
		"period":       "monthly",
	}
	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    reports,
	})
}

// GetAdminStockHistory returns stock history for any item
func GetAdminStockHistory(c *gin.Context) {
	itemIDStr := c.Param("id")
	itemID, err := primitive.ObjectIDFromHex(itemIDStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid item ID",
		})
		return
	}

	collection := database.GetCollection("outlet_stock_history")

	opts := options.Find().SetSort(bson.M{"created_at": -1})
	cursor, err := collection.Find(c, bson.M{"item_id": itemID}, opts)
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
