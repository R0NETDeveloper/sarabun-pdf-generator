#!/bin/bash
# ============================================
# Sarabun PDF API - Docker Rebuild Script (Linux/Mac)
# ============================================
# Usage: ./docker-rebuild.sh
# Configuration: แก้ไขไฟล์ .env เพื่อเปลี่ยน port และ settings

# Load .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✅ Loaded config from .env"
else
    echo "⚠️ .env file not found, using defaults"
fi

# Set defaults if not defined
HOST_PORT=${HOST_PORT:-8889}
CONTAINER_PORT=${CONTAINER_PORT:-8888}
IMAGE_NAME=${IMAGE_NAME:-sarabun-pdf-api}
IMAGE_TAG=${IMAGE_TAG:-1.0.0}
CONTAINER_NAME=${CONTAINER_NAME:-sarabun-pdf-api}
JAVA_OPTS=${JAVA_OPTS:--Xms256m -Xmx512m}

FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"

echo ""
echo "═══════════════════════════════════════════"
echo "  Sarabun PDF API - Docker Rebuild"
echo "═══════════════════════════════════════════"
echo "  Image:     $FULL_IMAGE_NAME"
echo "  Container: $CONTAINER_NAME"
echo "  Port:      $HOST_PORT -> $CONTAINER_PORT"
echo "═══════════════════════════════════════════"
echo ""

echo "🔄 [1/5] Stopping old container..."
docker stop $CONTAINER_NAME 2>/dev/null

echo "🗑️ [2/5] Removing old container..."
docker rm $CONTAINER_NAME 2>/dev/null

echo "🔨 [3/5] Building new image..."
docker build -t $FULL_IMAGE_NAME .

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "🚀 [4/5] Starting new container..."
docker run -d \
    --name $CONTAINER_NAME \
    -p ${HOST_PORT}:${CONTAINER_PORT} \
    -e "JAVA_OPTS=$JAVA_OPTS" \
    $FULL_IMAGE_NAME

echo "⏳ [5/5] Waiting for startup..."
sleep 10

echo ""
echo "🔍 Health check..."
curl -s http://localhost:${HOST_PORT}/actuator/health

echo ""
echo "═══════════════════════════════════════════"
echo "✅ Done! Container is running"
echo "═══════════════════════════════════════════"
echo ""
echo "📍 API Endpoint:  http://localhost:${HOST_PORT}/api/v1/pdf/generate"
echo "📖 Swagger UI:    http://localhost:${HOST_PORT}/swagger-ui.html"
echo "🧪 API Tester:    http://localhost:${HOST_PORT}/api-tester.html"
echo "❤️ Health Check:  http://localhost:${HOST_PORT}/actuator/health"
echo ""
