# Business Management System Documentation

## Overview

This documentation outlines a comprehensive business management system for a warehouse-outlet retail operation. The system manages inventory flow from warehouse to outlets and sales to end customers, with reporting capabilities for profits and sales tracking.

### Business Flow

1. **Warehouse Operations**:
   - Purchases items from suppliers
   - Stores inventory
   - Sells items to outlets via invoices
   - Generates reports on sales and profits

2. **Outlet Operations**:
   - Purchases items from warehouse
   - Stores inventory
   - Sells items to customers
   - Generates receipts for sales

3. **Reporting**:
   - Warehouse sales volume and revenue
   - Warehouse net profit (revenue - cost of goods)
   - Outlet profit margins

## Technology Stack

- **Backend**: Golang REST API server
- **Database**: MongoDB
- **Containerization**: Docker
- **Mobile App**: Kotlin Native Android application
- **Deployment**: Docker containers

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Admin App     │    │ Warehouse App   │    │   Outlet App    │
│   (Phone)       │    │   (Phone)       │    │   (Tablet)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │   Golang API Server │
                    │     (Docker)        │
                    └─────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │      MongoDB        │
                    │     (Docker)        │
                    └─────────────────────┘
```

## Database Schema (MongoDB Collections)

### Users Collection
```json
{
  "_id": ObjectId,
  "username": "string",
  "password": "hashed_string",
  "role": "admin|warehouse|cashier",
  "outlet_id": "ObjectId (nullable)",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### WarehouseItems Collection
```json
{
  "_id": ObjectId,
  "name": "string",
  "description": "string",
  "sku": "string",
  "category": "string",
  "cost_price": "decimal",
  "warehouse_id": "ObjectId",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### OutletItems Collection
```json
{
  "_id": ObjectId,
  "name": "string",
  "description": "string",
  "sku": "string",
  "category": "string",
  "unit_price": "decimal",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### Outlets Collection
```json
{
  "_id": ObjectId,
  "name": "string",
  "address": "string",
  "phone": "string",
  "created_at": "ISODate",
  "updated_at": "ISODate"
}
```

### Purchases Collection (Warehouse purchasing from suppliers)
```json
{
  "_id": ObjectId,
  "supplier_name": "string",
  "supplier_id": "ObjectId (nullable)",
  "items": [
    {
      "warehouse_item_id": "ObjectId",
      "quantity": "integer",
      "unit_cost": "decimal",
      "total_cost": "decimal"
    }
  ],
  "total_amount": "decimal",
  "purchase_date": "ISODate",
  "warehouse_id": "ObjectId",
  "created_by": "ObjectId (user)",
  "status": "pending|completed|cancelled"
}
```

### Invoices Collection (Warehouse selling to outlets)
```json
{
  "_id": ObjectId,
  "invoice_number": "string",
  "outlet_id": "ObjectId",
  "warehouse_id": "ObjectId",
  "items": [
    {
      "warehouse_item_id": "ObjectId",
      "quantity": "integer",
      "unit_price": "decimal",
      "total_price": "decimal"
    }
  ],
  "total_amount": "decimal",
  "invoice_date": "ISODate",
  "due_date": "ISODate",
  "status": "pending|paid|overdue",
  "created_by": "ObjectId (user)"
}
```

### OutletPurchases Collection (Outlet purchasing from warehouse)
```json
{
  "_id": ObjectId,
  "invoice_id": "ObjectId",
  "outlet_id": "ObjectId",
  "items": [
    {
      "warehouse_item_id": "ObjectId",
      "quantity": "integer",
      "unit_price": "decimal",
      "total_price": "decimal"
    }
  ],
  "total_amount": "decimal",
  "purchase_date": "ISODate",
  "status": "pending|received|cancelled",
  "received_by": "ObjectId (user)"
}
```

### Sales Collection (Outlet selling to customers)
```json
{
  "_id": ObjectId,
  "receipt_number": "string",
  "outlet_id": "ObjectId",
  "customer_name": "string (optional)",
  "customer_phone": "string (optional)",
  "items": [
    {
      "outlet_item_id": "ObjectId",
      "quantity": "integer",
      "unit_price": "decimal",
      "total_price": "decimal"
    }
  ],
  "subtotal": "decimal",
  "tax_amount": "decimal",
  "discount_amount": "decimal",
  "total_amount": "decimal",
  "payment_method": "cash|card|transfer",
  "sale_date": "ISODate",
  "created_by": "ObjectId (user)"
}
```

### InventoryTransactions Collection
```json
{
  "_id": ObjectId,
  "item_id": "ObjectId",
  "item_type": "warehouse|outlet",
  "transaction_type": "purchase|sale|transfer_in|transfer_out",
  "quantity": "integer",
  "reference_id": "ObjectId (purchase/invoice/sale id)",
  "reference_type": "purchase|invoice|sale",
  "warehouse_id": "ObjectId (nullable)",
  "outlet_id": "ObjectId (nullable)",
  "transaction_date": "ISODate",
  "created_by": "ObjectId (user)"
}
```

**Backend Stock Calculation Requirements:**
The backend must implement automatic stock calculation functions that aggregate transaction history in real-time:

- **Warehouse Stock**: `SUM(quantity) WHERE transaction_type='purchase' AND warehouse_id={id} - SUM(quantity) WHERE transaction_type='transfer_out' AND warehouse_id={id}`
- **Outlet Stock**: `SUM(quantity) WHERE transaction_type='transfer_in' AND outlet_id={id} - SUM(quantity) WHERE transaction_type='sale' AND outlet_id={id}`
- **Stock Level API**: Implement endpoints to query current stock levels by item and location
- **Transaction Triggers**: Every purchase, sale, transfer must create corresponding inventory transaction records
- **Real-time Updates**: Stock levels must be calculated on-demand, not cached, to ensure accuracy

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user info

### Admin Endpoints
- `GET /api/admin/dashboard` - Get dashboard statistics
- `GET /api/admin/outlets` - List all outlets
- `POST /api/admin/outlets` - Create new outlet
- `PUT /api/admin/outlets/{id}` - Update outlet
- `DELETE /api/admin/outlets/{id}` - Delete outlet
- `GET /api/admin/warehouse/stats` - Get warehouse statistics
- `GET /api/admin/outlets/{id}/stats` - Get outlet statistics
- `GET /api/admin/reports/sales` - Get sales reports
- `GET /api/admin/reports/profits` - Get profit reports
- `GET /api/admin/analytics/outlets/overview` - Get comprehensive outlet analytics overview
- `GET /api/admin/analytics/outlets/{id}/performance` - Get detailed outlet performance metrics
- `GET /api/admin/analytics/outlets/{id}/growth` - Get outlet growth trends and projections
- `GET /api/admin/analytics/outlets/{id}/inventory` - Get outlet inventory analytics
- `GET /api/admin/analytics/outlets/{id}/sales-breakdown` - Get detailed sales breakdown by category, time, etc.
- `GET /api/admin/analytics/outlets/{id}/profit-margins` - Get profit margin analysis
- `GET /api/admin/analytics/outlets/comparison` - Compare outlets performance
- `GET /api/admin/analytics/outlets/top-performers` - Get top performing outlets by various metrics
- `GET /api/admin/analytics/trends` - Get overall business trends and growth metrics

### Warehouse Endpoints
- `GET /api/warehouse/dashboard` - Get warehouse dashboard
- `GET /api/warehouse/items` - List warehouse items
- `POST /api/warehouse/items` - Add new item
- `PUT /api/warehouse/items/{id}` - Update item
- `DELETE /api/warehouse/items/{id}` - Delete item
- `POST /api/warehouse/purchases` - Create purchase order
- `GET /api/warehouse/purchases` - List purchase orders
- `PUT /api/warehouse/purchases/{id}/status` - Update purchase status
- `POST /api/warehouse/invoices` - Create invoice for outlet
- `GET /api/warehouse/invoices` - List invoices
- `GET /api/warehouse/invoices/{id}` - Get invoice details
- `GET /api/warehouse/reports/sales` - Get warehouse sales report
- `GET /api/warehouse/reports/profits` - Get warehouse profit report

### Outlet Endpoints
- `GET /api/outlet/dashboard` - Get outlet dashboard
- `GET /api/outlet/items` - List outlet items
- `POST /api/outlet/purchases` - Create purchase from warehouse
- `GET /api/outlet/purchases` - List purchases
- `PUT /api/outlet/purchases/{id}/receive` - Mark purchase as received
- `POST /api/outlet/sales` - Create sale transaction
- `GET /api/outlet/sales` - List sales
- `GET /api/outlet/sales/{id}` - Get sale receipt
- `GET /api/outlet/reports/sales` - Get outlet sales report
- `GET /api/outlet/reports/profits` - Get outlet profit report

### Common Endpoints
- `GET /api/items/search` - Search items
- `GET /api/items/{id}` - Get item details
- `GET /api/stock/warehouse/{item_id}` - Get current warehouse stock for item
- `GET /api/stock/outlet/{item_id}` - Get current outlet stock for item
- `GET /api/stock/transactions` - Get inventory transaction history

## Analytics & Reporting

### Outlet Analytics Features
The admin interface must provide comprehensive analytics for monitoring outlet performance and growth:

#### Key Metrics to Track:
- **Sales Performance**: Daily/weekly/monthly sales volume, revenue trends, average transaction value
- **Profit Analysis**: Gross profit, net profit, profit margins, cost analysis
- **Growth Metrics**: Month-over-month growth, year-over-year comparisons, growth rate projections
- **Inventory Efficiency**: Stock turnover rate, inventory holding costs, out-of-stock incidents
- **Customer Insights**: Customer acquisition rate, repeat purchase rate, customer lifetime value
- **Operational KPIs**: Order fulfillment time, customer satisfaction scores, employee productivity

#### Detailed Reports Include:
- **Performance Dashboard**: Real-time metrics with visual charts and KPIs
- **Trend Analysis**: Historical data with forecasting capabilities
- **Comparative Analysis**: Outlet vs outlet, outlet vs warehouse performance
- **Custom Date Ranges**: Flexible reporting periods (daily, weekly, monthly, quarterly, yearly)
- **Export Capabilities**: PDF/Excel export for offline analysis
- **Alert System**: Automated alerts for significant changes or thresholds

#### Growth Tracking:
- Revenue growth rate
- Customer base expansion
- Market share analysis
- Seasonal performance trends
- Competitive benchmarking

## Data Structures

### User
```go
type User struct {
    ID        primitive.ObjectID `json:"id" bson:"_id,omitempty"`
    Username  string             `json:"username" bson:"username"`
    Password  string             `json:"-" bson:"password"`
    Role      string             `json:"role" bson:"role"`
    OutletID  *primitive.ObjectID `json:"outlet_id,omitempty" bson:"outlet_id,omitempty"`
    CreatedAt time.Time          `json:"created_at" bson:"created_at"`
    UpdatedAt time.Time          `json:"updated_at" bson:"updated_at"`
}
```

### WarehouseItem
```go
type WarehouseItem struct {
    ID          primitive.ObjectID `json:"id" bson:"_id,omitempty"`
    Name        string             `json:"name" bson:"name"`
    Description string             `json:"description" bson:"description"`
    SKU         string             `json:"sku" bson:"sku"`
    Category    string             `json:"category" bson:"category"`
    CostPrice   float64            `json:"cost_price" bson:"cost_price"`
    WarehouseID primitive.ObjectID `json:"warehouse_id" bson:"warehouse_id"`
    CreatedAt   time.Time          `json:"created_at" bson:"created_at"`
    UpdatedAt   time.Time          `json:"updated_at" bson:"updated_at"`
}
```

### OutletItem
```go
type OutletItem struct {
    ID          primitive.ObjectID `json:"id" bson:"_id,omitempty"`
    Name        string             `json:"name" bson:"name"`
    Description string             `json:"description" bson:"description"`
    SKU         string             `json:"sku" bson:"sku"`
    Category    string             `json:"category" bson:"category"`
    UnitPrice   float64            `json:"unit_price" bson:"unit_price"`
    CreatedAt   time.Time          `json:"created_at" bson:"created_at"`
    UpdatedAt   time.Time          `json:"updated_at" bson:"updated_at"`
}
```

### Outlet
```go
type Outlet struct {
    ID        primitive.ObjectID `json:"id" bson:"_id,omitempty"`
    Name      string             `json:"name" bson:"name"`
    Address   string             `json:"address" bson:"address"`
    Phone     string             `json:"phone" bson:"phone"`
    CreatedAt time.Time          `json:"created_at" bson:"created_at"`
    UpdatedAt time.Time          `json:"updated_at" bson:"updated_at"`
}
```

### Invoice
```go
type Invoice struct {
    ID            primitive.ObjectID `json:"id" bson:"_id,omitempty"`
    InvoiceNumber string             `json:"invoice_number" bson:"invoice_number"`
    OutletID      primitive.ObjectID `json:"outlet_id" bson:"outlet_id"`
    WarehouseID   primitive.ObjectID `json:"warehouse_id" bson:"warehouse_id"`
    Items         []InvoiceItem      `json:"items" bson:"items"`
    TotalAmount   float64            `json:"total_amount" bson:"total_amount"`
    InvoiceDate   time.Time          `json:"invoice_date" bson:"invoice_date"`
    DueDate       time.Time          `json:"due_date" bson:"due_date"`
    Status        string             `json:"status" bson:"status"`
    CreatedBy     primitive.ObjectID `json:"created_by" bson:"created_by"`
}

type InvoiceItem struct {
    WarehouseItemID primitive.ObjectID `json:"warehouse_item_id" bson:"warehouse_item_id"`
    Quantity        int                `json:"quantity" bson:"quantity"`
    UnitPrice       float64            `json:"unit_price" bson:"unit_price"`
    TotalPrice      float64            `json:"total_price" bson:"total_price"`
}
```

### Sale
```go
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
    OutletItemID primitive.ObjectID `json:"outlet_item_id" bson:"outlet_item_id"`
    Quantity     int                `json:"quantity" bson:"quantity"`
    UnitPrice    float64            `json:"unit_price" bson:"unit_price"`
    TotalPrice   float64            `json:"total_price" bson:"total_price"`
}
```

## Android App Architecture

### Admin App (Phone Layout)
- **Login Screen**: Username/password authentication
- **Dashboard**: Overview statistics, recent activities
- **Outlets Management**:
  - List all outlets
  - Add new outlet form
  - Edit outlet details
  - View outlet statistics
- **Warehouse Monitoring**:
  - Current inventory levels
  - Recent purchases
  - Sales performance
- **Analytics & Reports**:
  - Comprehensive outlet analytics dashboard
  - Performance metrics and KPIs
  - Growth trend analysis and projections
  - Comparative outlet performance reports
  - Custom reporting with filters and exports
  - Real-time alerts and notifications
- **Reports**:
  - Sales reports by period
  - Profit reports
  - Outlet performance comparison

### Warehouse App (Phone Layout)
- **Login Screen**: Username/password authentication
- **Dashboard**: Today's sales, pending invoices, low stock alerts
- **Inventory Management**:
  - View current stock
  - Add new items
  - Update stock levels
  - Search items
- **Purchasing**:
  - Create purchase orders from suppliers
  - View purchase history
  - Update purchase status
- **Invoicing**:
  - Create invoices for outlets
  - View pending invoices
  - Invoice history
- **Reports**:
  - Daily/weekly/monthly sales
  - Profit calculations

### Outlet App (Tablet Layout)
- **Login Screen**: Username/password authentication
- **Dashboard**: Today's sales, current inventory, pending purchases
- **Purchasing from Warehouse**:
  - View available items from warehouse
  - Create purchase orders
  - Receive deliveries
  - Purchase history
- **Sales Management**:
  - Point of sale interface
  - Add items to cart
  - Apply discounts
  - Process payments
  - Generate receipts
- **Inventory**:
  - Current stock levels
  - Low stock alerts
  - Inventory adjustments
- **Reports**:
  - Daily sales summary
  - Profit reports
  - Customer transaction history

## Docker Configuration

### Dockerfile (Backend)
```dockerfile
FROM golang:1.21-alpine AS builder
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go build -o main .

FROM alpine:latest
RUN apk --no-cache add ca-certificates
WORKDIR /root/
COPY --from=builder /app/main .
EXPOSE 8080
CMD ["./main"]
```

### docker-compose.yml
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - MONGO_URI=mongodb://mongo:27017
    depends_on:
      - mongo

  mongo:
    image: mongo:7.0
    ports:
      - "27017:27017"
    volumes:
      - mongo_data:/data/db

volumes:
  mongo_data:
```

## Deployment Instructions

1. Clone the repository
2. Build the Docker images: `docker-compose build`
3. Start the services: `docker-compose up -d`
4. The API will be available at `http://localhost:8080`
5. MongoDB will be available at `localhost:27017`

## Security Considerations

- JWT tokens for authentication
- Password hashing with bcrypt
- Role-based access control
- Input validation and sanitization
- HTTPS in production
- Database connection encryption

## Future Enhancements

- Real-time notifications
- Barcode scanning for inventory
- Advanced analytics and forecasting
- Multi-warehouse support
- Customer loyalty programs
- Integration with accounting software
- Mobile payment integration