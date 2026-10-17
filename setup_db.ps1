# ===============================================================
# SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP (POWERSHELL)
# ===============================================================

param(
    [string]$Password = ""
)

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "    SMART INVENTORY MANAGEMENT SYSTEM - DATABASE SETUP" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($Password)) {
    $Password = Read-Host "Enter your MySQL root password (press Enter if blank)"
}

Write-Host "[*] Importing schema and seed data into MySQL..." -ForegroundColor Yellow

$schemaPath = Join-Path $PSScriptRoot "database\schema.sql"

if ([string]::IsNullOrEmpty($Password)) {
    & mysql.exe -u root -e "source $schemaPath"
} else {
    & mysql.exe -u root "-p$Password" -e "source $schemaPath"
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "`n[-] Failed to import database schema. Please check your MySQL root password." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "`n[+] Database 'smart_inventory_db' initialized and seeded successfully!" -ForegroundColor Green

# Update config/db.properties
$configContent = @"
# Smart Inventory Management System - Database Configuration
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/smart_inventory_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
db.username=root
db.password=$Password
db.initialPoolSize=5
db.maxPoolSize=20
"@

Set-Content -Path "config\db.properties" -Value $configContent
Write-Host "[+] Updated config/db.properties with your credentials." -ForegroundColor Green
Write-Host "[*] You can now start the application using: .\run.ps1 or .\run.bat`n" -ForegroundColor Yellow
