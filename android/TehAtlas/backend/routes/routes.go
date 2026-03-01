package routes

import (
	"net/http"

	"github.com/kevinseptian/tehatlas-backend/handlers"
	"github.com/kevinseptian/tehatlas-backend/middleware"
)

func SetupRoutes() *http.ServeMux {
	mux := http.NewServeMux()

	// Public routes
	mux.HandleFunc("/api/auth/login", handlers.Login)

	// Public search and reporting routes (authenticated)
	mux.Handle("/api/items/search", middleware.AuthMiddleware(http.HandlerFunc(handlers.SearchItems)))
	mux.Handle("/api/stock/transactions", middleware.AuthMiddleware(http.HandlerFunc(handlers.GetStockTransactions)))

	// Protected routes with middleware
	mux.Handle("/api/outlet/items", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetOutletItems), "admin", "cashier")))
	mux.Handle("/api/outlet/sales", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreateSale), "admin", "cashier")))
	mux.Handle("/api/outlet/sales/", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetSales), "admin", "cashier")))
	mux.Handle("/api/outlet/sales/receipt/", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetSaleReceipt), "admin", "cashier")))
	mux.Handle("/api/outlet/dashboard", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetOutletDashboard), "admin", "cashier")))

	// Inventory management routes for cashiers
	mux.Handle("/api/outlet/inventory/add", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.AddOutletStock), "admin", "cashier")))
	mux.Handle("/api/outlet/inventory/adjust", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.AdjustOutletStock), "admin", "cashier")))
	mux.Handle("/api/outlet/purchases", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreatePurchaseOrder), "admin", "cashier")))
	mux.Handle("/api/outlet/inventory/transactions", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetOutletInventoryTransactions), "admin", "cashier")))
	mux.Handle("/api/outlet/inventory/items", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetOutletInventoryItems), "admin", "cashier")))

	// Warehouse routes
	mux.Handle("/api/warehouse/items", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetWarehouseItems), "admin", "warehouse")))
	mux.Handle("/api/warehouse/dashboard", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetWarehouseDashboard), "admin", "warehouse")))
	mux.Handle("/api/warehouse/transfers", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreateTransfer), "admin", "warehouse")))
	mux.Handle("/api/warehouse/transfers/", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetTransfers), "admin", "cashier")))
	mux.Handle("/api/warehouse/transfers/receive/", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.ReceiveTransfer), "admin", "cashier")))

	// Admin routes
	mux.Handle("/api/admin/clear-database", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.ClearDatabase), "admin")))
	mux.Handle("/api/admin/outlets", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetOutlets), "admin")))
	mux.Handle("/api/admin/outlets/create", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreateOutlet), "admin")))
	mux.Handle("/api/admin/users", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetUsers), "admin")))
	mux.Handle("/api/admin/users/create", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreateUser), "admin")))
	mux.Handle("/api/admin/dashboard", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetAdminDashboard), "admin")))
	mux.Handle("/api/admin/suppliers", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetSuppliers), "admin")))
	mux.Handle("/api/admin/suppliers/create", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.CreateSupplier), "admin")))
	mux.Handle("/api/admin/inventory-transactions", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetAllInventoryTransactions), "admin")))
	mux.Handle("/api/admin/sales", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetAllSales), "admin")))
	mux.Handle("/api/admin/transfers", middleware.AuthMiddleware(middleware.RoleMiddleware(http.HandlerFunc(handlers.GetAllTransfers), "admin")))

	return mux
}