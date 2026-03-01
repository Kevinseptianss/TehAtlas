package handlers

import (
	"net/http"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

// GetUsers returns all users (without passwords)
func GetUsers(c *gin.Context) {
	collection := database.GetCollection("users")

	cursor, err := collection.Find(c, bson.M{})
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error fetching users",
		})
		return
	}
	defer cursor.Close(c)

	users := []models.User{}
	if err := cursor.All(c, &users); err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error decoding users",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Users retrieved successfully",
		Data:    users,
	})
}

// CreateUser creates a new user with plain text password
func CreateUser(c *gin.Context) {
	var req struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
		Role     string `json:"role" binding:"required"`
		OutletID string `json:"outlet_id,omitempty"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request: " + err.Error(),
		})
		return
	}

	// Validate role
	if req.Role != "admin" && req.Role != "warehouse" && req.Role != "cashier" {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid role. Must be admin, warehouse, or cashier",
		})
		return
	}

	collection := database.GetCollection("users")

	// Check if username already exists
	count, _ := collection.CountDocuments(c, bson.M{"username": req.Username})
	if count > 0 {
		c.JSON(http.StatusConflict, models.APIResponse{
			Success: false,
			Message: "Username already exists",
		})
		return
	}

	user := models.User{
		Username:  req.Username,
		Password:  req.Password, // Plain text
		Role:      req.Role,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	// Set outlet_id if provided
	if req.OutletID != "" {
		oid, err := primitive.ObjectIDFromHex(req.OutletID)
		if err == nil {
			user.OutletID = &oid
		}
	}

	result, err := collection.InsertOne(c, user)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Error creating user",
		})
		return
	}

	user.ID = result.InsertedID.(primitive.ObjectID)

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "User created successfully",
		Data:    user,
	})
}

// UpdateUser updates an existing user
func UpdateUser(c *gin.Context) {
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid user ID",
		})
		return
	}

	var req map[string]interface{}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request",
		})
		return
	}

	update := bson.M{"updated_at": time.Now()}
	if v, ok := req["username"]; ok {
		update["username"] = v
	}
	if v, ok := req["password"]; ok {
		update["password"] = v // Plain text
	}
	if v, ok := req["role"]; ok {
		update["role"] = v
	}

	collection := database.GetCollection("users")
	result, err := collection.UpdateOne(c, bson.M{"_id": id}, bson.M{"$set": update})
	if err != nil || result.MatchedCount == 0 {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "User not found",
		})
		return
	}

	var user models.User
	collection.FindOne(c, bson.M{"_id": id}).Decode(&user)

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "User updated successfully",
		Data:    user,
	})
}

// DeleteUser deletes a user
func DeleteUser(c *gin.Context) {
	id, err := primitive.ObjectIDFromHex(c.Param("id"))
	if err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid user ID",
		})
		return
	}

	collection := database.GetCollection("users")
	result, err := collection.DeleteOne(c, bson.M{"_id": id})
	if err != nil || result.DeletedCount == 0 {
		c.JSON(http.StatusNotFound, models.APIResponse{
			Success: false,
			Message: "User not found",
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "User deleted successfully",
	})
}
