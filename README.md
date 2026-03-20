# Scouter 🔭

A full-stack native Android application for real-time Docker container monitoring and analytics. Scouter reads your container's CPU, memory, network, and disk stats in real time — directly on your Android phone.

![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![FastAPI](https://img.shields.io/badge/FastAPI-009688?style=flat&logo=fastapi&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

---

## What is Scouter?

Most Docker monitoring tools like Portainer and Lazydocker are web or terminal based — not optimized for mobile. Scouter brings real-time container analytics directly to your Android phone. Connect to any Docker host, see all your running containers, and watch their stats update live.

---

## Features

- **Real-time monitoring** — Live CPU, memory, network, and disk stats via WebSocket streaming
- **Historical analytics** — Container performance saved to PostgreSQL every 10 seconds via Celery background workers
- **Container management** — Start, stop, and restart containers directly from your phone
- **Multi-host support** — Add and switch between multiple Docker hosts
- **Anomaly detection** — Automatic background detection of CPU spikes, memory leaks, and network anomalies using NumPy Z-score analysis
- **Auto-discovery** — Automatically discovers all running containers on a connected host - no manual setup

---

## Architecture
```
Native Android App (Kotlin + Jetpack Compose)
                ↓  REST + WebSocket
        FastAPI Backend (Python)
                ↓
Docker Remote API + PostgreSQL Database
                ↓
        Celery + Redis
        (Background Polling & Anomaly Detection)
```

---

## Tech Stack

### Android
| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | UI framework |
| MPAndroidChart | Real-time graphs |
| Retrofit + OkHttp | REST API calls |
| OkHttp WebSocket | Live stats streaming |
| Hilt | Dependency injection |
| DataStore | Local host preferences |
| Material Design 3 | Design system |

### Backend
| Technology | Purpose |
|---|---|
| FastAPI | Python web framework |
| PostgreSQL | Historical stats storage |
| SQLAlchemy + Alembic | ORM and migrations |
| Celery + Redis | Background task queue |
| NumPy | Z-score anomaly detection |
| Docker Remote API | Container communication |
| WebSockets | Live stats streaming |

---

## Getting Started

### Prerequisites
- Docker Desktop installed and running
- Docker Remote API enabled on port 2375
- Python 3.11+
- Android Studio

### Backend Setup

1. Clone the repository:
```bash
git clone https://github.com/Eternull30/Scouter.git
cd Scouter
```

2. Create virtual environment:
```bash
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac
```

3. Install dependencies:
```bash
pip install -r requirements.txt --only-binary=:all:
pip install numpy --only-binary=:all:
```

4. Create `.env` file:
```
DOCKER_HOST=http://localhost:2375
DATABASE_URL=postgresql+asyncpg://postgres:yourpassword@localhost:5432/scouter
REDIS_URL=redis://localhost:6379/0
SECRET_KEY=your-secret-key
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440
POLL_INTERVAL=10
```

5. Run PostgreSQL and Redis:
```bash
docker run -d --name dev-db -p 5432:5432 -e POSTGRES_PASSWORD=yourpassword -e POSTGRES_DB=scouter postgres
docker run -d --name redis-broker -p 6379:6379 redis
```

6. Run migrations:
```bash
alembic upgrade head
```

7. Start the backend (3 terminals):
```bash
# Terminal 1 - FastAPI
uvicorn app.main:app --reload --host 0.0.0.0

# Terminal 2 - Celery Worker
celery -A app.celery_app worker --loglevel=info -P solo

# Terminal 3 - Celery Beat
celery -A app.celery_app beat --loglevel=info
```

### Android Setup

1. Open `android/` folder in Android Studio
2. Let Gradle sync complete
3. Run the app on your device
4. Enter your server IP and port 8000 on the setup screen
5. All running containers are auto-discovered instantly

---

## How It Works

### Live Stats Flow
1. Android opens a WebSocket to `/ws/containers/{id}/stats`
2. FastAPI opens a streaming connection to Docker Remote API
3. Docker streams stats every second
4. FastAPI parses raw data into clean values (CPU %, MB, KB)
5. Stats forwarded to Android and UI updates every second
6. Celery simultaneously saves stats to PostgreSQL in the background

### Anomaly Detection
Celery runs NumPy Z-score analysis on every new batch of stats:
- **CPU spike** — CPU > 85% with Z-score > 2
- **Memory leak** — Memory continuously increasing over 10 readings
- **Network spike** — Abnormal network I/O with Z-score > 3
- All anomalies logged to PostgreSQL

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/containers/` | List all containers |
| GET | `/containers/{id}/stats` | Live stats snapshot |
| GET | `/containers/{id}/stats/history` | Historical stats |
| GET | `/containers/{id}/logs` | Container logs |
| POST | `/containers/{id}/start` | Start container |
| POST | `/containers/{id}/stop` | Stop container |
| POST | `/containers/{id}/restart` | Restart container |
| WS | `/ws/containers/{id}/stats` | Live WebSocket stream |
| GET | `/hosts/` | List Docker hosts |
| POST | `/hosts/` | Add Docker host |
| GET | `/hosts/{id}/ping` | Check host connectivity |

---

## Future Scope

- **Cloudflare Tunnel integration** — Access containers from anywhere without port forwarding
- **Docker Compose stack visualization** — Manage grouped services as a single unit
- **Container logs screen** — Real-time scrollable logs with search and filtering
- **Push notifications** — Firebase alerts when anomalies are detected
- **Home screen widget** — Quick container health glance without opening the app
- **Image management** — Pull and manage Docker images from the app
- **Kubernetes support** — Extend monitoring to K8s pods and deployments
- **AI-powered insights** — LLM analysis of container logs and anomaly patterns
- **Team collaboration** — Share host access with role-based permissions

---

## Project Structure
```
Scouter/
├── app/                                      # FastAPI backend
│   ├── main.py                               # App entry point
│   ├── config.py                             # Settings
│   ├── database.py                           # DB connection
│   ├── celery_app.py                         # Celery config
│   ├── tasks.py                              # Background polling + anomaly detection
│   ├── models/                               # SQLAlchemy models
│   ├── routers/                              # API endpoints
│   ├── schemas/                              # Pydantic schemas
│   └── services/                             # Docker Remote API service
├── alembic/                                  # Database migrations
├── android/                                  # Android app
│   └── app/src/main/java/com/jeet/Scouter/
│       ├── data/                             # Models, API, Repository
│       ├── di/                               # Hilt DI
│       └── ui/                               # Screens and components
└── requirements.txt
```
