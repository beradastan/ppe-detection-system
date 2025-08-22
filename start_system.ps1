Param(
    [string]$BackendPort = "8000",
    [string]$FrontendPort = "3000"
)

Write-Host "PPE Detection System başlatılıyor..." -ForegroundColor Cyan

# Backend
Push-Location backend
if (-Not (Test-Path venv)) {
    python -m venv venv
}
& .\venv\Scripts\Activate.ps1
python -m pip install --upgrade pip | Out-Null
pip install fastapi "uvicorn[standard]" ultralytics opencv-python-headless numpy "passlib[bcrypt]" pyjwt "qrcode[pil]" | Out-Null

$backend = Start-Process python -ArgumentList "main.py" -PassThru
Pop-Location

# Frontend
Push-Location frontend
if (-Not (Test-Path node_modules)) {
    npm install --legacy-peer-deps --silent
}
$frontend = Start-Process npm -ArgumentList "start" -PassThru
Pop-Location

Write-Host "Sistem başlatıldı!" -ForegroundColor Green
Write-Host "Frontend: http://localhost:$FrontendPort"
Write-Host "Backend:  http://localhost:$BackendPort"

Write-Host "Durdurmak için bu pencereyi kapatın veya aşağıdaki işlemleri sonlandırın:" -ForegroundColor Yellow
Write-Host "Backend PID: $($backend.Id)"
Write-Host "Frontend PID: $($frontend.Id)"

