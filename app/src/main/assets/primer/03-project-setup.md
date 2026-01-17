

# Project Setup & Daily Workflow Checklist

Use this guide the next time you spin up a fresh Angular repository. Every step is practical, opinionated, and battle-tested so you can hit the ground running.

## 1. Establish the Environment

1. Install Node.js 18 LTS (or newer) with your version manager of choice (`nvm use 18`).
2. Install the Angular CLI globally:
   ```bash
   npm install -g @angular/cli@17
   ```
3. Confirm the basics:
   ```bash
   node --version    # should be >= 18
   npm --version     # should be >= 9
   ng version        # check Angular CLI + workspace defaults
   ```

## 2. Scaffold the Workspace

```bash
ng new angular-primer \
  --standalone \
  --routing \
  --style=scss \
  --ssr \
  --strict
cd angular-primer
```

- `--standalone` removes the need for `NgModule`.
- `--ssr` wires up Angular Universal at creation time.
- `--strict` enforces the compiler options Angular will rely on going forward.

Run the dev server once (`npm run dev:ssr`) to verify both the browser and server builds succeed.

## 3. Apply Essential Feature Flags

Inside `main.ts`:

```ts
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    provideAnimations(),
    provideExperimentalZonelessChangeDetection(), // opt-in to signals-first CD
  ]
});
```

Template syntax update checklist:

- Replace all `*ngIf`/`*ngFor` with the `@if`/`@for` control flow.
- Use signals for component state (no more ad-hoc subjects for synchronous UI data).
- Wrap heavy UI fragments with `@defer` if they are not critical to first paint.

## 4. Linting & Formatting Baseline

Install the required dev dependencies:

```bash
npm install --save-dev \
  @angular-eslint/eslint-plugin \
  @angular-eslint/eslint-plugin-template \
  eslint \
  prettier \
  eslint-config-prettier
```

Add lint scripts and enforce them via Git:

```json
{
  "scripts": {
    "lint": "ng lint && npm run lint:format",
    "lint:format": "prettier --check \"src/**/*.{ts,html,scss}\"",
    "lint:fix": "ng lint --fix && prettier --write \"src/**/*.{ts,html,scss}\""
  },
  "lint-staged": {
    "src/**/*.{ts,json,html,css,scss}": [
      "prettier --write",
      "ng lint --fix --files"
    ]
  }
}
```

Then configure Husky or a simple `pre-commit` hook later to guarantee consistency.

## 5. Testing, Storybook, and Tooling

1. Swap Karma/Jasmine for your preferred runner (e.g. Vitest):
   ```bash
   ng add @analogjs/vite
   npm install --save-dev vitest @vitest/ui jsdom
   ```
2. For component previews or design systems, initialize Storybook:
   ```bash
   npx storybook@latest init --type angular
   ```
3. Enable Angular DevTools in Chrome when debugging signals and hydration.

## 6. CI/CD Ready in Minutes

Create `.github/workflows/ci.yml` (or the equivalent in your platform):

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
      - run: npm run test -- --watch=false
      - run: npm run build
```

## 7. Committing the Baseline

Before sharing the repository:

- Document the decisions above in `README.md`.
- Use conventional commits or whatever naming convention your team adopts.
- Tag the repo (e.g. `git tag setup-baseline && git push --tags`) so it’s easy to diff future changes.

## Upcoming Practical Guides

| Priority | Working Title | Purpose |
| -------- | ------------- | ------- |
| High | `04-signals-in-depth.md` | Real-world patterns for component and shared stores using signals. |
| High | `05-ssr-hydration-recipes.md` | Deploy-ready SSR + hydration setup including caching and guards. |
| Medium | `06-routing-patterns.md` | Route-level defer strategies, guards, and lazy loading best practices. |
| Medium | `07-testing-modern-stack.md` | Vitest/Jest configuration, component harnesses, and pragmatic coverage goals. |
| Low | `08-ci-deploy-automation.md` | Automating preview environments, Play Store deployment, and release checklists. |
