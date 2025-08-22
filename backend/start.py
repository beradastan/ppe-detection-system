"""

PPE Detection System Startup Script

Bu script, sistemin tüm bileşenlerini başlatır ve FastAPI uygulamasını çalıştırır.

"""

import sys

import os

import logging

from pathlib import Path

project_root = Path(__file__).parent

sys.path.insert(0, str(project_root))


def setup_logging():

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
        handlers=[logging.StreamHandler(sys.stdout), logging.FileHandler("ppe_system.log")],
    )


def check_dependencies():

    required_modules = ["fastapi", "uvicorn", "ultralytics", "cv2", "torch", "numpy", "pydantic", "passlib", "jwt"]

    missing_modules = []

    for module in required_modules:

        try:

            __import__(module)

        except ImportError:

            missing_modules.append(module)

    if missing_modules:

        print(f"HATA: Eksik modüller: {', '.join(missing_modules)}")

        print("Lütfen 'pip install -r requirements.txt' komutunu çalıştırın.")

        return False

    print("Başarı: Tüm bağımlılıklar mevcut")

    return True


def check_model_file():

    from config import MODEL_PATH

    if os.path.exists(MODEL_PATH):

        print(f"Başarı: Model dosyası bulundu: {MODEL_PATH}")

        return True

    else:

        print(f"HATA: Model dosyası bulunamadı: {MODEL_PATH}")

        print("Lütfen model dosyasının doğru konumda olduğundan emin olun.")

        return False


def main():

    print("PPE Detection System başlatılıyor...")

    print("=" * 50)

    setup_logging()

    logger = logging.getLogger(__name__)

    try:

        if not check_dependencies():

            sys.exit(1)

        if not check_model_file():

            sys.exit(1)

        print("Sistem kontrolleri tamamlandı")

        print("=" * 50)

        from main import app

        import uvicorn

        from config import SERVER_HOST, SERVER_PORT

        print(f"Sunucu başlatılıyor: http://{SERVER_HOST}:{SERVER_PORT}")

        print(f"API Dokümantasyonu: http://{SERVER_HOST}:{SERVER_PORT}/docs")

        print(f"Health Check: http://{SERVER_HOST}:{SERVER_PORT}/health")

        print("=" * 50)

        print("Sunucuyu durdurmak için Ctrl+C tuşlayın")

        print("=" * 50)

        uvicorn.run(app, host=SERVER_HOST, port=SERVER_PORT, log_level="info", reload=False)

    except KeyboardInterrupt:

        print("\nSunucu kullanıcı tarafından durduruldu")

    except Exception as e:

        logger.error(f"Sistem başlatma hatası: {e}")

        print(f"HATA: {e}")

        sys.exit(1)


if __name__ == "__main__":

    main()
