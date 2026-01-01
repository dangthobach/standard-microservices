# CRM Angular Frontend

Modern Angular application with Material Design for Customer Relationship Management system.

## Features

- ✅ **Angular 21** with standalone components
- ✅ **Material Design** - Beautiful UI components
- ✅ **Dark/Light Theme** - Toggle between themes with persistence
- ✅ **Responsive Design** - Mobile-first approach
- ✅ **Reusable Components** - Data table, search filter, layout
- ✅ **TypeScript Signals** - Modern reactive state management
- ✅ **OAuth2 PKCE** - Secure authentication with Keycloak
- ✅ **Lazy Loading** - Optimized performance

## Tech Stack

- **Framework**: Angular 21
- **UI Library**: Angular Material 21
- **State Management**: TypeScript Signals + NgRx
- **Authentication**: angular-oauth2-oidc
- **Build Tool**: Angular CLI with Vite
- **Language**: TypeScript 5.7

## Prerequisites

**IMPORTANT**: You need Node.js version 20 or 22 (LTS versions). Version 21 is NOT supported.

```bash
# Check your Node version
node --version

# Should be v20.x.x or v22.x.x
```

If you have Node v21, please upgrade to v22 (latest LTS):
- Download from: https://nodejs.org/
- Or use nvm: `nvm install 22 && nvm use 22`

## Installation

```bash
# Install dependencies
npm install --legacy-peer-deps

# Or if you prefer
npm ci --legacy-peer-deps
```

## Development

```bash
# Start development server
npm start

# Application will be available at http://localhost:4200
```

## Build

```bash
# Production build
npm run build

# Output will be in dist/enterprise-frontend
```

## Project Structure

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/                    # Core module (guards, interceptors, services)
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   └── services/
│   │   │
│   │   ├── shared/                  # Shared module (reusable components)
│   │   │   ├── components/
│   │   │   │   ├── layout/          # Layout component
│   │   │   │   ├── header/          # Header component
│   │   │   │   ├── sidebar/         # Sidebar component
│   │   │   │   ├── data-table/      # Reusable data table
│   │   │   │   └── search-filter/   # Search & filter component
│   │   │   ├── services/
│   │   │   │   └── theme.service.ts # Dark/Light theme service
│   │   │   ├── models/
│   │   │   ├── directives/
│   │   │   └── pipes/
│   │   │
│   │   ├── features/                # Feature modules
│   │   │   ├── auth/                # Authentication
│   │   │   ├── dashboard/           # Dashboard
│   │   │   ├── customers/           # CRM Customers module ⭐
│   │   │   ├── users/               # User management
│   │   │   ├── organizations/       # Organization management
│   │   │   └── settings/            # Settings
│   │   │
│   │   ├── app.component.ts         # Root component
│   │   ├── app.config.ts            # App configuration
│   │   └── app.routes.ts            # Route configuration
│   │
│   ├── environments/                # Environment configs
│   ├── assets/                      # Static assets
│   └── styles.css                   # Global styles with theme variables
│
├── angular.json                     # Angular CLI config
├── package.json                     # Dependencies
└── tsconfig.json                    # TypeScript config
```

## Key Components

### 1. Layout System

- **LayoutComponent**: Main layout wrapper
- **HeaderComponent**: Top navigation with theme toggle and user menu
- **SidebarComponent**: Collapsible sidebar navigation

### 2. CRM Customer Module

Located in `src/app/features/customers/`

- **CustomersComponent**: Main customer list page
- **CustomersService**: Data service with mock data
- **Customer Model**: TypeScript interfaces

Features:
- Advanced data table with sorting and pagination
- Search and filter functionality
- Status badges (Enabled/Disabled)
- Action buttons (Edit/Delete)
- Status history section

### 3. Reusable Components

#### Data Table Component
- Sortable columns
- Pagination
- Custom column types (text, date, badge, actions)
- Loading state
- No data state
- Responsive design

#### Search Filter Component
- Search input with debounce
- Filter chips (Enabled, Disabled, Created Today, Last 7 Days)
- Clear all filters
- Filter state management

### 4. Theme Service

Dark/Light mode with:
- LocalStorage persistence
- System preference detection
- Smooth transitions
- CSS variable-based theming

## Usage Examples

### Using Data Table

```typescript
import { DataTableComponent, TableColumn, TableAction } from '@shared/components/data-table';

columns: TableColumn[] = [
  { key: 'username', label: 'Username', sortable: true, type: 'text' },
  { key: 'enabled', label: 'Status', sortable: true, type: 'badge' },
  { key: 'createdAt', label: 'Created', sortable: true, type: 'date' }
];

actions: TableAction[] = [
  { icon: 'edit', label: 'Edit', callback: (row) => this.onEdit(row) }
];
```

```html
<app-data-table
  [data]="customers()"
  [columns]="columns"
  [actions]="actions"
  [totalCount]="totalCount()"
  (pageChange)="onPageChange($event)"
  (sortChange)="onSortChange($event)">
</app-data-table>
```

### Using Theme Service

```typescript
import { ThemeService } from '@shared/services/theme.service';

constructor(public themeService: ThemeService) {}

// Toggle theme
toggleTheme() {
  this.themeService.toggleTheme();
}

// Set specific theme
setDarkMode() {
  this.themeService.setTheme('dark');
}

// Check current theme
isDark = this.themeService.isDarkMode();
```

## Styling

Global styles use CSS custom properties for theming:

```css
:root {
  --primary-color: #1976d2;
  --bg-primary: #ffffff;
  --text-primary: rgba(0, 0, 0, 0.87);
}

.dark-theme {
  --bg-primary: #1e1e1e;
  --text-primary: rgba(255, 255, 255, 0.87);
}
```

## Responsive Breakpoints

- Mobile: `max-width: 480px`
- Tablet: `max-width: 768px`
- Desktop: `min-width: 769px`

## Mock Data

Customer data is provided by `CustomersService` with 15 mock records for testing.

To connect to real API:
1. Update `environment.ts` with API URL
2. Create HTTP service in `customers.service.ts`
3. Replace mock data with HTTP calls

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Performance

- Lazy loaded routes
- OnPush change detection where applicable
- Tree-shakable providers
- Production build with optimization

## Future Enhancements

- [ ] Add customer detail view
- [ ] Implement customer create/edit forms
- [ ] Add real-time updates with WebSocket
- [ ] Export to CSV/Excel functionality
- [ ] Advanced filtering with date range picker
- [ ] Batch operations
- [ ] Customer activity timeline

## Troubleshooting

### Node version error
**Error**: `Node.js version v21.5.0 detected. Odd numbered versions not supported`

**Solution**: Install Node.js v20 or v22 (LTS versions)

### Build errors
Try clearing cache and reinstalling:
```bash
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

### Material icons not showing
Ensure you have Material Icons in `index.html`:
```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```

## Contributing

1. Follow Angular style guide
2. Use TypeScript strict mode
3. Write meaningful commit messages
4. Test on mobile and desktop

## License

MIT License - Enterprise Microservices Platform
