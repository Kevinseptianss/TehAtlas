package models

import (
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// User model
type User struct {
	ID        string    `json:"id,omitempty" bson:"_id,omitempty"`
	Username  string    `json:"username" bson:"username"`
	Password  string    `json:"-" bson:"password"`
	Role      string    `json:"role" bson:"role"`
	OutletID  *string   `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	CreatedAt time.Time `json:"created_at" bson:"created_at"`
	UpdatedAt time.Time `json:"updated_at" bson:"updated_at"`
}

// OutletItem model
type OutletItem struct {
	ItemID    string    `json:"item_id"`
	Name      string    `json:"name"`
	Stock     int       `json:"stock"`
	MinStock  int       `json:"min_stock"`
	Unit      string    `json:"unit"`
	Category  string    `json:"category"`
	UpdatedAt time.Time `json:"updated_at"`
}

// InventoryTransaction model
type InventoryTransaction struct {
	ID              string     `json:"id,omitempty" bson:"_id,omitempty"`
	ItemID          string     `json:"item_id" bson:"item_id"`
	ItemName        string     `json:"item_name" bson:"item_name"`
	TransactionType string     `json:"transaction_type" bson:"transaction_type"`
	Quantity        int        `json:"quantity" bson:"quantity"`
	PreviousStock   int        `json:"previous_stock" bson:"previous_stock"`
	NewStock        int        `json:"new_stock" bson:"new_stock"`
	Reason          *string    `json:"reason,omitempty" bson:"reason,omitempty"`
	BatchID         *string    `json:"batch_id,omitempty" bson:"batch_id,omitempty"`
	Supplier        *string    `json:"supplier,omitempty" bson:"supplier,omitempty"`
	UnitCost        *float64   `json:"unit_cost,omitempty" bson:"unit_cost,omitempty"`
	TotalCost       *float64   `json:"total_cost,omitempty" bson:"total_cost,omitempty"`
	OutletID        *string    `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	UserID          string     `json:"user_id" bson:"user_id"`
	TransactionDate time.Time  `json:"transaction_date" bson:"transaction_date"`
}

// InventoryBatch model
type InventoryBatch struct {
	ID           string     `json:"id,omitempty" bson:"_id,omitempty"`
	ItemID       string     `json:"item_id" bson:"item_id"`
	ItemName     string     `json:"item_name" bson:"item_name"`
	BatchNumber  string     `json:"batch_number" bson:"batch_number"`
	Quantity     int        `json:"quantity" bson:"quantity"`
	RemainingQty int        `json:"remaining_quantity" bson:"remaining_quantity"`
	ExpiryDate   *time.Time `json:"expiry_date,omitempty" bson:"expiry_date,omitempty"`
	Supplier     string     `json:"supplier" bson:"supplier"`
	UnitCost     float64    `json:"unit_cost" bson:"unit_cost"`
	OutletID     *string    `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	ReceivedDate time.Time  `json:"received_date" bson:"received_date"`
}

// PurchaseOrder model
type PurchaseOrder struct {
	ID           string         `json:"id,omitempty" bson:"_id,omitempty"`
	SupplierName string         `json:"supplier_name" bson:"supplier_name"`
	OrderNumber  string         `json:"order_number" bson:"order_number"`
	Items        []PurchaseItem `json:"items" bson:"items"`
	TotalAmount  float64        `json:"total_amount" bson:"total_amount"`
	OrderDate    time.Time      `json:"order_date" bson:"order_date"`
	ExpectedDate *time.Time     `json:"expected_date,omitempty" bson:"expected_date,omitempty"`
	Status       string         `json:"status" bson:"status"`
	OutletID     *string        `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	UserID       string         `json:"user_id" bson:"user_id"`
	CreatedAt    time.Time      `json:"created_at" bson:"created_at"`
	UpdatedAt    time.Time      `json:"updated_at" bson:"updated_at"`
}

// PurchaseItem model
type PurchaseItem struct {
	ItemName  string  `json:"item_name"`
	Quantity  int     `json:"quantity"`
	UnitCost  float64 `json:"unit_cost"`
	TotalCost float64 `json:"total_cost"`
}

// API Request/Response models
type APIResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

type LoginRequest struct {
	Username string `json:"username" validate:"required"`
	Password string `json:"password" validate:"required"`
}

type LoginResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}

// Inventory API models
type AddStockRequest struct {
	ItemID     string  `json:"item_id" validate:"required"`
	Quantity   int     `json:"quantity" validate:"required,min=1"`
	Supplier   string  `json:"supplier" validate:"required"`
	UnitCost   float64 `json:"unit_cost" validate:"required,min=0"`
	BatchID    *string `json:"batch_id,omitempty"`
	ExpiryDate *string `json:"expiry_date,omitempty"`
}

