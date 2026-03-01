package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/golang-jwt/jwt/v5"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"github.com/kevinseptian/tehatlas-backend/config"
	"github.com/kevinseptian/tehatlas-backend/database"
	"github.com/kevinseptian/tehatlas-backend/models"
)


func AuthMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			http.Error(w, `{"success": false, "message": "Authorization header required"}`, http.StatusUnauthorized)
			return
		}

		// Check if it starts with "Bearer "
		if !strings.HasPrefix(authHeader, "Bearer ") {
			http.Error(w, `{"success": false, "message": "Invalid authorization format"}`, http.StatusUnauthorized)
			return
		}

		tokenString := strings.TrimPrefix(authHeader, "Bearer ")

		// Parse and validate token
		token, err := jwt.ParseWithClaims(tokenString, &models.Claims{}, func(token *jwt.Token) (interface{}, error) {
			return []byte(config.AppConfig.JWTSecret), nil
		})

		if err != nil || !token.Valid {
			http.Error(w, `{"success": false, "message": "Invalid token"}`, http.StatusUnauthorized)
			return
		}

		claims, ok := token.Claims.(*models.Claims)
		if !ok {
			http.Error(w, `{"success": false, "message": "Invalid token claims"}`, http.StatusUnauthorized)
			return
		}

		// Verify user exists in database
		if collection := database.GetCollection("users"); collection != nil {
			var user models.User
			ctx := context.Background()
			oid, err := primitive.ObjectIDFromHex(claims.UserID)
			if err != nil {
				http.Error(w, `{"success": false, "message": "Invalid user ID"}`, http.StatusUnauthorized)
				return
			}
			err = collection.FindOne(ctx, map[string]interface{}{"_id": oid}).Decode(&user)
			if err != nil {
				http.Error(w, `{"success": false, "message": "User not found"}`, http.StatusUnauthorized)
				return
			}
		} else {
			// If no database, allow for mock mode (but this should not happen in production)
			// For now, we'll allow it to maintain compatibility with existing mock setup
		}

		// Add claims to request context
		ctx := context.WithValue(r.Context(), "user_id", claims.UserID)
		ctx = context.WithValue(ctx, "username", claims.Username)
		ctx = context.WithValue(ctx, "role", claims.Role)
		ctx = context.WithValue(ctx, "outlet_id", claims.OutletID)

		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func RoleMiddleware(next http.Handler, allowedRoles ...string) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		role, ok := r.Context().Value("role").(string)
		if !ok {
			http.Error(w, `{"success": false, "message": "Role not found in context"}`, http.StatusForbidden)
			return
		}

		for _, allowedRole := range allowedRoles {
			if role == allowedRole {
				next.ServeHTTP(w, r)
				return
			}
		}

		http.Error(w, `{"success": false, "message": "Insufficient permissions"}`, http.StatusForbidden)
	})
}

func CORS(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusOK)
			return
		}

		next.ServeHTTP(w, r)
	})
}