from fastapi import APIRouter, Depends, HTTPException

from fastapi.responses import StreamingResponse

from typing import Dict, Any, Optional

import json

import qrcode

from io import BytesIO

from models import (
    FrameRequest,
    LoginRequest,
    RegisterRequest,
    UserResponse,
    LoginResponse,
    RegisterResponse,
    LogsResponse,
    LogStatsResponse,
    SupervisorResponse,
)

from auth import auth_manager

from database import db_manager

from ppe_detection import ppe_system

from utils import decode_base64_frame, encode_frame_to_base64, validate_user_data, generate_qr_base64

from config import logger

router = APIRouter()


@router.post("/auth/login", response_model=LoginResponse)
async def login(body: LoginRequest):

    try:

        user = auth_manager.authenticate_user(body.username, body.password)

        if not user:

            raise HTTPException(status_code=401, detail="Hatalı kimlik bilgileri")

        token = auth_manager.create_user_token(user)

        return {
            "access_token": token,
            "token_type": "bearer",
            "user": {"id": user["id"], "username": user["username"], "role": user["role"]},
        }

    except HTTPException:

        raise

    except Exception as e:

        logger.error(f"login hatası: {e }")

        raise HTTPException(status_code=500, detail="Giriş başarısız")


@router.post("/auth/register", response_model=RegisterResponse)
async def register(body: RegisterRequest):

    try:

        validation = validate_user_data(body.username, body.password, body.role, body.email, body.full_name)

        if not validation["is_valid"]:

            raise HTTPException(status_code=400, detail="; ".join(validation["errors"]))

        if db_manager.get_user_by_username(body.username):

            raise HTTPException(status_code=400, detail="Bu kullanıcı adı zaten kullanılıyor")

        if body.email:

            try:

                conn = db_manager.get_connection_with_row_factory()

                cur = conn.cursor()

                cur.execute("SELECT 1 FROM users WHERE email = ? AND is_active = 1", (body.email,))

                exists = cur.fetchone() is not None

                conn.close()

                if exists:

                    raise HTTPException(status_code=400, detail="Bu email adresi zaten kullanılıyor")

            except HTTPException:

                raise

            except Exception as e:

                logger.error(f"Email kontrol hatası: {e }")

        role = body.role.strip().lower()

        supervisor_id_to_set: Optional[int] = None

        if role == "worker":

            if body.supervisor_id is None:

                raise HTTPException(status_code=400, detail="İşçiler için supervisor seçimi zorunludur")

            supervisor = db_manager.get_user_by_id(body.supervisor_id)

            if not supervisor or supervisor["role"] != "supervisor":

                raise HTTPException(status_code=400, detail="Geçersiz supervisor_id")

            supervisor_id_to_set = body.supervisor_id

        else:

            supervisor_id_to_set = None

        password_hash = auth_manager.hash_password(body.password)

        qr_payload = json.dumps({"user": body.username, "user_role": role})

        qr_image_base64 = generate_qr_base64(qr_payload)

        logger.info(
            f"Kullanıcı kayıt verisi: username={body .username }, role={role }, email={body .email }, full_name={body .full_name }, supervisor_id={supervisor_id_to_set }"
        )

        user_id = db_manager.create_user(
            username=body.username,
            password_hash=password_hash,
            role=role,
            qr_payload=qr_payload,
            qr_image_base64=qr_image_base64,
            email=body.email,
            full_name=body.full_name,
            supervisor_id=supervisor_id_to_set,
        )

        logger.info(f"Kullanıcı başarıyla oluşturuldu: ID={user_id }, username={body .username }")

        token = auth_manager.create_user_token({"id": user_id, "username": body.username, "role": role})

        return {
            "message": "Kayıt başarılı",
            "access_token": token,
            "token_type": "bearer",
            "user": {
                "id": user_id,
                "username": body.username,
                "role": role,
                "email": body.email,
                "full_name": body.full_name,
                "supervisor_id": supervisor_id_to_set,
                "qr_image_base64": qr_image_base64,
            },
        }

    except HTTPException:

        raise

    except Exception as e:

        logger.error(f"Kayıt hatası: {e }")

        logger.error(f"Kayıt hatası detayı: {type (e ).__name__ }: {str (e )}")

        raise HTTPException(status_code=500, detail=f"Kayıt işlemi başarısız: {str (e )}")


@router.get("/user/me", response_model=UserResponse)
async def me(user: Dict[str, Any] = Depends(auth_manager.get_current_user)):

    try:

        user_data = db_manager.get_user_by_id(user["id"])

        if user_data:

            return {
                "id": user_data["id"],
                "username": user_data["username"],
                "role": user_data["role"],
                "full_name": user_data.get("full_name"),
                "qr_image_base64": user_data.get("qr_image_base64"),
            }

        return {
            "id": user["id"],
            "username": user["username"],
            "role": user["role"],
            "full_name": None,
            "qr_image_base64": None,
        }

    except Exception:

        return {
            "id": user["id"],
            "username": user["username"],
            "role": user["role"],
            "full_name": None,
            "qr_image_base64": None,
        }


@router.get("/user/qr")
async def user_qr(user: Dict[str, Any] = Depends(auth_manager.get_current_user)):

    payload = json.dumps({"user": user["username"], "user_role": user["role"]})

    qr = qrcode.QRCode(version=1, box_size=8, border=3)

    qr.add_data(payload)

    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")

    buf = BytesIO()

    img.save(buf, format="PNG")

    buf.seek(0)

    return StreamingResponse(buf, media_type="image/png")