type AdjustStockRequest struct {
	ItemID    string `json:"item_id" validate:"required"`
	Adjustment int    `json:"adjustment" validate:"required"`
	Reason     string `json:"reason" validate:"required"`
}

type PurchaseOrderRequest struct {
	SupplierName string         `json:"supplier_name" validate:"required"`
	OrderNumber  string         `json:"order_number" validate:"required"`
	Items        []PurchaseItem `json:"items" validate:"required,min=1"`
	TotalAmount  float64        `json:"total_amount" validate:"required,min=0"`
	ExpectedDate *time.Time     `json:"expected_date,omitempty"`
}

// Dashboard models
type OutletDashboard struct {
	TotalItems       int     `json:"total_items"`
	LowStockItems    int     `json:"low_stock_items"`
	TotalSales       float64 `json:"total_sales"`
	TodaySales       float64 `json:"today_sales"`
	RecentTransactions []Sale `json:"recent_transactions"`
}

// SaleItem represents an item in a sale
type SaleItem struct {
	ItemID   string  `json:"item_id" bson:"item_id"`
	ItemName string  `json:"item_name" bson:"item_name"`
	Quantity int     `json:"quantity" bson:"quantity"`
	UnitPrice float64 `json:"unit_price" bson:"unit_price"`
	Subtotal float64 `json:"subtotal" bson:"subtotal"`
}

type Sale struct {
	ID       string `json:"id,omitempty" bson:"_id,omitempty"`
	Items    []SaleItem `json:"items" bson:"items"`
	Total    float64   `json:"total" bson:"total"`
	Date     time.Time `json:"date" bson:"date"`
	UserID   string    `json:"user_id" bson:"user_id"`
	OutletID *string   `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
}

type SaleRequest struct {
	Items []SaleItem `json:"items" validate:"required,min=1"`
	Total float64    `json:"total" validate:"required,min=0"`
}

type SaleReceipt struct {
	SaleID string    `json:"sale_id"`
	Total  float64   `json:"total"`
	Date   time.Time `json:"date"`
}

type WarehouseItem struct {
	ItemID   string `json:"item_id"`
	Name     string `json:"name"`
	Stock    int    `json:"stock"`
	Unit     string `json:"unit"`
	Category string `json:"category"`
}

type WarehouseDashboard struct {
	TotalItems        int     `json:"total_items"`
	LowStockItems     int     `json:"low_stock_items"`
	PendingTransfers  int     `json:"pending_transfers"`
	TotalStockValue   float64 `json:"total_stock_value"`
}

// TransferItem represents an item in transfers
type TransferItem struct {
	ItemID   string `json:"item_id" bson:"item_id"`
	Quantity int    `json:"quantity" bson:"quantity"`
}

type Transfer struct {
	ID           string `json:"id,omitempty" bson:"_id,omitempty"`
	FromOutletID string `json:"from_outlet_id" bson:"from_outlet_id"`
	ToOutletID   string `json:"to_outlet_id" bson:"to_outlet_id"`
	Status       string `json:"status" bson:"status"`
	Items        []TransferItem `json:"items" bson:"items"`
	UserID       string `json:"user_id" bson:"user_id"`
	CreatedAt    time.Time `json:"created_at" bson:"created_at"`
}

type TransferRequest struct {
	FromOutletID string `json:"from_outlet_id"`
	ToOutletID   string `json:"to_outlet_id"`
	Items        []TransferItem `json:"items"`
}

type Outlet struct {
	ID      string `json:"id,omitempty" bson:"_id,omitempty"`
	Name    string `json:"name" bson:"name"`
	Address string `json:"address" bson:"address"`
	CreatedAt time.Time `json:"created_at" bson:"created_at"`
}

type OutletRequest struct {
	Name    string `json:"name"`
	Address string `json:"address"`
}

type AdminDashboard struct {
	TotalOutlets     int     `json:"total_outlets"`
	TotalUsers       int     `json:"total_users"`
	TotalSales       float64 `json:"total_sales"`
	MonthlyRevenue   float64 `json:"monthly_revenue"`
}

type UserRequest struct {
	Username string  `json:"username"`
	Password string  `json:"password"`
	Role     string  `json:"role"`
	OutletID *string `json:"outlet_id,omitempty"`
}

type Supplier struct {
	Name    string `json:"name"`
	Contact string `json:"contact"`
}

type SupplierRequest struct {
	Name    string `json:"name"`
	Contact string `json:"contact"`
}

type Item struct {
	Name     string `json:"name"`
	Category string `json:"category"`
}

type StockTransaction struct {
	ItemName string    `json:"item_name"`
	Type     string    `json:"type"`
	Quantity int       `json:"quantity"`
	Date     time.Time `json:"date"`
}

// JWT Claims
type Claims struct {
	UserID   string `json:"user_id"`
	Username string `json:"username"`
	Role     string `json:"role"`
	OutletID string `json:"outlet_id"`
	jwt.RegisteredClaims
}