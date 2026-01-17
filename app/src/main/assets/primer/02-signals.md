# Signal-Based Reactivity Crash Course

Signals arrived in Angular 16 and are poised to become the preferred way to model component state in Angular 19+. They offer deterministic reactivity, fine-grained change propagation, and better performance without juggling subscriptions manually.

## Why Signals Matter

- **Predictable rendering**: Change detection only touches the components that depend on the changed signal.
- **Composable stores**: Signals can be derived (`computed`) and react to updates via lightweight `effect`s.
- **Interop friendly**: Seamless bridging with RxJS observables and existing forms APIs.

## Core Building Blocks

```ts
import { signal, computed, effect } from '@angular/core';

const count = signal(0);
const doubled = computed(() => count() * 2);

effect(() => {
  console.log('Count changed to', count());
});
```