@echo off
echo ======================================
echo   Building CodeSync Microservices
echo ======================================

REM Stop if any error occurs
setlocal enabledelayedexpansion

REM Root directory
cd /d E:\CodeSync-Project\codesync-backend

REM Build order (IMPORTANT)
set services=^
eureka-server ^
admin-server ^
api-gateway ^
auth-service ^
collab-service ^
comment-service ^
execution-service ^
file-service ^
notification-service ^
payment-service ^
project-service ^
version-service

for %%s in (%services%) do (
    echo.
    echo ===============================
    echo Building %%s
    echo ===============================
    
    cd %%s
    call mvnw.cmd clean package -DskipTests

    IF ERRORLEVEL 1 (
        echo ❌ Build failed for %%s
        exit /b 1
    )

    cd ..
)

echo.
echo ======================================
echo ✅ All services built successfully!
echo ======================================
pause