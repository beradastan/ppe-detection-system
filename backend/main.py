import os
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from config import CORS_ORIGINS, SERVER_HOST, SERVER_PORT, logger
from routes import router
from auth import auth_manager

app = FastAPI(title="PPE Detection System", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.include_router(router)
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
static_dir = os.path.join(BASE_DIR, "static")
if not os.path.isdir(static_dir):
    static_dir = os.path.normpath(os.path.join(BASE_DIR, "..", "backend", "static"))
app.mount("/static", StaticFiles(directory=static_dir), name="static")


@app.on_event("startup")
async def startup_event():

    try:
        logger.info("PPE Detection System başlatılıyor...")
        auth_manager.seed_default_users()
        logger.info("Sistem başarıyla başlatıldı!")
    except Exception as e:
        logger.error(f"Sistem başlatma hatası: {e}")
        raise


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host=SERVER_HOST, port=SERVER_PORT, log_level="info")
