# Makefile for Business Management API

.PHONY: build run clean test docker-build docker-run docker-stop

# Go commands
build:
	go build -o bin/main cmd/main.go

run:
	go run cmd/main.go

clean:
	rm -rf bin/

test:
	go test ./...

# Docker commands
docker-build:
	docker build -t business-management-api .

docker-run:
	docker-compose up -d

docker-stop:
	docker-compose down

docker-logs:
	docker-compose logs -f app

docker-restart:
	docker-compose restart app

# Development
dev:
	docker-compose up --build

# Database
db-init:
	docker-compose exec mongo mongo business_management /docker-entrypoint-initdb.d/init-mongo.js

# Full deployment
deploy: docker-build docker-run
	@echo "Application deployed successfully"
	@echo "API available at: http://localhost"
	@echo "MongoDB available at: localhost:27017"