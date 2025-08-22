# Database

Bu klasör PPE Detection System'in veritabanı dosyalarını içerir.

## Dosyalar

- `ppe_logs.db` - SQLite veritabanı dosyası (geçiş logları ve kullanıcılar)

## Yapı

Veritabanı `access_logs` ve `users` tablolarını içerir:

`access_logs` tablosu:
- `id` - Otomatik artan benzersiz ID
- `timestamp` - Log kaydının tarih ve saati
- `can_pass` - Geçiş izni verildi mi
- `status` - Geçiş durumu
- `message` - Sistem mesajı
- `missing_required` - Eksik zorunlu ekipmanlar
- `missing_optional` - Eksik opsiyonel ekipmanlar
- `detected_items` - Tespit edilen ekipmanlar (JSON)
- `person_detected` - Kişi tespit edildi mi
- `confidence_scores` - Tespit güven skorları (JSON)
- `frame_image` - Başarılı geçişlerde çekilen frame (Base64 encoded JPEG)
- `user_id` - Geçişe konu olan kullanıcı ID'si (opsiyonel)
- `username` - Kullanıcı adı (opsiyonel)
- `user_role` - Kullanıcı rolü (opsiyonel)

`users` tablosu:
- `id` - Otomatik artan ID
- `username` - Eşsiz kullanıcı adı
- `password_hash` - Bcrypt ile hashlenmiş parola
- `role` - Kullanıcı rolü (ör. worker, supervisor, admin)
- `qr_payload` - Kullanıcıya özel sabit QR içeriği (ör. {"user":"u","user_role":"r"})
- `qr_image_base64` - QR kodunun base64 PNG formatında görüntüsü (mobil uygulama için)
- `email` - Email adresi (opsiyonel, kayıt için)
- `full_name` - Ad soyad (opsiyonel, kayıt için)
- `is_active` - Aktif/pasif
- `created_at` - Oluşturulma tarihi

## Not

Veritabanı dosyası otomatik olarak backend tarafından oluşturulur ve yönetilir.

**Frame Kaydetme**: Sadece başarılı geçişlerde (`can_pass = true`) frame veritabanına kaydedilir. Frame'ler JPEG formatında, %80 kalitede ve Base64 encoded olarak saklanır.

**Kimlik Doğrulama ve QR**:
- `/auth/register` ile kullanıcı kayıt olabilir (basit seviye, email ve ad soyad opsiyonel).
- `/auth/create_user` ile kullanıcı oluşturulur (şirket tahsisi).
- `/auth/login` ile giriş yapılır, JWT döner.
- `/user/qr` girişli kullanıcı için sabit QR PNG üretir. QR payload formatı: `{ "user": "example_user", "user_role": "example_role" }`.
- PPE akışında QR içeriği `process_frame` body'sine veya `/ws?user=...&user_role=...` query parametrelerine iletilerek kullanıcı bağlamı loglara yazılır.

**Kayıt Sistemi**:
- Basit seviye kayıt formu: username (zorunlu), password (min 6 karakter, zorunlu), email (opsiyonel), full_name (opsiyonel), role (worker/supervisor/admin).
- Kullanıcı adı ve email eşsizlik kontrolü.
- Başarılı kayıt sonrası otomatik giriş ve QR üretimi.
