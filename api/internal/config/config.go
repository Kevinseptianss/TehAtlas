package config

import (
	"os"
)

// Config holds application configuration
type Config struct {
	MongoURI string
	JWTSecret string
	Port      string
}

// Load loads configuration from environment variables
func Load() *Config {
	return &Config{
		MongoURI: getEnv("MONGO_URI", "mongodb://mongo:27017"),
		JWTSecret: getEnv("JWT_SECRET", "your-secret-key"),
		Port:      getEnv("PORT", "8080"),
	}
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}