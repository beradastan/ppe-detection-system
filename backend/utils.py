import base64
import qrcode
from io import BytesIO
from typing import Dict, Any, Optional

from config import logger


def generate_qr_base64(payload_text: str) -> str:
    """QR kod oluşturur ve base64 formatında döner."""
    try:
        qr = qrcode.QRCode(version=1, box_size=8, border=3)
        qr.add_data(payload_text)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        buf = BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)
        qr_base64 = base64.b64encode(buf.getvalue()).decode("utf-8")
        return qr_base64
    except Exception as e:
        logger.error(f"QR kod oluşturma hatası: {e}")
        raise


def encode_frame_to_base64(frame, quality: int = 60) -> str:
    """Frame'i base64 formatında encode eder."""
    try:
        import cv2

        _, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, quality])
        frame_base64 = base64.b64encode(buffer).decode("utf-8")
        return f"data:image/jpeg;base64,{frame_base64}"
    except Exception as e:
        logger.error(f"Frame encoding hatası: {e}")
        raise


def decode_base64_frame(frame_data: str):
    """Base64 formatındaki frame'i decode eder."""
    try:
        import cv2
        import numpy as np

        if "," in frame_data:
            frame_data = frame_data.split(",")[1]
        frame_bytes = base64.b64decode(frame_data)
        nparr = np.frombuffer(frame_bytes, np.uint8)
        frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if frame is None:
            raise ValueError("Frame decode edilemedi")
        return frame
    except Exception as e:
        logger.error(f"Frame decoding hatası: {e}")
        raise


def validate_user_data(
    username: str, password: str, role: str, email: Optional[str] = None, full_name: Optional[str] = None
) -> Dict[str, Any]:
    """Kullanıcı verilerini doğrular."""
    errors = []
    if not username or len(username.strip()) < 3:
        errors.append("Kullanıcı adı en az 3 karakter olmalıdır")
    if not password or len(password) < 6:
        errors.append("Şifre en az 6 karakter olmalıdır")
    valid_roles = ["worker", "supervisor", "admin"]
    if role not in valid_roles:
        errors.append(f"Geçersiz rol. Geçerli roller: {', '.join(valid_roles)}")
    if email and "@" not in email:
        errors.append("Geçersiz email formatı")
    return {"is_valid": len(errors) == 0, "errors": errors}
