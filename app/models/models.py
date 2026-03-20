from datetime import datetime
from sqlalchemy import String, Float, Integer, ForeignKey, DateTime, Text, BigInteger
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database import Base


class Host(Base):
    __tablename__ = "hosts"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(100))
    ip_address: Mapped[str] = mapped_column(String(255))
    port: Mapped[int] = mapped_column(Integer, default=2375)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    containers: Mapped[list["Container"]] = relationship("Container", back_populates="host")


class Container(Base):
    __tablename__ = "containers"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    host_id: Mapped[int] = mapped_column(Integer, ForeignKey("hosts.id"))
    container_id: Mapped[str] = mapped_column(String(100), unique=True)  # Docker's own ID
    name: Mapped[str] = mapped_column(String(100))
    image: Mapped[str] = mapped_column(String(255))
    status: Mapped[str] = mapped_column(String(50))
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    host: Mapped["Host"] = relationship("Host", back_populates="containers")
    stats: Mapped[list["Stat"]] = relationship("Stat", back_populates="container")
    anomalies: Mapped[list["Anomaly"]] = relationship("Anomaly", back_populates="container")


class Stat(Base):
    __tablename__ = "stats"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    container_id: Mapped[int] = mapped_column(Integer, ForeignKey("containers.id"))
    cpu_percent: Mapped[float] = mapped_column(Float, default=0.0)
    memory_usage: Mapped[int] = mapped_column(BigInteger, default=0)   # bytes
    memory_limit: Mapped[int] = mapped_column(BigInteger, default=0)   # bytes
    network_rx: Mapped[int] = mapped_column(BigInteger, default=0)     # bytes received
    network_tx: Mapped[int] = mapped_column(BigInteger, default=0)     # bytes sent
    disk_read: Mapped[int] = mapped_column(BigInteger, default=0)      # bytes read
    disk_write: Mapped[int] = mapped_column(BigInteger, default=0)     # bytes written
    recorded_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    container: Mapped["Container"] = relationship("Container", back_populates="stats")


class Anomaly(Base):
    __tablename__ = "anomalies"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    container_id: Mapped[int] = mapped_column(Integer, ForeignKey("containers.id"))
    type: Mapped[str] = mapped_column(String(50))       # cpu_spike / memory_leak / crash
    severity: Mapped[str] = mapped_column(String(20))   # low / medium / high
    message: Mapped[str] = mapped_column(Text)
    detected_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    resolved_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    container: Mapped["Container"] = relationship("Container", back_populates="anomalies")


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    email: Mapped[str] = mapped_column(String(255), unique=True)
    hashed_password: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)