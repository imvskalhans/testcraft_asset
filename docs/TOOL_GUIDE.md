# TestCraft functionality guide

This guide explains the product behavior, screen-by-screen workflow, integration model, and traceability rules. TestCraft is a workflow layer over Jira, an AI model, and Zephyr; Jira and Zephyr remain the systems of record.

## End-to-end flow

```text
Jira issue
  → fetch context
  → AI generation and human review
  → publish test case and step script to Zephyr
  → link test case ↔ Jira story/change request
  → create test cycle
  → create test executions for the cycle's test cases
  → link test cycle ↔ story/change request
```

## Screens

### Jira Story

Enter a key such as `KAN-7` and choose **Fetch**. TestCraft loads the summary, issue type, status, priority, description, acceptance criteria, labels, and comments. Jira rich-text/ADF fields are converted to readable text.

The fetched issue is reused as AI context. The second card can post a QA comment back to Jira.

### Generate Tests

Choose the number of cases, test type, and prompt style. The AI is asked to return structured JSON containing:

- Test name
- Objective
- Preconditions
- Executable steps
- Expected result
- Priority
- Test type

Cases are drafts until reviewed. Every field and step can be edited, added, removed, or reordered through the UI. Mock mode produces predictable sample cases without an AI credential.

### Publish & Link

The publish screen has three stages:

1. Review generated cases.
2. Select a Zephyr project and test-case folder.
3. Publish cases and link their returned keys to Jira issues.

For Scale Cloud, publishing works in two API operations:

1. Create the test case with its metadata.
2. Call Zephyr's dedicated `/testcases/{key}/teststeps` endpoint using `OVERWRITE` mode and inline step items.

This second operation is required by Zephyr for step-by-step scripts. The published key is shown as a direct Test Script link.

Test-case links are coverage links. They are visible from both the Jira issue and Zephyr test-case sides. Repeating a link is treated as already linked.

### Test Cycles

#### Fetch

Fetching a new change request/story loads its linked stories and existing cycles. It also clears the previous cycle results and link fields so the page cannot show stale data from another story.

#### Create

Choose one or more types, such as Functional, Regression, or Performance. TestCraft creates or reuses a cycle named from the story and type, for example:

```text
KAN-7 - Performance - Homepage design
```

The UI displays the Zephyr cycle key, such as `KAN-R13`, together with the readable name and a working Zephyr UI link.

#### Attach and trace

For a single-story workflow, TestCraft passes the exact published keys from the current publish session. For broader workflows, it uses Zephyr linked-case discovery as a fallback.

For every matching case, TestCraft creates a Zephyr test execution with status `Not Executed`. In Zephyr Scale, the test execution is what places a test case inside a cycle; a Jira coverage link alone does not attach a case to a cycle.

The cycle is also linked to the individual story and its parent change request. These are separate from the test-case coverage links.

### AI Actions

AI Actions runs configured story or release analysis. The action list is supplied by the backend so the frontend remains independent of provider-specific logic.

### Settings

Settings provides connection and setup diagnostics without exposing secrets:

- Current Jira user and dynamic account/Owner ID
- Jira URL, auth type, and connection state
- Zephyr provider, project key/ID, and token-configured state
- AI provider and mock/live state
- Project lookup, folders, statuses, and priorities
- Setup guide and configuration-copy helpers

The Owner ID is an account identifier used for ownership resolution, not an authentication credential.

## Traceability model

| Relationship | What TestCraft creates |
|---|---|
| Test case ↔ Jira issue | Zephyr coverage link |
| Test case inside cycle | Zephyr test execution |
| Test cycle ↔ Jira story/CR | Zephyr cycle issue link |
| Test run/cycle ↔ story/CR in Jira-hosted mode | Zephyr test-run trace links |

Creating a test-case/Jira link does not automatically add that case to a cycle. Creating a cycle/Jira link does not automatically add cases. TestCraft performs the test-execution step separately during cycle creation.

## Configuration behavior

### Jira

Jira supplies issue content, comments, linked stories, current-user information, issue IDs, and project resolution. The configured identity needs permission to browse issues and create comments if that feature is used.

### Zephyr

`scale-cloud` uses the public Zephyr Scale Cloud API and a separate Zephyr token. `jira-plugin` uses Zephyr endpoints hosted on the Jira site. EU Scale tenants must use `https://eu.api.zephyrscale.smartbear.com/v2`.

Folder discovery requires appropriate Zephyr access. `zephyr.known-folders` can provide a controlled fallback of `name:numericId` entries.

### AI

`ai.provider=mock` is intended for local development and UI/integration smoke tests. Live providers require their own provider key, endpoint, and model settings.

### Owner resolution

Publishing resolves ownership from the operation owner, then the current Jira account, then the configured default owner. The Settings page shows the resolved account identifier to make configuration diagnosable.

## Operational recommendations

- Fetch the intended Jira story before generating tests.
- Treat AI output as a draft and review it before publishing.
- Publish and link cases before creating cycles for automatic attachment.
- Re-run cycle creation after publishing new cases so executions are added to an existing/reused cycle.
- Restart Spring Boot after editing `application-local.properties`.
- Check the backend terminal for provider response details when an operation fails.

## Common failures

| Failure | What to check |
|---|---|
| Jira issue cannot be fetched | Jira URL, credentials, issue key, and browse permission |
| Acceptance criteria is empty | The Jira field must be named “Acceptance Criteria” or its custom-field ID must be configured |
| Folder list is empty | Zephyr token, project ID, regional endpoint, or `known-folders` fallback |
| Test script is empty | Ensure the case has non-empty steps and publish through the current backend; Scale Cloud uses the `/teststeps` endpoint |
| Cycle has zero cases | Cases must be published/linked first; rerun cycle creation so test executions are created |
| Cycle link opens incorrectly | Use a newly created cycle response; it contains the current Zephyr UI key URL |
| AI remains in mock mode | Configure a live provider and restart the backend |

## Developer extension points

- Add or change integration calls in `backend/src/main/java/com/acc/testcraft_backend/client`.
- Keep validation and orchestration in `backend/src/main/java/com/acc/testcraft_backend/service`.
- Add REST contracts in `backend/src/main/java/com/acc/testcraft_backend/controller` and models in `model`.
- Add frontend API calls under `frontend/src/api`.
- Keep cross-screen state in `frontend/src/context/AppContext.jsx`.
- Add reusable UI in `frontend/src/components` and screen-specific behavior in `frontend/src/pages`.

When adding a new setting, update all three places: the example properties file, `config/SETUP.md`, and the Settings/setup guide if the setting affects a user-visible workflow.
