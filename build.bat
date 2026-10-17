@echo off
REM ===============================================================
REM SMART INVENTORY MANAGEMENT SYSTEM - BUILD SCRIPT (WINDOWS)
REM ===============================================================

echo [*] Cleaning and preparing bin directory...
if not exist "bin" mkdir "bin"
if not exist "webapp\WEB-INF\classes" mkdir "webapp\WEB-INF\classes"

echo [*] Compiling Java source files...
javac -encoding UTF-8 -cp "lib/*" -d bin src/com/smartinventory/model/*.java src/com/smartinventory/dao/*.java src/com/smartinventory/util/*.java src/com/smartinventory/filter/*.java src/com/smartinventory/servlet/*.java src/com/smartinventory/server/*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [-] Build failed! Check the compiler errors above.
    pause
    exit /b %ERRORLEVEL%
)

echo [*] Copying compiled classes to webapp/WEB-INF/classes for Tomcat/Servlet containers...
xcopy /s /e /y /q bin\* webapp\WEB-INF\classes\ >nul

echo.
echo [+] BUILD SUCCESSFUL! All classes compiled cleanly.
echo [*] To launch the system, run: run.bat
echo ===============================================================
