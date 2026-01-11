import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

export interface MenuItem {
  icon: string;
  label: string;
  route: string;
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatListModule,
    MatIconModule,
    MatTooltipModule
  ],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  isCollapsed = input<boolean>(false);
  itemClick = output<void>();

  menuItems: MenuItem[] = [
    { icon: 'dashboard', label: 'Dashboard', route: '/dashboard' },
    { icon: 'inventory_2', label: 'Products', route: '/products' },
    { icon: 'people', label: 'Customers', route: '/customers', badge: 50 },
    { icon: 'business', label: 'Organizations', route: '/organizations' },
    { icon: 'person', label: 'Users', route: '/users' },
    { icon: 'settings', label: 'Settings', route: '/settings' },
  ];

  onItemClick(): void {
    this.itemClick.emit();
  }
}
