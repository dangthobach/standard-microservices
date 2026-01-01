import { Injectable } from '@angular/core';
import { Customer, FilterOptions, PaginationParams } from '../../shared/models/customer.model';

@Injectable({
  providedIn: 'root'
})
export class CustomersService {
  private mockCustomers: Customer[] = [
    {
      id: '1',
      username: 'john.doe',
      email: 'john.doe@example.com',
      enabled: true,
      createdAt: new Date('2023-10-26T09:30:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '2',
      username: 'jane.smith',
      email: 'jane.smith@example.com',
      enabled: false,
      createdAt: new Date('2023-10-25T14:15:00'),
      updatedAt: new Date('2023-10-27T10:00:00')
    },
    {
      id: '3',
      username: 'mike.brown',
      email: 'mike.brown@example.com',
      enabled: true,
      createdAt: new Date('2023-10-24T10:00:00'),
      updatedAt: new Date('2023-10-26T15:30:00')
    },
    {
      id: '4',
      username: 'john.doe',
      email: 'john.doe@example.com',
      enabled: true,
      createdAt: new Date('2023-10-26T09:30:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '5',
      username: 'jane.smith',
      email: 'jane.smith@example.com',
      enabled: true,
      createdAt: new Date('2023-10-25T14:15:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '6',
      username: 'john.doe',
      email: 'john.doe@example.com',
      enabled: true,
      createdAt: new Date('2023-10-26T09:30:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '7',
      username: 'jane.smith',
      email: 'jane.smith@example.com',
      enabled: false,
      createdAt: new Date('2023-10-25T14:15:00'),
      updatedAt: new Date('2023-10-27T10:00:00')
    },
    {
      id: '8',
      username: 'john.doe',
      email: 'mike.doe@example.com',
      enabled: true,
      createdAt: new Date('2023-10-24T09:30:00'),
      updatedAt: new Date('2023-10-26T15:30:00')
    },
    {
      id: '9',
      username: 'mike.brown',
      email: 'mike.brown@example.com',
      enabled: true,
      createdAt: new Date('2023-10-24T10:00:00'),
      updatedAt: new Date('2023-10-26T15:30:00')
    },
    {
      id: '10',
      username: 'john.doe',
      email: 'john.doe@example.com',
      enabled: true,
      createdAt: new Date('2023-10-26T09:30:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '11',
      username: 'jane.smith',
      email: 'jane.smith@example.com',
      enabled: true,
      createdAt: new Date('2023-10-25T14:15:00'),
      updatedAt: new Date('2023-10-27T11:45:00')
    },
    {
      id: '12',
      username: 'sarah.wilson',
      email: 'sarah.wilson@example.com',
      enabled: true,
      createdAt: new Date('2023-10-23T08:20:00'),
      updatedAt: new Date('2023-10-25T09:10:00')
    },
    {
      id: '13',
      username: 'david.lee',
      email: 'david.lee@example.com',
      enabled: false,
      createdAt: new Date('2023-10-22T16:45:00'),
      updatedAt: new Date('2023-10-24T14:30:00')
    },
    {
      id: '14',
      username: 'emily.clark',
      email: 'emily.clark@example.com',
      enabled: true,
      createdAt: new Date('2023-10-21T11:30:00'),
      updatedAt: new Date('2023-10-23T13:20:00')
    },
    {
      id: '15',
      username: 'robert.taylor',
      email: 'robert.taylor@example.com',
      enabled: true,
      createdAt: new Date('2023-10-20T09:15:00'),
      updatedAt: new Date('2023-10-22T10:45:00')
    }
  ];

  getCustomers(
    pagination: PaginationParams,
    filters: FilterOptions
  ): { data: Customer[]; total: number } {
    let filtered = [...this.mockCustomers];

    // Apply filters
    if (filters.search) {
      const search = filters.search.toLowerCase();
      filtered = filtered.filter(c =>
        c.username.toLowerCase().includes(search) ||
        c.email.toLowerCase().includes(search)
      );
    }

    // Apply date range filter
    if (filters.dateFrom || filters.dateTo) {
      filtered = filtered.filter(c => {
        const createdDate = new Date(c.createdAt);
        createdDate.setHours(0, 0, 0, 0);

        if (filters.dateFrom && filters.dateTo) {
          const fromDate = new Date(filters.dateFrom);
          fromDate.setHours(0, 0, 0, 0);
          const toDate = new Date(filters.dateTo);
          toDate.setHours(23, 59, 59, 999);
          return createdDate >= fromDate && createdDate <= toDate;
        } else if (filters.dateFrom) {
          const fromDate = new Date(filters.dateFrom);
          fromDate.setHours(0, 0, 0, 0);
          return createdDate >= fromDate;
        } else if (filters.dateTo) {
          const toDate = new Date(filters.dateTo);
          toDate.setHours(23, 59, 59, 999);
          return createdDate <= toDate;
        }
        return true;
      });
    }

    // Apply sorting
    if (pagination.sort) {
      filtered.sort((a, b) => {
        const aVal = a[pagination.sort as keyof Customer];
        const bVal = b[pagination.sort as keyof Customer];

        if (aVal < bVal) return pagination.direction === 'asc' ? -1 : 1;
        if (aVal > bVal) return pagination.direction === 'asc' ? 1 : -1;
        return 0;
      });
    }

    const total = filtered.length;

    // Apply pagination
    const start = pagination.page * pagination.size;
    const end = start + pagination.size;
    const data = filtered.slice(start, end);

    return { data, total };
  }
}
