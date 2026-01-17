# Observability & Alerting

Modern Angular apps—especially those that combine SSR, hydration, and companion Android builds—need visibility into every layer. This guide shows you how to instrument logs, metrics, tracing, and alerts so issues surface before your users notice.

## 1. Observability Goals

- **Detect** errors and regressions within minutes of deployment.
- **Diagnose** client, server, and infrastructure issues from one dashboard.
- **Measure** user experience (Core Web Vitals, hydration health, app startup).
- **Alert** only on actionable signals to avoid pager fatigue.
- **Document** every monitor so new developers understand what “healthy” means.

## 2. Core Pillars

1. **Logging:** structured, queryable records for client and server.
2. **Metrics:** numerical measurements (latency, error rate, bundle size).
3. **Tracing:** request-level timing, especially across SSR and APIs.
4. **Alerts:** thresholds tied to SLIs/SLOs (service level indicators/objectives).

Pick tools that unify these pillars (e.g., Datadog, New Relic, Grafana stack, Sentry + Prometheus). Avoid siloed point solutions unless they integrate cleanly.

## 3. Client-Side Instrumentation (Angular)

### 3.1 Error reporting

```ts
@Injectable({ providedIn: 'root' })
export class GlobalErrorHandler extends ErrorHandler {
  private readonly analytics = inject(AnalyticsService);

  override handleError(error: unknown): void {
    this.analytics.captureException(error);
    super.handleError(error);
  }
}
```

Register it:

```ts
bootstrapApplication(AppComponent, {
  providers: [{ provide: ErrorHandler, useClass: GlobalErrorHandler }]
});
```

Use Sentry, LogRocket, or Bugsnag for rich stack traces and session replay.

### 3.2 Performance metrics

```ts
import { onCLS, onFID, onLCP } from 'web-vitals';

const sendMetric = (metric: Metric) =>
  fetch('/api/metrics', {
    method: 'POST',
    body: JSON.stringify(metric),
    keepalive: true
  });

onCLS(sendMetric);
onFID(sendMetric);
onLCP(sendMetric);
```

Correlate metrics with deployment IDs (e.g., include `X-Release-Id` header).

### 3.3 Hydration health

- Enable Angular DevTools Hydration overlay in staging to catch mismatches.
- Expose a diagnostic endpoint (`/diagnostics/hydration`) that returns hydration warnings found in logs.

## 4. Server-Side Instrumentation (SSR + APIs)

### 4.1 Structured logging

Use a JSON logger (Pino, Winston):

```ts
import pino from 'pino';

export const logger = pino({
  level: process.env.LOG_LEVEL ?? 'info',
  transport: process.env.NODE_ENV === 'development'
    ? { target: 'pino-pretty' }
    : undefined
});

// Inside server.ts
logger.info({ url, userAgent: req.headers['user-agent'] }, 'SSR request');
```

Send logs to a central system (CloudWatch, Stackdriver, ELK) with log shipping agents.

### 4.2 Metrics & tracing

- Wrap SSR handlers with OpenTelemetry to emit traces that include render time and cache hits.
- Instrument API clients used during SSR:

```ts
const tracer = trace.getTracer('ssr');
await tracer.startActiveSpan('fetch-products', async (span) => {
  const response = await productApi.list();
  span.setAttribute('count', response.length);
  span.end();
});
```

- Export metrics (Prometheus, Stackdriver) for request latency and error rate.

## 5. Android App Monitoring

- Integrate Firebase Crashlytics or Sentry for native crash reporting.
- Add performance monitoring (Firebase Performance, Datadog RUM) to track cold start, slow renders, or network issues.
- Align release builds with Angular deployments—use the same release identifiers in Crashlytics to correlate app versions and backend behavior.

## 6. Dashboards & Alerts

### 6.1 Essential dashboards

- **Frontend health:** Core Web Vitals, JS errors, hydration success rate, SSR vs CSR load distribution.
- **Backend health:** request rate, latency percentiles, SSR render time, cache hit ratio, node memory/CPU.
- **Release overview:** combine deployment events with error counts and performance metrics.
- **Android app:** crash-free user percentage, cold start time, network failure rate.

### 6.2 Alert policies

Define SLO-backed alerts. Examples:

- **Critical:** error rate > 2% for 5 minutes, p95 SSR latency > 2s, Crashlytics crash-free users < 99%.
- **Warning:** hydrate mismatch counts spike, build pipeline failing for > 1 hour.
- **Informational:** successful deployment, feature flags toggled.

Route alerts to Slack/Teams for non-critical and PagerDuty for urgent incidents. Rotate on-call coverage and document runbooks.

## 7. Deployment Integration

- Annotate dashboards with deployment events via API (Datadog events, Grafana annotations).
- Add health checks to CI/CD (e.g., smoke tests hitting `/health`, hydration diagnostics).
- Publish release notes with links to relevant dashboards to short-cut investigations.

## 8. Documentation & Onboarding

Create `docs/observability.md` summarizing:

- Logging standards (fields, correlation IDs).
- How to access dashboards and raw logs.
- Alert escalation paths and contact rotation.
- Runbooks for common incidents (hydration failure, API latency, Android crash spike).
- Links to instrumentation code (error handlers, metrics clients, tracing setup).

Keep the document live—update it after every incident review.

## 9. Continuous Improvement

1. Review alerts quarterly; retire noisy ones.
2. Run incident postmortems with action items.
3. Track MTTR (mean time to resolve) and MTTD (mean time to detect).
4. Automate synthetic tests (Lighthouse CI, Playwright) to catch regressions before users do.

## 10. Quick Checklist

- [ ] Centralized logging for SSR, APIs, and Android builds.
- [ ] Client-side crash and performance monitoring in place.
- [ ] Tracing covers SSR requests and critical API calls.
- [ ] Dashboards for frontend, backend, and mobile health are shared with the team.
- [ ] Alerts tied to clear SLOs with documented response plans.
- [ ] Observability runbooks exist and are kept up to date.

With observability in place, you can iterate on Angular features confidently, knowing every regression bubbles up quickly and every deployment is traceable.