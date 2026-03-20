from celery import Celery
from celery.schedules import crontab
from app.config import settings

celery_app = Celery(
    "docker_monitor",
    broker=settings.redis_url,
    backend=settings.redis_url,
    include=["app.tasks"],
)

celery_app.conf.update(
    task_serializer="json",
    accept_content=["json"],
    result_serializer="json",
    timezone="UTC",
    enable_utc=True,
)

# Beat schedule — runs every POLL_INTERVAL seconds
celery_app.conf.beat_schedule = {
    "poll-all-containers": {
        "task": "app.tasks.poll_all_containers",
        "schedule": settings.poll_interval,  # every 10 seconds by default
    },
}