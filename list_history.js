
const dbName = 'business_management';
const db = db.getSiblingDB(dbName);
db.stock_history.find().forEach(function (d) {
    print(JSON.stringify(d));
});
