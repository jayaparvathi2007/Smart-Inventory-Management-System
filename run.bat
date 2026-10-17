@echo off
REM ===============================================================
REM SMART INVENTORY MANAGEMENT SYSTEM - RUN SCRIPT (WINDOWS)
REM ===============================================================

if not exist "bin\com\smartinventory\server\EmbeddedServer.class" (
    echo [*] Project is not compiled yet. Running build.bat first...
    call build.bat
    if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
)

echo.
echo [*] Starting Smart Inventory Management System...
echo [*] Launching Embedded Server on port 8080...
echo.

java -cp "bin;lib/*" com.smartinventory.server.EmbeddedServer

pause
