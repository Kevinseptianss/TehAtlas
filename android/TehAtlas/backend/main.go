package main

import (
	"log"
	"net/http"

	"github.com/kevinseptian/tehatlas-backend/config"
	"github.com/kevinseptian/tehatlas-backend/database"
	"github.com/kevinseptian/tehatlas-backend/routes"
)

func main() {
	// Load configuration
	config.LoadConfig()

	// Initialize database
	database.InitDB()

	// Setup routes
	mux := routes.SetupRoutes()

	// Start server
	log.Printf("Server starting on port %s", config.AppConfig.ServerPort)
	log.Fatal(http.ListenAndServe(":"+config.AppConfig.ServerPort, mux))
}