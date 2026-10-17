# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - RUN SCRIPT (POWERSHELL)
# ===============================================================

if (!(Test-Path -Path "bin\com\smartinventory\server\EmbeddedServer.class")) {
    Write-Host "[*] Project not compiled yet. Running build.ps1 first..." -ForegroundColor Cyan
    & .\build.ps1
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "`n[*] Starting Smart Inventory Management System..." -ForegroundColor Green
Write-Host "[*] Launching Embedded Server on port 8080...`n" -ForegroundColor Cyan

java -cp "bin;lib/*" com.smartinventory.server.EmbeddedServer
