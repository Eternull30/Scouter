import asyncio
import numpy as np
from datetime import datetime
from celery import shared_task
from app.celery_app import celery_app
from app.services.docker_service import DockerService
from app.database import AsyncSessionLocal
from app.models.models import Container, Stat, Anomaly
from sqlalchemy import select, desc


# ── Helper to run async code inside Celery (which is sync) ───
def run_async(coro):
    try:
        loop = asyncio.get_event_loop()
        if loop.is_closed():
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
    except RuntimeError:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    return loop.run_until_complete(coro)


# ── Anomaly Detection ─────────────────────────────────────────
async def detect_anomalies(db, container: Container, new_stat: Stat):
    """
    Run Z-score anomaly detection on the last 20 readings.
    Flags CPU spikes, memory leaks, and abnormal network usage.
    """
    # Fetch last 20 stats for this container
    result = await db.execute(
        select(Stat)
        .where(Stat.container_id == container.id)
        .order_by(desc(Stat.recorded_at))
        .limit(20)
    )
    recent = result.scalars().all()

    if len(recent) < 10:
        return  # Not enough data yet

    cpu_values = np.array([s.cpu_percent for s in recent])
    mem_values = np.array([s.memory_usage for s in recent])

    # Z-score: how many standard deviations from mean
    def z_score(values, current):
        mean = np.mean(values)
        std = np.std(values)
        if std == 0:
            return 0
        return abs((current - mean) / std)

    anomalies_to_save = []

    # CPU spike — Z-score > 2 AND cpu > 85%
    cpu_z = z_score(cpu_values, new_stat.cpu_percent)
    if cpu_z > 2 and new_stat.cpu_percent > 85:
        anomalies_to_save.append(Anomaly(
            container_id=container.id,
            type="cpu_spike",
            severity="high",
            message=f"CPU spike detected: {new_stat.cpu_percent:.1f}% (Z-score: {cpu_z:.2f})",
        ))
        print(f"[ANOMALY] CPU spike on {container.name}: {new_stat.cpu_percent:.1f}%")

    # Memory leak — memory consistently increasing over last 10 readings
    if len(mem_values) >= 10:
        # Check if memory has been monotonically increasing
        diffs = np.diff(mem_values[:10])
        if np.all(diffs > 0):
            anomalies_to_save.append(Anomaly(
                container_id=container.id,
                type="memory_leak",
                severity="medium",
                message=f"Memory continuously increasing over last 10 readings. Current: {new_stat.memory_usage / 1024 / 1024:.1f} MB",
            ))
            print(f"[ANOMALY] Memory leak on {container.name}")

    # Abnormal network spike — Z-score > 3
    net_values = np.array([s.network_rx + s.network_tx for s in recent])
    current_net = new_stat.network_rx + new_stat.network_tx
    net_z = z_score(net_values, current_net)
    if net_z > 3:
        anomalies_to_save.append(Anomaly(
            container_id=container.id,
            type="network_spike",
            severity="low",
            message=f"Abnormal network activity detected (Z-score: {net_z:.2f})",
        ))
        print(f"[ANOMALY] Network spike on {container.name}")

    # Save all detected anomalies
    for anomaly in anomalies_to_save:
        db.add(anomaly)
    if anomalies_to_save:
        await db.commit()


# ── Main polling task ─────────────────────────────────────────
async def _poll_all_containers():
    docker = DockerService()
    print(f"[CELERY] Polling containers at {datetime.utcnow().strftime('%H:%M:%S')}")

    try:
        containers = await docker.list_containers(all=False)
    except Exception as e:
        print(f"[CELERY] Docker unreachable: {e}")
        return

    for c in containers:
        container_id = c["Id"]
        name = c["Names"][0].lstrip("/") if c["Names"] else "unknown"

        # Fresh session per container to avoid async conflicts
        async with AsyncSessionLocal() as db:
            try:
                raw = await docker.get_stats_snapshot(container_id)
                parsed = docker.parse_stats(raw)

                result = await db.execute(
                    select(Container).where(Container.container_id == container_id)
                )
                container = result.scalar_one_or_none()

                if not container:
                    container = Container(
                        host_id=1,
                        container_id=container_id,
                        name=name,
                        image=c.get("Image", "unknown"),
                        status="running",
                    )
                    db.add(container)
                    await db.commit()
                    await db.refresh(container)

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
                await db.refresh(stat)

                await detect_anomalies(db, container, stat)

                print(f"[CELERY] {name} — CPU: {parsed.cpu_percent:.3f}% | RAM: {parsed.memory_usage_mb:.1f}MB")

            except Exception as e:
                print(f"[CELERY] Error polling {name}: {e}")
                continue


@celery_app.task(name="app.tasks.poll_all_containers")
def poll_all_containers():
    """Celery task — called every POLL_INTERVAL seconds by Celery Beat."""
    coro = _poll_all_containers()
    run_async(coro)