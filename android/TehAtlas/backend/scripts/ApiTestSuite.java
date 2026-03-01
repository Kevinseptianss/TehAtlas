package com.tehatlas.api.test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TehAtlas Backend API Test Suite
 * Tests all API endpoints and generates comprehensive test report
 */
public class ApiTestSuite {

    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String authToken = "";
    private final List<TestResult> testResults = new ArrayList<>();

    public static void main(String[] args) {
        ApiTestSuite testSuite = new ApiTestSuite();
        testSuite.runAllTests();
        testSuite.generateReport();
    }

    public void runAllTests() {
        System.out.println("🚀 Starting TehAtlas Backend API Test Suite");
        System.out.println("==========================================\n");

        // Test Authentication
        testLogin();

        if (!authToken.isEmpty()) {
            // Test Outlet/Cashier endpoints
            testGetOutletItems();
            testAddStock();
            testAdjustStock();
            testCreatePurchaseOrder();
            testGetInventoryTransactions();
            testGetOutletDashboard();
            testGetOutletInventoryItems();
            testCreateSale();
            testGetSales();

            // Test Admin endpoints (should fail for cashier role)
            testAdminAccessControl();
            testGetOutlets();
            testGetAdminDashboard();

            // Test Warehouse endpoints (should fail for cashier role)
            testWarehouseAccessControl();
            testWarehouseDashboard();
            testCreateTransfer();
            testGetTransfers();
        }

        // All tests completed
    }

