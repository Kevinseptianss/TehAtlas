package handlers

import (
	"context"
	"net/http"
	"time"

	"business-management-api/internal/database"
	"business-management-api/internal/models"

	"github.com/gin-gonic/gin"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// GetLatestVersion returns the latest app version info (public, no auth required)
func GetLatestVersion(c *gin.Context) {
	collection := database.GetCollection("app_versions")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Find the document with the highest version_code
	opts := options.FindOne().SetSort(bson.D{{Key: "version_code", Value: -1}})
	var version models.AppVersion
	err := collection.FindOne(ctx, bson.M{}, opts).Decode(&version)
	if err != nil {
		c.JSON(http.StatusOK, models.APIResponse{
			Success: true,
			Message: "No version available",
			Data:    nil,
		})
		return
	}

	c.JSON(http.StatusOK, models.APIResponse{
		Success: true,
		Message: "Latest version retrieved",
		Data:    version,
	})
}

// CreateAppVersion inserts a new app version record (admin only)
func CreateAppVersion(c *gin.Context) {
	var input struct {
		VersionCode  int      `json:"version_code" binding:"required"`
		VersionName  string   `json:"version_name" binding:"required"`
		DownloadURL  string   `json:"download_url" binding:"required"`
		ReleaseNotes []string `json:"release_notes"`
		ForceUpdate  bool     `json:"force_update"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, models.APIResponse{
			Success: false,
			Message: "Invalid request: " + err.Error(),
		})
		return
	}

	version := models.AppVersion{
		VersionCode:  input.VersionCode,
		VersionName:  input.VersionName,
		DownloadURL:  input.DownloadURL,
		ReleaseNotes: input.ReleaseNotes,
		ForceUpdate:  input.ForceUpdate,
		CreatedAt:    time.Now(),
	}

	collection := database.GetCollection("app_versions")
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	result, err := collection.InsertOne(ctx, version)
	if err != nil {
		c.JSON(http.StatusInternalServerError, models.APIResponse{
			Success: false,
			Message: "Failed to create version: " + err.Error(),
		})
		return
	}

	version.ID = result.InsertedID.(primitive.ObjectID)

	c.JSON(http.StatusCreated, models.APIResponse{
		Success: true,
		Message: "Version created successfully",
		Data:    version,
	})
}
