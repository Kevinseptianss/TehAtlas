#!/bin/bash
# deploy-apk.sh — Deploy a new TehAtlas APK to the server
#
# Usage:
#   ./deploy-apk.sh <version_code> <version_name> [release_notes...]
#
# Example:
#   ./deploy-apk.sh 3 "1.1.0" "Bug fixes" "Performance improvements" "New dashboard"

set -e

# ─── Configuration ──────────────────────────────────────────────────────
SERVER_USER="adminroot"
SERVER_HOST="202.74.75.126"
SERVER_APK_DIR="/root/TehAtlas/api/apk"
API_BASE="https://api.tehatlas.my.id"
APK_SOURCE="android/TehAtlas/app/build/outputs/apk/debug/app-debug.apk"

# ─── Parse Arguments ────────────────────────────────────────────────────
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <version_code> <version_name> [release_notes...]"
    echo "Example: $0 3 1.1.0 \"Bug fixes\" \"New feature\""
    exit 1
fi

VERSION_CODE=$1
VERSION_NAME=$2
shift 2

# Collect release notes into JSON array
NOTES_JSON="["
FIRST=true
for note in "$@"; do
    if [ "$FIRST" = true ]; then
        NOTES_JSON="$NOTES_JSON\"$note\""
        FIRST=false
    else
        NOTES_JSON="$NOTES_JSON,\"$note\""
    fi
done
NOTES_JSON="$NOTES_JSON]"

# If no notes provided, use default
if [ "$NOTES_JSON" = "[]" ]; then
    NOTES_JSON='["Update terbaru tersedia"]'
fi

APK_FILENAME="app-debug-v${VERSION_CODE}.apk"
DOWNLOAD_URL="${API_BASE}/download/${APK_FILENAME}"

echo "═══════════════════════════════════════════════════"
echo "  TehAtlas APK Deployment"
echo "═══════════════════════════════════════════════════"
echo "  Version Code : $VERSION_CODE"
echo "  Version Name : $VERSION_NAME"
echo "  APK File     : $APK_FILENAME"
echo "  Download URL : $DOWNLOAD_URL"
echo "  Release Notes: $NOTES_JSON"
echo "═══════════════════════════════════════════════════"

# ─── Step 1: Verify APK exists ──────────────────────────────────────────
if [ ! -f "$APK_SOURCE" ]; then
    echo "❌ APK not found at: $APK_SOURCE"
    echo "   Please build the APK first."
    exit 1
fi

echo ""
echo "📦 Step 1/3: Uploading APK to server..."
ssh ${SERVER_USER}@${SERVER_HOST} "mkdir -p ${SERVER_APK_DIR}"
scp "$APK_SOURCE" "${SERVER_USER}@${SERVER_HOST}:${SERVER_APK_DIR}/${APK_FILENAME}"
echo "✅ APK uploaded successfully"

# ─── Step 2: Get admin token ────────────────────────────────────────────
echo ""
echo "🔑 Step 2/3: Authenticating with API..."
echo "   Enter admin credentials:"
read -p "   Username: " ADMIN_USER
read -s -p "   Password: " ADMIN_PASS
echo ""

TOKEN_RESPONSE=$(curl -s -X POST "${API_BASE}/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"${ADMIN_USER}\",\"password\":\"${ADMIN_PASS}\"}")

TOKEN=$(echo "$TOKEN_RESPONSE" | grep -o '"token":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "❌ Failed to authenticate. Response:"
    echo "$TOKEN_RESPONSE"
    exit 1
fi
echo "✅ Authenticated successfully"

# ─── Step 3: Create version record ─────────────────────────────────────
echo ""
echo "📝 Step 3/3: Publishing version metadata..."

API_RESPONSE=$(curl -s -X POST "${API_BASE}/api/admin/app/version" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${TOKEN}" \
    -d "{
        \"version_code\": ${VERSION_CODE},
        \"version_name\": \"${VERSION_NAME}\",
        \"download_url\": \"${DOWNLOAD_URL}\",
        \"release_notes\": ${NOTES_JSON},
        \"force_update\": false
    }")

SUCCESS=$(echo "$API_RESPONSE" | grep -o '"success":true')

if [ -z "$SUCCESS" ]; then
    echo "❌ Failed to publish version. Response:"
    echo "$API_RESPONSE"
    exit 1
fi

echo "✅ Version published successfully"

# ─── Step 4: Restart nginx to pick up new APK ──────────────────────────
echo ""
echo "🔄 Restarting nginx..."
ssh ${SERVER_USER}@${SERVER_HOST} "cd /root/TehAtlas/api && docker-compose restart nginx"
echo "✅ Nginx restarted"

echo ""
echo "═══════════════════════════════════════════════════"
echo "  ✅ Deployment Complete!"
echo "  Version ${VERSION_NAME} (code ${VERSION_CODE}) is live."
echo "  Download: ${DOWNLOAD_URL}"
echo "═══════════════════════════════════════════════════"
