// Initialize database with sample data
db = db.getSiblingDB('tehatlas');

// Create users collection with sample users
db.users.insertMany([
  {
    username: "admin",
    password: "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // password: "admin123"
    role: "admin",
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    username: "cashier1",
    password: "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // password: "admin123"
    role: "cashier",
    outlet_id: ObjectId(),
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    username: "warehouse1",
    password: "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi", // password: "admin123"
    role: "warehouse",
    created_at: new Date(),
    updated_at: new Date()
  }
]);

// Create outlet_items collection with sample items
db.outlet_items.insertMany([
  {
    name: "Green Tea",
    description: "Premium green tea leaves",
    sku: "GT-001",
    category: "Beverages",
    unit_price: 25000,
    stock: 45,
    min_stock: 10,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Black Coffee",
    description: "Freshly ground black coffee",
    sku: "BC-002",
    category: "Beverages",
    unit_price: 35000,
    stock: 23,
    min_stock: 8,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Cappuccino",
    description: "Rich cappuccino blend",
    sku: "CC-003",
    category: "Beverages",
    unit_price: 40000,
    stock: 12,
    min_stock: 5,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Herbal Tea",
    description: "Assorted herbal tea collection",
    sku: "HT-004",
    category: "Beverages",
    unit_price: 20000,
    stock: 67,
    min_stock: 15,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Oolong Tea",
    description: "Traditional oolong tea",
    sku: "OT-005",
    category: "Beverages",
    unit_price: 45000,
    stock: 8,
    min_stock: 5,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Sandwich",
    description: "Freshly made sandwich",
    sku: "SW-006",
    category: "Food",
    unit_price: 15000,
    stock: 15,
    min_stock: 10,
    created_at: new Date(),
    updated_at: new Date()
  },
  {
    name: "Croissant",
    description: "Buttery croissant",
    sku: "CR-007",
    category: "Food",
    unit_price: 12000,
    stock: 22,
    min_stock: 8,
    created_at: new Date(),
    updated_at: new Date()
  }
]);

// Create inventory_transactions collection with sample data
db.inventory_transactions.insertMany([
  {
    item_id: ObjectId(),
    item_name: "Green Tea",
    transaction_type: "add_stock",
    quantity: 50,
    previous_stock: 25,
    new_stock: 75,
    supplier: "Warehouse A",
    unit_cost: 20000,
    total_cost: 1000000,
    transaction_date: new Date()
  },
  {
    item_id: ObjectId(),
    item_name: "Black Coffee",
    transaction_type: "sale",
    quantity: -3,
    previous_stock: 26,
    new_stock: 23,
    transaction_date: new Date(Date.now() - 4 * 60 * 60 * 1000) // 4 hours ago
  },
  {
    item_id: ObjectId(),
    item_name: "Cappuccino",
    transaction_type: "adjust_stock",
    quantity: -2,
    previous_stock: 14,
    new_stock: 12,
    reason: "Damaged items",
    transaction_date: new Date(Date.now() - 24 * 60 * 60 * 1000) // 1 day ago
  }
]);

print("Database initialized with sample data");