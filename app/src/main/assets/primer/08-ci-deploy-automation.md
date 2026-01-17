# CI & Deployment Automation

This guide streamlines how your Angular project moves from commit to release. It covers CI pipelines, artifact handling, and deployment workflows that work equally well for SSR-enabled apps and static builds.

## 1. Pipeline Goals

- **Confidence:** every merge is linted, tested, and built.
- **Speed:** caches and incremental builds keep feedback under five minutes.
- **Traceability:** release artifacts, logs, and environment metadata are easy to find later.
- **Rollback-ready:** you can revert to the previous deploy in one command.

## 2. Branch & Release Strategy

| Workflow | Recommendation |
| -------- | -------------- |
| Main branch | Always deployable. PRs must pass the full CI suite. |
| Feature branches | Run lint + unit tests only (optional e2e). |
| Release tagging | Use semantic tags (e.g. `v1.3.0`). Auto-generate changelog snippets. |
| Hotfixes | Branch from the tag you deployed (e.g. `hotfix/v1.3.1`), run the same pipeline. |

## 3. CI Environment Setup

1. **Cache Node modules:**
   ```yaml
   - uses: actions/setup-node@v4
     with:
       node-version: 18
       cache: npm
   - run: npm ci
   ```

2. **Secrets management:**
   - GitHub → Settings → Secrets and variables → Actions → add entries like `FIREBASE_TOKEN`, `ANDROID_KEYSTORE`.
   - Never hard-code tokens in workflows or `gradle.properties`.

3. **Baseline scripts (package.json):**
   ```json
   {
     "scripts": {
       "lint": "ng lint && prettier --check \"src/**/*.{ts,html,scss}\"",
       "test": "vitest run",
       "test:watch": "vitest",
       "e2e": "playwright test",
       "build": "ng build",
       "build:ssr": "ng build && ng run angular-primer:server",
       "serve:ssr": "node dist/angular-primer/server/main.mjs"
     }
   }
   ```

## 4. GitHub Actions Template

Create `.github/workflows/ci.yml`:

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 18
          cache: npm
      - run: npm ci

      - run: npm run lint
      - run: npm run test -- --runInBand

      - name: Install Playwright
        run: npx playwright install --with-deps
      - run: npm run e2e

      - run: npm run build:ssr

      - name: Upload browser bundle
        uses: actions/upload-artifact@v4
        with:
          name: browser-dist
          path: dist/angular-primer/browser
      - name: Upload server bundle
        uses: actions/upload-artifact@v4
        with:
          name: server-dist
          path: dist/angular-primer/server
```

- Artifacts let you retrieve an exact build later (for QA or rollback).
- If Playwright is too heavy for PRs, gate `npm run e2e` behind `if: github.ref == 'refs/heads/main'`.

## 5. Automated Releases

### 5.1 Changelog and version tagging

1. Install `standard-version`:
   ```bash
   npm install --save-dev standard-version
   ```

2. Add script:
   ```json
   "release": "standard-version"
   ```

3. Run:
   ```bash
   npm run release                 # bumps version + updates CHANGELOG.md
   git push --follow-tags
   ```

### 5.2 Release workflow (optional)

Create `.github/workflows/release.yml` triggered on tag pushes:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*.*.*'

jobs:
  build-and-release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/setup-node@v4
        with:
          node-version: 18
          cache: npm
      - run: npm ci
      - run: npm run build:ssr
      - run: npm run build:android          # if you bundle the Android app here
      - uses: actions/upload-artifact@v4
        with:
          name: release-browser
          path: dist/angular-primer/browser
      - uses: actions/upload-artifact@v4
        with:
          name: release-server
          path: dist/angular-primer/server
      - uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: app/build/outputs/apk/release/app-release.apk
      - name: Publish GitHub Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            dist/angular-primer/browser/**
            dist/angular-primer/server/**
            app/build/outputs/apk/release/app-release.apk
```

## 6. Deployment Targets

### 6.1 SSR with Node (Render, Fly.io, AWS, GCP)

- Package `dist/angular-primer/browser` as static assets.
- Serve `dist/angular-primer/server/main.mjs` via Node 18 runtime.
- Example `Procfile` (Heroku/Fly.io):
  ```
  web: node dist/angular-primer/server/main.mjs
  ```
- Configure process scaling based on memory (each SSR request instantiates Angular).

### 6.2 Static hosting with edge functions (Vercel, Netlify)

- Deploy CSR build from `dist/angular-primer/browser`.
- Use serverless function (Vercel) or Netlify Edge Functions for SSR endpoints if needed.
- Add `vercel.json` or `netlify.toml` to map routes to the SSR handler.

### 6.3 Firebase Hosting + Cloud Functions

`firebase.json` snippet:

```json
{
  "hosting": {
    "public": "dist/angular-primer/browser",
    "rewrites": [
      {
        "source": "**",
        "function": "ssr"
      }
    ]
  }
}
```

Deploy command:

```bash
npm run build:ssr
firebase deploy --only hosting,functions
```

### 6.4 Play Store (Android app)

1. Ensure `assembleRelease` runs in the CI release job.
2. Use `r0adkll/upload-google-play@v1` action or manually upload the `.aab` through the Play Console.
3. Store the service account JSON in repository secrets (`PLAY_STORE_SERVICE_ACCOUNT`).

## 7. Environment Management

| Stage | Config Source | Notes |
| ----- | ------------- | ----- |
| Local | `.env.local` | Never commit. Load via `dotenv` or Angular environment files. |
| CI | Workflow secrets / GitHub env vars | Access using `${{ secrets.MY_VAR }}`. |
| Production | Cloud provider secrets manager | Inject at runtime (Node) or compile-time for browser-only settings. |

Use Angular’s `environment.ts` pattern for compile-time toggles, but keep secrets server-side.

## 8. Monitoring & Alerts

- **Frontend:** set up Sentry or LogRocket to capture runtime errors after deploys.
- **Server:** pipe logs to the platform’s log sink (Stackdriver, CloudWatch, etc.).
- **CI:** enable Slack or Teams notifications for failed pipelines using `actions/slack@v1` or similar.

## 9. Rollback Procedures

1. **SSR / Node:** redeploy the previous artifact (keep last two builds accessible).
2. **Static hosting:** revert to the previous version from the hosting dashboard (Netlify/Vercel have one-click rollbacks).
3. **Play Store:** retain an older release in the “Production” track and promote it.
4. **Documentation:** log rollback steps in a `RELEASES.md` or issue tracker for future reference.

## 10. Deployment Checklist

- [ ] Pipeline is reproducible locally (`npm run lint && npm run test && npm run build:ssr`).
- [ ] Release tags map to changelog entries.
- [ ] Artifacts stored for at least one previous release.
- [ ] Secrets are scoped appropriately (production vs staging).
- [ ] Monitoring alerts configured for build failures and production errors.
- [ ] Rollback strategy documented and tested.

Automated CI/CD ensures every merge is production-ready and every release is traceable. Combine these steps with the routing, SSR, testing, and workflow guides to maintain a reliable Angular delivery pipeline.
