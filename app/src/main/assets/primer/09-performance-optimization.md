# Performance Optimization Playbook

Angular’s modern stack (signals, standalone APIs, zoneless change detection) makes it easier than ever to ship fast applications—but only if you measure and tune the right things. This playbook walks through practical steps you can apply incrementally.

## 1. Establish Performance Budgets

| Metric | Baseline Target | Notes |
| ------ | --------------- | ----- |
| First Contentful Paint (FCP) | < 2.0 s on mid-range mobile | Test via Lighthouse on the SSR server build. |
| Largest Contentful Paint (LCP) | < 2.5 s | Serve hero content directly in SSR output. |
| Time to Interactive (TTI) | < 3.0 s | Control hydration cost; defer non-critical widgets. |
| JavaScript bundle | < 200 KB gzipped | Enforce via `budgets` in `angular.json`. |
| API latency | < 400 ms for critical endpoints | Cache at the edge when possible. |

Add budgets to `angular.json`:

```json
"budgets": [
  {
    "type": "initial",
    "maximumWarning": "200kb",
    "maximumError": "250kb"
  },
  {
    "type": "anyComponentStyle",
    "maximumWarning": "6kb",
    "maximumError": "10kb"
  }
]
```

## 2. Optimize Change Detection with Signals

1. **Refine derived state:** use `computed` to precalculate values rather than recalculating in templates.
2. **Zoneless mode:** adopt `provideExperimentalZonelessChangeDetection()` early to remove Zone.js overhead.
3. **Chunk heavy views:** wrap non-critical sections with `@defer` to avoid hydrating everything up front.
4. **Avoid redundant signals:** if a value never changes after initial render, keep it as a constant or input.

## 3. Bundle & Asset Strategies

### 3.1 Lean bundles

- Remove polyfills you no longer need. Modern browsers already support ES2020+.
- Use standalone lazy routes for features to benefit from code splitting.
- Prefer dynamic imports for admin or rarely used widgets.

### 3.2 Asset handling

- Store images in WebP/AVIF; serve via a CDN.
- Lazy-load images using native `loading="lazy"` or IntersectionObserver directives.
- Inline critical CSS with Angular CLI’s `inlineCritical` option, then defer the rest.

## 4. Network & API Efficiency

- Coalesce API calls: aggregate requests with `forkJoin` or batch endpoints.
- Enable HTTP caching headers (`ETag`, `Cache-Control`) in your backend.
- In SSR, use resolvers to prefetch data once per request rather than duplicating fetches on the client.
- Monitor GraphQL/REST queries—misconfigured caching can negate SSR gains.

## 5. Profiling Workflow

1. **Local analysis**
   - Run `npm run build:ssr` and serve it.
   - Use Lighthouse (`npm run lighthouse`) on the served site for lab metrics.
   - Inspect bundle sizes with `ng build --stats-json` and load the stats in `webpack-bundle-analyzer`.

2. **Runtime profiling**
   - Chrome DevTools Performance tab → record interactions, ensure there are no long tasks (>50 ms).
   - Angular DevTools → Signals tab to inspect dependency graphs and effect frequency.

3. **Production metrics**
   - Capture Core Web Vitals with a library like `web-vitals`.
   - Send metrics to your observability stack (Datadog, New Relic, Grafana).

## 6. Hydration Diagnostics

- Turn on Angular DevTools’ **Hydration** overlay.
- In SSR builds, look for mismatches or unhydrated islands (highlighted nodes).
- Ensure placeholders (`@placeholder`) and spinners (`@loading`) match the hydrated DOM to avoid layout shifts.

## 7. Edge Cases & Fixes

| Symptom | Root Cause | Fix |
| ------- | ---------- | ---- |
| Layout shifts after hydration | Dynamic data replaces placeholder content | Reserve space using skeleton components or fixed heights. |
| Long TTI | Too many eager components | Defer non-essential components; reduce global providers. |
| High memory usage | Large in-memory stores or caches | Scope services to routes when possible; release references in `onDestroy`. |
| Bundle spikes after dependency updates | New heavy library added | Monitor `npm install` diff; remove unused exports or replace with lighter alternatives. |

## 8. Frontend Observability

- Wrap key user flows with custom events (e.g., `analytics.track('page_ready', { lcp })`).
- Capture errors with Sentry or another error tracking service.
- Log slow API responses client-side and compare against server logs to identify network bottlenecks.

## 9. Automation

- Integrate Lighthouse CI (or PageSpeed Insights API) into your build pipeline.
- Gate merges if budgets are exceeded:
  ```yaml
  - run: npm run build -- --stats-json
  - run: npm run analyze-bundle
  ```
- Version the `dist/` output in your CI pipeline to compare sizes between releases.

## 10. Quick Checklist Before Every Release

- [ ] Lighthouse score ≥ 90 for Performance on representative devices.
- [ ] Bundle size within defined Angular budgets.
- [ ] No hydration warnings or mismatches in browsers.
- [ ] No API call duplicates on initial render.
- [ ] Observability dashboards show stable latency and error rates.
- [ ] Document any deviations or temporary regressions in the release notes.

Following this playbook keeps your Angular app responsive and maintainable, letting new developers focus on features instead of firefighting performance issues.
