import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Product, Page } from '../models/product.model';
import { environment } from '../../../environments/environment';

export interface ProductAttachment {
    id: string;
    productId: string;
    fileName: string;
    fileType: string;
    fileSize: number;
    storageKey: string;
    uploadDate: string;
}

@Injectable({
    providedIn: 'root'
})
export class ProductService {
    private apiUrl = `${environment.apiUrl}/business/products`; // /api is usually in environment base URL

    constructor(private http: HttpClient) { }

    getProducts(page: number = 0, size: number = 20): Observable<Page<Product>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<Page<Product>>(this.apiUrl, { params });
    }

    getProduct(id: string): Observable<Product> {
        return this.http.get<Product>(`${this.apiUrl}/${id}`);
    }

    createProduct(product: Partial<Product>): Observable<Product> {
        return this.http.post<Product>(this.apiUrl, product);
    }

    updateProduct(id: string, product: Partial<Product>): Observable<Product> {
        return this.http.put<Product>(`${this.apiUrl}/${id}`, product);
    }

    deleteProduct(id: string): Observable<void> {
        return this.http.delete<void>(`${this.apiUrl}/${id}`);
    }

    // --- File Management ---

    uploadFile(productId: string, file: File): Observable<ProductAttachment> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<ProductAttachment>(`${this.apiUrl}/${productId}/files`, formData);
    }

    getFiles(productId: string): Observable<ProductAttachment[]> {
        return this.http.get<ProductAttachment[]>(`${this.apiUrl}/${productId}/files`);
    }

    getViewUrl(productId: string, fileId: string): string {
        // Returns the Direct URL to the API, which will redirect to Nginx -> SeaweedFS
        return `${this.apiUrl}/${productId}/files/${fileId}/view`;
    }

    getDownloadUrl(productId: string, fileId: string): string {
        return `${this.apiUrl}/${productId}/files/${fileId}/download`;
    }
}
