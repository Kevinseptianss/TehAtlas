# deploy-apk.ps1 — Deploy a new TehAtlas APK to the server
#
# Usage:
#   .\deploy-apk.ps1 [release_notes...]
#
# Example:
#   .\deploy-apk.ps1 "Bug fixes", "Performance improvements", "New dashboard"

param (
    [string[]]$ReleaseNotes = @("Update terbaru tersedia")
)

$ErrorActionPreference = "Stop"

# ─── Configuration ──────────────────────────────────────────────────────
$SERVER_USER = "adminroot"
$SERVER_HOST = "202.74.75.126"
$SERVER_APK_DIR = "/home/adminroot/api/apk"
$API_BASE = "https://api.tehatlas.my.id"
$APK_SOURCE = "android\TehAtlas\app\build\outputs\apk\debug\app-debug.apk"

# ─── Verification ───────────────────────────────────────────────────────
if (-not (Test-Path $APK_SOURCE)) {
    Write-Host "[!] APK not found at: $APK_SOURCE" -ForegroundColor Red
    Write-Host "   Please build the APK first (Build > Build Bundle(s) / APK(s) > Build APK(s))."
    exit 1
}

# ─── Step 0: Extract Metadata ───────────────────────────────────────────
Write-Host "[*] Step 0: Extracting metadata from APK..." -ForegroundColor Yellow

# Find aapt.exe in Android SDK
$AAPT_SEARCH_PATH = "$env:LOCALAPPDATA\Android\Sdk\build-tools"
$AAPT_EXE = Get-ChildItem -Path $AAPT_SEARCH_PATH -Filter "aapt.exe" -Recurse | Sort-Object -Descending -Property FullName | Select-Object -First 1 -ExpandProperty FullName

if (-not $AAPT_EXE) {
    Write-Host "[!] aapt.exe not found in $AAPT_SEARCH_PATH" -ForegroundColor Red
    Write-Host "   Please ensure Android SDK Build-Tools are installed."
    exit 1
}

# Extract versionCode and versionName
$Badging = & $AAPT_EXE dump badging $APK_SOURCE
$PackageLine = ($Badging | Select-String -Pattern "package: " | Select-Object -First 1).ToString()

if ($PackageLine -match "versionCode='(\d+)'") {
    $VersionCode = [int]$matches[1]
} else {
    Write-Host "[!] Could not extract versionCode from APK" -ForegroundColor Red
    exit 1
}

if ($PackageLine -match "versionName='([^']+)'") {
    $VersionName = $matches[1]
} else {
    Write-Host "[!] Could not extract versionName from APK" -ForegroundColor Red
    exit 1
}

$APK_FILENAME = "app-latest.apk"
$VERSIONED_FILENAME = "app-debug-v$VersionCode.apk"
$DOWNLOAD_URL = "$API_BASE/download/$APK_FILENAME"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  TehAtlas APK Deployment (Fully Automated)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Version Code : $VersionCode (Extracted)"
Write-Host "  Version Name : $VersionName (Extracted)"
Write-Host "  Static URL   : $DOWNLOAD_URL"
Write-Host "  History File : $VERSIONED_FILENAME"
Write-Host "  Release Notes: $($ReleaseNotes -join ', ')"
Write-Host "===================================================" -ForegroundColor Cyan

# ─── Step 1: Upload APK to server ────────────────────────────────────────
Write-Host "`n[*] Step 1/3: Uploading APK to server..." -ForegroundColor Yellow

# Ensure directory exists on server
ssh "${SERVER_USER}@${SERVER_HOST}" "mkdir -p ${SERVER_APK_DIR}"

# Upload as versioned for history AND as latest for the static link
scp "$APK_SOURCE" "${SERVER_USER}@${SERVER_HOST}:${SERVER_APK_DIR}/${VERSIONED_FILENAME}"
ssh "${SERVER_USER}@${SERVER_HOST}" "cp ${SERVER_APK_DIR}/${VERSIONED_FILENAME} ${SERVER_APK_DIR}/${APK_FILENAME}"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Failed to upload APK via SCP." -ForegroundColor Red
    exit 1
}
Write-Host "[+] APK uploaded successfully" -ForegroundColor Green

# ─── Step 2: Create version record directly in DB ──────────────────────
Write-Host "`n[*] Step 2/3: Publishing version metadata directly to MongoDB..." -ForegroundColor Yellow

# Format release notes as a JS array of strings
$NotesArray = @()
foreach ($note in $ReleaseNotes) {
    # Escape single quotes in the note
    $escapedNote = $note -replace "'", "\'"
    $NotesArray += "'$escapedNote'"
}
$NotesJs = "[" + ($NotesArray -join ", ") + "]"

$MongoCommand = @"
db.app_versions.insertOne({
    version_code: $VersionCode,
    version_name: '$VersionName',
    download_url: '$DOWNLOAD_URL',
    release_notes: $NotesJs,
    force_update: false,
    created_at: new Date()
});
"@

# Run the command via SSH using mongosh, passing the command via stdin to avoid quote nesting issues
$SshCommand = "mongosh business_management --quiet"
$MongoCommand | ssh "${SERVER_USER}@${SERVER_HOST}" "$SshCommand"

if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Failed to update database via SSH." -ForegroundColor Red
    exit 1
}
Write-Host "[+] Version published successfully in database" -ForegroundColor Green

# ─── Step 4: Restart nginx ─────────────────────────────────────────────
Write-Host "`n[*] Step 4/4: Reloading nginx..." -ForegroundColor Yellow
ssh "${SERVER_USER}@${SERVER_HOST}" "sudo systemctl reload nginx"
Write-Host "[+] Nginx reloaded" -ForegroundColor Green

Write-Host "`n===================================================" -ForegroundColor Cyan
Write-Host "  [+] Deployment Complete!" -ForegroundColor Green
Write-Host "  Version $VersionName (code $VersionCode) is live."
Write-Host "  Download Link: $DOWNLOAD_URL" -ForegroundColor White
Write-Host "===================================================" -ForegroundColor Cyan
