<div align="center">
<br>
    <img src="./assets/Scouter.png" alt="Scouter logo" title="Scouter logo" width="100"/>
    <br>

</div>    

# Scouter 

A full-stack native Android application for real-time Docker container monitoring and analytics. Just like a Scouter reads power levels in Dragon Ball, Scouter reads your container's CPU, memory, network, and disk stats in real time - directly on your Android phone from anywhere in the world.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat&logo=fastapi&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?style=flat&logo=redis&logoColor=white)
![Celery](https://img.shields.io/badge/Celery-37814A?style=flat&logo=celery&logoColor=white)

---

## What is Scouter?

Most Docker monitoring tools like Portainer and Lazydocker are web or terminal based - not optimized for mobile. Scouter brings real-time container analytics directly to your Android phone. Deploy the backend on any server, expose it via ngrok or Tailscale, connect the Android app to it, and monitor all your running containers from anywhere in the world.

---

## Features

- **Real-time monitoring** - Live CPU, memory, network I/O, and disk stats via WebSocket streaming
- **Live charts** - Smooth animated line charts updating every second with hardware acceleration
- **Historical analytics** - Container metrics saved to PostgreSQL every 10 seconds via Celery background workers
- **Container management** - Start, stop, and restart containers directly from your phone
- **Stopped container visibility** - Stopped containers remain visible with a red indicator so you can restart them
- **Auto-discovery** - Automatically discovers all running and stopped containers on a connected host
- **Background polling** - Celery + Redis task queue continuously collects stats even when app is closed
- **Dark / Light mode** - Fully adaptive UI following system theme with Material Design 3
- **Custom typography** - Inter for UI text, JetBrains Mono for stats and numbers
- **Pull to refresh** - Swipe down on dashboard to refresh container list
- **Multi-server support** - Connect to any Docker host by entering its URL
- **Access from anywhere** - Backend exposed globally via ngrok tunnel over HTTPS/WSS

---

## Architecture
```
Native Android App (Kotlin + Jetpack Compose)
                ↓  HTTPS / WSS
           ngrok tunnel
                ↓
        FastAPI Backend (Python)
        runs on your server / PC
                ↓
Docker Remote API + PostgreSQL
                ↓
        Celery + Redis
    (Background polling every 10s)
```

---

## How It Works

### Live Stats Flow
1. Android opens WebSocket to `/ws/containers/{id}/stats`
2. FastAPI opens streaming connection to Docker Remote API with `stream=true`
3. Docker streams raw stats every ~500 millisecond continuously
4. FastAPI parses raw nanosecond CPU counters into percentages using delta calculations across 2 consecutive readings
5. Clean stats forwarded to Android - charts update smoothly via hardware-accelerated MPAndroidChart
6. Celery simultaneously saves stats to PostgreSQL in background every 10 seconds

### Background Data Pipeline
```
Celery Beat (every 10s)
    → drops task message into Redis queue (milliseconds)
        → Celery Worker picks it up
            → polls all containers via Docker Remote API
                → saves CPU, memory, network, disk rows to PostgreSQL
                    → Redis message discarded
```

### ngrok Tunnel
```
Android (anywhere) → ngrok cloud → persistent tunnel → your PC → FastAPI → Docker
```
Your PC initiates an outbound connection to ngrok's servers. ngrok forwards incoming requests through that tunnel - no port forwarding or firewall rules needed.

---

## Tech Stack

### Android
| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | UI framework |
| MPAndroidChart | Hardware-accelerated real-time charts |
| Retrofit + OkHttp | REST API calls |
| OkHttp WebSocket | Live stats streaming |
| Hilt | Dependency injection |
| DataStore | Persistent host preferences |
| Material Design 3 | Design system with dark/light mode |
| Inter + JetBrains Mono | Custom typography |

### Backend
| Technology | Purpose |
|---|---|
| FastAPI | Async Python web framework |
| PostgreSQL | Historical stats storage |
| SQLAlchemy + Alembic | Async ORM and schema migrations |
| Celery + Redis | Background task queue |
| Docker Remote API | Container communication via HTTP on port 2375 |
| WebSockets | Bidirectional live stats streaming |
| ngrok | Global HTTPS/WSS tunnel |

---

## Self-Hosting Guide

### Prerequisites
- Docker Desktop (Windows/Mac) or Docker Engine (Linux) installed and running
- Python 3.11+
- ngrok account (free tier works)
- Android device with Android 10+

### Backend Setup

**1 - Clone the repo:**
```bash
git clone https://github.com/Eternull30/Scouter.git
cd Scouter
```

**2 - Create virtual environment:**

Windows:
```powershell
python -m venv venv
venv\Scripts\activate
```

Mac / Linux:
```bash
python3.11 -m venv venv
source venv/bin/activate
```

**3 - Install dependencies:**
```bash
pip install -r requirements.txt --only-binary=:all:
pip install numpy --only-binary=:all:
```

**4 - Create `.env` file:**
```
DOCKER_HOST=http://localhost:2375
DATABASE_URL=postgresql+asyncpg://postgres:yourpassword@localhost:5432/dockermonitor
REDIS_URL=redis://localhost:6379/0
SECRET_KEY=your-secret-key-here
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440
POLL_INTERVAL=10
```

**5 - Start PostgreSQL and Redis:**
```bash
docker run -d --name dev-db -p 0.0.0.0:5432:5432 \
  -e POSTGRES_PASSWORD=yourpassword \
  -e POSTGRES_DB=dockermonitor postgres

docker run -d --name redis-broker -p 6379:6379 redis
```

**6 - Enable Docker Remote API:**

Windows - Docker Desktop → Settings → General → enable **"Expose daemon on tcp://localhost:2375 without TLS"** → Apply & Restart

Mac - Docker Desktop → Settings → General → enable **"Expose daemon on tcp://localhost:2375 without TLS"** → Apply & Restart

Linux:
```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo bash -c 'cat > /etc/systemd/system/docker.service.d/override.conf << EOF
[Service]
ExecStart=
ExecStart=/usr/bin/dockerd -H fd:// -H tcp://0.0.0.0:2375 --containerd=/run/containerd/containerd.sock --tls=false
EOF'
sudo systemctl daemon-reload
sudo systemctl restart docker
```

**7 - Run migrations:**
```bash
alembic upgrade head
```

**8 - Start all services:**

Windows (3 separate PowerShell windows):
```powershell
# Window 1 - FastAPI
uvicorn app.main:app --host 0.0.0.0 --port 8000

# Window 2 - Celery Worker
celery -A app.celery_app worker --loglevel=info -P solo

# Window 3 - Celery Beat
celery -A app.celery_app beat --loglevel=info
```

Mac / Linux (3 separate terminals):
```bash
# Terminal 1 - FastAPI
uvicorn app.main:app --host 0.0.0.0 --port 8000

# Terminal 2 - Celery Worker
celery -A app.celery_app worker --loglevel=info

# Terminal 3 - Celery Beat
celery -A app.celery_app beat --loglevel=info
```

**9 - Expose publicly with ngrok:**

Windows:
```powershell
ngrok http 8000
```

Mac / Linux:
```bash
ngrok http 8000
```

Copy the `https://...ngrok-free.app` URL - this is what you enter in the Android app.

### Android Setup

1. Open `android/` folder in Android Studio
2. Let Gradle sync complete
3. Run on your Android device (API 29+ / Android 10+)
4. Enter your ngrok URL and port `443` on the setup screen
5. All containers auto-discovered instantly

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/containers/` | List all containers including stopped |
| GET | `/containers/{id}/stats` | Single stats snapshot |
| GET | `/containers/{id}/stats/history` | Historical stats from PostgreSQL |
| GET | `/containers/{id}/logs` | Container logs |
| POST | `/containers/{id}/start` | Start a container |
| POST | `/containers/{id}/stop` | Stop a container |
| POST | `/containers/{id}/restart` | Restart a container |
| WS | `/ws/containers/{id}/stats` | Live WebSocket stats stream |
| GET | `/hosts/` | List Docker hosts |
| POST | `/hosts/` | Add a Docker host |
| GET | `/hosts/{id}/ping` | Check host connectivity |

---

## Project Structure
```
Scouter/
├── app/                              # FastAPI backend
│   ├── main.py                       # Entry point + default host creation
│   ├── config.py                     # Pydantic settings from .env
│   ├── database.py                   # Async SQLAlchemy engine
│   ├── celery_app.py                 # Celery + Beat schedule
│   ├── tasks.py                      # Background polling task
│   ├── models/models.py              # ORM models (hosts, containers, stats, anomalies)
│   ├── routers/
│   │   ├── containers.py             # Container REST endpoints
│   │   ├── hosts.py                  # Host management
│   │   └── websocket.py              # WebSocket streaming with disconnect handling
│   ├── schemas/schemas.py            # Pydantic request/response schemas
│   └── services/docker_service.py   # Docker Remote API client + stat parser
├── alembic/                          # Database migrations
├── android/
│   └── app/src/main/java/com/jeet/dockermonitor/
│       ├── data/
│       │   ├── Network.kt            # Retrofit API interface
│       │   ├── repository.kt         # Data layer + WebSocket flow
│       │   └── model/Models.kt       # Data classes + UiState
│       ├── di/AppModule.kt           # Hilt + DataStore setup
│       └── ui/
│           ├── setup.kt              # Host connection screen
│           ├── dashboard.kt          # Container list + pull to refresh
│           ├── detail.kt             # Live charts + container controls
│           ├── screens.kt            # Navigation + state management
│           └── theme/                # Colors, typography, dark/light theme
└── requirements.txt
```

---

## Future Scope

- **Docker Compose deployment** - one command to start the entire backend
- **History screen** - dedicated time-series graphs from PostgreSQL
- **Anomaly detection** - NumPy Z-score analysis for CPU spikes and memory leaks
- **Push notifications** - Firebase alerts when containers crash or spike
- **Multi-host screen** - add and switch between multiple Docker hosts in app
- **Container logs screen** - real-time scrollable logs with search
- **Tailscale integration** - permanent private tunnel replacing ngrok
- **Kubernetes support** - extend monitoring to K8s pods

---


## Author

**Jeet Tanwar** - Second year CS student building full-stack products.

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=flat&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/jeet-tanwar/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat&logo=github&logoColor=white)](https://github.com/Eternull30)

---

*It's over 9000 - and so is your container's uptime.* 🐉
