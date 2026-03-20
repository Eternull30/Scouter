from datetime import datetime
from pydantic import BaseModel


# ── Host schemas ──────────────────────────────────────────────
class HostCreate(BaseModel):
    name: str
    ip_address: str
    port: int = 2375


class HostResponse(BaseModel):
    id: int
    name: str
    ip_address: str
    port: int
    created_at: datetime

    model_config = {"from_attributes": True}


# ── Container schemas ─────────────────────────────────────────
class ContainerResponse(BaseModel):
    id: int
    container_id: str
    name: str
    image: str
    status: str
    created_at: datetime

    model_config = {"from_attributes": True}


# ── Stats schemas ─────────────────────────────────────────────
class StatResponse(BaseModel):
    id: int
    container_id: int
    cpu_percent: float
    memory_usage: int
    memory_limit: int
    network_rx: int
    network_tx: int
    disk_read: int
    disk_write: int
    recorded_at: datetime

    model_config = {"from_attributes": True}


class LiveStatResponse(BaseModel):
    container_name: str
    cpu_percent: float
    memory_usage_mb: float
    memory_limit_mb: float
    memory_percent: float
    network_rx_kb: float
    network_tx_kb: float
    disk_read_kb: float
    disk_write_kb: float
    timestamp: datetime


# ── Anomaly schemas ───────────────────────────────────────────
class AnomalyResponse(BaseModel):
    id: int
    container_id: int
    type: str
    severity: str
    message: str
    detected_at: datetime
    resolved_at: datetime | None

    model_config = {"from_attributes": True}


# ── Auth schemas ──────────────────────────────────────────────
class UserCreate(BaseModel):
    email: str
    password: str


class Token(BaseModel):
    access_token: str
    token_type: str