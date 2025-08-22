# PPE Detection System - Backend

Bu proje, Kişisel Koruyucu Ekipman (PPE) tespiti yapan bir sistemin backend kısmıdır. YOLO modeli kullanarak gerçek zamanlı PPE tespiti yapar ve kullanıcı kimlik doğrulaması ile birlikte çalışır.

## Proje Yapısı

Proje, mantıklı bir şekilde ayrılmış modüller halinde organize edilmiştir:

```
backend/
├── main.py              # Ana uygulama dosyası (sadece FastAPI konfigürasyonu)
├── config.py            # Konfigürasyon ve sabitler
├── database.py          # Veritabanı işlemleri
├── auth.py              # Kimlik doğrulama ve JWT işlemleri
├── ppe_detection.py     # PPE tespit sistemi (YOLO modeli)
├── routes.py            # API endpoint'leri
├── models.py            # Pydantic modelleri
├── utils.py             # Yardımcı fonksiyonlar
├── requirements.txt     # Python bağımlılıkları
└── README.md           # Bu dosya
```

## Kurulum

1. **Python sanal ortamı oluşturun:**
   ```bash
   python -m venv venv
   source venv/bin/activate  # Linux/Mac
   # veya
   venv\Scripts\activate     # Windows
   ```

2. **Bağımlılıkları yükleyin:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Uygulamayı çalıştırın:**
   ```bash
   python main.py
   ```

## Modül Açıklamaları

### `config.py`
- Tüm konfigürasyon ayarları
- Veritabanı yolları
- JWT ayarları
- Model konfigürasyonu
- CORS ayarları

### `database.py`
- SQLite veritabanı yönetimi
- Kullanıcı CRUD işlemleri
- Log kayıtları
- Veritabanı şema yönetimi

### `auth.py`
- JWT token oluşturma/doğrulama
- Şifre hash'leme
- Kullanıcı kimlik doğrulaması
- Test kullanıcıları oluşturma

### `ppe_detection.py`
- YOLO model yükleme
- PPE tespit işlemleri
- Uyumluluk analizi
- Cihaz seçimi (CPU/GPU/MPS)

### `routes.py`
- Tüm API endpoint'leri
- HTTP ve WebSocket route'ları
- Request/response işleme

### `models.py`
- Pydantic veri modelleri
- Request/response şemaları
- Veri doğrulama

### `utils.py`
- QR kod oluşturma
- Frame encoding/decoding
- Yardımcı fonksiyonlar

## Konfigürasyon

`config.json` dosyası ile aşağıdaki ayarları yapabilirsiniz:

```json
{
  "server": {
    "host": "0.0.0.0",
    "port": 8000,
    "cors_origins": ["http://localhost:3000"]
  },
  "model": {
    "path": "../runs/detect/train2/weights/best.pt",
    "confidence_threshold": 0.5
  },
  "ppe_rules": {
    "required_items": ["helmet", "vest", "boots"],
    "optional_items": ["glove", "Dust Mask", "Eye Wear", "Shield"]
  }
}
```

## API Endpoint'leri

### Kimlik Doğrulama
- `POST /auth/login` - Kullanıcı girişi
- `POST /auth/register` - Kullanıcı kaydı
- `POST /auth/create_user` - Kullanıcı oluşturma (Admin)

### Kullanıcı İşlemleri
- `GET /user/me` - Mevcut kullanıcı bilgileri
- `GET /user/qr` - Kullanıcı QR kodu
- `GET /users/supervisors` - Supervisor listesi

### PPE Tespiti
- `POST /process_frame` - Frame işleme (HTTP)
- `WebSocket /ws` - Gerçek zamanlı tespit

### Loglar
- `GET /logs` - Log kayıtları
- `GET /logs/stats` - Log istatistikleri
- `GET /logs/{id}/frame` - Log frame'i

## Güvenlik

- JWT tabanlı kimlik doğrulama
- Şifre hash'leme (bcrypt)
- Role-based access control
- CORS koruması

## Test

Sistem başlatıldığında otomatik olarak test kullanıcıları oluşturulur:

- **Ahmet Yılmaz** (Worker): `ahmet_yilmaz` / `AhmetYilmaz!123`
- **Mehmet Demir** (Supervisor): `mehmet_demir` / `MehmetDemir!123`

Test JWT token'ı `dev_token.txt` dosyasına yazılır.

## Loglama

Sistem, kullanıcı başına akıllı loglama yapar:
- Başarılı geçişler: Tek log kaydı
- Başarısız geçişler: En az eksik ekipmanlı log korunur
- Frame fotoğrafları Base64 olarak saklanır

## Performans

- YOLO modeli otomatik cihaz seçimi (CPU/GPU/MPS)
- Veritabanı bağlantı havuzu
- Asenkron işleme
- WebSocket desteği

## Geliştirme

Yeni özellik eklemek için:
1. İlgili modülü bulun
2. Gerekli fonksiyonları ekleyin
3. `routes.py`'de endpoint'i tanımlayın
4. `models.py`'de gerekli şemaları ekleyin

## Destek

Herhangi bir sorun yaşarsanız, lütfen log dosyalarını kontrol edin ve gerekli modüllerde hata ayıklama yapın.
