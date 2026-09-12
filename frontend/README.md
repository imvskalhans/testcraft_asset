# TestCraft Frontend

React UI for the TestCraft QA platform.

## Configuration

Only one optional file — defaults work for local dev:

```bash
cp .env.example .env
```

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_URL` | *(empty)* | Empty = Vite proxies `/api` to `http://localhost:8080` |

All Jira, Zephyr, and AI credentials live in the **backend** config file.

## Run

```bash
npm install
npm run dev
```

Open http://localhost:5173

## Structure

```
src/
├── api/           # HTTP clients (jira, zephyr, release, config)
├── components/    # Reusable UI (layout, ui, testcases, settings)
├── constants/     # Theme, navigation, options
├── context/       # App-wide state (AppContext)
├── pages/         # One page per nav item
├── styles/        # Shared form styles
└── utils/         # Pure helpers
```

## License

Copyright (c) 2026 Vishal Singh. All rights reserved. See [../LICENSE](../LICENSE).
