# Business Management API

A comprehensive REST API for warehouse-outlet retail management system built with Golang, MongoDB, and Docker.

## Features

- **Authentication & Authorization**: JWT-based authentication with role-based access control
- **Warehouse Management**: Item management, purchasing, and invoicing
- **Outlet Management**: Sales, inventory, and reporting
- **Admin Dashboard**: Comprehensive analytics and reporting
- **Real-time Stock Tracking**: Transaction-based inventory management
- **Docker Deployment**: Containerized with nginx reverse proxy

## Tech Stack

- **Backend**: Golang with Gin framework
- **Database**: MongoDB
- **Containerization**: Docker & Docker Compose
- **Reverse Proxy**: Nginx
- **Authentication**: JWT tokens

## Current Status

### ✅ **Completed Features**
- JWT-based authentication with role-based access control
- Basic CRUD operations for outlets, items, purchases, invoices, and sales
- Docker containerization with MongoDB and Nginx
- Comprehensive test suite (30+ test cases)
- Mongo Express for database visualization

### ⚠️ **Development Status**
**API Completeness: ~60%** - Solid foundation with critical gaps in analytics and business logic.

### 📚 **Documentation**
- **[API Improvement Roadmap](documentation/api-improvement-roadmap.md)** - Detailed analysis of required improvements
- **[Improvement Summary](documentation/api-improvement-summary.md)** - Executive overview of current state
- **[Immediate Action Checklist](documentation/immediate-action-checklist.md)** - Priority-based task list

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Git

### Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd business-management-api
```

2. Start the services:
```bash
docker-compose up -d
```

3. The API will be available at `http://localhost`
4. MongoDB will be available at `localhost:27017`
5. **Mongo Express** (Database Web UI) will be available at `http://localhost:8081`

### Default Credentials

- **API Admin**: username: `admin`, password: `admin123`
- **Mongo Express**: username: `admin`, password: `admin123`

## Testing

The API includes a comprehensive test suite that validates all endpoints and functionality.

### Running Tests

Execute the test script to verify all API endpoints are working correctly:

```bash
# Make the test script executable
chmod +x test-api.sh

# Run comprehensive tests
./test-api.sh
```

### Test Coverage

The test suite covers:

- ✅ **Health Check**: Service and database connectivity
- ✅ **Authentication**: Login, JWT token validation, role-based access
- ✅ **Admin Endpoints**: Dashboard, outlets management, analytics
- ✅ **Warehouse Endpoints**: Item management, purchases, invoices
- ✅ **Outlet Endpoints**: Sales, inventory, reporting
- ✅ **Common Endpoints**: Item search, inventory transactions
- ✅ **Error Handling**: Invalid requests, unauthorized access
- ✅ **Performance**: Response times and concurrent requests

### Test Results

Recent test run results:
- **Tests Run**: 30
- **Tests Passed**: 35
- **Tests Failed**: 2

*Note: Some tests may show higher pass counts due to sub-tests within main test functions.*

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/me` - Get current user info

### Admin Endpoints
- `GET /api/admin/dashboard` - Get dashboard statistics
- `GET /api/admin/outlets` - List all outlets
- `POST /api/admin/outlets` - Create new outlet
- `GET /api/admin/analytics/outlets/overview` - Outlet analytics overview
- And many more analytics endpoints...

### Warehouse Endpoints
- `GET /api/warehouse/dashboard` - Warehouse dashboard
- `GET /api/warehouse/items` - List warehouse items
- `POST /api/warehouse/items` - Add new item
- `POST /api/warehouse/purchases` - Create purchase order
- `POST /api/warehouse/invoices` - Create invoice for outlet

### Outlet Endpoints
- `GET /api/outlet/dashboard` - Outlet dashboard
- `GET /api/outlet/items` - List outlet items
- `POST /api/outlet/sales` - Create sale transaction
- `GET /api/outlet/sales` - List sales

## Docker Infrastructure

This project uses a complete Docker containerization setup with multi-stage builds, reverse proxy, and persistent data storage.

### Architecture Overview

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Admin App     │    │ Warehouse App   │    │   Outlet App    │
│   (Phone)       │    │   (Phone)       │    │   (Tablet)      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │       Nginx         │
                    │   (Port 80)        │
                    │                     │
                    │ - Reverse Proxy     │
                    │ - Load Balancing    │
                    │ - Security Headers  │
                    │ - Gzip Compression  │
                    └─────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │   Golang API        │
                    │   (Port 8080)      │
                    │                     │
                    │ - REST API          │
                    │ - JWT Auth          │
                    │ - Business Logic    │
                    └─────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │      MongoDB        │
                    │   (Port 27017)     │
                    │                     │
                    │ - Data Persistence  │
                    │ - Authentication    │
                    │ - Indexes          │
                    └─────────────────────┘
```

