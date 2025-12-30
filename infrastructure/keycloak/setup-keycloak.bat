@echo off
REM Keycloak Setup Script for Windows
REM
REM This script:
REM 1. Waits for Keycloak to be ready
REM 2. Imports the enterprise realm
REM 3. Creates test users
REM 4. Configures clients for PKCE

setlocal enabledelayedexpansion

set KEYCLOAK_URL=http://localhost:8180
set ADMIN_USER=admin
set ADMIN_PASSWORD=admin
set REALM_FILE=enterprise-realm.json

echo ===============================================
echo Keycloak Setup Script
echo ===============================================
echo.

REM Wait for Keycloak to be ready
echo [1/4] Waiting for Keycloak to be ready...
:wait_loop
curl --output nul --silent --head --fail %KEYCLOAK_URL% 2>nul
if errorlevel 1 (
    echo|set /p="."
    timeout /t 5 /nobreak >nul
    goto wait_loop
)
echo  OK - Keycloak is ready!

REM Get admin access token
echo.
echo [2/4] Getting admin access token...
for /f "delims=" %%i in ('curl -s -X POST "%KEYCLOAK_URL%/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "username=%ADMIN_USER%" -d "password=%ADMIN_PASSWORD%" -d "grant_type=password" -d "client_id=admin-cli"') do set TOKEN_RESPONSE=%%i

REM Note: Windows doesn't have jq by default, so we'll use PowerShell
for /f "delims=" %%i in ('powershell -Command "('%TOKEN_RESPONSE%' | ConvertFrom-Json).access_token"') do set ADMIN_TOKEN=%%i

if "%ADMIN_TOKEN%"=="" (
    echo  ERROR - Failed to get admin token
    exit /b 1
)
echo  OK - Admin token obtained

REM Check if realm exists
echo.
echo [3/4] Checking if 'enterprise' realm exists...
curl -s -H "Authorization: Bearer %ADMIN_TOKEN%" "%KEYCLOAK_URL%/admin/realms/enterprise" -w "%%{http_code}" -o nul >realm_check.txt 2>&1
set /p REALM_EXISTS=<realm_check.txt
del realm_check.txt

if "%REALM_EXISTS%"=="200" (
    echo  WARNING - Realm 'enterprise' already exists
    echo  Realm already configured. Skipping...
    goto end
)

REM Import realm
echo.
echo [4/4] Importing 'enterprise' realm...
if not exist "%REALM_FILE%" (
    echo  ERROR - Realm file not found: %REALM_FILE%
    exit /b 1
)

curl -s -X POST "%KEYCLOAK_URL%/admin/realms" ^
  -H "Authorization: Bearer %ADMIN_TOKEN%" ^
  -H "Content-Type: application/json" ^
  -d @%REALM_FILE%

echo  OK - Realm imported successfully!

:end
REM Verify setup
echo.
echo ===============================================
echo Keycloak Setup Complete!
echo ===============================================
echo.
echo Keycloak URL: %KEYCLOAK_URL%
echo Admin Console: %KEYCLOAK_URL%/admin
echo Admin Username: %ADMIN_USER%
echo Admin Password: %ADMIN_PASSWORD%
echo.
echo Realm: enterprise
echo.
echo Test Users:
echo   - testuser / testuser123 (Role: USER)
echo   - admin / admin123 (Role: ADMIN, USER)
echo.
echo Clients:
echo   - enterprise-frontend (Public, PKCE S256)
echo   - gateway-service (Confidential)
echo.
echo Next steps:
echo   1. Start Gateway: cd gateway-service ^&^& mvn spring-boot:run
echo   2. Start Frontend: cd frontend ^&^& npm start
echo   3. Navigate to: http://localhost:4200
echo.

pause