@router.get("/users/supervisors", response_model=list[SupervisorResponse])
async def list_supervisors():

    return db_manager.list_supervisors()


@router.get("/logs", response_model=LogsResponse)
async def get_logs(limit: int = 100, offset: int = 0, user: Dict[str, Any] = Depends(auth_manager.get_current_user)):

    if user["role"] == "supervisor":

        return db_manager.get_logs(limit=limit, offset=offset, supervisor_id=user["id"])

    return db_manager.get_logs(limit=limit, offset=offset)


@router.get("/logs/stats", response_model=LogStatsResponse)
async def get_log_stats(user: Dict[str, Any] = Depends(auth_manager.get_current_user)):

    try:

        if user["role"] == "supervisor":

            return db_manager.get_log_stats(supervisor_id=user["id"])

        return db_manager.get_log_stats()

    except Exception as e:

        logger.error(f"istatistik hatası: {e }")

        raise HTTPException(status_code=500, detail="İstatistik getirilemedi")


@router.post("/process_frame")
async def process_frame_endpoint(request: FrameRequest):

    try:

        logger.info(f"Frame processing request - User: {request .user }, Role: {request .user_role }")

        frame = decode_base64_frame(request.frame)

        if frame is None:

            return {"error": "Frame decode edilemedi"}

        detection_result = ppe_system.detect_ppe(frame)

        if request.user and request.user_role:

            logger.info(f"QR verisi alındı - User: '{request .user }', Role: '{request .user_role }'")

            try:

                user = db_manager.get_user_by_username(request.user)

                if user and user["role"] == request.user_role:

                    detection_result["user_id"] = user["id"]

                    detection_result["username"] = user["username"]

                    detection_result["user_role"] = user["role"]

                    logger.info(f"Kullanıcı DB'den bulundu: {user['username']} ({user['role']}) - ID: {user['id']}")

                else:

                    logger.warning(f"Kullanıcı DB'de bulunamadı: {request.user} ({request.user_role})")

                    detection_result["user_id"] = None

                    detection_result["username"] = request.user

                    detection_result["user_role"] = request.user_role

                    logger.info(f"Request'ten alınan bilgiler kullanılıyor: {request.user} ({request.user_role})")

            except Exception as e:

                logger.error(f"Kullanıcı bağlama hatası: {e }")

                detection_result["user_id"] = None

                detection_result["username"] = request.user

                detection_result["user_role"] = request.user_role

        else:

            logger.warning("QR'dan kullanıcı bilgisi gelmedi - request.user ve request.user_role boş")

        logger.info(
            f"Detection result user info - ID: {detection_result .get ('user_id')}, Username: {detection_result .get ('username')}, Role: {detection_result .get ('user_role')}"
        )

        if detection_result.get("user_id"):

            frame_base64 = encode_frame_to_base64(frame, quality=60)

            detection_result["frame_image"] = frame_base64

            if detection_result.get("analysis", {}).get("can_pass", False):

                logger.info("Başarılı geçiş - fotoğraf kaydedilecek")

            else:

                logger.info("Başarısız geçiş - fotoğraf kaydedilecek (en az eksik ekipman için)")

        try:

            db_manager.log_access(detection_result)

        except Exception as log_err:

            logger.warning(f"Log kayıt hatası (atlandı): {log_err }")

        return detection_result

    except Exception as e:

        logger.error(f"Frame işleme hatası: {e }")

        return {"error": f"Frame işleme hatası: {str (e )}"}


@router.get("/auth/random_jwt")
async def random_jwt(user: str = "ahmet_yilmaz", role: str = "worker"):

    try:

        user_data = db_manager.get_user_by_username(user)

        if not user_data:

            password_hash = auth_manager.hash_password("ŞİFRE")

            qr_payload = json.dumps({"user": user, "user_role": role})

            qr_image_base64 = generate_qr_base64(qr_payload)

            user_id = db_manager.create_user(
                username=user, password_hash=password_hash, role=role, qr_payload=qr_payload, qr_image_base64=qr_image_base64
            )

        else:

            user_id = user_data["id"]

        token = auth_manager.create_user_token({"id": user_id, "username": user, "role": role})

        return {"access_token": token, "token_type": "bearer", "user": {"id": user_id, "username": user, "role": role}}

    except Exception as e:

        logger.error(f"random_jwt hatası: {e }")

        raise HTTPException(status_code=500, detail="Token üretilemedi")


@router.get("/test/users")
async def test_users(user: Dict[str, Any] = Depends(auth_manager.get_current_user)):

    try:

        conn = db_manager.get_connection_with_row_factory()

        cur = conn.cursor()

        sql = """

            SELECT

                u.id,

                u.username,

                u.role,

                u.qr_payload,

                COALESCE(u.full_name, '')           AS full_name,

                u.supervisor_id,

                COALESCE(s.username, '')            AS supervisor_username,

                COALESCE(s.full_name, '')           AS supervisor_full_name

            FROM users u

            LEFT JOIN users s ON s.id = u.supervisor_id

            WHERE u.is_active = 1

        """

        params = []

        if user["role"] == "supervisor":

            sql += " AND u.role = 'worker' AND u.supervisor_id = ?"

            params.append(user["id"])

        sql += " ORDER BY u.id"

        cur.execute(sql, params)

        users = [dict(row) for row in cur.fetchall()]

        conn.close()

        return {"users": users}

    except Exception as e:

        return {"error": str(e)}


@router.get("/")
async def read_root():

    return {"message": "PPE Detection System is running"}


@router.get("/health")
async def health_check():

    return {"status": "healthy", "model_loaded": True}
