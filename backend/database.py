import sqlite3

import json

from typing import Dict, Any, Optional, List

from config import DB_PATH, logger


class DatabaseManager:

    def __init__(self):

        self.db_path = DB_PATH

        self.init_database()

    def init_database(self):

        try:

            import os

            os.makedirs(os.path.dirname(self.db_path), exist_ok=True)

            conn = sqlite3.connect(self.db_path)

            cursor = conn.cursor()

            cursor.execute("PRAGMA foreign_keys = ON")

            cursor.execute("PRAGMA journal_mode = WAL")

            cursor.execute("PRAGMA user_version")

            user_version = cursor.fetchone()[0] or 0

            cursor.execute(
                """

                CREATE TABLE IF NOT EXISTS users (

                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    username TEXT UNIQUE NOT NULL,

                    password_hash TEXT NOT NULL,

                    role TEXT NOT NULL CHECK (role IN ('worker', 'supervisor', 'admin')),

                    qr_payload TEXT NOT NULL,

                    qr_image_base64 TEXT,

                    email TEXT,

                    full_name TEXT,

                    supervisor_id INTEGER,  -- 🔹 yeni eklenen kolon

                    is_active BOOLEAN DEFAULT 1,

                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP

                )

            """
            )

            cursor.execute(
                """

                CREATE TABLE IF NOT EXISTS access_logs (

                    id INTEGER PRIMARY KEY AUTOINCREMENT,

                    user_id INTEGER NOT NULL,

                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,

                    can_pass BOOLEAN NOT NULL,

                    status TEXT NOT NULL CHECK (status IN ('full_compliance', 'partial_compliance', 'non_compliance', 'no_person', 'error')),

                    missing_required TEXT,

                    missing_optional TEXT,

                    detected_items TEXT,

                    person_detected BOOLEAN DEFAULT 0,

                    confidence_scores TEXT,

                    frame_image TEXT,

                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE

                )

            """
            )

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_access_logs_user_id ON access_logs(user_id)")

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_access_logs_timestamp ON access_logs(timestamp)")

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_access_logs_can_pass ON access_logs(can_pass)")

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)")

            cursor.execute("CREATE INDEX IF NOT EXISTS idx_users_role ON users(role)")

            if user_version == 0:

                cursor.execute("PRAGMA user_version = 1")

            conn.commit()

            conn.close()

            logger.info("✅ Veritabanı hazır: mevcutsa korundu, yoksa oluşturuldu.")

        except Exception as e:

            logger.error(f"Veritabanı init hatası: {e }")

            raise

    def get_connection(self):

        conn = sqlite3.connect(self.db_path, timeout=1.0)

        conn.execute("PRAGMA foreign_keys = ON")

        conn.execute("PRAGMA journal_mode = WAL")

        return conn

    def get_connection_with_row_factory(self):

        conn = self.get_connection()

        conn.row_factory = sqlite3.Row

        return conn

    def get_user_by_id(self, user_id: int) -> Optional[Dict[str, Any]]:

        try:

            conn = self.get_connection_with_row_factory()

            cur = conn.cursor()

            cur.execute("SELECT * FROM users WHERE id = ? AND is_active = 1", (user_id,))

            row = cur.fetchone()

            conn.close()

            if row:

                return dict(row)

            return None

        except Exception as e:

            logger.error(f"Kullanıcı ID ile getirme hatası: {e }")

            return None

    def get_user_by_username(self, username: str) -> Optional[Dict[str, Any]]:

        try:

            conn = self.get_connection_with_row_factory()

            cur = conn.cursor()

            cur.execute("SELECT * FROM users WHERE username = ? AND is_active = 1", (username,))

            row = cur.fetchone()

            conn.close()

            if row:

                return dict(row)

            return None

        except Exception as e:

            logger.error(f"Kullanıcı username ile getirme hatası: {e }")

            return None

    def create_user(
        self,
        username: str,
        password_hash: str,
        role: str,
        qr_payload: str,
        qr_image_base64: str,
        email: Optional[str] = None,
        full_name: Optional[str] = None,
        supervisor_id: Optional[int] = None,
    ) -> int:

        try:

            conn = self.get_connection()

            cur = conn.cursor()

            cur.execute(
                """

                INSERT INTO users (username, password_hash, role, qr_payload, qr_image_base64, 

                                 email, full_name, supervisor_id) 

                VALUES (?, ?, ?, ?, ?, ?, ?, ?)

            """,
                (username, password_hash, role, qr_payload, qr_image_base64, email, full_name, supervisor_id),
            )

            conn.commit()

            user_id = cur.lastrowid

            conn.close()

            return user_id

        except Exception as e:

            logger.error(f"Kullanıcı oluşturma hatası: {e }")

            raise

    def update_user_qr(self, user_id: int, qr_image_base64: str):

        try:

            conn = self.get_connection()

            cur = conn.cursor()

            cur.execute("UPDATE users SET qr_image_base64 = ? WHERE id = ?", (qr_image_base64, user_id))

            conn.commit()

            conn.close()

        except Exception as e:

            logger.error(f"QR güncelleme hatası: {e }")

            raise

    def list_supervisors(self) -> List[Dict[str, Any]]:

        try:

            conn = self.get_connection_with_row_factory()

            cur = conn.cursor()

            cur.execute(
                """

                SELECT id, username, COALESCE(full_name, '') AS full_name

                FROM users

                WHERE role='supervisor' AND is_active=1

                ORDER BY (full_name IS NULL OR TRIM(full_name)=''), full_name, username

            """
            )

            rows = [dict(r) for r in cur.fetchall()]

            conn.close()

            return rows

        except Exception as e:

            logger.error(f"Supervisor listeleme hatası: {e }")

            return []

    def get_logs(self, limit: int = 100, offset: int = 0, supervisor_id: Optional[int] = None) -> Dict[str, Any]:

        try:

            conn = self.get_connection()

            cursor = conn.cursor()

            if supervisor_id is None:

                cursor.execute("SELECT COUNT(*) FROM access_logs")

                total_count = cursor.fetchone()[0]

                cursor.execute(
                    """

                               SELECT al.id,

                                      al.user_id,

                                      al.timestamp,

                                      al.can_pass,

                                      al.status,

                                      al.missing_required,

                                      al.missing_optional,

                                      al.detected_items,

                                      al.person_detected,

                                      al.confidence_scores,

                                      al.frame_image,

                                      COALESCE(u.username, 'Bilinmeyen') as username,

                                      COALESCE(u.role, 'unknown')        as role,

                                      u.full_name,

                                      u.email

                               FROM access_logs al

                                        LEFT JOIN users u ON al.user_id = u.id

                               ORDER BY al.timestamp DESC LIMIT ?

                               OFFSET ?

                               """,
                    (limit, offset),
                )

            else:

                cursor.execute(
                    """

                               SELECT COUNT(*)

                               FROM access_logs al

                                        JOIN users u ON al.user_id = u.id

                               WHERE u.is_active = 1

                                 AND u.supervisor_id = ?

                               """,
                    (supervisor_id,),
                )

                total_count = cursor.fetchone()[0]

                cursor.execute(
                    """

                               SELECT al.id,

                                      al.user_id,

                                      al.timestamp,

                                      al.can_pass,

                                      al.status,

                                      al.missing_required,

                                      al.missing_optional,

                                      al.detected_items,

                                      al.person_detected,

                                      al.confidence_scores,

                                      al.frame_image,

                                      COALESCE(u.username, 'Bilinmeyen') as username,

                                      COALESCE(u.role, 'unknown')        as role,

                                      u.full_name,

                                      u.email

                               FROM access_logs al

                                        JOIN users u ON al.user_id = u.id

                               WHERE u.is_active = 1

                                 AND u.supervisor_id = ?

                               ORDER BY al.timestamp DESC LIMIT ?

                               OFFSET ?

                               """,
                    (supervisor_id, limit, offset),
                )

            rows = cursor.fetchall()

            logs = []

            for row in rows:

                logs.append(
                    {
                        "id": row[0],
                        "user_id": row[1],
                        "timestamp": row[2],
                        "can_pass": bool(row[3]),
                        "status": row[4],
                        "missing_required": row[5].split(", ") if row[5] else [],
                        "missing_optional": row[6].split(", ") if row[6] else [],
                        "detected_items": json.loads(row[7]) if row[7] else [],
                        "person_detected": bool(row[8]),
                        "confidence_scores": json.loads(row[9]) if row[9] else {},
                        "frame_image": row[10],
                        "username": row[11],
                        "user_role": row[12],
                        "full_name": row[13],
                        "email": row[14],
                    }
                )

            conn.close()

            return {"logs": logs, "total_count": total_count, "limit": limit, "offset": offset}

        except Exception as e:

            logger.error(f"Log getirme hatası: {e }")

            return {"error": f"Log getirme hatası: {str (e )}"}

    def get_log_frame(self, log_id: int) -> Dict[str, Any]:

        try:

            conn = self.get_connection()

            cursor = conn.cursor()

            cursor.execute(
                """

                SELECT frame_image FROM access_logs 

                WHERE id = ? AND frame_image IS NOT NULL

            """,
                (log_id,),
            )

            result = cursor.fetchone()

            conn.close()

            if result and result[0]:

                return {"frame_image": result[0]}

            else:

                return {"error": "Frame bulunamadı"}

        except Exception as e:

            logger.error(f"Frame getirme hatası: {e }")

            return {"error": f"Frame getirme hatası: {str (e )}"}

    def get_log_stats(self, supervisor_id: Optional[int] = None) -> Dict[str, Any]:

        try:

            conn = self.get_connection()

            cursor = conn.cursor()

            if supervisor_id is None:

                cursor.execute("SELECT COUNT(DISTINCT user_id) FROM access_logs WHERE can_pass = 1")

                total_passed = cursor.fetchone()[0]

                cursor.execute("SELECT COUNT(DISTINCT user_id) FROM access_logs WHERE can_pass = 0")

                total_denied = cursor.fetchone()[0]

                cursor.execute(
                    """

                               SELECT COUNT(DISTINCT user_id)

                               FROM access_logs

                               WHERE can_pass = 1 AND DATE (timestamp) = DATE ('now')

                               """
                )

                today_passed = cursor.fetchone()[0]

                cursor.execute(
                    """

                               SELECT COUNT(DISTINCT user_id)

                               FROM access_logs

                               WHERE can_pass = 0 AND DATE (timestamp) = DATE ('now')

                               """
                )

                today_denied = cursor.fetchone()[0]

                cursor.execute("SELECT COUNT(*) FROM users WHERE is_active = 1")

                total_users = cursor.fetchone()[0]

            else:

                base = """

                    FROM access_logs al

                    JOIN users u ON al.user_id = u.id

                    WHERE u.is_active = 1

                      AND u.role = 'worker'

                      AND u.supervisor_id = ?

                """

                cursor.execute("SELECT COUNT(DISTINCT al.user_id) " + base + " AND al.can_pass = 1", (supervisor_id,))

                total_passed = cursor.fetchone()[0]

                cursor.execute("SELECT COUNT(DISTINCT al.user_id) " + base + " AND al.can_pass = 0", (supervisor_id,))

                total_denied = cursor.fetchone()[0]

                cursor.execute(
                    "SELECT COUNT(DISTINCT al.user_id) " + base + " AND al.can_pass = 1 AND DATE(al.timestamp) = DATE('now')",
                    (supervisor_id,),
                )

                today_passed = cursor.fetchone()[0]

                cursor.execute(
                    "SELECT COUNT(DISTINCT al.user_id) " + base + " AND al.can_pass = 0 AND DATE(al.timestamp) = DATE('now')",
                    (supervisor_id,),
                )

                today_denied = cursor.fetchone()[0]

                cursor.execute(
                    """

                               SELECT COUNT(*)

                               FROM users

                               WHERE is_active = 1

                                 AND role = 'worker'

                                 AND supervisor_id = ?

                               """,
                    (supervisor_id,),
                )

                total_users = cursor.fetchone()[0]

            conn.close()

            return {
                "total_passed": total_passed,
                "total_denied": total_denied,
                "today_passed": today_passed,
                "today_denied": today_denied,
                "total_users": total_users,
            }

        except Exception as e:

            logger.error(f"İstatistik getirme hatası: {e }")

            return {"error": f"İstatistik getirme hatası: {str (e )}"}

    def log_access(self, detection_result: Dict[str, Any]):

        try:

            conn = self.get_connection()

            cursor = conn.cursor()

            user_id = detection_result.get("user_id")

            username = detection_result.get("username")

            user_role = detection_result.get("user_role")

            logger.info(f"Log fonksiyonuna gelen veriler - ID: {user_id }, Username: {username }, Role: {user_role }")

            if not user_id:

                logger.warning(f"Log atlandı - user_id eksik: {user_id }")

                conn.close()

                return

            cursor.execute("SELECT id, username, role FROM users WHERE id = ? AND is_active = 1", (user_id,))

            user_row = cursor.fetchone()

            if not user_row:

                logger.warning(f"Log atlandı - user_id {user_id } DB'de bulunamadı")

                conn.close()

                return

            logger.info(f"Log kaydı başlatılıyor - User: {user_row [1 ]} ({user_row [2 ]}) ID: {user_id }")

            detected_items_json = json.dumps([item["class_name"] for item in detection_result.get("detected_items", [])])

            confidence_scores = {}

            for item in detection_result.get("detected_items", []):

                confidence_scores[item["class_name"]] = item["confidence"]

            confidence_json = json.dumps(confidence_scores)

            missing_required_list = detection_result.get("analysis", {}).get("missing_required", [])

            missing_optional_list = detection_result.get("analysis", {}).get("missing_optional", [])

            missing_required_str = ", ".join(missing_required_list)

            missing_optional_str = ", ".join(missing_optional_list)

            can_pass = detection_result.get("analysis", {}).get("can_pass", False)

            status = detection_result.get("analysis", {}).get("status", "unknown")



            person_detected = detection_result.get("person_detected", False)

            if status == "no_person":

                logger.info(f"No-person durumu algılandı (user_id={user_id }). DB log atlandı.")

                conn.close()

                return

            frame_image_base64 = None

            if "frame_image" in detection_result:

                frame_image_base64 = detection_result["frame_image"]

            cursor.execute(
                """

                SELECT id, can_pass, frame_image, missing_required, missing_optional 

                FROM access_logs 

                WHERE user_id = ?

                ORDER BY timestamp DESC

            """,
                (user_id,),
            )

            existing_logs = cursor.fetchall()

            if can_pass:

                if existing_logs:

                    cursor.execute("DELETE FROM access_logs WHERE user_id = ?", (user_id,))

                    logger.info(f"Kullanıcı ID {user_id } için önceki tüm loglar silindi (başarılı geçiş)")

                cursor.execute(
                    """

                    INSERT INTO access_logs (

                        user_id, can_pass, status, missing_required, missing_optional,

                        detected_items, person_detected, confidence_scores, frame_image

                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)

                """,
                    (
                        user_id,
                        can_pass,
                        status,
                        missing_required_str,
                        missing_optional_str,
                        detected_items_json,
                        person_detected,
                        confidence_json,
                        frame_image_base64,
                    ),
                )

                logger.info(f"Kullanıcı ID {user_id } için başarılı geçiş logu eklendi")

            else:

                current_missing_count = len(missing_required_list) + len(missing_optional_list)

                best_log = None

                best_missing_count = float("inf")

                for log in existing_logs:

                    log_id, log_can_pass, log_frame, log_missing_req, log_missing_opt = log

                    if not log_can_pass:

                        log_req_count = len(log_missing_req.split(", ")) if log_missing_req else 0

                        log_opt_count = len(log_missing_opt.split(", ")) if log_missing_opt else 0

                        log_total_missing = log_req_count + log_opt_count

                        if log_total_missing < best_missing_count:

                            best_missing_count = log_total_missing

                            best_log = log

                if current_missing_count < best_missing_count:

                    if existing_logs:

                        cursor.execute("DELETE FROM access_logs WHERE user_id = ?", (user_id,))

                        logger.info(f"Kullanıcı ID {user_id } için önceki loglar silindi (daha iyi başarısız log)")

                    cursor.execute(
                        """

                        INSERT INTO access_logs (

                            user_id, can_pass, status, missing_required, missing_optional,

                            detected_items, person_detected, confidence_scores, frame_image

                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)

                    """,
                        (
                            user_id,
                            can_pass,
                            status,
                            missing_required_str,
                            missing_optional_str,
                            detected_items_json,
                            person_detected,
                            confidence_json,
                            frame_image_base64,
                        ),
                    )

                    logger.info(
                        f"Kullanıcı ID {user_id } için daha iyi başarısız log eklendi ({current_missing_count } eksik) + fotoğraf"
                    )

                elif current_missing_count == best_missing_count and best_log and frame_image_base64:

                    best_log_id = best_log[0]

                    cursor.execute(
                        """

                        UPDATE access_logs SET 

                            timestamp = CURRENT_TIMESTAMP,

                            status = ?, detected_items = ?, 

                            person_detected = ?, confidence_scores = ?, frame_image = ?

                        WHERE id = ?

                    """,
                        (status, detected_items_json, person_detected, confidence_json, frame_image_base64, best_log_id),
                    )

                    logger.info(f"Kullanıcı ID {user_id } için aynı seviye başarısız log güncellendi + yeni fotoğraf")

                elif best_log:

                    best_log_id = best_log[0]

                    cursor.execute("UPDATE access_logs SET timestamp = CURRENT_TIMESTAMP WHERE id = ?", (best_log_id,))

                    logger.info(f"Kullanıcı ID {user_id } için mevcut en iyi log korundu ({best_missing_count } eksik)")

                else:

                    cursor.execute(
                        """

                        INSERT INTO access_logs (

                            user_id, can_pass, status, missing_required, missing_optional,

                            detected_items, person_detected, confidence_scores, frame_image

                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)

                    """,
                        (
                            user_id,
                            can_pass,
                            status,
                            missing_required_str,
                            missing_optional_str,
                            detected_items_json,
                            person_detected,
                            confidence_json,
                            frame_image_base64,
                        ),
                    )

                    logger.info(f"Kullanıcı ID {user_id } için ilk başarısız log eklendi + fotoğraf")

            conn.commit()

            conn.close()

        except sqlite3.OperationalError as e:

            if "locked" in str(e).lower():

                logger.warning("Veritabanı kilitli, log atlandı")

            else:

                logger.error(f"SQLite hatası: {e }")

        except Exception as e:

            logger.error(f"Log kaydetme hatası: {e }")


db_manager = DatabaseManager()
