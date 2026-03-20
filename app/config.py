from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    docker_host: str = "http://localhost:2375"
    database_url: str = "postgresql+asyncpg://postgres:dev@localhost:5432/dockermonitor"
    redis_url: str = "redis://localhost:6379/0"
    secret_key: str = "your-secret-key-change-this-in-production"
    algorithm: str = "HS256"
    access_token_expire_minutes: int = 1440
    poll_interval: int = 10

    class Config:
        env_file = ".env"


settings = Settings()