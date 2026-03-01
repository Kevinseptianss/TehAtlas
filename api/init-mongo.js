// MongoDB initialization script
db = db.getSiblingDB('business_management');

// Create collections
db.createCollection('users');
db.createCollection('warehouse_items');
db.createCollection('outlet_items');
db.createCollection('outlets');
db.createCollection('purchases');
db.createCollection('invoices');
db.createCollection('outlet_purchases');
db.createCollection('sales');
db.createCollection('inventory_transactions');

// Create indexes
db.users.createIndex({ "username": 1 }, { unique: true });
db.warehouse_items.createIndex({ "sku": 1 }, { unique: true });
db.outlet_items.createIndex({ "sku": 1 }, { unique: true });
db.outlets.createIndex({ "name": 1 });
db.purchases.createIndex({ "created_at": -1 });
db.invoices.createIndex({ "created_at": -1 });
db.outlet_purchases.createIndex({ "created_at": -1 });
db.sales.createIndex({ "created_at": -1 });
db.inventory_transactions.createIndex({ "item_id": 1, "item_type": 1 });

// Insert default admin user (password: admin123)
db.users.insertOne({
  "username": "admin",
  "password": "admin123",
  "role": "admin",
  "created_at": new Date(),
  "updated_at": new Date()
});

// Insert default warehouse user (password: warehouse123)
db.users.insertOne({
  "username": "warehouse",
  "password": "warehouse123",
  "role": "warehouse",
  "created_at": new Date(),
  "updated_at": new Date()
});

print("Database initialized successfully - PRODUCTION MODE (Clean Database)");