from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import containers, hosts, websocket
from app.database import AsyncSessionLocal
from app.models.models import Host

app = FastAPI(
    title="Docker Monitor API",
    description="Backend for the Android Docker container monitoring app",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(containers.router)
app.include_router(hosts.router)
app.include_router(websocket.router)


@app.on_event("startup")
async def create_default_host():
    """Insert a default localhost host on startup if it doesn't exist."""
    async with AsyncSessionLocal() as db:
        from sqlalchemy import select
        result = await db.execute(select(Host).where(Host.id == 1))
        host = result.scalar_one_or_none()
        if not host:
            db.add(Host(
                id=1,
                name="localhost",
                ip_address="localhost",
                port=2375,
            ))
            await db.commit()
            print("[startup] Default host created")


@app.get("/")
async def root():
    return {
        "message": "Docker Monitor API",
        "version": "0.1.0",
        "docs": "/docs",
    }


@app.get("/health")
async def health():
    return {"status": "ok"}