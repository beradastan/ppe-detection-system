# Katkıda Bulunma Rehberi

Hoş geldiniz! Bu projeye katkıda bulunmak istediğiniz için teşekkür ederiz. İşte size yardımcı olacak bazı yönergeler.

## Nasıl Katkıda Bulunabilirim?

1. **Hata Bildirimi**
   - Bir hata bulduysanız, önce açılmamış bir issue olup olmadığını kontrol edin.
   - Yeni bir issue açarken, sorunu açıkça tanımlayın ve mümkünse adımları ekleyin.

2. **Yeni Özellik İsteği**
   - Yeni bir özellik eklemek istiyorsanız, önce bir issue açıp tartışmaya başlayın.
   - Özelliğin neden gerekli olduğunu ve nasıl çalışması gerektiğini açıklayın.

3. **Kod Katkısı**
   - Fork oluşturup kendi branch'inizde çalışın.
   - Anlamlı commit mesajları yazın.
   - Pull request açmadan önce testlerin geçtiğinden emin olun.
   - Kodunuzun dokümantasyonunu güncelleyin.

## Geliştirme Ortamı

### Backend

```bash
# Sanal ortam oluşturma
python -m venv venv
source venv/bin/activate  # veya Windows'ta .\venv\Scripts\activate

# Bağımlılıkları yükleme
cd backend
pip install -r requirements.txt
```

### Frontend

```bash
cd frontend
npm install
npm start
```

### Testler

```bash
# Backend testleri
cd backend
pytest

# Frontend testleri
cd frontend
npm test
```

## Kod Stili

- Python için PEP 8 standartlarını takip edin.
- JavaScript/TypeScript için projedeki mevcut stil kurallarını koruyun.
- Anlamlı değişken ve fonksiyon isimleri kullanın.
- Karmaşık fonksiyonlar için dokümantasyon yazın.

## Pull Request Süreci

1. Güncel `main` branch'ini çekin.
2. Anlamlı bir branch ismi ile yeni bir branch oluşturun.
3. Değişikliklerinizi yapın ve test edin.
4. Değişikliklerinizi commit edin.
5. Branch'inizi push edin ve bir Pull Request açın.
6. PR açıklamasında değişikliklerinizi detaylıca anlatın.

## Lisans

Bu projeye katkıda bulunarak, katkılarınızın projenin lisansı altında yayınlanacağını kabul etmiş olursunuz.
