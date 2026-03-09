
const productId = '69aa8723216de8e90c4aa87f';

print('--- Starting Robust Fix for Gelas Cup ---');

// 1. Find the incorrect history entry
const entryToDelete = db.stock_history.findOne({
    product_id: productId,
    type: 'receive',
    note: { $regex: /Pembelian Diterima/i }
});

if (entryToDelete) {
    print('Found entry to delete. ID:', entryToDelete._id);
    const delRes = db.stock_history.deleteOne({ _id: entryToDelete._id });
    print('Deleted history entry:', delRes.deletedCount);
} else {
    print('Entry to delete NOT FOUND by criteria.');
}

// 2. Update warehouse item stock
const updateResult = db.warehouse_items.updateOne(
    { _id: productId },
    {
        $set: {
            stock: 49900,
            warehouse_stock: 49900,
            updated_at: new Date()
        }
    }
);
if (updateResult.modifiedCount === 0) {
    // Try ObjectId if string failed
    db.warehouse_items.updateOne(
        { _id: ObjectId(productId) },
        {
            $set: {
                stock: 49900,
                warehouse_stock: 49900,
                updated_at: new Date()
            }
        }
    );
}
print('Updated warehouse item stock (check below for final state)');

// 3. Verify final state
const finalItem = db.warehouse_items.findOne({ $or: [{ _id: productId }, { _id: ObjectId(productId) }] });
if (finalItem) {
    print('Final Warehouse Item Stock:', finalItem.stock);
} else {
    print('Final Item NOT FOUND');
}

const remainingHist = db.stock_history.find({
    $or: [{ product_id: productId }, { product_id: ObjectId(productId) }]
}).sort({ created_at: -1 }).limit(5).toArray();

print('\n--- Latest 5 History Entries ---');
remainingHist.forEach(log => {
    print('[' + log.created_at + '] ID: ' + log._id.toString() + ' | Type: ' + log.type + ' | Change: ' + log.change_qty + ' | Balance: ' + log.balance);
});

print('\n--- Fix Completed ---');
