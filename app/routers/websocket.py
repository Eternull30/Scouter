import asyncio
import json
from datetime import datetime
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
import httpx
from app.config import settings
from app.services.docker_service import docker_service

router = APIRouter(tags=["websocket"])


@router.websocket("/ws/containers/{container_id}/stats")
async def websocket_stats(websocket: WebSocket, container_id: str):
    """
    WebSocket endpoint that streams live container stats.
    The Android app connects here and receives stats every second.

    Connect with: ws://localhost:8000/ws/containers/nginx-test/stats
    """
    await websocket.accept()
    print(f"[WS] Client connected for container: {container_id}")

    try:
        # Open a streaming connection to Docker Remote API
        async with httpx.AsyncClient() as client:
            async with client.stream(
                "GET",
                f"{settings.docker_host}/containers/{container_id}/stats",
                params={"stream": "true"},
                timeout=None,  # keep alive indefinitely
            ) as response:
                async for line in response.aiter_lines():
                    if not line.strip():
                        continue
                    try:
                        raw = json.loads(line)
                        parsed = docker_service.parse_stats(raw)
                        await websocket.send_json({
                            "container_name": parsed.container_name,
                            "cpu_percent": parsed.cpu_percent,
                            "memory_usage_mb": parsed.memory_usage_mb,
                            "memory_limit_mb": parsed.memory_limit_mb,
                            "memory_percent": parsed.memory_percent,
                            "network_rx_kb": parsed.network_rx_kb,
                            "network_tx_kb": parsed.network_tx_kb,
                            "disk_read_kb": parsed.disk_read_kb,
                            "disk_write_kb": parsed.disk_write_kb,
                            "timestamp": parsed.timestamp.isoformat(),
                        })
                    except Exception as parse_err:
                        print(f"[WS] Parse error: {parse_err}")
                        continue

    except WebSocketDisconnect:
        print(f"[WS] Client disconnected: {container_id}")
    except Exception as e:
        print(f"[WS] Error: {e}")
        try:
            await websocket.send_json({"error": str(e)})
        except Exception:
            pass
        await websocket.close()