# deploy-api.ps1 — Deploy the Business Management API to the server
#
# Usage:
#   .\deploy-api.ps1
#

$ErrorActionPreference = "Stop"

# ─── Configuration ──────────────────────────────────────────────────────
$SERVER_USER = "adminroot"
$SERVER_HOST = "202.74.75.126"
$REMOTE_DIR = "/home/adminroot/api"
$TAR_FILE = "api_deploy.tar.gz"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "      TehAtlas API Deployment (Automated)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Target Host : $SERVER_HOST"
Write-Host "  Remote Dir  : $REMOTE_DIR"
Write-Host "===================================================" -ForegroundColor Cyan

# ─── Step 1: Local Build ───────────────────────────────────────────────────
Write-Host "[*] Step 1/3: Building API locally for Linux/amd64..." -ForegroundColor Yellow

# Enter the api directory
Push-Location api

# Build for Linux
$env:GOOS = "linux"
$env:GOARCH = "amd64"
$env:CGO_ENABLED = "0"

go build -v -o business-api cmd/main.go

# Reset environment variables for local session
$env:GOOS = ""
$env:GOARCH = ""
$env:CGO_ENABLED = ""

Pop-Location

if (-not (Test-Path "api\business-api")) {
    Write-Host "[!] Local build failed." -ForegroundColor Red
    exit 1
}

$BinarySize = (Get-Item "api\business-api").Length / 1MB
Write-Host "[+] Build successful ($($BinarySize.ToString("N2")) MB)" -ForegroundColor Green

# ─── Step 2: Upload Binary & Source ────────────────────────────────────────
Write-Host "`n[*] Step 2/3: Uploading binary and source to server..." -ForegroundColor Yellow

# Package the binary and source files (excluding large/local folders and env)
# We include the new binary 'business-api'
tar czf $TAR_FILE -C api --exclude "apk" --exclude "bin" --exclude ".git" --exclude ".env" --exclude $TAR_FILE .

# Upload the package
scp $TAR_FILE "${SERVER_USER}@${SERVER_HOST}:${REMOTE_DIR}/"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Failed to upload package via SCP." -ForegroundColor Red
    Remove-Item $TAR_FILE -ErrorAction SilentlyContinue
    exit 1
}
Write-Host "[+] Package uploaded successfully" -ForegroundColor Green

# ─── Step 3: Deploy & Restart ─────────────────────────────────────────────
Write-Host "`n[*] Step 3/3: Deploying on the server..." -ForegroundColor Yellow

# Extraction and service restart
$RemoteCommands = "cd $REMOTE_DIR && " +
                  "mkdir -p tmp_extract && " +
                  "tar xzmf $TAR_FILE -C tmp_extract --no-same-owner --no-same-permissions && " +
                  "cp -rf tmp_extract/* . && " +
                  "rm -rf tmp_extract $TAR_FILE && " +
                  "chmod +x business-api && " +
                  "sudo systemctl restart tehatlas-api.service"

ssh "${SERVER_USER}@${SERVER_HOST}" "$RemoteCommands"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Remote deployment failed." -ForegroundColor Red
    exit 1
}
Write-Host "[+] Service restarted with new binary" -ForegroundColor Green

# ─── Step 4: Cleanup & Final Check ──────────────────────────────────────
Write-Host "`n[*] Step 4/4: Final checks..." -ForegroundColor Yellow

# Remove local tarball
Remove-Item $TAR_FILE -ErrorAction SilentlyContinue

# Check service status
ssh "${SERVER_USER}@${SERVER_HOST}" "systemctl status tehatlas-api.service --no-pager"

Write-Host "`n===================================================" -ForegroundColor Cyan
Write-Host "  [+] Deployment Complete!" -ForegroundColor Green
Write-Host "  The API is live at http://$SERVER_HOST"
Write-Host "===================================================" -ForegroundColor Cyan
