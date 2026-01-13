@echo off
REM ============================================
REM Sarabun PDF API - Docker Rebuild Script (Windows CMD)
REM ============================================
REM Usage: docker-rebuild.bat
REM Configuration: แก้ไขไฟล์ .env เพื่อเปลี่ยน port และ settings

REM Load .env file
if exist .env (
    for /f "tokens=1,2 delims==" %%a in ('type .env ^| findstr /v "^#"') do (
        set "%%a=%%b"
    )
    echo [OK] Loaded config from .env
) else (
    echo [WARN] .env file not found, using defaults
)

REM Set defaults if not defined
if not defined HOST_PORT set HOST_PORT=8889
if not defined CONTAINER_PORT set CONTAINER_PORT=8888
if not defined IMAGE_NAME set IMAGE_NAME=sarabun-pdf-api
if not defined IMAGE_TAG set IMAGE_TAG=1.0.0
if not defined CONTAINER_NAME set CONTAINER_NAME=sarabun-pdf-api
if not defined JAVA_OPTS set JAVA_OPTS=-Xms256m -Xmx512m

set FULL_IMAGE_NAME=%IMAGE_NAME%:%IMAGE_TAG%

echo.
echo ===============================================
echo   Sarabun PDF API - Docker Rebuild
echo ===============================================
echo   Image:     %FULL_IMAGE_NAME%
echo   Container: %CONTAINER_NAME%
echo   Port:      %HOST_PORT% -^> %CONTAINER_PORT%
echo ===============================================
echo.

echo [1/5] Stopping old container...
docker stop %CONTAINER_NAME% 2>nul

echo [2/5] Removing old container...
docker rm %CONTAINER_NAME% 2>nul

echo [3/5] Building new image...
docker build -t %FULL_IMAGE_NAME% .

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    exit /b 1
)

echo [4/5] Starting new container...
docker run -d --name %CONTAINER_NAME% -p %HOST_PORT%:%CONTAINER_PORT% -e "JAVA_OPTS=%JAVA_OPTS%" %FULL_IMAGE_NAME%

echo [5/5] Waiting for startup...
timeout /t 10 /nobreak >nul

echo.
echo ===============================================
echo [OK] Done! Container is running
echo ===============================================
echo.
echo API Endpoint:  http://localhost:%HOST_PORT%/api/v1/pdf/generate
echo Swagger UI:    http://localhost:%HOST_PORT%/swagger-ui.html
echo API Tester:    http://localhost:%HOST_PORT%/api-tester.html
echo Health Check:  http://localhost:%HOST_PORT%/actuator/health
echo.

REM Health check
curl -s http://localhost:%HOST_PORT%/actuator/health

pause
