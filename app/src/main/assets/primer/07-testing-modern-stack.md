# Testing the Modern Angular Stack

Angular’s signal-first architecture and standalone APIs pair beautifully with a modern testing toolchain. This guide walks through unit, component, and end-to-end testing using Vitest and Playwright, with practical patterns that integrate cleanly with your existing workflow.

## 1. Test Philosophy for Signals + Standalone

- **Test behavior, not implementation.** Focus on observable outputs (DOM, emitted events, state changes) to keep refactors safe.
- **Prefer synchronous tests.** Signals update synchronously by design—no need for `fakeAsync` or ticking the clock in most cases.
- **Thin wiring, thick render.** Standalone components can be tested as first-class citizens without TestBed modules.

## 2. Tooling Setup

### 2.1 Replace Karma/Jasmine with Vitest

1. Install dependencies:

   ```bash
   npm install --save-dev vitest @vitest/ui jsdom @angular-devkit/build-angular@latest
   ```

2. Create `vitest.config.ts`:

   ```ts
   import { defineConfig } from 'vitest/config';
   import angular from '@analogjs/vite-plugin-angular'; // lightweight Angular integration

   export default defineConfig({
     plugins: [angular()],
     test: {
       environment: 'jsdom',
       globals: true,
       setupFiles: ['./vitest.setup.ts'],
       include: ['src/**/*.spec.ts']
     }
   });
   ```

3. Add `vitest.setup.ts` with the Angular testing shim:

   ```ts
   import '@angular/compiler';
   ```

4. Update `package.json` scripts:

   ```json
   {
     "scripts": {
       "test": "vitest run",
       "test:watch": "vitest",
       "test:ui": "vitest --ui"
     }
   }
   ```

### 2.2 Component Harnesses without Modules

Use Angular’s `ComponentFixture` helper via `TestBed` while still benefiting from standalone declarations:

```ts
beforeEach(async () => {
  await TestBed.configureTestingModule({
    imports: [CounterComponent]
  }).compileComponents();
});
```

No `declarations` required; `imports` handles standalone components.

## 3. Testing Signals in Components

### 3.1 Simple component example

```ts
it('increments the counter when clicked', () => {
  const fixture = TestBed.createComponent(CounterComponent);
  fixture.detectChanges();

  const button = fixture.nativeElement.querySelector('button');
  button.click();
  fixture.detectChanges();

  expect(button.textContent).toContain('Clicked 1 times');
});
```

Because signals update synchronously, you only need `fixture.detectChanges()` to reflect the new state.

### 3.2 Testing derived signals

```ts
@Component({
  standalone: true,
  template: `<span>{{ summary() }}</span>`
})
class SummaryComponent {
  readonly count = signal(0);
  readonly summary = computed(() => `Total: ${this.count()}`);
}
```

Test:

```ts
it('updates summary when count changes', () => {
  const fixture = TestBed.createComponent(SummaryComponent);
  fixture.componentInstance.count.set(5);
  fixture.detectChanges();

  expect(fixture.nativeElement.textContent.trim()).toBe('Total: 5');
});
```

## 4. Mocking Dependencies

Use Angular’s `provideHttpClientTesting` or manual providers:

```ts
beforeEach(async () => {
  await TestBed.configureTestingModule({
    imports: [UserListComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting()
    ]
  }).compileComponents();
});
```

Spy example for a service:

```ts
const userService = jasmine.createSpyObj('UserService', ['load']);
userService.load.and.returnValue(of([{ id: 1, name: 'Alice' }]));

await TestBed.configureTestingModule({
  imports: [UserListComponent],
  providers: [{ provide: UserService, useValue: userService }]
}).compileComponents();
```

Under Vitest, replace `jasmine.createSpyObj` with `vi.fn()` and manual objects.

## 5. Testing Effects and Async Behavior

- Signals run synchronously, but async data still requires waiting for observables/promises.
- Use `firstValueFrom` in production code, but in tests rely on `fakeAsync` or manual `await` depending on the API.

Example with RxJS:

```ts
it('loads data on init', fakeAsync(() => {
  const fixture = TestBed.createComponent(DashboardComponent);
  fixture.detectChanges();

  tick(); // flush async data
  fixture.detectChanges();

  expect(fixture.nativeElement.textContent).toContain('Loaded');
}));
```

If you avoid `fakeAsync`, simply `await fixture.whenStable()` when using Promises.

## 6. Component Harnesses for Material Design

Material 3 components still provide harnesses:

```ts
import { MatButtonHarness } from '@angular/material/button/testing';

it('disables submit while saving', async () => {
  const fixture = TestBed.createComponent(ProfileFormComponent);
  fixture.detectChanges();

  const loader = TestbedHarnessEnvironment.loader(fixture);
  const button = await loader.getHarness(MatButtonHarness.with({ text: /save/i }));

  await button.click();
  expect(await button.isDisabled()).toBe(true);
});
```

Harnesses keep your tests resilient to internal DOM changes.

## 7. End-to-End with Playwright

Add Playwright for reliable browser automation:

```bash
npx playwright install
npm install --save-dev @playwright/test
```

Create `playwright.config.ts`:

```ts
import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: 'http://localhost:4200',
    headless: true
  },
  webServer: {
    command: 'npm run dev:ssr',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env.CI
  }
});
```

Sample test in `e2e/app.spec.ts`:

```ts
import { test, expect } from '@playwright/test';

test('navigates to docs list', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Docs' }).click();
  await expect(page).toHaveURL(/\/docs/);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Documentation');
});
```

## 8. CI Integration

Add to your GitHub Actions (from the earlier guide):

```yaml
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
      - run: npx playwright install --with-deps
      - run: npx playwright test
      - run: npm run build:ssr
```

## 9. Troubleshooting Checklist

| Symptom | Solution |
| ------- | -------- |
| Tests fail only in CI | Ensure deterministic data; disable flaky network calls; use `fakeAsync` or mocked timers. |
| Signal doesn’t update DOM | Missing `fixture.detectChanges()` or template not bound to the signal. |
| Hydration-specific bugs | Run E2E tests against the SSR server (`dev:ssr`) and verify hydration markers. |
| Playwright timeouts | Increase `expect` timeout or await the Network idle state before assertions. |

## 10. Daily Testing Routine

1. Run `npm run test:watch` during development.
2. Trigger Playwright smoke tests before merging feature branches.
3. Keep coverage meaningful—assert critical user flows rather than chasing arbitrary percentages.
4. Document any non-obvious testing utilities in `/docs/testing.md` so teammates have a reference.

A modern, signals-aware test suite keeps refactors safe and lets new developers trust their changes. Combine this stack with the routing, SSR, and workflow guides earlier in the series for a cohesive engineering baseline.
