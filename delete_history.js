
const dbName = 'business_management';
const db = db.getSiblingDB(dbName);
const result = db.stock_history.deleteOne({ _id: ObjectId("69aa8829216de8e90c4aa881") });
print("Delete result: " + JSON.stringify(result));
