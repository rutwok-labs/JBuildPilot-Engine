# GitHub Actions & Integration

JBuildPilot can act as an automated maintenance bot for your GitHub repositories via the `jbuildpilot-action` and `jbuildpilot-github` modules.

> ⚠️ **Preview.** In this first alpha release the GitHub Action is not yet published to the GitHub Marketplace, and the IssueOps command flow described below is a design target rather than a fully wired feature. The native `GitHubClient` (issue read + comment) is functional; higher-level automation (`PullRequestManager`, `@jbuildpilot approve …`) is still under construction.

## How It Works (The IssueOps Flow)

JBuildPilot is designed for **Zero-Trust Automation**. It will *never* push destructive changes directly to your `main` branch. 

1. **Scheduled Diagnosis**: A GitHub Action runs JBuildPilot on a cron schedule.
2. **Report Generation**: It identifies outdated dependencies and Java versions.
3. **Approval Wait**: It opens a GitHub Issue or PR with the proposed `MaintenancePlan`.
4. **Execution**: A repository owner approves specific tasks (e.g., commenting `@jbuildpilot approve JB-001`). JBuildPilot creates a branch, applies the task, tests it, and opens a Pull Request.

## Using the GitHub Action

We provide a pre-packaged GitHub Action in the `jbuildpilot-action` directory.

Create `.github/workflows/jbuildpilot-maintenance.yml` in your repository:

```yaml
name: 'JBuildPilot Maintenance'

on:
  workflow_dispatch:
  schedule:
    - cron: '0 3 * * 1' # Run every Monday at 3 AM

jobs:
  diagnose:
    runs-on: ubuntu-latest
    permissions:
      contents: write
      pull-requests: write
      issues: write

    steps:
      - uses: actions/checkout@v4

      - name: Run JBuildPilot Agent
        uses: rutwok-labs/jbuildpilot-action@v1
        with:
          mode: 'diagnose'
          github_token: ${{ secrets.GITHUB_TOKEN }}
```

### Action Inputs

| Input | Description | Default | Required |
|---|---|---|---|
| `mode` | Execution mode (`diagnose`, `plan`, `apply`) | `diagnose` | Yes |
| `github_token` | Token for creating PRs and commenting | `${{ github.token }}` | Yes |
| `task_id` | Optional specific task ID to apply (e.g. `JB-001`) | `null` | No |

## The Native GitHub Client (`jbuildpilot-github`)

To keep the JBuildPilot CLI lightweight and incredibly fast, the `jbuildpilot-github` module does **not** rely on massive external libraries like `org.kohsuke:github-api`.

Instead, it implements a zero-dependency `GitHubClient` using the native Java 11 `java.net.http.HttpClient`.

### API Usage Example

```java
GitHubClient client = new GitHubClient(System.getenv("GITHUB_TOKEN"));
PullRequestManager prManager = new PullRequestManager(client);

// Automatically generate a branch, commit changes, and open a PR
prManager.createMaintenancePR("example/project", maintenancePlan);
```
