from pydantic import BaseModel

from typing import Optional


class FrameRequest(BaseModel):

    frame: str

    user: Optional[str] = None

    user_role: Optional[str] = None


class LoginRequest(BaseModel):

    username: str

    password: str


class RegisterRequest(BaseModel):

    username: str

    password: str

    role: str = "worker"

    email: Optional[str] = None

    full_name: Optional[str] = None

    supervisor_id: Optional[int] = None


class CreateUserRequest(BaseModel):

    username: str

    password: str

    role: str


class UserResponse(BaseModel):

    id: int

    username: str

    role: str

    email: Optional[str] = None

    full_name: Optional[str] = None

    supervisor_id: Optional[int] = None

    qr_image_base64: Optional[str] = None


class LoginResponse(BaseModel):

    access_token: str

    token_type: str

    user: UserResponse


class RegisterResponse(BaseModel):

    message: str

    access_token: str

    token_type: str

    user: UserResponse


class LogEntry(BaseModel):

    id: int

    user_id: int

    timestamp: str

    can_pass: bool

    status: str

    missing_required: list

    missing_optional: list

    detected_items: list

    person_detected: bool

    confidence_scores: dict

    frame_image: Optional[str] = None

    username: str

    user_role: str

    full_name: Optional[str] = None

    email: Optional[str] = None


class LogsResponse(BaseModel):

    logs: list[LogEntry]

    total_count: int

    limit: int

    offset: int


class LogStatsResponse(BaseModel):

    total_passed: int

    total_denied: int

    today_passed: int

    today_denied: int

    total_users: int


class SupervisorResponse(BaseModel):

    id: int

    username: str

    full_name: str
