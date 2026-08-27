# TestCraft

TestCraft is a local QA workspace that connects Jira, an AI provider, and Zephyr Scale. It lets a QA engineer fetch a Jira story, generate and edit test cases, publish them to Zephyr, link them to Jira issues, and create traceable test cycles.

The repository contains a Spring Boot backend and a React/Vite frontend. The backend owns all credentials and integration calls; the browser talks only to the backend.

## Capabilities

- Fetch Jira issue details, acceptance criteria, labels, comments, linked stories, and the current user.
- Generate structured test cases with an AI provider, or use mock mode for local development.
- Review and edit names, objectives, preconditions, steps, expected results, priority, and status.
- Publish test cases and step-by-step scripts to Zephyr Scale Cloud or Jira-hosted Zephyr.
- Link test cases to Jira stories/change requests and create traceable test cycles.
- Attach published cases to cycles through Zephyr test executions.
- Run AI story/release analysis and post QA comments back to Jira.

Read [docs/TOOL_GUIDE.md](docs/TOOL_GUIDE.md) for the detailed functionality guide. Read [config/SETUP.md](config/SETUP.md) for the full property reference.

## Repository layout

```text
testcraft/
├── README.md
├── docs/TOOL_GUIDE.md                 # Detailed functionality and workflows
├── config/SETUP.md                    # Configuration and troubleshooting
├── backend/
│   ├── pom.xml                        # Spring Boot/Maven build
│   └── src/main/
│       ├── java/com/acc/testcraft_backend/
│       └── resources/
│           ├── application.properties # Safe defaults
│           └── application-local.properties.example
└── frontend/
    ├── package.json                   # React/Vite scripts
    └── src/
        ├── api/                       # Backend API clients
        ├── components/                # Shared UI and test-case editor
        ├── context/                   # Shared workflow state
        └── pages/                     # Application screens
```

## Prerequisites

| Tool | Minimum | Verify |
|---|---:|---|
| Java | 17 | `java -version` |
| Maven | 3.8 | `mvn -version` |
| Node.js | 18 | `node --version` |
| npm | Bundled with Node | `npm --version` |

You also need Jira access, Zephyr access, and an AI key if using a live AI provider. Mock AI mode does not require an AI key.

## Setup

### 1. Configure the backend

From the repository root:

```bash
cp backend/src/main/resources/application-local.properties.example \
   backend/src/main/resources/application-local.properties
```

Edit only `backend/src/main/resources/application-local.properties`. It is gitignored and must not be committed.

Minimum Jira configuration:

```properties
jira.base-url=https://YOUR-SITE.atlassian.net
jira.api-path=/rest/api/3
jira.auth-type=basic
jira.username=you@company.com
jira.api-token=YOUR_JIRA_API_TOKEN
```

For bearer authentication, use `jira.auth-type=bearer` and set `jira.bearer-token` instead of the email/token pair.

For Zephyr Scale Cloud:

```properties
zephyr.provider=scale-cloud
zephyr.default-project-key=PROJ
zephyr.default-project-id=10000
zephyr.scale-cloud-api-token=YOUR_ZEPHYR_SCALE_TOKEN
zephyr.scale-cloud-base-url=https://api.zephyrscale.smartbear.com/v2
```

For an EU tenant, use:

```properties
zephyr.scale-cloud-base-url=https://eu.api.zephyrscale.smartbear.com/v2
```

For Jira-hosted Zephyr, use `zephyr.provider=jira-plugin` and configure the Jira-hosted API paths from the example file.

For a no-cost local smoke test:

```properties
ai.provider=mock
```

Live providers are `gemini`, `groq`, `openai`, and `azure-gateway`; provider-specific settings are documented in `application-local.properties.example`.

### 2. Start the backend

