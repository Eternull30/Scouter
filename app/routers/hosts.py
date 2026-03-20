from fastapi import APIRouter, HTTPException
from app.services.docker_service import DockerService
from app.schemas.schemas import HostCreate

router = APIRouter(prefix="/hosts", tags=["hosts"])

# In-memory host store for now (will move to DB when we add PostgreSQL)
_hosts: dict[int, dict] = {
    1: {"id": 1, "name": "localhost", "ip_address": "localhost", "port": 2375}
}
_next_id = 2


@router.get("/")
async def list_hosts():
    return list(_hosts.values())


@router.post("/")
async def add_host(host: HostCreate):
    global _next_id
    new_host = {"id": _next_id, **host.model_dump()}
    _hosts[_next_id] = new_host
    _next_id += 1
    return new_host


@router.delete("/{host_id}")
async def remove_host(host_id: int):
    if host_id not in _hosts:
        raise HTTPException(status_code=404, detail="Host not found")
    del _hosts[host_id]
    return {"message": f"Host {host_id} removed"}


@router.get("/{host_id}/ping")
async def ping_host(host_id: int):
    if host_id not in _hosts:
        raise HTTPException(status_code=404, detail="Host not found")
    host = _hosts[host_id]
    service = DockerService(host=f"http://{host['ip_address']}:{host['port']}")
    reachable = await service.ping()
    return {"host_id": host_id, "reachable": reachable}