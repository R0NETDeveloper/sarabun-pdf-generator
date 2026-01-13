# ============================================
# Sarabun PDF API - Docker Rebuild Script (PowerShell)
# ============================================
# Usage: .\docker-rebuild.ps1
# Configuration: แก้ไขไฟล์ .env เพื่อเปลี่ยน port และ settings

# Load .env file
$envFile = ".\.env"
$config = @{}

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            $config[$key] = $value
        }
    }
    Write-Host "✅ Loaded config from .env" -ForegroundColor Green
} else {
    Write-Host "⚠️ .env file not found, using defaults" -ForegroundColor Yellow
}

# Get config values with defaults
$HOST_PORT = if ($config.HOST_PORT) { $config.HOST_PORT } else { "8889" }
$CONTAINER_PORT = if ($config.CONTAINER_PORT) { $config.CONTAINER_PORT } else { "8888" }
$IMAGE_NAME = if ($config.IMAGE_NAME) { $config.IMAGE_NAME } else { "sarabun-pdf-api" }
$IMAGE_TAG = if ($config.IMAGE_TAG) { $config.IMAGE_TAG } else { "1.0.0" }
$CONTAINER_NAME = if ($config.CONTAINER_NAME) { $config.CONTAINER_NAME } else { "sarabun-pdf-api" }
$JAVA_OPTS = if ($config.JAVA_OPTS) { $config.JAVA_OPTS } else { "-Xms256m -Xmx512m" }

$fullImageName = "${IMAGE_NAME}:${IMAGE_TAG}"

Write-Host ""
Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Sarabun PDF API - Docker Rebuild" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  Image:     $fullImageName" -ForegroundColor White
Write-Host "  Container: $CONTAINER_NAME" -ForegroundColor White
Write-Host "  Port:      $HOST_PORT -> $CONTAINER_PORT" -ForegroundColor White
Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""

Write-Host "🔄 [1/5] Stopping old container..." -ForegroundColor Yellow
docker stop $CONTAINER_NAME 2>$null

Write-Host "🗑️ [2/5] Removing old container..." -ForegroundColor Yellow
docker rm $CONTAINER_NAME 2>$null

Write-Host "🔨 [3/5] Building new image..." -ForegroundColor Cyan
docker build -t $fullImageName .

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "🚀 [4/5] Starting new container..." -ForegroundColor Green
docker run -d `
    --name $CONTAINER_NAME `
    -p "${HOST_PORT}:${CONTAINER_PORT}" `
    -e "JAVA_OPTS=$JAVA_OPTS" `
    $fullImageName

Write-Host "⏳ [5/5] Waiting for startup..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "🔍 Health check..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:${HOST_PORT}/actuator/health" -Method Get
    Write-Host "   Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "   ⚠️ Health check failed, container may still be starting..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "✅ Done! Container is running" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "📍 API Endpoint:  http://localhost:${HOST_PORT}/api/v1/pdf/generate" -ForegroundColor White
Write-Host "📖 Swagger UI:    http://localhost:${HOST_PORT}/swagger-ui.html" -ForegroundColor White
Write-Host "🧪 API Tester:    http://localhost:${HOST_PORT}/api-tester.html" -ForegroundColor White
Write-Host "❤️ Health Check:  http://localhost:${HOST_PORT}/actuator/health" -ForegroundColor White
Write-Host ""
