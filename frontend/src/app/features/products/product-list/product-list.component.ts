import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { Product, Page } from '../../../core/models/product.model';
import { ProductFormComponent } from '../product-form/product-form.component';

@Component({
    selector: 'app-product-list',
    standalone: true,
    imports: [CommonModule, RouterModule, ProductFormComponent],
    templateUrl: './product-list.component.html',
    styleUrls: ['./product-list.component.css']
})
export class ProductListComponent implements OnInit {
    products: Product[] = [];
    totalElements = 0;
    totalPages = 0;
    currentPage = 0;
    pageSize = 10;
    loading = true;
    error: string | null = null;
    showCreateForm = false;

    constructor(
        private productService: ProductService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.loadProducts();
    }

    loadProducts(): void {
        this.loading = true;
        this.error = null;

        this.productService.getProducts(this.currentPage, this.pageSize).subscribe({
            next: (page: Page<Product>) => {
                this.products = page.content;
                this.totalElements = page.totalElements;
                this.totalPages = page.totalPages;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Failed to load products. Please try again.';
                this.loading = false;
                console.error(err);
            }
        });
    }

    onCreateClick(): void {
        this.showCreateForm = true;
    }

    onCreateSubmit(formData: Partial<Product>): void {
        this.productService.createProduct(formData).subscribe({
            next: (product) => {
                this.showCreateForm = false;
                this.loadProducts(); // Refresh list
            },
            error: (err) => {
                console.error('Failed to create product:', err);
                alert('Failed to create product. Please check your input.');
            }
        });
    }

    onCreateCancel(): void {
        this.showCreateForm = false;
    }

    onViewProduct(product: Product): void {
        this.router.navigate(['/products', product.id]);
    }

    onDeleteProduct(product: Product, event: Event): void {
        event.stopPropagation();
        if (confirm(`Delete "${product.name}"?`)) {
            this.productService.deleteProduct(product.id).subscribe({
                next: () => this.loadProducts(),
                error: (err) => {
                    console.error('Failed to delete product:', err);
                    alert('Failed to delete product');
                }
            });
        }
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
            this.loadProducts();
        }
    }

    getPageNumbers(): number[] {
        const pages: number[] = [];
        const maxVisible = 5;
        const half = Math.floor(maxVisible / 2);

        let start = Math.max(0, this.currentPage - half);
        let end = Math.min(this.totalPages - 1, start + maxVisible - 1);

        if (end - start < maxVisible - 1) {
            start = Math.max(0, end - maxVisible + 1);
        }

        for (let i = start; i <= end; i++) {
            pages.push(i);
        }
        return pages;
    }
}
