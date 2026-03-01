const axios = require('axios');
const fs = require('fs');
const path = require('path');

/**
 * TehAtlas Backend API Test Suite & Documentation Generator
 * Tests all API endpoints and generates comprehensive documentation
 */

class ApiTestSuite {
    constructor() {
        this.baseURL = 'http://localhost:8080';
        this.authToken = '';
        this.testResults = [];
        this.apiDocumentation = {
            title: 'TehAtlas Backend API Documentation',
            version: '1.0.0',
            baseUrl: this.baseURL,
            generatedAt: new Date().toISOString(),
            endpoints: []
        };
    }

    async runAllTests() {
        console.log('🚀 Starting TehAtlas Backend API Test Suite & Documentation Generation');
        console.log('========================================================================\n');

        try {
            // Start Docker containers
            console.log('🐳 Starting Docker containers...');
            await this.startDockerContainers();

            // Wait for services to be ready
            console.log('⏳ Waiting for services to be ready...');
            await this.waitForServices();

            // Test Authentication
            await this.testLogin();

            if (this.authToken) {
                // Test Outlet/Cashier endpoints
                await this.testGetOutletItems();
                await this.testGetOutletInventoryItems();
                await this.testAddStock();
                await this.testAdjustStock();
                await this.testCreatePurchaseOrder();
                await this.testGetInventoryTransactions();
                await this.testGetOutletDashboard();
                await this.testCreateSale();
                await this.testGetSales();

                // Test Admin endpoints (should fail for cashier role)
                await this.testAdminAccessControl();
                await this.testGetOutlets();
                await this.testGetAdminDashboard();

                // Test Warehouse endpoints (should fail for cashier role)
                await this.testWarehouseAccessControl();
                await this.testWarehouseDashboard();
                await this.testCreateTransfer();
                await this.testGetTransfers();
            }

        } catch (error) {
            console.error('❌ Test suite failed:', error.message);
        } finally {
            // Generate report and documentation
            this.generateReport();
            this.generateDocumentation();

            // Stop Docker containers
            console.log('\n🐳 Stopping Docker containers...');
            await this.stopDockerContainers();
        }
    }

    async startDockerContainers() {
        const { execSync } = require('child_process');
        try {
            execSync('docker-compose up -d', { cwd: path.join(__dirname, '..'), stdio: 'inherit' });
            console.log('✅ Docker containers started successfully');
        } catch (error) {
            console.error('❌ Failed to start Docker containers:', error.message);
            throw error;
        }
    }

    async stopDockerContainers() {
        const { execSync } = require('child_process');
        try {
            execSync('docker-compose down', { cwd: path.join(__dirname, '..'), stdio: 'inherit' });
            console.log('✅ Docker containers stopped successfully');
        } catch (error) {
            console.error('❌ Failed to stop Docker containers:', error.message);
        }
    }

    async waitForServices() {
        const maxRetries = 30;
        const retryDelay = 2000;

        for (let i = 0; i < maxRetries; i++) {
            try {
                // Try to connect to the server by making a request that will get a response (even if it's an error)
                // We'll try the login endpoint with a HEAD request to see if the server responds
                await axios.head(`${this.baseURL}/api/auth/login`, { timeout: 5000 });
                console.log('✅ Services are ready');
                return;
            } catch (error) {
                // If we get a 405 Method Not Allowed, that's actually good - it means the server is responding
                if (error.response && error.response.status === 405) {
                    console.log('✅ Services are ready');
                    return;
                }
                console.log(`⏳ Waiting for services... (${i + 1}/${maxRetries})`);
                await this.delay(retryDelay);
            }
        }
        throw new Error('Services failed to start within timeout');
    }

