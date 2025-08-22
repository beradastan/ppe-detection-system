#!/bin/bash

echo "PPE Detection System - Backend Başlatılıyor..."

# Sanal ortam kontrol et
if [ ! -d "venv" ]; then
    echo "Sanal ortam oluşturuluyor..."
    python3 -m venv venv
fi

# Sanal ortamı aktifleştir
echo "Sanal ortam aktifleştiriliyor..."
source venv/bin/activate

# Bağımlılıkları yükle
echo "📚 Bağımlılıklar yükleniyor..."
pip install -r requirements.txt

# Model dosyasını kontrol et
MODEL_PATH="../runs/detect/train2/weights/best.pt"
if [ ! -f "$MODEL_PATH" ]; then
    echo "Model dosyası bulunamadı: $MODEL_PATH"
    echo "Lütfen train2 klasöründe best.pt dosyasının bulunduğundan emin olun."
    exit 1
fi

echo "Model dosyası bulundu: $MODEL_PATH"

# Sunucuyu başlat
echo "FastAPI sunucusu başlatılıyor..."
echo "Demo sayfası: http://localhost:8000/static/index.html"
echo "API dokümantasyonu: http://localhost:8000/docs"
echo ""
echo "Durdurmak için Ctrl+C tuşlayın..."

python -c "import main; main.seed_default_user();" || true
python main.py
