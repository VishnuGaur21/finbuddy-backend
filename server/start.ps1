Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "Starting Finbuddy Node.js + MongoDB Backend (Port 5000)" -ForegroundColor Green
Write-Host "Connecting to MongoDB Atlas..." -ForegroundColor Yellow
Write-Host "========================================================" -ForegroundColor Cyan

Set-Location $PSScriptRoot
node server.js
