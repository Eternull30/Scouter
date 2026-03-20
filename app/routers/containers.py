from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, desc
from app.services.docker_service import docker_service
from app.schemas.schemas import LiveStatResponse, StatResponse
from app.database import get_db
from app.models.models import Container, Stat

router = APIRouter(prefix="/containers", tags=["containers"])


# ── Helper: get or create container record in DB ──────────────
async def get_or_create_container(
    db: AsyncSession,
    container_id: str,
    name: str,
    image: str,
    status: str,
) -> Container:
    result = await db.execute(
        select(Container).where(Container.container_id == container_id)
    )
    container = result.scalar_one_or_none()
    if not container:
        container = Container(
            host_id=1,
            container_id=container_id,
            name=name,
            image=image,
            status=status,
        )
        db.add(container)
        await db.commit()
        await db.refresh(container)
    else:
        container.status = status
        await db.commit()
    return container


# ── Endpoints ─────────────────────────────────────────────────

@router.get("/")
async def list_containers(all: bool = False):
    """
    List all running containers.
    Pass ?all=true to include stopped containers.
    """
    try:
        raw = await docker_service.list_containers(all=all)
        return [
            {
                "id": c["Id"][:12],
                "full_id": c["Id"],
                "name": c["Names"][0].lstrip("/") if c["Names"] else "unknown",
                "image": c["Image"],
                "status": c["Status"],
                "state": c["State"],
                "created": c["Created"],
                "ports": c.get("Ports", []),
            }
            for c in raw
        ]
    except Exception as e:
        raise HTTPException(status_code=503, detail=f"Docker unreachable: {str(e)}")


@router.get("/{container_id}/stats", response_model=LiveStatResponse)
async def get_container_stats(
    container_id: str,
    db: AsyncSession = Depends(get_db),
):
    """
    Get a single stats snapshot and save it to PostgreSQL.
    Returns clean parsed values — CPU %, memory in MB, etc.
    """
    try:
        raw = await docker_service.get_stats_snapshot(container_id)
        parsed = docker_service.parse_stats(raw)

        # Save to DB for historical graphs
        container = await get_or_create_container(
            db,
            container_id=raw["id"],
            name=parsed.container_name,
            image=container_id,
            status="running",
        )

        stat = Stat(
            container_id=container.id,
            cpu_percent=parsed.cpu_percent,
            memory_usage=int(parsed.memory_usage_mb * 1024 * 1024),
            memory_limit=int(parsed.memory_limit_mb * 1024 * 1024),
            network_rx=int(parsed.network_rx_kb * 1024),
            network_tx=int(parsed.network_tx_kb * 1024),
            disk_read=int(parsed.disk_read_kb * 1024),
            disk_write=int(parsed.disk_write_kb * 1024),
        )
        db.add(stat)
        await db.commit()

        return parsed

    except Exception as e:
        print(f"FULL ERROR:{type(e). __name__}:{e}")
        raise HTTPException(status_code=503, detail=str(e))


@router.get("/{container_id}/stats/history", response_model=list[StatResponse])
async def get_stats_history(
    container_id: str,
    limit: int = 60,
    db: AsyncSession = Depends(get_db),
):
    """
    Fetch historical stats from PostgreSQL.
    Default: last 60 records.
    This powers the time-series graphs in the Android app.
    """
    result = await db.execute(
        select(Container).where(Container.name == container_id)
    )
    container = result.scalar_one_or_none()

    if not container:
        raise HTTPException(
            status_code=404,
            detail=f"No history found for container: {container_id}"
        )

    stats_result = await db.execute(
        select(Stat)
        .where(Stat.container_id == container.id)
        .order_by(desc(Stat.recorded_at))
        .limit(limit)
    )
    stats = stats_result.scalars().all()

    # Reverse so oldest is first (left→right on graph)
    return list(reversed(stats))


@router.get("/{container_id}/logs")
async def get_container_logs(container_id: str, tail: int = 100):
    """Fetch the last N lines of container logs."""
    try:
        logs = await docker_service.get_logs(container_id, tail=tail)
        return {"container_id": container_id, "logs": logs}
    except Exception as e:
        raise HTTPException(status_code=503, detail=str(e))


@router.post("/{container_id}/start")
async def start_container(container_id: str):
    success = await docker_service.start_container(container_id)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to start container")
    return {"message": f"Container {container_id} started"}


@router.post("/{container_id}/stop")
async def stop_container(container_id: str):
    success = await docker_service.stop_container(container_id)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to stop container")
    return {"message": f"Container {container_id} stopped"}


@router.post("/{container_id}/restart")
async def restart_container(container_id: str):
    success = await docker_service.restart_container(container_id)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to restart container")
    return {"message": f"Container {container_id} restarted"}


@router.delete("/{container_id}")
async def remove_container(container_id: str):
    success = await docker_service.remove_container(container_id)
    if not success:
        raise HTTPException(status_code=500, detail="Failed to remove container")
    return {"message": f"Container {container_id} removed"}