
# Signals In Depth: Patterns for Real Projects

Signals reshape how Angular components react to state, replacing ad-hoc observable plumbing with deterministic, declarative data flow. This guide dives into practical patterns you can apply immediately.

## 1. State Construction Patterns

### 1.1 Local component state
Use signals for anything that feeds the template directly. Prefer methods over inline state mutations to keep intent clear.

```ts
@Component({
  standalone: true,
  template: `
    <button (click)="increment()">Clicked {{ count() }} times</button>
  `
})
export class CounterComponent {
  readonly count = signal(0);

  increment(): void {
    this.count.update((value) => value + 1);
  }
}
```

### 1.2 Derived state with `computed`
Use `computed` for values that can be expressed as pure transformations.

```ts
readonly todos = signal<Todo[]>([]);
readonly incompleteCount = computed(
  () => this.todos().filter((todo) => !todo.completed).length
);
```

### 1.3 Managed state with `writeSignal`
When you need to expose a read-only signal but still mutate internally, wrap the writable handle:

```ts
private readonly _selectedId = signal<number | null>(null);
readonly selectedId = this._selectedId.asReadonly();

select(id: number): void {
  this._selectedId.set(id);
}
```

## 2. Bridge RxJS ↔ Signals

### 2.1 Observables to signals

```ts
@Injectable({ providedIn: 'root' })
export class UserStore {
  private readonly api = inject(UserApi);

  readonly users = toSignal(this.api.getUsers(), {
    initialValue: []
  });

  // Derived state stays reactive
  readonly adminUsers = computed(() =>
    this.users().filter((user) => user.role === 'admin')
  );
}
```

### 2.2 Signals to observables

When you must expose a signal back to an observable API:

```ts
const selected$ = toObservable(this.selectedId);
selected$.pipe(/* operators */).subscribe();
```

Avoid this unless an external library demands it; prefer keeping state in signal form.

## 3. Effects: Practical Techniques

### 3.1 Triggering side effects

```ts
@Component({ /* … */ })
export class CartSummaryComponent {
  private readonly analytics = inject(AnalyticsService);
  readonly cartTotal = computed(() => calculateTotal(this.items()));

  constructor() {
    effect(() => {
      const total = this.cartTotal();
      this.analytics.track('cart_total_changed', { total });
    });
  }
}
```

### 3.2 Cleanup with `onCleanup`

```ts
effect((onCleanup) => {
  const sub = this.socket
    .listenTo(this.activeRoom())
    .subscribe(this.messages.set);

  onCleanup(() => sub.unsubscribe());
});
```

### 3.3 Effect scheduling
Every `effect` runs synchronously after dependencies change. If you want to throttle or queue work, move the heavy operation into an async service. Do not call `await` inside `effect`.

## 4. Form Handling Patterns

### 4.1 Template-driven binding with `toSignal`

```ts
const control = new FormControl('');
const name = toSignal(control.valueChanges, { initialValue: control.value });

const greeting = computed(() => `Hello ${name().trim() || 'stranger'}!`);
```

### 4.2 Signal-driven reactive forms
Keep form state in signals and push it into the `FormGroup` only when necessary:

```ts
readonly profile = signal<UserProfile>({ name: '', about: '' });
readonly form = new FormGroup({
  name: new FormControl(this.profile().name),
  about: new FormControl(this.profile().about)
});

effect(() => {
  const profile = this.profile();
  this.form.patchValue(profile, { emitEvent: false });
});
```

## 5. Shelving Zone.js for Signals

When you opt into zoneless change detection, only signal updates trigger re-rendering. That means:

- Use signals (or `setTimeout` and then update signals) for asynchronous updates.
- Call `markDirty` manually only when bridging with code that mutates state without using signals (rare in modern apps).

Zoneless bootstrap reminder:

```ts
bootstrapApplication(AppComponent, {
  providers: [provideExperimentalZonelessChangeDetection()]
});
```

## 6. Debugging and DevTools

1. Install the **Angular DevTools** browser extension.
2. Use the **Signals** panel to inspect dependencies and effect runs.
3. Leverage console helpers:
   ```ts
   import { debugSignal } from '@angular/core/primitives/signals';

   const state = signal(42);
   debugSignal(state); // logs mutations and dependents
   ```

4. For SSR + hydration debugging, inspect the hydration overlay for signal mismatches.

## 7. Common Pitfalls & Guardrails

| Pitfall | Strategy |
| ------- | -------- |
| Mixing mutable objects with signals | Treat signal values as immutable, update with new copies (`{ ...prev, foo: 'bar' }`). |
| Overusing `effect` for computed data | Use `computed` whenever the output is a pure function of other signals; reserve `effect` for side effects. |
| Forgetting to tear down RxJS subscriptions | Always call `onCleanup` inside effects or use `takeUntilDestroyed`. |
| Updating signals in loops without batching | Consolidate updates in a single `update()` call to avoid redundant computations. |

## 8. Checklist Before Shipping

- [ ] Every template-facing datum is a signal or computed value.
- [ ] No lingering `Subject` just to bridge template state.
- [ ] Effects are pure (no API orchestration or async inside).
- [ ] `toSignal` usage has a reliable initial value.
- [ ] DevTools shows stable dependency graphs (no runaway effects).

With these patterns in place, your application is ready for Angular’s signal-first future while remaining approachable for new developers.
