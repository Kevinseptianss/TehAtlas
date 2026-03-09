
print('--- Checking All Warehouse Items ---');
db.warehouse_items.find().forEach(item => {
    print('ID: ' + item._id + ' | Name: ' + item.name + ' | Stock: ' + item.stock + ' | WhStock: ' + item.warehouse_stock);
});