    private void testLogin() {
        System.out.println("Testing Authentication...");
        TestResult result = new TestResult("POST /api/auth/login", "User Login");

        try {
            String requestBody = "{\n" +
                "    \"username\": \"testuser\",\n" +
                "    \"password\": \"testpass\"\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    authToken = jsonResponse.get("data").get("token").asText();
                    result.setSuccess(true);
                    result.setResponse("Login successful, token received");
                } else {
                    result.setResponse("Login failed: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetOutletItems() {
        TestResult result = new TestResult("GET /api/outlet/items", "Get Outlet Items");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/items"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Retrieved " + jsonResponse.get("data").size() + " items");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testAddStock() {
        TestResult result = new TestResult("POST /api/outlet/inventory/add", "Add Stock");

        try {
            String requestBody = "{\n" +
                "    \"item_id\": \"test-item-1\",\n" +
                "    \"quantity\": 25,\n" +
                "    \"supplier\": \"Test Supplier\",\n" +
                "    \"unit_cost\": 10.50\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/inventory/add"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Stock added successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testAdjustStock() {
        TestResult result = new TestResult("POST /api/outlet/inventory/adjust", "Adjust Stock");

        try {
            String requestBody = "{\n" +
                "    \"item_id\": \"test-item-1\",\n" +
                "    \"adjustment\": -5,\n" +
                "    \"reason\": \"Damaged goods\"\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/inventory/adjust"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Stock adjusted successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testCreatePurchaseOrder() {
        TestResult result = new TestResult("POST /api/outlet/purchases", "Create Purchase Order");

        try {
            String requestBody = "{\n" +
                "    \"supplier_id\": \"supplier-1\",\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"item_name\": \"Coffee Beans\",\n" +
                "            \"quantity\": 50,\n" +
                "            \"unit_cost\": 12.50\n" +
                "        }\n" +
                "    ]\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/purchases"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Purchase order created successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetInventoryTransactions() {
        TestResult result = new TestResult("GET /api/outlet/inventory/transactions", "Get Inventory Transactions");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/inventory/transactions"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Retrieved " + jsonResponse.get("data").size() + " transactions");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetOutletDashboard() {
        TestResult result = new TestResult("GET /api/outlet/dashboard", "Get Outlet Dashboard");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/dashboard"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Dashboard data retrieved successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testAdminAccessControl() {
        TestResult result = new TestResult("GET /api/admin/users", "Admin Access Control Test");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/admin/users"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Admin access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testWarehouseAccessControl() {
        TestResult result = new TestResult("GET /api/warehouse/items", "Warehouse Access Control Test");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/warehouse/items"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Warehouse access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetOutletInventoryItems() {
        TestResult result = new TestResult("GET /api/outlet/inventory/items", "Get Outlet Inventory Items (Detailed)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/inventory/items"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Retrieved inventory items successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testCreateSale() {
        TestResult result = new TestResult("POST /api/outlet/sales", "Create Sale Transaction");

        try {
            String requestBody = "{\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"item_id\": \"item-1\",\n" +
                "            \"quantity\": 2,\n" +
                "            \"unit_price\": 5.50\n" +
                "        }\n" +
                "    ],\n" +
                "    \"total_amount\": 11.00\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/sales"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Sale created successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetSales() {
        TestResult result = new TestResult("GET /api/outlet/sales/", "Get Sales History");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/outlet/sales/"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                if (jsonResponse.get("success").asBoolean()) {
                    result.setSuccess(true);
                    result.setResponse("Retrieved sales records successfully");
                } else {
                    result.setResponse("API Error: " + jsonResponse.get("message").asText());
                }
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetOutlets() {
        TestResult result = new TestResult("GET /api/admin/outlets", "Get All Outlets (Admin)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/admin/outlets"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Admin access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetAdminDashboard() {
        TestResult result = new TestResult("GET /api/admin/dashboard", "Get Admin Dashboard (Admin)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/admin/dashboard"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Admin access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testWarehouseDashboard() {
        TestResult result = new TestResult("GET /api/warehouse/dashboard", "Get Warehouse Dashboard (Warehouse)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/warehouse/dashboard"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Warehouse access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testCreateTransfer() {
        TestResult result = new TestResult("POST /api/warehouse/transfers", "Create Stock Transfer (Warehouse)");

        try {
            String requestBody = "{\n" +
                "    \"from_outlet_id\": \"outlet-1\",\n" +
                "    \"to_outlet_id\": \"outlet-2\",\n" +
                "    \"items\": [\n" +
                "        {\n" +
                "            \"item_id\": \"item-1\",\n" +
                "            \"quantity\": 10\n" +
                "        }\n" +
                "    ]\n" +
                "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/warehouse/transfers"))
                    .header("Authorization", "Bearer " + authToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Warehouse access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void testGetTransfers() {
        TestResult result = new TestResult("GET /api/warehouse/transfers/", "Get Transfer History (Warehouse)");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/api/warehouse/transfers/"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403) {
                result.setSuccess(true);
                result.setResponse("Access correctly denied for cashier role");
            } else if (response.statusCode() == 200) {
                result.setResponse("Unexpected: Warehouse access granted to cashier role");
            } else {
                result.setResponse("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            result.setResponse("Exception: " + e.getMessage());
        }

        testResults.add(result);
        printResult(result);
    }

    private void printResult(TestResult result) {
        String status = result.isSuccess() ? "✅ PASS" : "❌ FAIL";
        System.out.println(status + " - " + result.getEndpoint());
        System.out.println("   " + result.getDescription());
        System.out.println("   Response: " + result.getResponse());
        System.out.println();
    }

    private void generateReport() {
        System.out.println("📊 Test Report Summary");
        System.out.println("======================");

        int totalTests = testResults.size();
        int passedTests = (int) testResults.stream().filter(TestResult::isSuccess).count();
        int failedTests = totalTests - passedTests;

        System.out.println("Total Tests: " + totalTests);
        System.out.println("Passed: " + passedTests + " ✅");
        System.out.println("Failed: " + failedTests + " ❌");
        System.out.println("Success Rate: " + String.format("%.1f", (passedTests * 100.0) / totalTests) + "%");

        System.out.println("\n📋 Detailed Results:");
        System.out.println("===================");

        for (TestResult result : testResults) {
            String status = result.isSuccess() ? "✅" : "❌";
            System.out.println(status + " " + result.getEndpoint() + " - " + result.getDescription());
        }

        System.out.println("\n🎯 Test Suite Completed!");
        if (failedTests == 0) {
            System.out.println("🎉 All tests passed! API is ready for production.");
        } else {
            System.out.println("⚠️  Some tests failed. Please review the issues above.");
        }
    }

    private static class TestResult {
        private final String endpoint;
        private final String description;
        private boolean success;
        private String response;

        public TestResult(String endpoint, String description) {
            this.endpoint = endpoint;
            this.description = description;
            this.success = false;
            this.response = "";
        }

        public String getEndpoint() { return endpoint; }
        public String getDescription() { return description; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
    }
}