```bash
cd backend
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to port 8080 by default. If the backend is elsewhere, create `frontend/.env`:

```properties
VITE_API_URL=http://localhost:8080
```

### 4. Verify the installation

Open **Settings** and check Jira connectivity, current user, Zephyr provider/token state, project key/ID, and AI mode. Non-secret status endpoints are:

```text
http://localhost:8080/api/config/status
http://localhost:8080/api/config/me
```

## Recommended workflow

1. **Jira Story** — enter an issue key such as `KAN-7` and fetch it.
2. **Generate Tests** — choose test count/type and generate cases.
3. Review and edit the cases, especially steps and expected results.
4. **Publish & Link** — select a Zephyr project/folder, publish, then link cases to Jira stories or change requests.
5. **Test Cycles** — fetch the change request/story, create cycle types, and update cycle traceability.
6. **Settings** — verify connections and resolve project/folder setup issues.

Publish and link cases before creating cycles when automatic cycle attachment is expected. After changing backend properties, restart Spring Boot.

## Useful commands

```bash
cd backend && mvn test       # Backend tests
cd frontend && npm run build # Frontend production build
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```

## API overview

| Area | Endpoint | Purpose |
|---|---|---|
| Config | `GET /api/config/status` | Non-secret integration status |
| Config | `GET /api/config/me` | Current Jira user and resolved owner |
| Config | `GET /api/config/setup-guide` | Setup content used by the UI |
| Jira | `GET /api/jira/issue/{key}` | Fetch a Jira issue |
| Jira | `POST /api/jira/comment?issueKey=...` | Post a Jira comment |
| Generation | `POST /api/generate` | Generate test cases |
| Review | `POST /api/ai-review` | Run story review |
| Zephyr | `GET /api/zephyr/projects` | List projects |
| Zephyr | `GET /api/zephyr/folders/{projectId}` | Resolve folders |
| Zephyr | `POST /api/zephyr/publish` | Publish one case |
| Zephyr | `POST /api/zephyr/link-bulk` | Link cases to Jira issues |
| Release | `POST /api/release-process/fetch-cr` | Fetch a CR and linked stories |
| Release | `POST /api/release-process/get-test-cycles` | Read existing cycles |
| Release | `POST /api/release-process/create-test-cycles` | Create/reuse cycles and traceability |
| Release | `POST /api/release-process/get-test-cases` | Read cases in cycles |

## Troubleshooting

| Symptom | Fix |
|---|---|
| Jira 401 | Check Jira URL, identity, token, and auth type. |
| Zephyr 401 | Use a Zephyr token, not the Jira token; check EU vs US API URL. |
| No projects | Confirm the user can browse the Jira/Zephyr project. |
| No folders found | Check the Zephyr token/project ID or configure `zephyr.known-folders`. |
| Owner error | Leave `testcraft.default-owner` blank to use the current user, or set a valid owner ID. |
| CORS error | Add the exact frontend origin to `testcraft.cors-origins` and restart the backend. |
| Mock AI output | Set a live `ai.provider` and its provider key. |
| Empty cycle | Publish/link cases first, then run cycle creation again. |

## Security

- Keep `application-local.properties` private and never commit it.
- Rotate credentials if they appear in source control, logs, screenshots, or chat.
- The frontend receives status, project IDs, and account IDs, but not API secrets.
- All external API calls and credentials are handled by the backend.

## Free demo deployment with Render

The repository includes [`render.yaml`](render.yaml), which defines a free Render static site for the frontend and a free Render web service for the Spring Boot backend. The backend runs in [`backend/Dockerfile`](backend/Dockerfile).

### Deploy

1. Push this repository to GitHub or GitLab. Do not commit `application-local.properties` or any token.
2. Create a Render account at [render.com](https://render.com) and choose **New → Blueprint**.
3. Connect the repository and select the branch to deploy.
4. Render reads `render.yaml` and creates `testcraft-api` and `testcraft-ui`.
5. In the `testcraft-api` service, fill the variables marked **sync: false**:
   - Jira URL, username, and API token (or bearer token)
   - Zephyr Scale token
   - Zephyr project key and numeric project ID
   - Optional AI key if changing `AI_PROVIDER` from `mock`
6. Deploy/redeploy the backend, then deploy the frontend.
7. Open `https://testcraft-ui.onrender.com` and verify **Settings**.

If you rename either Render service, update both `VITE_API_URL` on the static site and `TESTCRAFT_CORS_ORIGINS` on the backend to the resulting public URLs. For an EU Zephyr tenant, change `ZEPHYR_SCALECLOUDBASEURL` to `https://eu.api.zephyrscale.smartbear.com/v2`.

### Free-tier expectations

- The backend may sleep after inactivity; the first request can take a short time while it wakes.
- Free services have limited CPU, memory, and monthly usage, so this is appropriate for demos rather than production.
- The backend is the only service that stores/uses credentials. Never put integration secrets in the frontend environment.
- Jira and Zephyr API usage, AI usage, and their account permissions are separate from Render's free hosting.

### Render troubleshooting

| Symptom | Check |
|---|---|
| Frontend cannot reach API | Confirm `VITE_API_URL` points to the public `testcraft-api` URL and redeploy the frontend. |
| CORS error | Set `TESTCRAFT_CORS_ORIGINS` to the exact public frontend URL, including `https://`, then redeploy the backend. |
| API container fails | Open Render logs and confirm the Docker build completed and Java started on port 8080. |
| Jira/Zephyr not connected | Check the backend environment variables and regional Zephyr URL. |
| First request is slow | The free backend was sleeping; retry after it wakes. |
