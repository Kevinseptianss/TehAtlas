# TehAtlas API Test Suite & Documentation Generator

This directory contains comprehensive test suites for the TehAtlas backend API, including both Java and JavaScript implementations that test all endpoints and generate documentation.

## 📁 Files Overview

- `ApiTestSuite.java` - Java test suite using built-in HttpClient
- `api-test-suite.js` - JavaScript test suite with documentation generation
- `package.json` - Node.js dependencies and scripts
- `run-tests.sh` - Easy test runner script for both test suites
- `README.md` - This documentation

## 🚀 Quick Start

### Prerequisites

1. **Docker & Docker Compose** - For running the backend services
2. **Java 11+** - For running the Java test suite
3. **Node.js 16+** - For running the JavaScript test suite

### Easy Test Runner (Recommended)

```bash
# Navigate to scripts directory
cd backend/scripts

# Run all tests (both JavaScript and Java)
./run-tests.sh

# Or run specific test suites
./run-tests.sh js    # JavaScript tests only
./run-tests.sh java  # Java tests only
```

### JavaScript Test Suite

```bash
# Navigate to scripts directory
cd scripts

# Install dependencies
npm install

# Run the complete test suite and generate documentation
npm test
```

### Java Test Suite

```bash
# Download Jackson libraries (required for JSON parsing)
cd scripts
curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar
curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar
curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar

# Compile and run
npm run test:java
```

## 🧪 What the Tests Cover

### Authentication
- ✅ User login with JWT token generation
- ✅ Token validation and middleware testing

### Inventory Management (Cashier/Admin)
- ✅ Get outlet inventory items
- ✅ Add stock to inventory
- ✅ Adjust stock levels
- ✅ View inventory transactions
- ✅ Get outlet dashboard data

### Purchase Management
- ✅ Create purchase orders
- ✅ Purchase order validation

### Access Control
- ✅ Role-based access control testing
- ✅ Admin endpoint restrictions
- ✅ Warehouse endpoint restrictions

### Public Endpoints
- ✅ Item search functionality
- ✅ Global stock transaction viewing

## 📊 Test Results

Both test suites provide:
- ✅ **Real-time feedback** - See pass/fail status for each test
- ✅ **Detailed responses** - View actual API responses
- ✅ **Performance metrics** - Response times for each endpoint
- ✅ **Success/failure counts** - Overall test statistics

### Sample Output
```
🚀 Starting TehAtlas Backend API Test Suite
========================================================================

✅ PASS - POST /api/auth/login
   User Authentication
   Response: Login successful, token received
   Duration: 245ms

✅ PASS - GET /api/outlet/items
   Get Outlet Inventory Items
   Response: Retrieved 2 items
   Duration: 123ms

📊 Test Report Summary
======================
Total Tests: 10
Passed: 10 ✅
Failed: 0 ❌
Success Rate: 100.0%
```

## 📖 Documentation Generation

The JavaScript test suite automatically generates comprehensive API documentation:

- **API_DOCUMENTATION.md** - Complete API reference
- **Endpoint details** - Methods, paths, authentication requirements
- **Request/response examples** - Sample data structures
- **Test status** - Real-time test results included
- **Role permissions** - Access control information

### Documentation Features
- 📋 **Organized by category** - Authentication, Inventory, etc.
- 🔒 **Security information** - Auth requirements and role permissions
- ✅ **Test status indicators** - Visual pass/fail indicators
- 📊 **Performance data** - Response times and test history

## 🏗️ Architecture

### Java Test Suite
- Uses Java 11+ built-in `HttpClient`
- Jackson library for JSON parsing
- Comprehensive error handling
- Clean, readable test output

### JavaScript Test Suite
- Node.js with Axios for HTTP requests
- Automatic Docker container management
- Documentation generation
- Service health checking

## 🔧 Configuration

### Environment Variables
The tests automatically start Docker services, but you can configure:

```bash
# Custom base URL (default: http://localhost:8080)
BASE_URL=http://localhost:8080

# Docker compose file location
DOCKER_COMPOSE_PATH=../docker-compose.yml
```

### Test Data
Tests use mock data that works with the current backend implementation:
- Username: `testuser`
- Password: `testpass`
- Item IDs: `test-item-1`, `item-1`, `item-2`
- Supplier IDs: `supplier-1`

## 🚨 Troubleshooting

### Common Issues

1. **Port 8080 already in use**
   ```bash
   # Kill process using port 8080
   lsof -ti:8080 | xargs kill -9
   ```

2. **Docker permission denied**
   ```bash
   # Add user to docker group
   sudo usermod -aG docker $USER
   # Logout and login again
   ```

3. **Java compilation errors**
   ```bash
   # Ensure Java 11+ is installed
   java -version
   javac -version
   ```

4. **Node.js dependencies**
   ```bash
   # Clear npm cache and reinstall
   npm cache clean --force
   rm -rf node_modules package-lock.json
   npm install
   ```

## 📈 CI/CD Integration

Both test suites can be integrated into CI/CD pipelines:

### GitHub Actions Example
```yaml
- name: Run API Tests
  run: |
    cd backend/scripts
    npm install
    npm test
```

### Jenkins Pipeline
```groovy
stage('API Tests') {
    steps {
        dir('backend/scripts') {
            sh 'npm install'
            sh 'npm test'
        }
    }
}
```

## 🎯 Best Practices

1. **Run tests before deployment** - Ensure API stability
2. **Review generated documentation** - Keep API docs up-to-date
3. **Monitor test performance** - Track response time trends
4. **Update test data** - Reflect real API changes
5. **Regular test execution** - Catch regressions early

## 📞 Support

For issues with the test suites:
1. Check the troubleshooting section above
2. Verify Docker and backend services are running
3. Review the generated logs and error messages
4. Ensure all dependencies are properly installed

---

**Happy Testing! 🎉**

*Generated by TehAtlas Development Team*