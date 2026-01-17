# Angular ≥19 Feature Primer

Stay ahead of Angular’s roadmap with this living reference. Versions 16–18 already delivered the architectural shifts Angular 19+ will assume by default—signal-first change detection, standalone APIs, modern template control flow, and hydration-ready rendering. The articles in this primer guide you through adopting those capabilities today so your projects are ready the moment Angular bumps the major version.

## What You’ll Learn Across the Primer

**02 – Signals Crash Course**

- Introduces signals, computed values, and effects.  
- Goal: replace manual `Subject` plumbing with deterministic state.

**03 – Project Setup & Workflow**

- Practical scaffolding commands, linting/formatting conventions, and daily routines.  
- Goal: spin up repositories aligned with Angular’s future defaults.

**04 – Signals In Depth**

- Advanced patterns for signal stores, effects, and zoneless operations.  
- Goal: design scalable state systems without Zone.js crutches.

**05 – SSR & Hydration Recipes**

- Step-by-step SSR setup, hydration diagnostics, caching strategies.  
- Goal: ship SEO-friendly, performance-first deployments.

**06 – Routing Patterns**

- Standalone router configuration, guards, lazy loading, and defer usage.  
- Goal: maintain readable navigation that scales with features.

**07 – Testing the Modern Stack**

- Vitest, Playwright, harnesses, and signals-aware testing patterns.  
- Goal: keep refactors safe with fast, modern tooling.

**08 – CI & Deployment Automation**

- CI pipelines, release automation, artifact handling, environment management.  
- Goal: automate quality gates and deployments confidently.

**09 – Performance Optimization**

- Budgets, profiling workflow, hydration diagnostics, observability.  
- Goal: maintain measurable targets for responsiveness and bundle size.

> **How to use the primer:** Work through one page at a time, apply its checklist, then continue. Each article couples context with concrete tasks so teams can adopt Angular’s modern patterns incrementally.

## Roadmap Snapshot

- Signal-driven change detection is stable and displaces zone-based heuristics.
- Template control flow (`@if`, `@for`, `@switch`) is the new default; structural directives become legacy.
- SSR plus hydration is the baseline for fast, SEO-friendly apps.
- Standalone components/directives/pipes remain the standard; `NgModule` is now a compatibility layer.
- Tooling is shifting toward Vite/esbuild builders, Vitest, Playwright, and modern linting/formatting.

## Suggested Migration Order

1. **Standalone components** – remove `NgModule` friction and simplify DI boundaries.  
2. **Signals for component state** – achieve deterministic change detection with fewer subscriptions.  
3. **Template control flow** – produce cleaner markup and unlock compiler optimizations.  
4. **SSR & hydration** – secure faster first paint and SEO benefits.  
5. **Build/test tooling** – adopt Vite/esbuild/Vitest-ready pipelines and modern CI.

Use the matching primer page for each phase to track your progress.

## Quick Readiness Checklist

Ensure these items are true before diving deeper:

- Project runs on Angular 16 or later (ideally 17+).  
- RxJS version ≥ 7.8 and aligned with Angular’s support matrix.  
- CLI operates in strict mode (or you’re prepared to enable it).  
- TypeScript 5.x and ES2022+ features are standard.  
- ESLint/Prettier (or equivalent) enforce formatting and linting.  
- CI runs lint + unit tests on every merge.

If any item fails, pause and upgrade; the rest of the primer assumes a modern Angular baseline so you can focus on forward-compatible development.

## Looking Ahead

Upcoming additions to the primer include:

- `10-overview-observability.md` – wiring metrics, logs, and alerts around Angular SSR and Android builds.  
- `11-design-system-integration.md` – signal-friendly theming, host directives, and Material 3 harnessing.  
- `12-migration-playbook.md` – combining all chapters into an audit and execution plan for legacy apps.

Treat each page as a playbook, share it with your team, and revisit as Angular evolves—the primer will continue to track the framework’s trajectory toward a signal-first, hydration-ready future.