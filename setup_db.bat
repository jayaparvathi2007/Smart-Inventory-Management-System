@echo off
REM ===============================================================
REM SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP SCRIPT
REM ===============================================================

echo ===============================================================
echo     SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP
echo ===============================================================

set DB_PASS=%1

if "%DB_PASS%"=="" (
    set /p DB_PASS="Enter your MySQL root password (press Enter if no password): "
)

echo [*] Initializing MySQL Database 'smart_inventory_db'...

if "%DB_PASS%"=="" (
    mysql -u root < database/schema.sql
) else (
    mysql -u root -p%DB_PASS% < database/schema.sql
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [-] Failed to import database schema!
    echo [-] Please check your MySQL root password and ensure MySQL service is running.
    echo [-] Alternatively, open MySQL Workbench or command line and run:
    echo     SOURCE %~dp0database/schema.sql;
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [+] Database and sample tables created successfully!

REM Update config/db.properties with the provided password
(
echo # Smart Inventory Management System - Database Configuration
echo db.driver=com.mysql.cj.jdbc.Driver
echo db.url=jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false^&allowPublicKeyRetrieval=true^&serverTimezone=UTC^&characterEncoding=UTF-8
echo db.username=root
echo db.password=%DB_PASS%
echo db.initialPoolSize=5
echo db.maxPoolSize=20
) > config/db.properties

echo [+] Updated config/db.properties with your credentials.
echo.
echo [*] You can now compile and run the project using: run.bat
echo ===============================================================
pause
