# Admin UI (Flowable Process Management)

**React-based Admin Dashboard for IT/Operations Teams**

## Overview

This SPA provides administrative tooling for managing BPMN workflows, DMN decision tables, and process monitoring. It is designed for IT administrators and DevOps personnel who need to deploy, monitor, and troubleshoot business processes.

## Technology Stack

- **Framework:** React 18
- **UI Library:** Ant Design
- **Styling:** TailwindCSS
- **Charts:** Recharts
- **State:** React Hooks

## Features

| Feature | Description |
|---------|-------------|
| **Operations Dashboard** | Process metrics and system health |
| **Process Monitor** | Real-time process instance tracking |
| **Deployment Center** | BPMN deployment and version management |
| **Version Diff** | Compare BPMN versions visually |
| **DMN Management** | Edit and test decision tables |
| **Analytics Dashboard** | Deep workflow analytics |
| **Service Catalog** | API integration management |

## Getting Started

```bash
# Install dependencies
npm install

# Start development server
npm start
```

Open http://localhost:3000 to view the admin dashboard.

## Build for Production

```bash
npm run build
```

Output will be in the `build/` directory.

## Related

- [Enterprise Frontend](../frontend/README.md) - Angular SPA for Business Users
- [SPA Architecture](../docs/SPA_ARCHITECTURE.md) - Architecture documentation
