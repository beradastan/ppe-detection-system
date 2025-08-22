#!/usr/bin/env bash

echo "PPE Detection System başlatılıyor..."

# Backend'i başlat
echo "Backend başlatılıyor..."
cd backend
if [ ! -d "venv" ]; then
  python -m venv venv >/dev/null 2>&1 || true
fi
. venv/bin/activate
python -m pip install --upgrade pip >/dev/null 2>&1 || true
pip install -q fastapi uvicorn[standard] ultralytics opencv-python-headless numpy passlib[bcrypt] pyjwt qrcode[pil]
python main.py &
BACKEND_PID=$!
cd ..

# Frontend'i başlat
echo "Frontend başlatılıyor..."
cd frontend
npm install --legacy-peer-deps --silent
npm start &
FRONTEND_PID=$!
cd ..

echo "Sistem başlatıldı!"
echo "Frontend: http://localhost:3000"
echo "Backend: http://localhost:8000"
echo ""
echo "MacBook Pro M4 Pro için önemli notlar:"
echo "1. Tarayıcıda kamera izni verin"
echo "2. HTTPS kullanmayı deneyin: https://localhost:3000"
echo "3. Kamera seçimi dropdown'ından doğru kamerayı seçin"
echo "4. Safari yerine Chrome veya Firefox kullanın"
echo ""
echo "Sistemi durdurmak için Ctrl+C tuşlayın"

# Sinyal yakalama
trap "echo 'Sistem durduruluyor...'; kill $BACKEND_PID $FRONTEND_PID; exit" INT

# Bekle
wait
