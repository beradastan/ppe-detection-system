# PPE (Kişisel Koruyucu Ekipman) Tespit Sistemi

[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/downloads/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.104+-green.svg)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18.2+-blue.svg)](https://reactjs.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.8.0-purple.svg)](https://kotlinlang.org/)
[![SQLite](https://img.shields.io/badge/SQLite-3.40+-teal.svg)](https://www.sqlite.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**Kişisel Koruyucu Ekipman (PPE) Tespit Sistemi**, iş güvenliği standartlarını artırmak için geliştirilmiş, yapay zeka destekli bir çözümdür. Gerçek zamanlı görüntü işleme ile çalışan sistem, web arayüzü ve mobil uygulama ile entegre çalışmaktadır.

## Özellikler

- **Gerçek Zamanlı Tespit**: YOLOv8 ile anlık PPE analizi
- **Çoklu Platform**: Web ve mobil uygulama desteği
- **Canlı Akış**: Webcam ve IP kamera desteği
- **Kapsamlı Raporlama**: Detaylı geçiş kayıtları ve istatistikler
- **Güvenli**: JWT tabanlı kimlik doğrulama
- **Yüksek Performans**: Asenkron işleme ve WebSocket desteği
- **Çevrimdışı Çalışma**: Mobil uygulamada çevrimdışı destek

## Teknoloji Yığını

### Backend
- **Dil**: Python 3.8+
- **Framework**: FastAPI
- **Veritabanı**: SQLite (geliştirme), PostgreSQL (üretim)
- **AI Modeli**: YOLOv8
- **Kimlik Doğrulama**: JWT
- **API Dokümantasyonu**: Swagger UI, ReDoc

### Frontend
- **Dil**: JavaScript (React 18.2+)
- **Stil**: Tailwind CSS
- **State Yönetimi**: React Context API
- **HTTP İstemcisi**: Axios

### Mobil Uygulama
- **Dil**: Kotlin
- **Mimari**: MVVM
- **Ağ İşlemleri**: Retrofit
- **Görüntü İşleme**: ML Kit

## Hızlı Başlangıç

### Ön Gereksinimler

- Python 3.8+
- Node.js 16+
- Android Studio (mobil uygulama için)
- Git

### Kurulum

1. **Depoyu Klonlayın**
   ```bash
   git clone https://github.com/yourusername/ppe-detection-system.git
   cd ppe-detection-system
   ```

2. **Backend Kurulumu**
   ```bash
   cd backend
   python -m venv venv
   source venv/bin/activate  # Linux/Mac
   .\venv\Scripts\activate  # Windows
   pip install -r requirements.txt
   ```

3. **Frontend Kurulumu**
   ```bash
   cd ../frontend
   npm install
   ```

4. **Mobil Uygulama Kurulumu**
   - Android Studio'yu açın
   - `mobile` klasörünü içe aktarın
   - Gerekli bağımlılıkları senkronize edin

### Çalıştırma

1. **Backend'i Başlatın**
   ```bash
   cd backend
   uvicorn main:app --reload
   ```

2. **Frontend'i Başlatın** (yeni terminalde)
   ```bash
   cd frontend
   npm start
   ```

3. **Mobil Uygulamayı Çalıştırın**
   - Android Studio'da bir emülatör başlatın veya fiziksel cihaz bağlayın
   - Uygulamayı çalıştırın

## Proje Yapısı

```
ppe-detection-system/
├── backend/           # FastAPI sunucusu
├── frontend/          # React web arayüzü
├── mobile/            # Android uygulaması
├── database/          # Veritabanı şemaları ve migrasyonlar
├── config/            # Yapılandırma dosyaları
├── scripts/           # Yardımcı scriptler
└── README.md          # Bu dosya
```

## Detaylı Dokümantasyon

- [Backend Dokümantasyonu](./backend/README.md)
- [Frontend Dokümantasyonu](./frontend/README.md)
- [Mobil Uygulama Dokümantasyonu](./mobile/README.md)
- [Veritabanı Şeması](./database/README.md)

## Test

```bash
# Backend testleri
cd backend
pytest

# Frontend testleri
cd ../frontend
npm test

# Mobil testler
cd ../mobile
./gradlew test
```

## 🤝 Katkıda Bulunma

1. Fork'layın (https://github.com/yourusername/ppe-detection-system/fork)
2. Özellik dalınızı oluşturun (`git checkout -b feature/AmazingFeature`)
3. Değişikliklerinizi commit edin (`git commit -m 'Add some AmazingFeature'`)
4. Dalınıza push işlemi yapın (`git push origin feature/AmazingFeature`)
5. Pull Request açın

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır - detaylar için [LICENSE](LICENSE) dosyasına bakınız.

## İletişim

Eğer herhangi bir sorunuz veya öneriniz varsa lütfen bir issue açın.

---

<div align="center">
  <p>Geliştirilmiştir <a href="https://github.com/yourusername">Sizin Adınız</a> tarafından</p>
  <p>Projeyi beğendiyseniz yıldızlamayı unutmayın!</p>
</div>