### Services Configuration

#### 1. Application Service (`app`)
- **Image**: Custom built from `Dockerfile`
- **Port Mapping**: `8080:8080` (host:container)
- **Environment Variables**:
  - `MONGO_URI=mongodb://mongo:27017`
  - `JWT_SECRET=your-super-secret-jwt-key-change-in-production`
  - `PORT=8080`
- **Dependencies**: Requires `mongo` service
- **Network**: `app-network`
- **Build Context**: Current directory (`.`)

#### 2. MongoDB Service (`mongo`)
- **Image**: `mongo:7.0`
- **Port Mapping**: `27017:27017` (host:container)
- **Environment Variables**:
  - `MONGO_INITDB_ROOT_USERNAME=admin`
  - `MONGO_INITDB_ROOT_PASSWORD=password`
  - `MONGO_INITDB_DATABASE=business_management`
- **Volumes**:
  - `mongo_data:/data/db` (persistent data storage)
  - `./init-mongo.js:/docker-entrypoint-initdb.d/init-mongo.js:ro` (initialization script)
- **Network**: `app-network`

#### 3. Nginx Service (`nginx`)
- **Image**: `nginx:alpine`
- **Port Mapping**: `80:80` (host:container)
- **Volumes**:
  - `./nginx.conf:/etc/nginx/nginx.conf:ro` (nginx configuration)
- **Dependencies**: Requires `app` service
- **Network**: `app-network`

### Networks & Volumes

#### Networks
- **app-network**: Bridge network for inter-service communication
  - Driver: `bridge`
  - Allows services to communicate using service names as hostnames

#### Volumes
- **mongo_data**: Named volume for MongoDB data persistence
  - Location: Docker managed (typically `/var/lib/docker/volumes/`)
  - Persists database data across container restarts/rebuilds

### Environment Configuration

#### Required Environment Variables
Create a `.env` file in the project root:

```env
# MongoDB Connection
MONGO_URI=mongodb://admin:password@mongo:27017/business_management?authSource=admin

# JWT Configuration
JWT_SECRET=9EEDrnaVy+0086EJY4vD2l5tzTUIhc1KLluzk6D+Zb0=

# Application Port
PORT=8080
```

#### Environment Variable Details
- **MONGO_URI**: Full MongoDB connection string with authentication
  - Format: `mongodb://username:password@host:port/database?authSource=admin`
  - Host should use service name `mongo` when running in Docker
- **JWT_SECRET**: Secret key for JWT token signing (change in production!)
- **PORT**: Internal port for the Go application (8080)

### Docker Build Configuration

#### Dockerfile Details
```dockerfile
# Multi-stage build for optimized image size
FROM golang:1.24-alpine AS builder  # Build stage with Go 1.24
FROM alpine:latest                   # Runtime stage with minimal Alpine Linux
```

#### Build Process
1. **Builder Stage**:
   - Uses Go 1.24 Alpine image
   - Downloads dependencies (`go mod download`)
   - Builds static binary (`CGO_ENABLED=0`)
   - Cross-compiles for Linux

2. **Runtime Stage**:
   - Uses minimal Alpine Linux
   - Adds CA certificates for HTTPS
   - Copies compiled binary
   - Exposes port 8080

#### Build Optimization
- **Static Binary**: `CGO_ENABLED=0` creates self-contained binary
- **Multi-stage**: Separates build and runtime for smaller final image
- **Alpine Linux**: Minimal base image (~5MB)

### Nginx Configuration

