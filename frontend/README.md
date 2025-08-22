# PPE Detection System - Frontend

Bu proje, Kişisel Koruyucu Ekipman (PPE) tespit sisteminin React tabanlı web arayüzüdür.

## 🚀 Hızlı Başlangıç

### Gereksinimler
- Node.js 16+
- npm veya yarn

### Kurulum

```bash
# Bağımlılıkları yükleyin
npm install

# Geliştirme sunucusunu başlatın
npm start
```

### Üretim Build'i

```bash
# Build alın
npm run build

# Build'i yerel olarak çalıştırın
npx serve -s build
```

## 🏗️ Proje Yapısı

```
frontend/
├── public/              # Statik dosyalar
├── src/
│   ├── assets/         # Görseller ve ikonlar
│   ├── components/     # Yeniden kullanılabilir bileşenler
│   ├── context/        # React context'leri
│   ├── hooks/          # Özel React hook'ları
│   ├── pages/          # Sayfa bileşenleri
│   ├── services/       # API istekleri
│   ├── styles/         # Global stiller
│   ├── utils/          # Yardımcı fonksiyonlar
│   ├── App.js          # Ana uygulama bileşeni
│   └── index.js        # Uygulama giriş noktası
├── .env                # Ortam değişkenleri
├── package.json        # Bağımlılıklar ve script'ler
└── tailwind.config.js  # Tailwind CSS yapılandırması
```

## 🌐 Ana Özellikler

- Gerçek zamanlı video akışı ve PPE tespiti
- Kullanıcı yönetim paneli
- Geçiş logları ve istatistikler
- Responsive tasarım
- Karanlık mod desteği
- Çevrimdışı çalışabilme

## 🔧 Geliştirme

### Ortam Değişkenleri

`.env` dosyası oluşturun:

```env
REACT_APP_API_URL=http://localhost:8000
REACT_APP_WS_URL=ws://localhost:8000
```

### Kullanılan Teknolojiler

- React 18
- Tailwind CSS
- Axios (HTTP istekleri)
- React Router (Yönlendirme)
- React Context API (State yönetimi)
- Socket.IO (Gerçek zamanlı güncellemeler)

## 📱 Mobil Uyumluluk

- Tüm modern mobil tarayıcılarla uyumlu
- PWA desteği
- Düşük bant genişliği için optimize edilmiş

## 🧪 Test

```bash
# Testleri çalıştır
npm test

# Test kapsamı raporu oluştur
npm test -- --coverage
```

## 🛠️ Dağıtım

### Nginx ile Dağıtım

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/frontend/build;
        try_files $uri /index.html;
    }

    location /api {
        proxy_pass http://localhost:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

## 📞 Destek

Herhangi bir sorunuz veya öneriniz için lütfen bir issue açın.