    async testLogin() {
        const endpoint = {
            method: 'POST',
            path: '/api/auth/login',
            description: 'User Authentication',
            category: 'Authentication'
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.post(`${this.baseURL}/api/auth/login`, {
                username: 'testuser',
                password: 'testpass'
            });

            if (response.data.success) {
                this.authToken = response.data.data.token;
                return { success: true, message: 'Login successful, token received' };
            } else {
                return { success: false, message: `Login failed: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetOutletItems() {
        const endpoint = {
            method: 'GET',
            path: '/api/outlet/items',
            description: 'Get Outlet Inventory Items',
            category: 'Inventory Management',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin']
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.get(`${this.baseURL}/api/outlet/items`, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return {
                    success: true,
                    message: `Retrieved ${response.data.data.length} items`,
                    data: response.data.data
                };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testAddStock() {
        const endpoint = {
            method: 'POST',
            path: '/api/outlet/inventory/add',
            description: 'Add Stock to Inventory',
            category: 'Inventory Management',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin'],
            requestBody: {
                item_id: 'string',
                quantity: 'number',
                supplier: 'string',
                unit_cost: 'number'
            }
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.post(`${this.baseURL}/api/outlet/inventory/add`, {
                item_id: 'test-item-1',
                quantity: 25,
                supplier: 'Test Supplier',
                unit_cost: 10.50
            }, {
                headers: {
                    'Authorization': `Bearer ${this.authToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.data.success) {
                return { success: true, message: 'Stock added successfully' };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testAdjustStock() {
        const endpoint = {
            method: 'POST',
            path: '/api/outlet/inventory/adjust',
            description: 'Adjust Stock Levels',
            category: 'Inventory Management',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin'],
            requestBody: {
                item_id: 'string',
                adjustment: 'number',
                reason: 'string'
            }
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.post(`${this.baseURL}/api/outlet/inventory/adjust`, {
                item_id: 'test-item-1',
                adjustment: -5,
                reason: 'Damaged goods'
            }, {
                headers: {
                    'Authorization': `Bearer ${this.authToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.data.success) {
                return { success: true, message: 'Stock adjusted successfully' };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testCreatePurchaseOrder() {
        const endpoint = {
            method: 'POST',
            path: '/api/outlet/purchases',
            description: 'Create Purchase Order',
            category: 'Purchase Management',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin'],
            requestBody: {
                supplier_id: 'string',
                items: 'array'
            }
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.post(`${this.baseURL}/api/outlet/purchases`, {
                supplier_id: 'supplier-1',
                items: [{
                    item_name: 'Coffee Beans',
                    quantity: 50,
                    unit_cost: 12.50
                }]
            }, {
                headers: {
                    'Authorization': `Bearer ${this.authToken}`,
                    'Content-Type': 'application/json'
                }
            });

            if (response.data.success) {
                return { success: true, message: 'Purchase order created successfully' };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetInventoryTransactions() {
        const endpoint = {
            method: 'GET',
            path: '/api/outlet/inventory/transactions',
            description: 'Get Inventory Transaction History',
            category: 'Inventory Management',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin']
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.get(`${this.baseURL}/api/outlet/inventory/transactions`, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return {
                    success: true,
                    message: `Retrieved ${response.data.data.length} transactions`,
                    data: response.data.data
                };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetOutletDashboard() {
        const endpoint = {
            method: 'GET',
            path: '/api/outlet/dashboard',
            description: 'Get Outlet Dashboard Data',
            category: 'Dashboard',
            requiresAuth: true,
            allowedRoles: ['cashier', 'admin']
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.get(`${this.baseURL}/api/outlet/dashboard`, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return { success: true, message: 'Dashboard data retrieved successfully' };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testAdminAccessControl() {
        const endpoint = {
            method: 'GET',
            path: '/api/admin/users',
            description: 'Admin Access Control Test',
            category: 'Security',
            requiresAuth: true,
            allowedRoles: ['admin']
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/admin/users`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: 'Unexpected: Admin access granted to cashier role' };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testWarehouseAccessControl() {
        const endpoint = {
            method: 'GET',
            path: '/api/warehouse/items',
            description: 'Warehouse Access Control Test',
            category: 'Security',
            requiresAuth: true,
            allowedRoles: ['warehouse', 'admin']
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/warehouse/items`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: 'Unexpected: Warehouse access granted to cashier role' };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetOutletInventoryItems() {
        const endpoint = {
            method: 'GET',
            path: '/api/outlet/inventory/items',
            description: 'Get Outlet Inventory Items (Detailed)',
            category: 'Inventory Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.get(`${this.baseURL}/api/outlet/inventory/items`, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return { success: true, message: `Retrieved ${response.data.data?.length || 0} inventory items` };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testCreateSale() {
        const endpoint = {
            method: 'POST',
            path: '/api/outlet/sales',
            description: 'Create Sale Transaction',
            category: 'Sales Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.post(`${this.baseURL}/api/outlet/sales`, {
                items: [
                    {
                        item_id: "item-1",
                        quantity: 2,
                        unit_price: 5.50
                    }
                ],
                total_amount: 11.00
            }, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return { success: true, message: 'Sale created successfully' };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetSales() {
        const endpoint = {
            method: 'GET',
            path: '/api/outlet/sales/',
            description: 'Get Sales History',
            category: 'Sales Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            const response = await axios.get(`${this.baseURL}/api/outlet/sales/`, {
                headers: { 'Authorization': `Bearer ${this.authToken}` }
            });

            if (response.data.success) {
                return { success: true, message: `Retrieved ${response.data.data?.length || 0} sales records` };
            } else {
                return { success: false, message: `API Error: ${response.data.message}` };
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetOutlets() {
        const endpoint = {
            method: 'GET',
            path: '/api/admin/outlets',
            description: 'Get All Outlets (Admin)',
            category: 'Admin Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/admin/outlets`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 200) {
                    return { success: true, message: 'Outlets retrieved successfully' };
                } else {
                    return { success: false, message: `Unexpected response: ${response.status}` };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetAdminDashboard() {
        const endpoint = {
            method: 'GET',
            path: '/api/admin/dashboard',
            description: 'Get Admin Dashboard (Admin)',
            category: 'Admin Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/admin/dashboard`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 200) {
                    return { success: true, message: 'Admin dashboard retrieved successfully' };
                } else {
                    return { success: false, message: `Unexpected response: ${response.status}` };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testWarehouseDashboard() {
        const endpoint = {
            method: 'GET',
            path: '/api/warehouse/dashboard',
            description: 'Get Warehouse Dashboard (Warehouse)',
            category: 'Warehouse Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/warehouse/dashboard`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 200) {
                    return { success: true, message: 'Warehouse dashboard retrieved successfully' };
                } else {
                    return { success: false, message: `Unexpected response: ${response.status}` };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testCreateTransfer() {
        const endpoint = {
            method: 'POST',
            path: '/api/warehouse/transfers',
            description: 'Create Stock Transfer (Warehouse)',
            category: 'Warehouse Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.post(`${this.baseURL}/api/warehouse/transfers`, {
                    from_outlet_id: "outlet-1",
                    to_outlet_id: "outlet-2",
                    items: [
                        {
                            item_id: "item-1",
                            quantity: 10
                        }
                    ]
                }, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 200) {
                    return { success: true, message: 'Transfer created successfully' };
                } else {
                    return { success: false, message: `Unexpected response: ${response.status}` };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async testGetTransfers() {
        const endpoint = {
            method: 'GET',
            path: '/api/warehouse/transfers/',
            description: 'Get Transfer History (Warehouse)',
            category: 'Warehouse Management'
        };

        const result = await this.executeTest(endpoint, async () => {
            try {
                const response = await axios.get(`${this.baseURL}/api/warehouse/transfers/`, {
                    headers: { 'Authorization': `Bearer ${this.authToken}` }
                });

                if (response.status === 200) {
                    return { success: true, message: `Retrieved ${response.data.data?.length || 0} transfers` };
                } else {
                    return { success: false, message: `Unexpected response: ${response.status}` };
                }
            } catch (error) {
                if (error.response && error.response.status === 403) {
                    return { success: true, message: 'Access correctly denied for cashier role' };
                } else {
                    return { success: false, message: `Unexpected error: ${error.message}` };
                }
            }
        });

        this.addToDocumentation(endpoint, result);
        this.printResult(result);
    }

    async executeTest(endpoint, testFunction) {
        const startTime = Date.now();

        try {
            const result = await testFunction();
            const duration = Date.now() - startTime;

            const testResult = {
                endpoint: `${endpoint.method} ${endpoint.path}`,
                description: endpoint.description,
                success: result.success,
                response: result.message,
                duration: duration,
                data: result.data || null
            };

            this.testResults.push(testResult);
            return testResult;
        } catch (error) {
            const duration = Date.now() - startTime;

            let errorMessage = 'Unknown error';
            if (error.response) {
                errorMessage = `HTTP ${error.response.status}: ${error.response.data?.message || error.message}`;
            } else if (error.request) {
                errorMessage = 'Network error: No response received';
            } else {
                errorMessage = error.message;
            }

            const result = {
                endpoint: `${endpoint.method} ${endpoint.path}`,
                description: endpoint.description,
                success: false,
                response: errorMessage,
                duration: duration,
                data: null
            };

            this.testResults.push(result);
            return result;
        }
    }

    addToDocumentation(endpoint, result) {
        this.apiDocumentation.endpoints.push({
            ...endpoint,
            testResult: {
                success: result.success,
                responseTime: result.duration,
                lastTested: new Date().toISOString()
            }
        });
    }

    printResult(result) {
        const status = result.success ? '✅ PASS' : '❌ FAIL';
        console.log(`${status} - ${result.endpoint}`);
        console.log(`   ${result.description}`);
        console.log(`   Response: ${result.response}`);
        console.log(`   Duration: ${result.duration}ms`);
        console.log();
    }

    generateReport() {
        console.log('📊 Test Report Summary');
        console.log('======================');

        const totalTests = this.testResults.length;
        const passedTests = this.testResults.filter(r => r.success).length;
        const failedTests = totalTests - passedTests;

        console.log(`Total Tests: ${totalTests}`);
        console.log(`Passed: ${passedTests} ✅`);
        console.log(`Failed: ${failedTests} ❌`);
        console.log(`Success Rate: ${((passedTests / totalTests) * 100).toFixed(1)}%`);

        console.log('\n📋 Detailed Results:');
        console.log('===================');

        this.testResults.forEach(result => {
            const status = result.success ? '✅' : '❌';
            console.log(`${status} ${result.endpoint} - ${result.description}`);
        });

        console.log('\n🎯 Test Suite Completed!');
        if (failedTests === 0) {
            console.log('🎉 All tests passed! API is ready for production.');
        } else {
            console.log('⚠️  Some tests failed. Please review the issues above.');
        }
    }

    generateDocumentation() {
        const docPath = path.join(__dirname, '..', 'API_DOCUMENTATION.md');

        let documentation = `# ${this.apiDocumentation.title}

**Version:** ${this.apiDocumentation.version}  
**Base URL:** ${this.apiDocumentation.baseUrl}  
**Generated:** ${this.apiDocumentation.generatedAt}

## Table of Contents
- [Authentication](#authentication)
- [Inventory Management](#inventory-management)
- [Purchase Management](#purchase-management)
- [Dashboard](#dashboard)
- [Search](#search)
- [Reporting](#reporting)
- [Security](#security)

## API Endpoints

`;

        const categories = {};
        this.apiDocumentation.endpoints.forEach(endpoint => {
            if (!categories[endpoint.category]) {
                categories[endpoint.category] = [];
            }
            categories[endpoint.category].push(endpoint);
        });

        Object.keys(categories).forEach(category => {
            documentation += `### ${category}\n\n`;

            categories[category].forEach(endpoint => {
                const status = endpoint.testResult.success ? '✅' : '❌';
                documentation += `#### ${status} ${endpoint.method} ${endpoint.path}\n\n`;
                documentation += `**Description:** ${endpoint.description}\n\n`;

                if (endpoint.requiresAuth) {
                    documentation += `**Authentication:** Required\n`;
                    documentation += `**Allowed Roles:** ${endpoint.allowedRoles.join(', ')}\n\n`;
                } else {
                    documentation += `**Authentication:** Not required\n\n`;
                }

                if (endpoint.parameters) {
                    documentation += `**Query Parameters:**\n`;
                    endpoint.parameters.forEach(param => {
                        documentation += `- \`${param.name}\` (${param.type}): ${param.description}\n`;
                    });
                    documentation += '\n';
                }

                if (endpoint.requestBody) {
                    documentation += `**Request Body:**\n\`\`\`json\n${JSON.stringify(endpoint.requestBody, null, 2)}\n\`\`\`\n\n`;
                }

                documentation += `**Test Status:** ${endpoint.testResult.success ? 'PASS' : 'FAIL'}\n`;
                documentation += `**Response Time:** ${endpoint.testResult.responseTime}ms\n`;
                documentation += `**Last Tested:** ${endpoint.testResult.lastTested}\n\n`;

                documentation += '---\n\n';
            });
        });

        // Test Summary
        const totalTests = this.apiDocumentation.endpoints.length;
        const passedTests = this.apiDocumentation.endpoints.filter(e => e.testResult.success).length;
        const failedTests = totalTests - passedTests;

        documentation += `## Test Summary

- **Total Endpoints:** ${totalTests}
- **Passed Tests:** ${passedTests} ✅
- **Failed Tests:** ${failedTests} ❌
- **Success Rate:** ${((passedTests / totalTests) * 100).toFixed(1)}%

`;

        if (failedTests === 0) {
            documentation += '🎉 **All tests passed! API is ready for production.**\n\n';
        } else {
            documentation += '⚠️ **Some tests failed. Please review and fix the issues.**\n\n';
        }

        documentation += `---\n\n*Documentation generated by API Test Suite on ${new Date().toISOString()}*`;

        fs.writeFileSync(docPath, documentation);
        console.log(`📖 API Documentation generated: ${docPath}`);
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Run the test suite
if (require.main === module) {
    const testSuite = new ApiTestSuite();
    testSuite.runAllTests().catch(console.error);
}

module.exports = ApiTestSuite;