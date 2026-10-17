# ===============================================================
# PUSH PROJECT TO GITHUB (POWERSHELL)
# ===============================================================

Write-Host "[*] Checking git status..." -ForegroundColor Cyan
git status

Write-Host "`n[*] Pushing project to https://github.com/jayaparvathi2007/Smart-Inventory-Management-System.git ..." -ForegroundColor Green
Write-Host "[*] If a GitHub login window or browser opens, please complete sign-in to authorize.`n" -ForegroundColor Yellow

git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n[+] SUCCESS! All files are now live on GitHub!" -ForegroundColor Green
    Write-Host "[*] https://github.com/jayaparvathi2007/Smart-Inventory-Management-System`n" -ForegroundColor Cyan
} else {
    Write-Host "`n[-] Push did not complete. If you use a GitHub Personal Access Token (PAT):" -ForegroundColor Red
    Write-Host "    git remote set-url origin https://<TOKEN>@github.com/jayaparvathi2007/Smart-Inventory-Management-System.git" -ForegroundColor Yellow
    Write-Host "    git push -u origin main`n" -ForegroundColor Yellow
}
