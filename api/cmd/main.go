package main

import (
	"log"

	"business-management-api/internal/config"
	"business-management-api/internal/database"
	"business-management-api/internal/handlers"
	"business-management-api/internal/middleware"

	"github.com/gin-gonic/gin"
)

func main() {
	// Load configuration
	cfg := config.Load()

	// Set JWT secret from config
	handlers.SetJWTSecret([]byte(cfg.JWTSecret))
	middleware.SetJWTSecret([]byte(cfg.JWTSecret))

	// Connect to database
	if err := database.Connect(cfg.MongoURI); err != nil {
		log.Fatal("Failed to connect to database:", err)
	}
	defer database.Disconnect()

	// Initialize Gin router
	r := gin.Default()

	// CORS middleware
	r.Use(func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	})

	// Public routes
	r.POST("/api/auth/login", handlers.Login)
	r.POST("/api/auth/logout", handlers.Logout)
	r.GET("/api/health", handlers.HealthCheck)
	r.GET("/api/app/version", handlers.GetLatestVersion)

	// Protected routes
	auth := r.Group("/api")
	auth.Use(middleware.AuthMiddleware())
	{
		// Auth
		auth.GET("/auth/me", handlers.GetMe)

		// Admin routes
		admin := auth.Group("/admin")
		admin.Use(middleware.RoleMiddleware("admin"))
		{
			admin.GET("/dashboard", handlers.GetAdminDashboard)
			admin.POST("/outlets", handlers.CreateOutlet)
			admin.PUT("/outlets/:id", handlers.UpdateOutlet)
			admin.DELETE("/outlets/:id", handlers.DeleteOutlet)
			admin.GET("/warehouse/stats", handlers.GetWarehouseStats)
			admin.GET("/outlets/:id/stats", handlers.GetOutletStats)
			admin.GET("/reports/sales", handlers.GetSalesReports)
			admin.GET("/reports/profits", handlers.GetProfitReports)

			// User management
			admin.GET("/users", handlers.GetUsers)
			admin.POST("/users", handlers.CreateUser)
			admin.PUT("/users/:id", handlers.UpdateUser)
			admin.DELETE("/users/:id", handlers.DeleteUser)

			// Analytics endpoints
			admin.GET("/analytics/outlets/overview", handlers.GetAdminDashboard) // Placeholder
			admin.GET("/analytics/outlets/:id/performance", handlers.GetOutletStats)
			admin.GET("/analytics/outlets/:id/growth", handlers.GetOutletStats)
			admin.GET("/analytics/outlets/:id/inventory", handlers.GetOutletStats)
			admin.GET("/analytics/outlets/:id/sales-breakdown", handlers.GetOutletSalesReport)
			admin.GET("/analytics/outlets/:id/profit-margins", handlers.GetOutletProfitReport)
			admin.GET("/analytics/outlets/comparison", handlers.GetAdminDashboard)
			admin.GET("/analytics/outlets/top-performers", handlers.GetAdminDashboard)
			admin.GET("/analytics/trends", handlers.GetAdminDashboard)

			// App version management
			admin.POST("/app/version", handlers.CreateAppVersion)
		}

		// Warehouse routes
		warehouse := auth.Group("/warehouse")
		warehouse.Use(middleware.RoleMiddleware("admin", "warehouse"))
		{
			warehouse.GET("/dashboard", handlers.GetWarehouseDashboard)
			warehouse.GET("/items", handlers.GetWarehouseItems)
			warehouse.POST("/items", handlers.CreateWarehouseItem)
			warehouse.PUT("/items/:id", handlers.UpdateWarehouseItem)
			warehouse.DELETE("/items/:id", handlers.DeleteWarehouseItem)
			warehouse.GET("/purchases", handlers.GetPurchases)
			warehouse.POST("/purchases", handlers.CreatePurchase)
			warehouse.PUT("/purchases/:id/status", handlers.UpdatePurchaseStatus)
			// Custom endpoints
			warehouse.GET("/next-invoice-number", handlers.GetNextInvoiceNumber)
			warehouse.GET("/stock-history/:id", handlers.GetWarehouseStockHistory)
			warehouse.GET("/invoices", handlers.GetInvoices)
			warehouse.POST("/invoices", handlers.CreateInvoice)
			warehouse.GET("/invoices/:id", handlers.GetInvoiceDetails)
			warehouse.GET("/reports/sales", handlers.GetWarehouseSalesReport)
			warehouse.GET("/reports/profits", handlers.GetWarehouseProfitReport)
		}

		// Outlet routes
		outlet := auth.Group("/outlet")
		outlet.Use(middleware.RoleMiddleware("admin", "cashier"))
		{
			outlet.GET("/dashboard", handlers.GetOutletDashboard)
			outlet.GET("/items", handlers.GetOutletItems)
			outlet.POST("/items", handlers.CreateOutletItem)
			outlet.PUT("/items/:id", handlers.UpdateOutletItem)
			outlet.DELETE("/items/:id", handlers.DeleteOutletItem)
			outlet.POST("/purchases", handlers.CreateOutletPurchase)
			outlet.GET("/purchases", handlers.GetOutletPurchases)
			outlet.PUT("/purchases/:id/receive", handlers.ReceivePurchase)
			outlet.POST("/sales", handlers.CreateSale)
			outlet.GET("/sales", handlers.GetSales)
			outlet.GET("/sales/:id", handlers.GetSaleReceipt)
			outlet.GET("/reports/sales", handlers.GetOutletSalesReport)
			outlet.GET("/reports/profits", handlers.GetOutletProfitReport)
			outlet.GET("/stock-history/:id", handlers.GetStockHistory)
			outlet.POST("/expenses", handlers.CreateExpense)
			outlet.GET("/expenses", handlers.GetExpenses)
		}

		// Common routes
		auth.GET("/items/search", handlers.SearchItems)
		auth.GET("/outlets", handlers.GetOutlets)
		auth.GET("/items/:id", handlers.GetItemDetails)
		auth.GET("/stock/warehouse/:item_id", handlers.GetWarehouseStock)
		auth.GET("/stock/outlet/:item_id", handlers.GetOutletStock)
		auth.GET("/stock/transactions", handlers.GetInventoryTransactions)
	}

	// Start server
	log.Printf("Server starting on port %s", cfg.Port)
	if err := r.Run(":" + cfg.Port); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
