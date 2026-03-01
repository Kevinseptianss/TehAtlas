package database

import (
	"context"
	"log"
	"time"

	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

var Client *mongo.Client
var DB *mongo.Database

// Connect establishes connection to MongoDB
func Connect(uri string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	client, err := mongo.Connect(ctx, options.Client().ApplyURI(uri))
	if err != nil {
		return err
	}

	// Ping the database
	if err := client.Ping(ctx, nil); err != nil {
		return err
	}

	Client = client
	DB = client.Database("business_management")

	log.Println("Connected to MongoDB")
	return nil
}

// Disconnect closes the database connection
func Disconnect() error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	return Client.Disconnect(ctx)
}

// GetCollection returns a mongo collection
func GetCollection(name string) *mongo.Collection {
	return DB.Collection(name)
}