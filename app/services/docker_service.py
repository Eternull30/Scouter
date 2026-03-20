from datetime import datetime
import httpx
from app.config import settings
from app.schemas.schemas import LiveStatResponse


class DockerService:
    """Handles all communication with the Docker Remote API."""

    def __init__(self, host: str = None):
        self.host = host or settings.docker_host

    # ── Containers ────────────────────────────────────────────

    async def list_containers(self, all: bool = False) -> list[dict]:
        """List all containers. all=True includes stopped containers."""
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.host}/containers/json",
                params={"all": str(all).lower()},
                timeout=10,
            )
            response.raise_for_status()
            return response.json()

    async def get_container(self, container_id: str) -> dict:
        """Get details of a specific container."""
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.host}/containers/{container_id}/json",
                timeout=10,
            )
            response.raise_for_status()
            return response.json()

    async def start_container(self, container_id: str) -> bool:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.host}/containers/{container_id}/start",
                timeout=10,
            )
            return response.status_code in (204, 304)

    async def stop_container(self, container_id: str) -> bool:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.host}/containers/{container_id}/stop",
                timeout=15,
            )
            return response.status_code in (204, 304)

    async def restart_container(self, container_id: str) -> bool:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.host}/containers/{container_id}/restart",
                timeout=20,
            )
            return response.status_code in (204, 304)

    async def remove_container(self, container_id: str) -> bool:
        async with httpx.AsyncClient() as client:
            response = await client.delete(
                f"{self.host}/containers/{container_id}",
                timeout=10,
            )
            return response.status_code == 204

    async def get_logs(self, container_id: str, tail: int = 100) -> str:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.host}/containers/{container_id}/logs",
                params={"stdout": "true", "stderr": "true", "tail": tail},
                timeout=10,
            )
            response.raise_for_status()
            # Docker log output has 8-byte header per line — strip it
            raw = response.content
            lines = []
            i = 0
            while i < len(raw):
                if i + 8 > len(raw):
                    break
                size = int.from_bytes(raw[i + 4:i + 8], "big")
                lines.append(raw[i + 8:i + 8 + size].decode("utf-8", errors="replace"))
                i += 8 + size
            return "".join(lines)

    # ── Stats ─────────────────────────────────────────────────

    async def get_stats_snapshot(self, container_id: str) -> dict:
        """Single stats snapshot (not streaming). Takes ~1 second."""
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.host}/containers/{container_id}/stats",
                params={"stream": "false"},
                timeout=15,
            )
            response.raise_for_status()
            return response.json()

    def parse_stats(self, raw: dict) -> LiveStatResponse:
        """
        Parse raw Docker stats JSON into clean human-readable values.
        CPU % requires delta calculation between two readings.
        """
        # CPU % calculation
        cpu_delta = (
            raw["cpu_stats"]["cpu_usage"]["total_usage"]
            - raw["precpu_stats"]["cpu_usage"]["total_usage"]
        )
        system_delta = (
            raw["cpu_stats"]["system_cpu_usage"]
            - raw["precpu_stats"]["system_cpu_usage"]
        )
        num_cpus = raw["cpu_stats"].get("online_cpus", 1)
        cpu_percent = 0.0
        if system_delta > 0 and cpu_delta > 0:
            cpu_percent = (cpu_delta / system_delta) * num_cpus * 100.0

        # Memory
        memory_usage = raw["memory_stats"].get("usage", 0)
        memory_limit = raw["memory_stats"].get("limit", 1)

        # Network — sum all interfaces
        networks = raw.get("networks", {})
        rx_bytes = sum(v.get("rx_bytes", 0) for v in networks.values())
        tx_bytes = sum(v.get("tx_bytes", 0) for v in networks.values())

        # Disk I/O
        blkio = raw.get("blkio_stats", {}).get("io_service_bytes_recursive") or []
        disk_read = sum(e.get("value", 0) for e in blkio if e.get("op") == "read")
        disk_write = sum(e.get("value", 0) for e in blkio if e.get("op") == "write")

        return LiveStatResponse(
            container_name=raw.get("name", "unknown").lstrip("/"),
            cpu_percent=round(cpu_percent, 4),
            memory_usage_mb=round(memory_usage / 1024 / 1024, 2),
            memory_limit_mb=round(memory_limit / 1024 / 1024, 2),
            memory_percent=round((memory_usage / memory_limit) * 100, 4) if memory_limit else 0,
            network_rx_kb=round(rx_bytes / 1024, 2),
            network_tx_kb=round(tx_bytes / 1024, 2),
            disk_read_kb=round(disk_read / 1024, 2),
            disk_write_kb=round(disk_write / 1024, 2),
            timestamp=datetime.utcnow(),
        )

    # ── Images ────────────────────────────────────────────────

    async def list_images(self) -> list[dict]:
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{self.host}/images/json", timeout=10)
            response.raise_for_status()
            return response.json()

    async def pull_image(self, image_name: str) -> bool:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.host}/images/create",
                params={"fromImage": image_name},
                timeout=120,  # pulling can take a while
            )
            return response.status_code == 200

    # ── Ping ──────────────────────────────────────────────────

    async def ping(self) -> bool:
        """Check if Docker daemon is reachable."""
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(f"{self.host}/_ping", timeout=5)
                return response.status_code == 200
        except Exception:
            return False


# Single shared instance
docker_service = DockerService()