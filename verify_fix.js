
const productId = '69aa8723216de8e90c4aa87f';
const dbName = 'business_management';
const db = db.getSiblingDB(dbName);

print('--- Final Verification ---');

const item = db.warehouse_items.findOne({ _id: ObjectId(productId) });
print('Warehouse Item Stock: ' + (item ? item.stock : 'NOT FOUND'));

const historyTotal = db.stock_history.countDocuments({
    $or: [
        { product_id: productId },
        { product_id: ObjectId(productId) }
    ]
});
print('Total History Entries: ' + historyTotal);

const allHistory = db.stock_history.find({
    $or: [
        { product_id: productId },
        { product_id: ObjectId(productId) }
    ]
}).toArray();
print('Remaining History: ' + JSON.stringify(allHistory));

print('--- Verification Complete ---');
