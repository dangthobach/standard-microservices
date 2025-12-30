# Angular 21 + Material Setup Guide

## Architecture: Feature-First + Standalone Components

### Project Structure Created

```
src/app/
├── core/                    # Core services & guards (singleton)
│   ├── layout/             # Main layout components
│   ├── guards/             # Auth & role guards
│   ├── interceptors/       # HTTP interceptors
│   └── services/           # Core services
│
├── shared/                  # Shared resources
│   ├── components/         # Reusable standalone components
│   ├── directives/         # Custom directives
│   ├── pipes/              # Custom pipes
│   ├── models/             # TypeScript interfaces
│   └── utils/              # Helper functions
│
└── features/                # Feature modules (lazy loaded)
    ├── auth/               # Authentication
    ├── dashboard/          # Dashboard
    ├── users/              # User management
    ├── organizations/      # Organization management
    └── settings/           # Settings
```

### Dependencies Installed

**Core**:
- @angular/core: 21.0.0
- @angular/router: 21.0.0

**UI**:
- @angular/material: 21.0.0
- @angular/cdk: 21.0.0

**State Management**:
- @ngrx/store: 18.1.1
- @ngrx/effects: 18.1.1
- @ngrx/store-devtools: 18.1.1

### Quick Start

```bash
cd frontend
npm install
npm start
```

### Next Steps

1. Run `npm install` to install dependencies
2. Create standalone components in features/
3. Configure Material theme in styles/theme.scss
4. Implement authentication flow
5. Build feature modules

### Example: Standalone Component

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule, MatButtonModule],
  template: `<button mat-raised-button>Click</button>`
})
export class ExampleComponent {}
```

### Lazy Loading Example

```typescript
// app.routes.ts
{
  path: 'users',
  loadChildren: () => import('./features/users/users.routes')
    .then(m => m.USER_ROUTES)
}
```

---

**Status**: ✅ Structure Created
**Ready for**: Development
