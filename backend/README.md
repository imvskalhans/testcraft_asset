# TestCraft Backend

Spring Boot API for Jira, Zephyr Scale, AI test generation, and release cycles.

## Configuration (plug-and-play)

**Teams edit only one file:**

```
src/main/resources/application-local.properties
```

Copy from `application-local.properties.example`, fill in Jira / Zephyr / AI credentials, restart.

| Prefix | Purpose |
|--------|---------|
| `jira.*` | Jira Cloud URL, email, API token |
| `zephyr.*` | Zephyr Scale project, EU/US API URL, JWT token |
| `ai.*` | AI provider (mock, openai, azure-gateway) |
| `testcraft.*` | Owner, CORS origins |

Properties bind to `@ConfigurationProperties` classes — services never hardcode URLs or tokens.

## Run

```bash
mvn spring-boot:run
```

API: http://localhost:8080

## Key endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/config/status` | Non-secret config status |
| `GET /api/config/setup-guide` | Setup steps for UI |
| `GET /api/config/me` | Current Jira user |

See parent [../config/SETUP.md](../config/SETUP.md) for full setup guide.

## License

Copyright (c) 2026 Vishal Singh. All rights reserved. See [../LICENSE](../LICENSE).
