
const entryId = '69ad797f394179377ce22d4c';
const dbName = 'business_management';
const db = db.getSiblingDB(dbName);

print('--- Searching for Specific History Entry ID ---');

const entry = db.stock_history.findOne({
    $or: [
        { _id: entryId },
        { _id: ObjectId(entryId) }
    ]
});

print('Found entry: ' + JSON.stringify(entry));

if (entry) {
    print('Deleting found entry...');
    const delRes = db.stock_history.deleteOne({ _id: entry._id });
    print('Delete result: ' + delRes.deletedCount);
}

print('--- Process Complete ---');
