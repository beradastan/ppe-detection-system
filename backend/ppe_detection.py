import numpy as np

import torch

from typing import List, Dict, Any

from ultralytics import YOLO

from config import MODEL_PATH, CONFIDENCE_THRESHOLD, CLASS_NAMES, REQUIRED_PPE, OPTIONAL_PPE, logger


class PPEDetectionSystem:

    def __init__(self):

        self.model = self._load_model()

        self.confidence_threshold = CONFIDENCE_THRESHOLD

        self.device = self._setup_device()

    def _load_model(self) -> YOLO:

        try:

            model = YOLO(MODEL_PATH)

            logger.info(f"Model başarıyla yüklendi: {MODEL_PATH }")

            return model

        except Exception as e:

            logger.error(f"Model yükleme hatası: {e } - MODEL_PATH={MODEL_PATH }")

            raise

    def _setup_device(self) -> torch.device:

        if torch.backends.mps.is_available():

            device = torch.device("mps")

            logger.info("Apple Silicon MPS backend kullanılıyor")

        elif torch.cuda.is_available():

            device = torch.device("cuda")

            logger.info("CUDA backend kullanılıyor")

        else:

            device = torch.device("cpu")

            logger.info("CPU backend kullanılıyor")

        self.model.to(device)

        return device

    def detect_ppe(self, frame: np.ndarray) -> Dict[str, Any]:

        try:

            results = self.model(frame, conf=self.confidence_threshold)

            detected_items = []

            person_detected = False

            for result in results:

                boxes = result.boxes

                if boxes is not None:

                    for box in boxes:

                        class_id = int(box.cls[0])

                        confidence = float(box.conf[0])

                        class_name = CLASS_NAMES[class_id]

                        x1, y1, x2, y2 = box.xyxy[0].tolist()

                        detected_items.append({"class_name": class_name, "confidence": confidence, "bbox": [x1, y1, x2, y2]})

                        if class_name == "person":

                            person_detected = True

            analysis = self.analyze_ppe_compliance(detected_items, person_detected)

            return {"detected_items": detected_items, "person_detected": person_detected, "analysis": analysis}

        except Exception as e:

            logger.error(f"Tespit hatası: {e }")

            return {
                "detected_items": [],
                "person_detected": False,
                "analysis": {
                    "can_pass": False,
                    "status": "error",
                    "message": "Tespit sırasında hata oluştu",
                    "missing_required": REQUIRED_PPE,
                    "missing_optional": [],
                },
            }

    def analyze_ppe_compliance(self, detected_items: List[Dict], person_detected: bool) -> Dict[str, Any]:

        if not person_detected:

            return {
                "can_pass": False,
                "status": "no_person",
                "message": "Kişi algılanmadı",
                "missing_required": REQUIRED_PPE,
                "missing_optional": [],
            }

        detected_ppe = [item["class_name"] for item in detected_items if item["class_name"] != "person"]

        missing_required = []

        for required_item in REQUIRED_PPE:

            if required_item not in detected_ppe:

                missing_required.append(required_item)

        missing_optional = []

        for optional_item in OPTIONAL_PPE:

            if optional_item not in detected_ppe:

                missing_optional.append(optional_item)

        if len(missing_required) == 0:

            if len(missing_optional) == 0:

                return {
                    "can_pass": True,
                    "status": "full_compliance",
                    "message": "Tüm ekipmanlar mevcut. Geçiş izni verildi.",
                    "missing_required": [],
                    "missing_optional": [],
                    "total_missing": 0,
                }

            else:

                return {
                    "can_pass": True,
                    "status": "partial_compliance",
                    "message": "Opsiyonel eksikler mevcut. Geçiş izni verildi.",
                    "missing_required": [],
                    "missing_optional": missing_optional,
                    "total_missing": len(missing_optional),
                }

        else:

            missing_turkish = []

            for item in missing_required:

                if item == "helmet":

                    missing_turkish.append("KASK")

                elif item == "vest":

                    missing_turkish.append("YELEK")

                elif item == "boots":

                    missing_turkish.append("BOT")

            missing_items = ", ".join(missing_turkish)

            return {
                "can_pass": False,
                "status": "non_compliance",
                "message": f"❌ Geçiş reddedildi. Eksik: {missing_items }",
                "missing_required": missing_required,
                "missing_optional": missing_optional,
                "total_missing": len(missing_required) + len(missing_optional),
            }


ppe_system = PPEDetectionSystem()
