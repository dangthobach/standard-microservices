export interface Product {
  id: string; // UUID
  name: string;
  sku: string;
  description?: string;
  price: number;
  category?: string;
  stockQuantity: number;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // page number
}
