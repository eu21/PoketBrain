# SSR & Hydration Recipes

Server-side rendering (SSR) combined with hydration delivers the “fast first paint + rich interactivity” experience modern Angular apps need. This guide walks through a realistic setup and the daily workflow to keep it running smoothly.

## 1. Enable SSR and Verify Hydration

If your project wasn’t scaffolded with `--ssr`, add it now:

```bash
ng add @angular/ssr
```

This command creates `server.ts`, updates `main.server.ts`, wires builders, and adds the `npm run dev:ssr` script.

Immediately verify the integration:

```bash
npm run dev:ssr      # Node server + client watching
npm run build:ssr    # Production build (client + server)
npm run serve:ssr    # Serve the production bundle
```

When you load the app, open the browser devtools and confirm the `Angular SSR Hydration` overlay lights up without warnings.

## 2. Optimize the Server Entrypoint

Focus on these settings inside `server.ts`:

```ts
import 'zone.js/node';
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './src/app/app.component';
import { appConfig } from './src/app/app.config';

export default async function render(url: string, document: string) {
  globalThis['navigator'] = globalThis['navigator'] || { userAgent: 'SSR' };

  return bootstrapApplication(AppComponent, appConfig).then((ref) =>
    ref.whenStable().then(() => document)
  );
}
```

- Use `bootstrapApplication` with the same providers you ship in the browser.
- Avoid global mutable singletons – SSR runs per request.
- Keep `document` returning early; clean up long-lived connections (e.g. DB, sockets) in your backend layer instead.

## 3. Route-Level Hydration Strategy

Not every route needs the same hydration story. Configure patterns using Angular’s router:

```ts
export const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    providers: [
      { provide: ISLAND_MODE, useValue: 'full' } // default full hydration
    ]
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/dashboard.component'),
    providers: [
      { provide: ISLAND_MODE, useValue: 'partial' } // isolate heavy widgets
    ]
  }
];
```

`ISLAND_MODE` is an upcoming Angular token; until it stabilizes, simulate the pattern by manually deferring heavier child components (see next section).

## 4. Defer Heavy Islands

Use the new template syntax to hydrate only when needed:

```html
@defer (on viewport) {
  <analytics-panel></analytics-panel>
} @placeholder {
  <app-skeleton title="Analytics loading…"></app-skeleton>
} @loading {
  <app-linear-spinner></app-linear-spinner>
}
```

- Prefetch data on the server if you know the user will need it.
- Switch `on viewport` to `on idle` or `on interaction` depending on when the widget becomes critical.

## 5. Server Data + Client Signals

Pre-hydrate expensive data so the client doesn’t immediately refetch what the server already delivered.

**Server guard/service example:**

```ts
@Injectable()
export class ProductsResolver {
  private readonly api = inject(ProductApi);

  resolve(): Promise<Product[]> {
    return firstValueFrom(this.api.list());
  }
}
```

**Component using signals:**

```ts
@Component({
  standalone: true,
  templateUrl: './product-list.component.html'
})
export class ProductListComponent {
  private readonly route = inject(ActivatedRoute);
  readonly products = signal<Product[]>([]);

  constructor() {
    const initial = this.route.snapshot.data['products'] as Product[];
    this.products.set(initial);

    // Refresh only after hydration to avoid double fetch
    effect(() => {
      if (!isPlatformBrowser(inject(PLATFORM_ID))) return;
      this.fetchLatest();
    });
  }

  private fetchLatest() {
    inject(ProductApi)
      .list()
      .subscribe((items) => this.products.set(items));
  }
}
```

The resolver hydrates the first render; the effect updates the list once the client is active.

## 6. Caching Strategy

### 6.1 HTTP server
If you deploy on Node:

- Use HTTP caching headers (`Cache-Control`, `ETag`) for static assets.
- Consider server-level caches (Redis/memory) for data fetches that don’t change per request.

### 6.2 Edge / serverless
When hosting on Cloudflare Workers, Vercel, or Firebase Functions:

- Store fetch results in the platform’s cache API.
- Hydrate with the cached data to avoid hitting your origin on each request.
- Keep per-user data server-side and inject via cookies or HTTP headers to avoid caching issues.

## 7. Analytics and Third-Party Scripts

Inject third-party scripts only after hydration:

```ts
effect(() => {
  const doc = inject(DOCUMENT);
  const isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  if (!isBrowser) return;

  const script = doc.createElement('script');
  script.src = 'https://example.com/analytics.js';
  doc.body.appendChild(script);
});
```

SSR won’t attempt to fetch this script, but the hydrated client will.

## 8. Development Workflow

1. **Run both environments locally**  
   - `npm run dev:ssr` for full-stack development.
   - `npm run dev` for quick client-only prototyping, then port changes back.

2. **Check hydration markers**  
   - The Angular DevTools Hydration tab surfaces any mismatched DOM.

3. **Write smoke tests**  
   - Use `npm run build:ssr && npm run serve:ssr` in CI to catch hydration regressions.
   - Optional: add Playwright/Cypress tests hitting the SSR server to mimic user flows.

## 9. Deployment Checklist

| Step | Action | Notes |
| ---- | ------ | ----- |
| Build | `npm run build:ssr` | Outputs `browser/` + `server/`. |
| Package | Zip `dist/angular-primer/browser` for static hosting; deploy `dist/angular-primer/server` to Node or serverless runtime. |
| Environment variables | Inject API URLs via `process.env` on the server. Mirror them for the client using `environment.ts`. |
| Monitoring | Set up logging for SSR render failures; capture client hydration mismatches. |
| Rollback plan | Keep the previous deployment artifacts handy. SSR failures need quick recovery. |

## 10. Daily Maintenance Tips

- Run Lighthouse against the SSR server to validate performance metrics.
- Track server memory usage—each concurrent request bootstraps Angular, so plan resources accordingly.
- Automate dependency updates (Dependabot/Renovate). Angular’s SSR packages should stay aligned with the framework version.

## Conclusion

SSR + hydration is not an optional extra in the Angular roadmap—it’s a baseline expectation for fast, user-friendly apps. By combining route-aware hydration, defer directives, and signal-driven state, you get a setup that’s both performant and straightforward for your team to maintain.
