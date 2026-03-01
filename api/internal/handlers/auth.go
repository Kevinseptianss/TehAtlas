package handlers

import (
	"net/http"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"github.com/golang-jwt/jwt/v5"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

var jwtSecret = []byte("9EEDrnaVy+0086EJY4vD2l5tzTUIhc1KLluzk6D+Zb0=") // Temporary hardcoded for testing

// SetJWTSecret sets the JWT secret key
func SetJWTSecret(secret []byte) {
	jwtSecret = secret
}

// Login handles user authentication
func Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request format",
		})
		return
	}

	collection := database.GetCollection("users")
	var user models.User

	err := collection.FindOne(c, bson.M{"username": req.Username}).Decode(&user)
	if err != nil {
		c.JSON(http.StatusUnauthorized, models.APIResponse{
			Success: false,
			Message: "Invalid credentials",
		})
		return
	}

	// Plain text password comparison
	if user.Password != req.Password {
		c.JSON(http.StatusUnauthorized, models.APIResponse{
			Success: false,
			Message: "Invalid credentials",
		})
		return
	}

	// Generate JWT token
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"user_id":   user.ID.Hex(),
		"role":      user.Role,
		"outlet_id": user.OutletID,
		"exp":       time.Now().Add(time.Hour * 24 * 30).Unix(),
	})

	tokenString, err := token.SignedString(jwtSecret)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error generating token",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Login successful",
		Data: models.LoginResponse{
			Token: tokenString,
			User:  user,
		},
	})
}

// Logout handles user logout
func Logout(c *gin.Context) {
	// In a stateless JWT system, logout is handled client-side
	// by removing the token
	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Logged out successfully",
	})
}

// GetMe returns current user information
func GetMe(c *gin.Context) {
	userID, exists := c.Get("user_id")
	if !exists {
		c.JSON(http.StatusUnauthorized, models.APIResponse{
			Success: false,
			Message: "User not authenticated",
		})
		return
	}

	userIDStr, ok := userID.(string)
	if !ok {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Invalid user ID format",
		})
		return
	}

	objID, err := primitive.ObjectIDFromHex(userIDStr)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Invalid user ID",
		})
		return
	}

	collection := database.GetCollection("users")
	var user models.User

	err = collection.FindOne(c, bson.M{"_id": objID}).Decode(&user)
	if err != nil {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "User not found",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Data:    user,
	})
}
