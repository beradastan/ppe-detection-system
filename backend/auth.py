from jose import jwt

import uuid

from datetime import datetime, timedelta

from typing import Dict, Any, Optional

from passlib.context import CryptContext

from fastapi import HTTPException, Request

from config import JWT_SECRET, JWT_ALGORITHM, JWT_EXPIRE_MINUTES, logger

from database import db_manager

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthManager:

    def __init__(self):

        self.jwt_secret = JWT_SECRET

        self.jwt_algorithm = JWT_ALGORITHM

        self.jwt_expire_minutes = JWT_EXPIRE_MINUTES

    def hash_password(self, password: str) -> str:

        return pwd_context.hash(password)

    def verify_password(self, plain: str, hashed: str) -> bool:

        try:

            return pwd_context.verify(plain, hashed)

        except Exception:

            return False

    def create_token(self, payload: Dict[str, Any], expires_minutes: int = None) -> str:

        if expires_minutes is None:

            expires_minutes = self.jwt_expire_minutes

        expire = datetime.utcnow() + timedelta(minutes=expires_minutes)

        to_encode = payload.copy()

        to_encode.update({"exp": expire})

        return jwt.encode(to_encode, self.jwt_secret, algorithm=self.jwt_algorithm)

    def decode_token(self, token: str) -> Dict[str, Any]:

        try:

            return jwt.decode(token, self.jwt_secret, algorithms=[self.jwt_algorithm])

        except jwt.ExpiredSignatureError:

            raise HTTPException(status_code=401, detail="Token süresi doldu")

        except jwt.InvalidTokenError:

            raise HTTPException(status_code=401, detail="Geçersiz token")

    def get_current_user(self, request: Request) -> Dict[str, Any]:

        auth = request.headers.get("Authorization")

        if not auth or not auth.lower().startswith("bearer "):

            raise HTTPException(status_code=401, detail="Yetkilendirme gerekli")

        token = auth.split(" ", 1)[1]

        payload = self.decode_token(token)

        username = payload.get("username")

        role = payload.get("role")

        if not username or not role:

            raise HTTPException(status_code=401, detail="Geçersiz token")

        user = db_manager.get_user_by_username(username)

        if not user or user["role"] != role:

            raise HTTPException(status_code=401, detail="Kullanıcı bulunamadı")

        return {"id": user["id"], "username": user["username"], "role": user["role"]}

    def authenticate_user(self, username: str, password: str) -> Optional[Dict[str, Any]]:

        user = db_manager.get_user_by_username(username)

        if not user:

            return None

        if not self.verify_password(password, user["password_hash"]):

            return None

        return user

    def create_user_token(self, user: Dict[str, Any]) -> str:

        payload = {"username": user["username"], "role": user["role"], "user_id": user["id"], "jti": str(uuid.uuid4())}

        return self.create_token(payload)

    def seed_default_users(self):

        try:

            username = "ahmet_yilmaz"

            role = "worker"

            user = db_manager.get_user_by_username(username)

            if not user:

                password = "AhmetYilmaz!123"

                password_hash = self.hash_password(password)

                qr_payload = f'{{"user": "{username }", "user_role": "{role }"}}'

                from utils import generate_qr_base64

                qr_image_base64 = generate_qr_base64(qr_payload)

                user_id = db_manager.create_user(
                    username=username,
                    password_hash=password_hash,
                    role=role,
                    qr_payload=qr_payload,
                    qr_image_base64=qr_image_base64,
                    email="ahmet@test.com",
                    full_name="Ahmet Yılmaz",
                )

                logger.info(f"Kullanıcı 'ahmet_yilmaz' oluşturuldu. ID: {user_id }")

            else:

                user_id = user["id"]

                if not user.get("qr_image_base64"):

                    qr_payload = f'{{"user": "{username }", "user_role": "{role }"}}'

                    qr_image_base64 = generate_qr_base64(qr_payload)

                    db_manager.update_user_qr(user_id, qr_image_base64)

                    logger.info(f"Kullanıcı 'ahmet_yilmaz' QR kodu güncellendi. ID: {user_id }")

            test_username = "mehmet_demir"

            test_role = "supervisor"

            test_user = db_manager.get_user_by_username(test_username)

            if not test_user:

                test_password_hash = self.hash_password("MehmetDemir!123")

                test_qr_payload = f'{{"user": "{test_username }", "user_role": "{test_role }"}}'

                test_qr_image_base64 = generate_qr_base64(test_qr_payload)

                test_user_id = db_manager.create_user(
                    username=test_username,
                    password_hash=test_password_hash,
                    role=test_role,
                    qr_payload=test_qr_payload,
                    qr_image_base64=test_qr_image_base64,
                    email="mehmet@test.com",
                    full_name="Mehmet Demir",
                )

                logger.info(f"Test kullanıcısı 'mehmet_demir' oluşturuldu. ID: {test_user_id }")

            token = self.create_user_token({"id": user_id, "username": username, "role": role})

            try:

                with open("dev_token.txt", "w") as f:

                    f.write(token)

            except Exception as fe:

                logger.warning(f"dev_token.txt yazılamadı: {fe }")

            logger.info("Seed kullanıcılar hazır. Test JWT dev_token.txt dosyasına yazıldı.")

            logger.info(f'QR test: {{"user": "{username }", "user_role": "{role }"}}')

            logger.info(f'QR test: {{"user": "{test_username }", "user_role": "{test_role }"}}')

        except Exception as e:

            logger.error(f"Seed kullanıcı oluşturma hatası: {e }")


auth_manager = AuthManager()
