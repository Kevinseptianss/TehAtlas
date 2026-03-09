package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"
)

// User represents a system user
type User struct {
	ID        primitive.ObjectID  `json:"id" bson:"_id,omitempty"`
	Username  string              `json:"username" bson:"username"`
	Password  string              `json:"-" bson:"password"`
	Role      string              `json:"role" bson:"role"` // admin|warehouse|cashier
	OutletID  *primitive.ObjectID `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	CreatedAt time.Time           `json:"created_at" bson:"created_at"`
	UpdatedAt time.Time           `json:"updated_at" bson:"updated_at"`
}

// Product represents items available in a specific location (Warehouse or Outlet)
type Product struct {
	ID          primitive.ObjectID  `json:"id" bson:"_id,omitempty"`
	LocationID  *primitive.ObjectID `json:"location_id,omitempty" bson:"location_id,omitempty"` // nil = Warehouse
	Name        string              `json:"name" bson:"name"`
	Description string              `json:"description" bson:"description"`
	SKU         string              `json:"sku" bson:"sku"`
	Category    string              `json:"category" bson:"category"`
	Stock       int                 `json:"stock" bson:"stock"`
	CostPrice   float64             `json:"cost_price" bson:"cost_price"` // Individual cost per location
	UnitPrice   float64             `json:"unit_price" bson:"unit_price"` // Standardized Selling Price
	CreatedAt   time.Time           `json:"created_at" bson:"created_at"`
	UpdatedAt   time.Time           `json:"updated_at" bson:"updated_at"`
}

// Outlet represents a retail outlet
type Outlet struct {
	ID        primitive.ObjectID `json:"id" bson:"_id,omitempty"`
	Name      string             `json:"name" bson:"name"`
	Address   string             `json:"address" bson:"address"`
	Phone     string             `json:"phone" bson:"phone"`
	CreatedAt time.Time          `json:"created_at" bson:"created_at"`
	UpdatedAt time.Time          `json:"updated_at" bson:"updated_at"`
}

// Purchase represents warehouse purchasing from suppliers
type Purchase struct {
	ID           primitive.ObjectID  `json:"id" bson:"_id,omitempty"`
	SupplierName string              `json:"supplier_name" bson:"supplier_name"`
	SupplierID   *primitive.ObjectID `json:"supplier_id,omitempty" bson:"supplier_id,omitempty"`
	Items        []PurchaseItem      `json:"items" bson:"items"`
	TotalAmount  float64             `json:"total_amount" bson:"total_amount"`
	PurchaseDate time.Time           `json:"purchase_date" bson:"purchase_date"`
	WarehouseID  primitive.ObjectID  `json:"warehouse_id" bson:"warehouse_id"`
	CreatedBy    primitive.ObjectID  `json:"created_by" bson:"created_by"`
	Status       string              `json:"status" bson:"status"` // pending|completed|cancelled
}

type PurchaseItem struct {
	ProductID   primitive.ObjectID `json:"product_id" bson:"product_id"`
	ProductName string             `json:"product_name" bson:"product_name"`
	SKU         string             `json:"sku" bson:"sku"`
	Category    string             `json:"category" bson:"category"`
	Quantity    int                `json:"quantity" bson:"quantity"`
	UnitCost    float64            `json:"unit_cost" bson:"unit_cost"`
	TotalCost   float64            `json:"total_cost" bson:"total_cost"`
}

// Invoice represents warehouse selling to outlets
type Invoice struct {
	ID            primitive.ObjectID `json:"id" bson:"_id,omitempty"`
	InvoiceNumber string             `json:"invoice_number" bson:"invoice_number"`
	OutletID      primitive.ObjectID `json:"outlet_id" bson:"outlet_id"`
	WarehouseID   primitive.ObjectID `json:"warehouse_id" bson:"warehouse_id"`
	Items         []InvoiceItem      `json:"items" bson:"items"`
	TotalAmount   float64            `json:"total_amount" bson:"total_amount"`
	InvoiceDate   time.Time          `json:"invoice_date" bson:"invoice_date"`
	DueDate       time.Time          `json:"due_date" bson:"due_date"`
	Status        string             `json:"status" bson:"status"` // pending|paid|overdue
	CreatedBy     primitive.ObjectID `json:"created_by" bson:"created_by"`
}

type InvoiceItem struct {
	ProductID  primitive.ObjectID `json:"product_id" bson:"product_id"`
	Quantity   int                `json:"quantity" bson:"quantity"`
	UnitPrice  float64            `json:"unit_price" bson:"unit_price"`
	TotalPrice float64            `json:"total_price" bson:"total_price"`
}

// OutletPurchase represents outlet purchasing from warehouse
type OutletPurchase struct {
	ID           primitive.ObjectID   `json:"id" bson:"_id,omitempty"`
	InvoiceID    primitive.ObjectID   `json:"invoice_id" bson:"invoice_id"`
	OutletID     primitive.ObjectID   `json:"outlet_id" bson:"outlet_id"`
	Items        []OutletPurchaseItem `json:"items" bson:"items"`
	TotalAmount  float64              `json:"total_amount" bson:"total_amount"`
	PurchaseDate time.Time            `json:"purchase_date" bson:"purchase_date"`
	Status       string               `json:"status" bson:"status"` // pending|received|cancelled
	Notes        string               `json:"notes,omitempty" bson:"notes,omitempty"`
	ReceivedBy   primitive.ObjectID   `json:"received_by" bson:"received_by"`
}

type OutletPurchaseItem struct {
	ProductID   primitive.ObjectID `json:"product_id" bson:"product_id"`
	ProductName string             `json:"product_name,omitempty" bson:"product_name,omitempty"`
	Quantity    int                `json:"quantity" bson:"quantity"`
	UnitPrice   float64            `json:"unit_price" bson:"unit_price"`
	TotalPrice  float64            `json:"total_price" bson:"total_price"`
}

// Sale represents outlet selling to customers
type Sale struct {
	ID             primitive.ObjectID `json:"id" bson:"_id,omitempty"`
	ReceiptNumber  string             `json:"receipt_number" bson:"receipt_number"`
	OutletID       primitive.ObjectID `json:"outlet_id" bson:"outlet_id"`
	CustomerName   *string            `json:"customer_name,omitempty" bson:"customer_name,omitempty"`
	CustomerPhone  *string            `json:"customer_phone,omitempty" bson:"customer_phone,omitempty"`
	Items          []SaleItem         `json:"items" bson:"items"`
	Subtotal       float64            `json:"subtotal" bson:"subtotal"`
	TaxAmount      float64            `json:"tax_amount" bson:"tax_amount"`
	DiscountAmount float64            `json:"discount_amount" bson:"discount_amount"`
	TotalAmount    float64            `json:"total_amount" bson:"total_amount"`
	PaymentMethod  string             `json:"payment_method" bson:"payment_method"`
	SaleDate       time.Time          `json:"sale_date" bson:"sale_date"`
	CreatedBy      primitive.ObjectID `json:"created_by" bson:"created_by"`
}

type SaleItem struct {
	ProductID   primitive.ObjectID `json:"product_id" bson:"product_id"`
	ProductName string             `json:"product_name,omitempty" bson:"product_name,omitempty"`
	Quantity    int                `json:"quantity" bson:"quantity"`
	UnitPrice   float64            `json:"unit_price" bson:"unit_price"`
	TotalPrice  float64            `json:"total_price" bson:"total_price"`
	CostPrice   float64            `json:"cost_price,omitempty" bson:"cost_price,omitempty"`
}

// InventoryTransaction tracks all inventory movements
type InventoryTransaction struct {
	ID              primitive.ObjectID  `json:"id" bson:"_id,omitempty"`
	ItemID          primitive.ObjectID  `json:"item_id" bson:"item_id"`
	ItemType        string              `json:"item_type" bson:"item_type"`               // warehouse|outlet
	TransactionType string              `json:"transaction_type" bson:"transaction_type"` // purchase|sale|transfer_in|transfer_out
	Quantity        int                 `json:"quantity" bson:"quantity"`
	ReferenceID     primitive.ObjectID  `json:"reference_id" bson:"reference_id"`
	ReferenceType   string              `json:"reference_type" bson:"reference_type"` // purchase|invoice|sale
	WarehouseID     *primitive.ObjectID `json:"warehouse_id,omitempty" bson:"warehouse_id,omitempty"`
	OutletID        *primitive.ObjectID `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
	TransactionDate time.Time           `json:"transaction_date" bson:"transaction_date"`
	CreatedBy       primitive.ObjectID  `json:"created_by" bson:"created_by"`
}

