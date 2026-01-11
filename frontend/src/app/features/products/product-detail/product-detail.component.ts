import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ProductService, ProductAttachment } from '../../../core/services/product.service';
import { Product } from '../../../core/models/product.model';
import { ProductFormComponent } from '../product-form/product-form.component';

@Component({
    selector: 'app-product-detail',
    standalone: true,
    imports: [CommonModule, RouterModule, ProductFormComponent],
    templateUrl: './product-detail.component.html',
    styleUrls: ['./product-detail.component.css']
})
export class ProductDetailComponent implements OnInit {
    product: Product | null = null;
    files: ProductAttachment[] = [];
    loading = true;
    error: string | null = null;
    showEditForm = false;
    uploadingFile = false;
    deletingProduct = false;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private productService: ProductService
    ) { }

    ngOnInit(): void {
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.loadProduct(id);
            this.loadFiles(id);
        }
    }

    loadProduct(id: string): void {
        this.loading = true;
        this.productService.getProduct(id).subscribe({
            next: (product) => {
                this.product = product;
                this.loading = false;
            },
            error: (err) => {
                this.error = 'Failed to load product';
                this.loading = false;
                console.error(err);
            }
        });
    }

    loadFiles(productId: string): void {
        this.productService.getFiles(productId).subscribe({
            next: (files) => this.files = files,
            error: (err) => console.error('Failed to load files:', err)
        });
    }

    onEdit(): void {
        this.showEditForm = true;
    }

    onEditSubmit(formData: Partial<Product>): void {
        if (!this.product) return;

        this.productService.updateProduct(this.product.id, formData).subscribe({
            next: (updated) => {
                this.product = updated;
                this.showEditForm = false;
            },
            error: (err) => {
                console.error('Failed to update product:', err);
                alert('Failed to update product');
            }
        });
    }

    onEditCancel(): void {
        this.showEditForm = false;
    }

    onDelete(): void {
        if (!this.product) return;

        if (confirm(`Are you sure you want to delete "${this.product.name}"?`)) {
            this.deletingProduct = true;
            this.productService.deleteProduct(this.product.id).subscribe({
                next: () => {
                    this.router.navigate(['/products']);
                },
                error: (err) => {
                    this.deletingProduct = false;
                    console.error('Failed to delete product:', err);
                    alert('Failed to delete product');
                }
            });
        }
    }

    onFileUpload(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files?.length || !this.product) return;

        const file = input.files[0];
        this.uploadingFile = true;

        this.productService.uploadFile(this.product.id, file).subscribe({
            next: (attachment) => {
                this.files.push(attachment);
                this.uploadingFile = false;
                input.value = ''; // Reset input
            },
            error: (err) => {
                this.uploadingFile = false;
                console.error('Failed to upload file:', err);
                alert('Failed to upload file');
            }
        });
    }

    getViewUrl(fileId: string): string {
        return this.product ? this.productService.getViewUrl(this.product.id, fileId) : '';
    }

    getDownloadUrl(fileId: string): string {
        return this.product ? this.productService.getDownloadUrl(this.product.id, fileId) : '';
    }

    formatFileSize(bytes: number): string {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    }

    goBack(): void {
        this.router.navigate(['/products']);
    }
}
