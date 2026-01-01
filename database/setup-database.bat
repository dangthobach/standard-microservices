@echo off
REM ============================================================================
REM Database Setup Script for Enterprise Microservices (Windows)
REM ============================================================================
REM Purpose: Initialize PostgreSQL database with IAM and Business schemas
REM Usage: setup-database.bat
REM ============================================================================

setlocal enabledelayedexpansion

echo ============================================================================
echo Enterprise Microservices - Database Setup
echo ============================================================================
echo.

REM Configuration (use environment variables or defaults)
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=5432
if "%DB_NAME%"=="" set DB_NAME=postgres
if "%DB_USER%"=="" set DB_USER=postgres
if "%DB_PASSWORD%"=="" set DB_PASSWORD=postgres

echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Database: %DB_NAME%
echo   User: %DB_USER%
echo.

REM Check if PostgreSQL is available
echo Checking PostgreSQL connection...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "\q" 2>nul
if errorlevel 1 (
    echo [X] ERROR: Cannot connect to PostgreSQL
    echo.
    echo Please ensure:
    echo   1. PostgreSQL is running
    echo   2. Connection details are correct
    echo   3. psql command is in PATH
    echo   4. User '%DB_USER%' has CREATE privileges
    echo.
    pause
    exit /b 1
)
echo [OK] PostgreSQL connection successful
echo.

REM Confirm before proceeding
echo WARNING: This will DROP and RECREATE schemas:
echo   - iam_schema (all data will be lost)
echo   - business_schema (all data will be lost)
echo.
set /p confirm="Continue? (yes/no): "
if /i not "%confirm%"=="yes" (
    echo Setup cancelled.
    pause
    exit /b 0
)
echo.

REM Run initialization script
echo Running database initialization script...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f init-schemas-fixed.sql

if errorlevel 1 (
    echo.
    echo [X] ERROR: Database setup failed!
    echo Please check the error messages above.
    pause
    exit /b 1
)

echo.
echo ============================================================================
echo [OK] Database setup completed successfully!
echo ============================================================================
echo.
echo Schemas created:
echo   [OK] iam_schema
echo   [OK] business_schema
echo.
echo Database users created:
echo   [OK] iam_user / iam_password_123
echo   [OK] business_user / business_password_123
echo.
echo Test accounts (password: password123):
echo   [OK] admin@enterprise.com (ADMIN)
echo   [OK] developer@enterprise.com (DEVELOPER)
echo   [OK] user@enterprise.com (USER)
echo.
echo Next steps:
echo   1. Update service application.yml files
echo   2. Start IAM Service
echo   3. Start Business Service
echo   4. Verify dashboard database metrics
echo.
echo Connection strings:
echo   IAM:      jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%?currentSchema=iam_schema
echo   Business: jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%?currentSchema=business_schema
echo ============================================================================
echo.
pause
