# TestCraft — plug-and-play QA platform

Share this folder with any team. They configure **one backend file** and run.

## Structure

```
testcraft/
├── config/SETUP.md              ← human-readable setup guide
├── backend/                     ← Spring Boot API (port 8080)
│   └── src/main/resources/
│       ├── application.properties
│       └── application-local.properties   ← TEAM EDITS THIS (gitignored)
└── frontend/                    ← React UI (port 5173)
    └── .env                     ← optional (see .env.example)
```

## Quick start

```bash
# 1. Config (only step teams must customize)
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties

# 2. Backend
cd backend && mvn spring-boot:run

# 3. Frontend (new terminal)
cd frontend && npm install && npm run dev
```

Open **http://localhost:5173** → **Settings** → click **i** for setup instructions.

## What teams configure

| File | Contains |
|------|----------|
| `backend/.../application-local.properties` | Jira URL, API token, Zephyr JWT, AI keys, project defaults |
| `frontend/.env` | Optional API URL override |

No Java or React code changes required.

## API

- Health/config: `GET /api/config/status`
- Setup guide: `GET /api/config/setup-guide`

See [config/SETUP.md](config/SETUP.md) for full property reference.
