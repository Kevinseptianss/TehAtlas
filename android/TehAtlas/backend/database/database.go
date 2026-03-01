package database

import (
	"context"
	"log"
	"os"
	"time"

	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

var (
	Client         *mongo.Client
	Database       *mongo.Database
	IsInitialized  = false
	DatabaseName   = "tehatlas"
)

func InitDB() {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	mongoURI := os.Getenv("MONGO_URI")
	if mongoURI == "" {
		mongoURI = "mongodb://localhost:27017"
	}

	clientOptions := options.Client().ApplyURI(mongoURI)
	client, err := mongo.Connect(ctx, clientOptions)
	if err != nil {
		log.Printf("Failed to connect to MongoDB: %v", err)
		log.Println("Falling back to in-memory mode")
		IsInitialized = true
		return
	}

	err = client.Ping(ctx, nil)
	if err != nil {
		log.Printf("Failed to ping MongoDB: %v", err)
		log.Println("Falling back to in-memory mode")
		IsInitialized = true
		return
	}

	Client = client
	Database = client.Database(DatabaseName)
	IsInitialized = true

	log.Println("Database initialized successfully with MongoDB")
}

func GetCollection(collectionName string) *mongo.Collection {
	if Database == nil {
		log.Printf("Warning: Database not connected, returning nil for collection %s", collectionName)
		return nil
	}
	return Database.Collection(collectionName)
}