import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { Product, Page } from '../../../core/models/product.model';
import { Observable } from 'rxjs';

@Component({
    selector: 'app-product-list',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './product-list.component.html',
    styleUrls: ['./product-list.component.css']
})
export class ProductListComponent implements OnInit {
    products$: Observable<Page<Product>> | null = null;
    error: string | null = null;

    constructor(private productService: ProductService) { }

    ngOnInit(): void {
        this.loadProducts();
    }

    loadProducts(): void {
        this.products$ = this.productService.getProducts();
    }
}
