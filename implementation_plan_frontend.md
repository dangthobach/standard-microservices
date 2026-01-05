# Implementation Plan: Product API & Frontend Integration

## Goal
Expose Product APIs in `business-service` and integrate them into the existing Frontend application.

## 1. Backend Implementation (`business-service`)
The `ProductService` exists, but the Controller is missing.
- **Create `ProductController`**:
    - `GET /api/business/products`: List products (Paginated).
    - `GET /api/business/products/{id}`: Get detail.
    - `POST /api/business/products`: Create (Admin/Manager).
    - `PUT /api/business/products/{id}`: Update.
    - `DELETE /api/business/products/{id}`: Delete.

## 2. Frontend Implementation (`frontend` directory)
Assuming Angular (based on project history).
- **Verify Project Structure**: Check `package.json`.
- **Create Model**: `src/app/models/product.model.ts`.
- **Create Service**: `src/app/services/product.service.ts`.
- **Create Component**: `src/app/features/product-list/product-list.component.ts`.
- **Update Routing**: Add route `/products`.

## Verification
- Run Backend.
- Run Frontend.
- Verify data loading.
