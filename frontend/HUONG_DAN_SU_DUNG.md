# Hướng Dẫn Sử Dụng CRM Angular UI

## 🎯 Tổng Quan

Ứng dụng CRM (Customer Relationship Management) được xây dựng với Angular 21 và Material Design, cung cấp giao diện hiện đại, responsive và dễ sử dụng để quản lý khách hàng.

## 📦 Yêu Cầu Hệ Thống

### ⚠️ QUAN TRỌNG: Phiên bản Node.js

**BẮT BUỘC**: Node.js phiên bản 20.x.x hoặc 22.x.x (LTS)

```bash
# Kiểm tra phiên bản Node hiện tại
node --version

# Kết quả phải là v20.x.x hoặc v22.x.x
# VÍ DỤ: v20.11.0 hoặc v22.0.0
```

**Nếu bạn đang dùng Node v21.x.x:**
1. Tải Node.js LTS từ: https://nodejs.org/
2. Hoặc dùng NVM: `nvm install 22 && nvm use 22`

### Công nghệ sử dụng
- Angular 21
- Angular Material 21
- TypeScript 5.7
- RxJS 7.8
- NgRx 19 (State Management)

## 🚀 Cài Đặt

### Bước 1: Cài đặt dependencies

```bash
# Di chuyển vào thư mục frontend
cd frontend

# Cài đặt packages (PHẢI dùng --legacy-peer-deps)
npm install --legacy-peer-deps
```

### Bước 2: Chạy development server

```bash
# Khởi động server
npm start

# Hoặc
npm run start
```

Ứng dụng sẽ chạy tại: **http://localhost:4200**

### Bước 3: Build cho production

```bash
# Build ứng dụng
npm run build

# File build sẽ nằm trong thư mục: dist/enterprise-frontend
```

## 📱 Tính Năng Chính

### 1. Giao Diện Responsive
- ✅ Tự động điều chỉnh theo kích thước màn hình
- ✅ Hỗ trợ mobile, tablet, desktop
- ✅ Menu collapse trên mobile
- ✅ Touch-friendly cho màn hình cảm ứng

### 2. Dark/Light Theme (Chế độ Sáng/Tối)
- ✅ Chuyển đổi dễ dàng giữa 2 chế độ
- ✅ Lưu preferences vào LocalStorage
- ✅ Tự động áp dụng theme khi mở lại
- ✅ Nút toggle ở header

**Cách dùng:**
- Click icon mặt trời/mặt trăng ở góc phải header
- Theme sẽ được lưu tự động

### 3. Header (Thanh Điều Hướng Trên)
- Logo/Tên ứng dụng
- Nút menu (mobile)
- Nút chuyển theme
- Menu người dùng với:
  - Profile
  - Settings
  - Logout

### 4. Sidebar (Menu Bên Trái)
- Dashboard
- Customers (Khách hàng) ⭐
- Organizations
- Users
- Settings

**Tính năng:**
- Collapse/Expand
- Active menu highlighting
- Badge thông báo
- Icon material design

### 5. Trang Customers (Quản Lý Khách Hàng)

#### Tìm Kiếm & Lọc
- **Search box**: Tìm theo username hoặc email
- **Filter chips**:
  - Enabled: Chỉ hiện khách hàng đang active
  - Disabled: Chỉ hiện khách hàng bị vô hiệu hóa
  - Created Today: Khách hàng tạo hôm nay
  - Last 7 Days: Khách hàng tạo 7 ngày gần đây

#### Bảng Dữ Liệu (Data Table)
**Columns (Cột):**
- Username
- Email
- Enabled (Status badge màu xanh/đỏ)
- Created At (Ngày tạo)
- Updated At (Ngày cập nhật)

**Tính năng:**
- ✅ Sắp xếp (click vào header cột)
- ✅ Phân trang (10/25/50 items per page)
- ✅ Action buttons (Edit/Delete)
- ✅ Loading state
- ✅ Empty state khi không có data
- ✅ Responsive scrolling

#### Status History (Lịch Sử Trạng Thái)
Hiển thị lịch sử thay đổi của khách hàng:
- Date/Time
- User thực hiện
- Action
- Details

## 🎨 Thiết Kế & UI/UX

