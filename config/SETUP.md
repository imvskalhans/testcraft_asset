# TestCraft — Team Setup Guide

TestCraft is **plug-and-play**: each team configures their own Jira, Zephyr, and AI credentials in **one file**. No code changes required.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |

## Step 1 — Backend configuration (required)

Copy the template:

```bash
cp testcraft/backend/src/main/resources/application-local.properties.example \
   testcraft/backend/src/main/resources/application-local.properties
```

Edit **only** `application-local.properties`:

### Jira Cloud

```properties
jira.base-url=https://YOUR-SITE.atlassian.net
jira.username=you@company.com
jira.api-token=YOUR_JIRA_API_TOKEN
```

Create an API token: [Atlassian API tokens](https://id.atlassian.com/manage-profile/security/api-tokens)

### Zephyr Scale Cloud

```properties
zephyr.default-project-key=PROJ
zephyr.default-project-id=10000
zephyr.scale-cloud-api-token=YOUR_ZEPHYR_JWT
```

- **EU sites** (e.g. `eu.app.tm4j.smartbear.com`):
  `zephyr.scale-cloud-base-url=https://eu.api.zephyrscale.smartbear.com/v2`
- **US sites**: leave default `https://api.zephyrscale.smartbear.com/v2`

Create token: Jira → **Apps** → **Zephyr Scale** → **API Access Tokens**

Optional fallback if folders cannot be listed live:

```properties
zephyr.known-folders=Test Folder:123456,Demo Folder:123457
```

### AI (optional)

```properties
ai.provider=mock          # mock | openai | azure-gateway
ai.openai.api-key=...
ai.openai.model=gpt-4o-mini
```

### App

```properties
testcraft.default-owner=          # blank = jira.username
testcraft.cors-origins=http://localhost:5173
```

> **Never commit** `application-local.properties` — it is gitignored.

## Step 2 — Frontend configuration (optional)

```bash
cp testcraft/frontend/.env.example testcraft/frontend/.env
```

Default: Vite proxies `/api` to `http://localhost:8080`. Change only if backend runs elsewhere.

## Step 3 — Run

```bash
# Terminal 1 — Backend
cd testcraft/backend
mvn spring-boot:run

# Terminal 2 — Frontend
cd testcraft/frontend
npm install
npm run dev
```

- UI: http://localhost:5173
- API: http://localhost:8080
- Config status: http://localhost:8080/api/config/status

## Step 4 — Verify in UI

1. Open **Settings**
2. Click **Test connections**
3. Confirm Jira connected, Zephyr token configured, user resolved

## Config reference

| Property prefix | Purpose |
|----------------|---------|
| `jira.*` | Jira Cloud URL, auth, custom fields |
| `zephyr.*` | Zephyr Scale, project, folders, EU/US API |
| `ai.*` | AI provider and model settings |
| `testcraft.*` | Owner, CORS, comment author |

All keys bind to Java `@ConfigurationProperties` classes — services read config only, never hardcoded URLs.

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Folders empty | Add `zephyr.scale-cloud-api-token` or `zephyr.known-folders` |
| Zephyr 401 Unknown token | Use EU base URL for EU instances |
| Publish owner error | `testcraft.default-owner` or Jira email must match a valid user |
| CORS errors | Add your UI origin to `testcraft.cors-origins` |