// Auth request/response models
type LoginRequest struct {
	Username string `json:"username" binding:"required"`
	Password string `json:"password" binding:"required"`
}

type LoginResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}

// Common response models
type APIResponse struct {
	Success bool        `json:"success"`
	Message string      `json:"message"`
	Data    interface{} `json:"data,omitempty"`
}

type PaginatedResponse struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data"`
	Page    int         `json:"page"`
	Limit   int         `json:"limit"`
	Total   int64       `json:"total"`
}

// StockHistory tracks inventory movements for all products globally
type StockHistory struct {
	ID          primitive.ObjectID  `json:"id" bson:"_id,omitempty"`
	LocationID  *primitive.ObjectID `json:"location_id,omitempty" bson:"location_id,omitempty"` // ID of Outlet OR Warehouse
	ProductID   primitive.ObjectID  `json:"product_id" bson:"product_id"`
	Type        string              `json:"type" bson:"type"` // purchase|sale|adjustment|invoice
	ChangeQty   int                 `json:"change_qty" bson:"change_qty"`
	Balance     int                 `json:"balance" bson:"balance"`
	CostPrice   float64             `json:"cost_price" bson:"cost_price"`
	ReferenceID primitive.ObjectID  `json:"reference_id,omitempty" bson:"reference_id,omitempty"`
	CreatedAt   time.Time           `json:"created_at" bson:"created_at"`
}

// Expense represents a store expense recorded by a cashier
type Expense struct {
	ID          primitive.ObjectID `json:"id" bson:"_id,omitempty"`
	OutletID    primitive.ObjectID `json:"outlet_id" bson:"outlet_id"`
	Description string             `json:"description" bson:"description"`
	Amount      float64            `json:"amount" bson:"amount"`
	Category    string             `json:"category" bson:"category"`
	ExpenseDate time.Time          `json:"expense_date" bson:"expense_date"`
	CreatedBy   primitive.ObjectID `json:"created_by" bson:"created_by"`
	CreatedAt   time.Time          `json:"created_at" bson:"created_at"`
}

// AppVersion represents a released app version for OTA updates
type AppVersion struct {
	ID           primitive.ObjectID `json:"id" bson:"_id,omitempty"`
	VersionCode  int                `json:"version_code" bson:"version_code"`
	VersionName  string             `json:"version_name" bson:"version_name"`
	DownloadURL  string             `json:"download_url" bson:"download_url"`
	ReleaseNotes []string           `json:"release_notes" bson:"release_notes"`
	ForceUpdate  bool               `json:"force_update" bson:"force_update"`
	CreatedAt    time.Time          `json:"created_at" bson:"created_at"`
}
