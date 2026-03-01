#!/bin/bash

# TehAtlas API Test Suite Runner
# This script provides an easy way to run both Java and JavaScript test suites

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 TehAtlas API Test Suite Runner"
echo "=================================="
echo ""

# Function to run JavaScript tests
run_js_tests() {
    echo "📋 Running JavaScript Test Suite..."
    echo "-----------------------------------"

    if ! command -v node &> /dev/null; then
        echo "❌ Node.js is not installed. Please install Node.js 16+ to run JavaScript tests."
        return 1
    fi

    if ! command -v npm &> /dev/null; then
        echo "❌ npm is not installed. Please install npm to run JavaScript tests."
        return 1
    fi

    # Install dependencies if node_modules doesn't exist
    if [ ! -d "node_modules" ]; then
        echo "📦 Installing dependencies..."
        npm install
    fi

    # Run the tests
    npm test
}

# Function to run Java tests
run_java_tests() {
    echo "☕ Running Java Test Suite..."
    echo "----------------------------"

    if ! command -v java &> /dev/null; then
        echo "❌ Java is not installed. Please install Java 11+ to run Java tests."
        return 1
    fi

    if ! command -v javac &> /dev/null; then
        echo "❌ javac is not installed. Please install JDK 11+ to run Java tests."
        return 1
    fi

    # Check if Jackson libraries exist, download if not
    if [ ! -f "jackson-core-2.15.2.jar" ] || [ ! -f "jackson-databind-2.15.2.jar" ] || [ ! -f "jackson-annotations-2.15.2.jar" ]; then
        echo "📦 Downloading Jackson libraries..."
        curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar
        curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar
        curl -O https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar
    fi

    # Start Docker containers for Java tests
    echo "🐳 Starting Docker containers for Java tests..."
    if command -v docker-compose &> /dev/null; then
        docker-compose -f ../docker-compose.yml up -d
    elif command -v docker &> /dev/null && docker compose version &> /dev/null; then
        docker compose -f ../docker-compose.yml up -d
    else
        echo "❌ Docker Compose not found. Please install Docker and Docker Compose."
        return 1
    fi

    # Wait for services to be ready
    echo "⏳ Waiting for services to be ready..."
    sleep 10

    # Compile and run
    echo "🔨 Compiling Java test suite..."
    mkdir -p com/tehatlas/api/test
    javac -cp ".:jackson-core-2.15.2.jar:jackson-databind-2.15.2.jar:jackson-annotations-2.15.2.jar" -d . ApiTestSuite.java

    echo "▶️  Running Java tests..."
    java -cp ".:jackson-core-2.15.2.jar:jackson-databind-2.15.2.jar:jackson-annotations-2.15.2.jar" com.tehatlas.api.test.ApiTestSuite

    # Stop Docker containers
    echo "🐳 Stopping Docker containers..."
    if command -v docker-compose &> /dev/null; then
        docker-compose -f ../docker-compose.yml down
    else
        docker compose -f ../docker-compose.yml down
    fi
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [option]"
    echo ""
    echo "Options:"
    echo "  js        Run JavaScript test suite only"
    echo "  java      Run Java test suite only"
    echo "  all       Run both test suites (default)"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0              # Run all tests"
    echo "  $0 js           # Run JavaScript tests only"
    echo "  $0 java         # Run Java tests only"
}

# Main logic
case "${1:-all}" in
    "js")
        run_js_tests
        ;;
    "java")
        run_java_tests
        ;;
    "all")
        run_js_tests
        echo ""
        echo "=========================================="
        echo ""
        run_java_tests
        ;;
    "help"|"-h"|"--help")
        show_usage
        ;;
    *)
        echo "❌ Invalid option: $1"
        echo ""
        show_usage
        exit 1
        ;;
esac

echo ""
echo "✅ Test execution completed!"