export interface Customer {
  id: string;
  username: string;
  email: string;
  enabled: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface CustomerStatusHistory {
  dateTime: Date;
  user: string;
  action: string;
  details: string;
}

export interface FilterOptions {
  search?: string;
  dateFrom?: Date | null;
  dateTo?: Date | null;
}

export interface PaginationParams {
  page: number;
  size: number;
  sort?: string;
  direction?: 'asc' | 'desc';
}
