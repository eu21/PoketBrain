# Routing Patterns That Scale

Angular’s standalone router plus the new template control flow makes multi-route applications easier to reason about than ever. This guide shows how to structure routes, protect them, and keep bundle sizes lean.

## 1. Route Configuration Baseline

Create a dedicated `app.routes.ts`:

```ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/home/home.page').then((m) => m.HomePage)
  },
  {
    path: 'docs',
    loadChildren: () =>
      import('./features/docs/docs.routes').then((m) => m.DOCS_ROUTES)
  },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () =>
      import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },
  {
    path: '**',
    loadComponent: () =>
      import('./shared/not-found/not-found.page').then((m) => m.NotFoundPage)
  }
];
```

- Prefer `loadChildren` for feature areas with multiple screens.
- Keep the root file focused on high-level route ownership only.

## 2. Feature Route Modules (Without Modules)

Example: `features/docs/docs.routes.ts`

```ts
import { Routes } from '@angular/router';

export const DOCS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./overview/docs-overview.page').then((m) => m.DocsOverviewPage),
    providers: [provideDocsBreadcrumbs()]
  },
  {
    path: ':slug',
    loadComponent: () =>
      import('./detail/docs-detail.page').then((m) => m.DocsDetailPage),
    resolve: { doc: docResolver }
  }
];
```

- Encapsulate per-feature providers (breadcrumbs, resolvers) here.
- Avoid re-exporting constant arrays from multiple files; one `ROUTES` file per feature keeps things predictable.

## 3. Functional Guards and Resolvers

### 3.1 Auth guard example

```ts
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthStore } from './auth.store';

export const adminGuard = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/auth/login'], {
    queryParams: { redirect: '/admin' }
  });
};
```

- Functional guards are tree-shakable.
- Prefer returning `UrlTree` instead of `false` to ensure navigation fallback.

### 3.2 Data resolver with signals

```ts
import { inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DocsApi } from '../api/docs.api';

export const docResolver = async (route: ActivatedRouteSnapshot) => {
  const slug = route.paramMap.get('slug') ?? '';
  const api = inject(DocsApi);

  return firstValueFrom(api.getDocument(slug));
};
```

- Resolvers hydrate SSR pages and keep the client from flashing empty states.

## 4. Template-Level Navigation Enhancements

### 4.1 Highlight active links

```html
<nav class="sidebar">
  <a
    routerLink="/docs"
    routerLinkActive="active"
    [routerLinkActiveOptions]="{ exact: true }"
    >Docs</a
  >
  <a routerLink="/admin" routerLinkActive="active">Admin</a>
</nav>
```

- `routerLinkActiveOptions="{ exact: true }"` avoids partial matches on nested routes.

### 4.2 Use `@if` for contextual actions

```html
@inject AuthStore as auth

<nav class="top-nav">
  @if (auth.isAuthenticated()) {
    <button (click)="auth.logout()">Sign out</button>
  } @else {
    <a routerLink="/auth/login">Sign in</a>
  }
</nav>
```

- Inject stores directly into templates for simple conditional UI.

## 5. Preloading & Performance

### 5.1 Smart preloading strategy

```ts
import { PreloadingStrategy, Route } from '@angular/router';
import { Observable, of } from 'rxjs';

export class OnIdlePreloadStrategy implements PreloadingStrategy {
  preload(route: Route, load: () => Observable<unknown>): Observable<unknown> {
    if (route.data?.['preload'] === false) {
      return of(null);
    }
    return load();
  }
}
```

Register it:

```ts
bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes, withPreloading(OnIdlePreloadStrategy)),
    // …
  ]
});
```

Add `data: { preload: false }` on routes you want to keep strictly lazy.

### 5.2 Combine with `@defer`

Within a page, lazy-hydrate expensive components:

```html
@defer (on idle) {
  <app-heavy-metrics [resourceId]="id()"></app-heavy-metrics>
} @placeholder {
  <app-skeleton title="Analytics loading…"></app-skeleton>
}
```

## 6. URL Structure & SEO Tips

- Use semantic paths (`/docs`, `/pricing`, `/blog/:slug`) instead of technical ones.
- Always provide a catch-all case for unknown routes.
- If SSR is enabled, ensure meta tags and canonical URLs are supplied per route using `Title` and `Meta` services in resolvers or route providers.

Example:

```ts
export const provideDocsBreadcrumbs = () => [
  {
    provide: ResolveEndListener,
    useFactory: () => {
      const title = inject(Title);
      const meta = inject(Meta);
      return (event: ResolveEnd) => {
        title.setTitle(`Docs • ${event.state.root.firstChild?.data['title'] ?? 'Angular Primer'}`);
        meta.updateTag({ name: 'description', content: 'Angular docs and tutorials' });
      };
    }
  }
];
```

## 7. Router Debugging Playbook

1. Temporarily enable tracing:
   ```ts
   provideRouter(routing, withDebugTracing())
   ```
2. Inspect `Router` events via `router.events.subscribe(console.log)` during development.
3. Use Angular DevTools “Router Trees” tab to view the current and predicted state.

## 8. Checklist Before Shipping

- [ ] All routes lazy-load their feature areas (except tiny modules needed on first paint).
- [ ] Guards/resolvers return `UrlTree` or data, never long-running Promises without timeouts.
- [ ] Navigation handles unknown paths gracefully (custom 404).
- [ ] There is a clear strategy for preloading only critical routes.
- [ ] Route-specific providers don’t leak to unrelated features.

Once these routing patterns are in place, onboarding new Angular developers becomes far easier—they can see the entire structure, trust the navigation logic, and extend features without worrying about unintended coupling.
