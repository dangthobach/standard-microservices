import { Component, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DataTableComponent, TableColumn, TableAction } from '../../shared/components/data-table/data-table.component';
import { SearchFilterComponent } from '../../shared/components/search-filter/search-filter.component';
import { PageEvent } from '@angular/material/paginator';
import { Sort } from '@angular/material/sort';
import { Customer, FilterOptions, PaginationParams } from '../../shared/models/customer.model';
import { CustomersService } from './customers.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-customers',
  standalone: true,
  imports: [
    CommonModule,
    DataTableComponent,
    SearchFilterComponent,
    MatButtonModule,
    MatIconModule,
    MatCardModule
  ],
  templateUrl: './customers.component.html',
  styleUrls: ['./customers.component.css']
})
export class CustomersComponent implements OnInit {
  customers = signal<Customer[]>([]);
  loading = signal<boolean>(false);
  totalCount = signal<number>(0);

  paginationParams = signal<PaginationParams>({
    page: 0,
    size: 10,
    sort: 'createdAt',
    direction: 'desc'
  });

  filterOptions = signal<FilterOptions>({});

  columns: TableColumn[] = [
    { key: 'username', label: 'Username', sortable: true, type: 'text' },
    { key: 'email', label: 'Email', sortable: true, type: 'text' },
    { key: 'enabled', label: 'Enabled', sortable: true, type: 'badge' },
    { key: 'createdAt', label: 'Created At', sortable: true, type: 'date' },
    { key: 'updatedAt', label: 'Updated At', sortable: true, type: 'date' }
  ];

  actions: TableAction[] = [
    {
      icon: 'edit',
      label: 'Edit',
      color: 'primary',
      callback: (row: Customer) => this.onEdit(row)
    },
    {
      icon: 'delete',
      label: 'Delete',
      color: 'warn',
      callback: (row: Customer) => this.onDelete(row)
    }
  ];

  constructor(private customersService: CustomersService) {}

  ngOnInit(): void {
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.loading.set(true);

    // Simulate API call
    setTimeout(() => {
      const result = this.customersService.getCustomers(
        this.paginationParams(),
        this.filterOptions()
      );

      this.customers.set(result.data);
      this.totalCount.set(result.total);
      this.loading.set(false);
    }, 500);
  }

  onFilterChange(filters: FilterOptions): void {
    this.filterOptions.set(filters);
    this.paginationParams.update(params => ({ ...params, page: 0 }));
    this.loadCustomers();
  }

  onPageChange(event: PageEvent): void {
    this.paginationParams.update(params => ({
      ...params,
      page: event.pageIndex,
      size: event.pageSize
    }));
    this.loadCustomers();
  }

  onSortChange(sort: Sort): void {
    if (sort.direction) {
      this.paginationParams.update(params => ({
        ...params,
        sort: sort.active,
        direction: sort.direction as 'asc' | 'desc'
      }));
      this.loadCustomers();
    }
  }

  onEdit(customer: Customer): void {
    console.log('Edit customer:', customer);
    // Implement edit logic
  }

  onDelete(customer: Customer): void {
    console.log('Delete customer:', customer);
    // Implement delete logic
  }
}