### Màu Sắc
- **Primary**: Blue (#1976d2)
- **Accent**: Pink (#ff4081)
- **Success**: Green (#4caf50)
- **Warning**: Red (#f44336)

### Font
- Roboto (Material Design standard)
- Size: 14px (body), 16px-28px (headings)

### Spacing
- Consistent padding: 8px, 16px, 24px
- Card spacing: 16px
- Section margins: 24px, 32px

## 📊 Mock Data (Dữ Liệu Mẫu)

Ứng dụng có sẵn **15 customer records** để test:
- john.doe@example.com
- jane.smith@example.com
- mike.brown@example.com
- sarah.wilson@example.com
- david.lee@example.com
- emily.clark@example.com
- robert.taylor@example.com
- ... và nhiều hơn

**Bạn có thể:**
- Test search với các tên trên
- Test filter Enabled/Disabled
- Test pagination
- Test sorting

## 🔧 Sử Dụng Components

### 1. Data Table Component

```html
<app-data-table
  [data]="customers()"
  [columns]="columns"
  [actions]="actions"
  [totalCount]="totalCount()"
  [pageSize]="10"
  (pageChange)="onPageChange($event)"
  (sortChange)="onSortChange($event)">
</app-data-table>
```

### 2. Search Filter Component

```html
<app-search-filter
  (filterChange)="onFilterChange($event)">
</app-search-filter>
```

### 3. Theme Service

```typescript
import { ThemeService } from '@shared/services/theme.service';

constructor(public themeService: ThemeService) {}

// Toggle theme
this.themeService.toggleTheme();

// Set specific theme
this.themeService.setTheme('dark');

// Check current theme
const isDark = this.themeService.isDarkMode();
```

## 📱 Responsive Breakpoints

```css
/* Mobile */
@media (max-width: 480px) { ... }

/* Tablet */
@media (max-width: 768px) { ... }

/* Desktop */
@media (min-width: 769px) { ... }
```

## 🗂️ Cấu Trúc Thư Mục

```
frontend/src/app/
├── core/                    # Services, guards, interceptors
├── shared/                  # Shared components
│   ├── components/
│   │   ├── layout/         # Layout wrapper
│   │   ├── header/         # Header navigation
│   │   ├── sidebar/        # Sidebar menu
│   │   ├── data-table/     # Reusable table
│   │   └── search-filter/  # Search & filter
│   ├── services/
│   │   └── theme.service.ts
│   └── models/
│       └── customer.model.ts
└── features/
    └── customers/          # Customer management ⭐
        ├── customers.component.ts
        ├── customers.component.html
        ├── customers.component.css
        └── customers.service.ts
```

## 🐛 Xử Lý Lỗi Thường Gặp

### Lỗi 1: Node version không hỗ trợ
```
Error: Node.js version v21.5.0 detected.
Odd numbered versions not supported
```

**Giải pháp:**
```bash
# Cài Node v20 hoặc v22
# Download từ: https://nodejs.org/
# Hoặc dùng nvm:
nvm install 22
nvm use 22
```

### Lỗi 2: Build thất bại
```bash
# Xóa cache và cài lại
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

### Lỗi 3: Material icons không hiển thị
**Kiểm tra file `index.html`:**
```html
<link href="https://fonts.googleapis.com/icon?family=Material+Icons" rel="stylesheet">
```

### Lỗi 4: Theme không lưu
- Kiểm tra LocalStorage của browser
- Clear browser cache
- Thử chế độ ẩn danh

## 🎯 Testing Checklist

### Test Responsive
- [ ] Mở trên điện thoại (hoặc DevTools mobile view)
- [ ] Test menu collapse
- [ ] Test table scrolling
- [ ] Test search box

### Test Theme
- [ ] Toggle Dark/Light mode
- [ ] Refresh page (theme phải giữ nguyên)
- [ ] Test trên nhiều pages

### Test Data Table
- [ ] Click header để sort
- [ ] Change page size
- [ ] Navigate giữa các pages
- [ ] Test action buttons

### Test Search & Filter
- [ ] Search với keyword
- [ ] Click filter chips
- [ ] Combine search + filter
- [ ] Clear all filters

## 🚀 Tích Hợp Backend

Để kết nối với API thật:

### 1. Update Environment
```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### 2. Update Service
```typescript
// customers.service.ts
import { HttpClient } from '@angular/common/http';
import { environment } from '@environments/environment';

constructor(private http: HttpClient) {}

getCustomers(params: any) {
  return this.http.get(`${environment.apiUrl}/customers`, { params });
}
```

### 3. Update Component
```typescript
this.customersService.getCustomers(this.paginationParams())
  .subscribe(result => {
    this.customers.set(result.data);
    this.totalCount.set(result.total);
  });
```

## 📚 Tài Liệu Tham Khảo

- [Angular Documentation](https://angular.dev/)
- [Material Design](https://material.angular.io/)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)
- [RxJS Guide](https://rxjs.dev/)

## 💡 Tips & Tricks

### Performance
- Sử dụng lazy loading cho routes
- Enable production mode khi deploy
- Optimize images trong assets
- Use OnPush change detection

### Development
- Use Angular DevTools extension
- Use Redux DevTools cho NgRx
- Enable source maps cho debugging
- Use Angular CLI generators

### Code Quality
- Follow Angular style guide
- Use TypeScript strict mode
- Write meaningful component names
- Keep components small and focused

## 🎉 Tính Năng Nổi Bật

✅ **Modern Stack**: Angular 21 + Material Design
✅ **Responsive**: Mobile, Tablet, Desktop
✅ **Dark Theme**: Protect your eyes
✅ **Reusable**: Component library
✅ **Type Safe**: Full TypeScript
✅ **Fast**: Optimized performance
✅ **Clean Code**: Well organized
✅ **Documentation**: Comprehensive guides

## 📞 Hỗ Trợ

Nếu gặp vấn đề:
1. Kiểm tra Node version (phải là v20 hoặc v22)
2. Đọc lại README.md
3. Check console log trong browser
4. Verify mock data trong service

## 🔮 Tính Năng Sẽ Thêm

- [ ] Customer detail page
- [ ] Create/Edit forms
- [ ] File upload
- [ ] Export to Excel
- [ ] Charts & Analytics
- [ ] Real-time notifications
- [ ] Multi-language
- [ ] Advanced search

---

**Chúc bạn code vui vẻ! 🚀**
