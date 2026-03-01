# TehAtlas Backend API

A comprehensive REST API for the TehAtlas business management system built with Go and MongoDB.

## Features

- **Authentication**: JWT-based authentication with role-based access control
- **Inventory Management**: Complete inventory tracking for outlets and warehouses
- **Sales Management**: POS transaction processing
- **Purchase Orders**: Supplier purchase order management
- **Dashboard Analytics**: Real-time business metrics
- **Stock Transfers**: Warehouse to outlet inventory transfers

## Tech Stack

- **Language**: Go 1.21
- **Framework**: Gin Web Framework
- **Database**: MongoDB
- **Authentication**: JWT tokens
- **Containerization**: Docker & Docker Compose

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Go 1.21+ (for local development)

### Running with Docker Compose

1. Clone the repository
2. Navigate to the backend directory
3. Run the application:

```bash
docker-compose up --build
```

The API will be available at `http://localhost:8080`

### Local Development

1. Install dependencies:
```bash
go mod download
```

2. Set up environment variables (create `.env` file):
```env
MONGO_URI=mongodb://localhost:27017
DATABASE_NAME=tehatlas
JWT_SECRET=your-super-secret-jwt-key
PORT=8080
```

3. Run MongoDB (via Docker):
```bash
docker run -d -p 27017:27017 --name mongodb mongo:7.0
```

4. Run the application:
```bash
go run main.go
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login

### Outlet Endpoints (Cashier Access)
- `GET /api/outlet/items` - Get outlet items
- `POST /api/outlet/sales` - Create sale transaction
- `GET /api/outlet/sales` - Get sales history
- `GET /api/outlet/dashboard` - Get outlet dashboard
- `POST /api/outlet/inventory/add` - Add stock to inventory ✅ **NEW**
- `POST /api/outlet/inventory/adjust` - Adjust stock levels ✅ **NEW**
- `POST /api/outlet/purchases` - Create purchase order ✅ **NEW**
- `GET /api/outlet/inventory/transactions` - Get inventory transactions ✅ **NEW**
- `GET /api/outlet/inventory/items` - Get inventory items with stock ✅ **NEW**

### Warehouse Endpoints (Warehouse/Admin Access)
- `GET /api/warehouse/items` - Get warehouse items
- `GET /api/warehouse/dashboard` - Get warehouse dashboard
- `POST /api/warehouse/transfers` - Create stock transfer
- `GET /api/warehouse/transfers` - Get transfers
- `PUT /api/warehouse/transfers/:id/receive` - Receive transfer

### Admin Endpoints (Admin Only)
- `GET /api/admin/outlets` - Get all outlets
- `POST /api/admin/outlets` - Create outlet
- `GET /api/admin/dashboard` - Get admin dashboard
- `POST /api/admin/users` - Create user
- `GET /api/admin/users` - Get users
- `POST /api/admin/suppliers` - Create supplier
- `GET /api/admin/suppliers` - Get suppliers

### Common Endpoints
- `GET /api/items/search` - Search items
- `GET /api/stock/transactions` - Get stock transactions

## Sample API Usage

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"cashier1","password":"admin123"}'
```

### Add Stock (Cashier)
```bash
curl -X POST http://localhost:8080/api/outlet/inventory/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "item_id": "507f1f77bcf86cd799439011",
    "quantity": 25,
    "supplier": "Local Supplier",
    "unit_cost": 20000,
    "batch_id": "BATCH-001",
    "expiry_date": "2026-12-31"
  }'
```

### Adjust Stock (Cashier)
```bash
curl -X POST http://localhost:8080/api/outlet/inventory/adjust \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "item_id": "507f1f77bcf86cd799439011",
    "quantity": -5,
    "reason": "Damaged items"
  }'
```

### Create Purchase Order (Cashier)
```bash
curl -X POST http://localhost:8080/api/outlet/purchases \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "supplier": "PT. Supplier Indonesia",
    "items": [
      {
        "item_name": "Green Tea",
        "quantity": 100,
        "unit_cost": 18000,
        "total_cost": 1800000
      }
    ],
    "expected_date": "2026-02-20"
  }'
```

## Database Schema

### Collections
- `users` - User accounts and authentication
- `outlet_items` - Items available at outlets with stock levels
- `warehouse_items` - Items stored in warehouse
- `inventory_transactions` - All stock movement records
- `inventory_batches` - Batch tracking for inventory
- `purchase_orders` - Supplier purchase orders
- `sales` - POS transaction records

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGO_URI` | `mongodb://localhost:27017` | MongoDB connection URI |
| `DATABASE_NAME` | `tehatlas` | MongoDB database name |
| `JWT_SECRET` | `your-secret-key` | JWT signing secret |
| `PORT` | `8080` | Server port |

## Development

### Project Structure
```
backend/
├── main.go                 # Application entry point
├── go.mod                  # Go modules
├── docker-compose.yml      # Docker compose configuration
├── Dockerfile             # Docker build configuration
├── init-mongo.js         # MongoDB initialization script
├── config/
│   └── config.go         # Configuration management
├── database/
│   └── database.go       # MongoDB connection
├── middleware/
│   └── auth.go           # Authentication middleware
├── models/
│   └── models.go         # Data models
├── routes/
│   └── routes.go         # Route definitions
└── handlers/
    ├── auth.go           # Authentication handlers
    └── inventory.go      # Inventory management handlers
```

### Testing
```bash
go test ./...
```

### Building
```bash
go build -o main .
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License.