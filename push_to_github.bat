@echo off
REM ===============================================================
REM PUSH PROJECT TO GITHUB
REM ===============================================================

echo [*] Checking git status...
git status

echo.
echo [*] Pushing project to https://github.com/jayaparvathi2007/Smart-Inventory-Management-System.git ...
echo [*] (If a GitHub login browser window pops up, please sign in to authorize)
echo.

git push -u origin main

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [+] SUCCESS! All project code is now pushed to GitHub!
    echo [*] View your repository at:
    echo     https://github.com/jayaparvathi2007/Smart-Inventory-Management-System
) else (
    echo.
    echo [-] Push failed. If you need a Personal Access Token (PAT):
    echo     1. Go to https://github.com/settings/tokens
    echo     2. Generate a token (with 'repo' scope)
    echo     3. Run: git remote set-url origin https://YOUR_TOKEN@github.com/jayaparvathi2007/Smart-Inventory-Management-System.git
    echo     4. Run: git push -u origin main
)

echo.
pause
