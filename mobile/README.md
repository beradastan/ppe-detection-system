# PPE Detection System - Mobile App

Bu proje, Kişisel Koruyucu Ekipman (PPE) tespit sisteminin Android uygulamasıdır.

## Özellikler

- Gerçek zamanlı PPE tespiti
- Çevrimdışı çalışabilme
- QR kod ile hızlı giriş
- Geçiş geçmişi ve istatistikler
- Karanlık mod desteği
- Çoklu dil desteği

## Gereksinimler

- Android Studio Giraffe | 2022.3.1 veya üzeri
- JDK 17
- Android SDK 34
- Kotlin 1.9.0+

## Kurulum

1. Projeyi klonlayın:
   ```bash
   git clone [repo-url]
   cd mobile
   ```

2. `local.properties` dosyasını oluşturun:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

3. Android Studio'da projeyi açın
4. Gerekli bağımlılıkları senkronize edin
5. Uygulamayı bir cihazda veya emülatörde çalıştırın

## Proje Yapısı

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/ppemobile/
│   │   │   ├── data/           # Veri katmanı
│   │   │   │   ├── local/      # Yerel veritabanı
│   │   │   │   ├── remote/     # API istekleri
│   │   │   │   └── repository/ # Veri kaynakları
│   │   │   ├── di/            # Dependency Injection
│   │   │   ├── domain/        # İş mantığı
│   │   │   ├── ui/            # Kullanıcı arayüzü
│   │   │   │   ├── components # Yeniden kullanılabilir bileşenler
│   │   │   │   ├── theme/     # Tema ve stiller
│   │   │   │   └── *.kt      # Ekranlar
│   │   │   └── App.kt        # Uygulama giriş noktası
│   │   └── res/              # Kaynaklar
│   └── test/                 # Unit testler
└── build.gradle.kts          # Modül seviyesi build dosyası
```

## Teknoloji Yığını

- **Dil**: Kotlin
- **Mimari**: MVVM (Model-View-ViewModel)
- **Jetpack Bileşenleri**:
  - Compose - Modern UI toolkit
  - ViewModel - UI veri yönetimi
  - Room - Yerel veritabanı
  - Hilt - Dependency Injection
  - Navigation - Ekran geçişleri
  - DataStore - Tercihler yönetimi
- **Ağ İşlemleri**: Retrofit + OkHttp
- **Görüntü İşleme**: ML Kit, CameraX
- **QR Kod**: ZXing
- **Test**: JUnit, MockK, Espresso

## Yapılandırma

### API Ayarları

`app/src/main/res/values/config.xml` dosyasında API URL'ini yapılandırın:

```xml
<resources>
    <string name="api_base_url">http://your-api-url.com/api/</string>
</resources>
```

### Gizli Anahtarlar

Gizli anahtarlar için `local.properties` dosyasını kullanın:

```properties
# local.properties
debug.keystore.password=your_password
api.key=your_api_key
```

## Test

### Unit Testler

```bash
./gradlew test
```

### UI Testleri

```bash
./gradlew connectedAndroidTest
```

### Test Kapsamı Raporu

```bash
./gradlew createDebugCoverageReport
```

## Derleme

### Debug APK

```bash
./gradlew assembleDebug
```

### Release APK

```bash
./gradlew assembleRelease
```

### Bundle (Play Store için)

```bash
./gradlew bundleRelease
```

## Sürekli Entegrasyon

Proje GitHub Actions ile entegredir. Her push işleminde:
1. Derleme
2. Testler
3. Lint
4. Test kapsamı raporu

işlemleri otomatik olarak çalıştırılır.

## Lisans

Bu proje MIT lisansı altında lisanslanmıştır - detaylar için [LICENSE](../LICENSE) dosyasına bakınız.
