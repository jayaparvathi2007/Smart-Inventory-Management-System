# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - BUILD SCRIPT (POWERSHELL)
# ===============================================================

Write-Host "[*] Preparing bin directory..." -ForegroundColor Cyan
if (!(Test-Path -Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
if (!(Test-Path -Path "webapp\WEB-INF\classes")) { New-Item -ItemType Directory -Path "webapp\WEB-INF\classes" -Force | Out-Null }

Write-Host "[*] Compiling Java source files..." -ForegroundColor Cyan
$javaFiles = Get-ChildItem -Recurse -Filter *.java -Path "src" | Select-Object -ExpandProperty FullName

javac -encoding UTF-8 -cp "lib/*" -d bin $javaFiles

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[-] Build failed! Check the compiler errors above." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[*] Copying compiled classes to webapp\WEB-INF\classes..." -ForegroundColor Cyan
Copy-Item -Path "bin\*" -Destination "webapp\WEB-INF\classes\" -Recurse -Force

Write-Host "`n[+] BUILD SUCCESSFUL! All classes compiled cleanly." -ForegroundColor Green
Write-Host "[*] To run the system, type: .\run.ps1 or .\run.bat`n" -ForegroundColor Yellow