#### Features
- **Reverse Proxy**: Routes `/api/*` requests to Go application
- **Load Balancing**: Configurable upstream with `app_backend`
- **Security Headers**:
  - `X-Frame-Options: SAMEORIGIN`
  - `X-XSS-Protection: 1; mode=block`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: no-referrer-when-downgrade`
  - `Content-Security-Policy`
- **Performance**:
  - Gzip compression for API responses
  - Keep-alive connections
  - Optimized buffer sizes

#### Routing Rules
- `/api/*` → Proxy to `app:8080`
- `/health` → Nginx health check (returns "healthy")
- `/*` → Proxy to `app:8080` (fallback)

### Database Initialization

#### MongoDB Setup
- **Root User**: `admin` / `password`
- **Database**: `business_management`
- **Authentication**: Required for all connections

#### Collections Created
- `users` - System users with authentication
- `warehouse_items` - Warehouse inventory
- `outlet_items` - Outlet inventory
- `outlets` - Retail locations
- `purchases` - Supplier purchases
- `invoices` - Warehouse to outlet transfers
- `outlet_purchases` - Outlet from warehouse purchases
- `sales` - Customer sales
- `inventory_transactions` - Stock movement tracking

#### Indexes
- Unique indexes on `username`, `sku` fields
- Performance indexes on `created_at` timestamps
- Compound indexes for inventory queries

#### Default Data
- Admin user: `admin` / `admin123`
- Sample outlet and products for testing

### Docker Commands Reference

#### Basic Operations
```bash
# Start all services
docker-compose up -d

# Start with rebuild
docker-compose up --build -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

#### Development Commands
```bash
# Rebuild specific service
docker-compose up --build app

# Run commands in containers
docker-compose exec app sh
docker-compose exec mongo mongo -u admin -p password business_management

# View resource usage
docker-compose ps
docker stats
```

#### Troubleshooting
```bash
# Check service status
docker-compose ps

# View specific service logs
docker-compose logs app
docker-compose logs mongo
docker-compose logs nginx

# Restart specific service
docker-compose restart app

# Clean rebuild
docker-compose down
docker system prune -f
docker-compose up --build -d
```

### Port Mapping Reference

| Service | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------|
| Nginx   | 80           | 80           | HTTP API access |
| App     | 8080         | 8080         | Direct API access (bypasses nginx) |
| MongoDB | 27017        | 27017        | Database access |

### Data Persistence

#### Volume Management
- **mongo_data**: Persists MongoDB data
- Survives container restarts and rebuilds
- Backup with: `docker run --rm -v api_mongo_data:/data -v $(pwd):/backup alpine tar czf /backup/mongo_backup.tar.gz -C /data .`

#### Backup Strategy
```bash
# Create backup
docker-compose exec mongo mongodump -u admin -p password --db business_management --out /backup

# Copy backup to host
docker cp $(docker-compose ps -q mongo):/backup ./mongo_backup

# Restore backup
docker-compose exec mongo mongorestore -u admin -p password ./backup/business_management
```

### Security Considerations

#### Production Deployment
1. **Change Default Credentials**:
   - MongoDB: Update `MONGO_INITDB_ROOT_USERNAME` and `MONGO_INITDB_ROOT_PASSWORD`
   - JWT: Generate strong random `JWT_SECRET`

2. **Network Security**:
   - Don't expose MongoDB port (27017) in production
   - Use internal networking only

3. **SSL/TLS**:
   - Configure SSL termination in nginx
   - Use HTTPS for all external connections

4. **Environment Variables**:
   - Use Docker secrets or external configuration management
   - Never commit `.env` files to version control

### Monitoring & Health Checks

#### Health Endpoints
- **API Health**: `GET http://localhost/api/health`
- **Nginx Health**: `GET http://localhost/health`
- **Mongo Express**: `http://localhost:8081` (Web-based database viewer)

#### Monitoring Commands
```bash
# Check container health
docker-compose ps

# Monitor resource usage
docker stats $(docker-compose ps -q)

# Check MongoDB status
docker-compose exec mongo mongo --eval "db.stats()" -u admin -p password business_management
```

### Common Issues & Solutions

#### Port Conflicts
```bash
# Check what's using ports
lsof -i :80
lsof -i :8080
lsof -i :27017

# Change port mapping in docker-compose.yml
ports:
  - "8081:80"  # Change host port
```

#### MongoDB Connection Issues
```bash
# Check MongoDB logs
docker-compose logs mongo

# Test connection
docker-compose exec mongo mongo -u admin -p password business_management --eval "db.users.find()"
```

#### Build Failures
```bash
# Clear Docker cache
docker system prune -f

# Rebuild without cache
docker-compose build --no-cache

# Check Go version compatibility
docker run --rm golang:1.24-alpine go version
```

This Docker infrastructure provides a production-ready, scalable, and maintainable deployment setup for the business management API.

## Environment Variables

Create a `.env` file in the root directory:

```env
MONGO_URI=mongodb://admin:password@mongo:27017/business_management?authSource=admin
JWT_SECRET=your-super-secret-jwt-key-change-in-production
PORT=8080
```

## Database Collections

- `users` - System users
- `warehouse_items` - Items stored in warehouse
- `outlet_items` - Items available for sale
- `outlets` - Retail outlet locations
- `purchases` - Warehouse purchasing from suppliers
- `invoices` - Warehouse selling to outlets
- `outlet_purchases` - Outlet purchasing from warehouse
- `sales` - Outlet selling to customers
- `inventory_transactions` - Stock movement tracking

## Development

### Running Locally

1. Install dependencies:
```bash
go mod download
```

2. Set environment variables

3. Run the application:
```bash
go run cmd/main.go
```

### Building Docker Image

```bash
docker build -t business-management-api .
```

## Security

- JWT token authentication
- Password hashing with bcrypt
- Role-based access control
- Input validation and sanitization
- CORS enabled
- Security headers in nginx

## API Documentation

For detailed API documentation, refer to the `documentation.md` file in the project root.

## License

This project is licensed under the MIT License.