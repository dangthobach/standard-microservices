import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService, ProductAttachment } from '../../../core/services/product.service';
import { HttpClientModule } from '@angular/common/http';
import { FileViewerComponent } from './file-viewer.component';

@Component({
    selector: 'app-product-file-list',
    standalone: true,
    imports: [CommonModule, HttpClientModule, FileViewerComponent],
    template: `
    <div class="file-manager">
      <div class="header">
        <h3>Product Attachments</h3>
        <label class="upload-btn">
          <input type="file" (change)="onFileSelected($event)" [disabled]="isUploading" hidden>
          <span *ngIf="!isUploading">Drag & Drop or Click to Upload</span>
          <span *ngIf="isUploading">Uploading...</span>
        </label>
      </div>

      <div class="file-grid">
        <div class="file-card" *ngFor="let file of files" (click)="openViewer(file)">
          <div class="icon">
            <span *ngIf="file.fileType.includes('image')">🖼️</span>
            <span *ngIf="file.fileType.includes('pdf')">📄</span>
            <span *ngIf="!file.fileType.includes('image') && !file.fileType.includes('pdf')">📁</span>
          </div>
          <div class="info">
            <div class="name" title="{{file.fileName}}">{{ file.fileName }}</div>
            <div class="meta">{{ (file.fileSize / 1024 / 1024).toFixed(2) }} MB</div>
          </div>
          <div class="actions" (click)="$event.stopPropagation()">
             <!-- Download Button (Should be guarded by permission logic) -->
             <a [href]="getDownloadLink(file)" target="_blank" class="btn-icon" title="Download">⬇️</a>
          </div>
        </div>
      </div>

      <app-file-viewer *ngIf="selectedFile" 
                       [file]="selectedFile" 
                       [viewUrl]="getViewLink(selectedFile)"
                       (close)="selectedFile = null">
      </app-file-viewer>
    </div>
  `,
    styles: [`
    .file-manager {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 8px;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    .upload-btn {
      padding: 0.5rem 1rem;
      background: #007bff;
      color: white;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.9rem;
    }
    .upload-btn:hover { background: #0056b3; }
    
    .file-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      gap: 1rem;
    }
    
    .file-card {
      background: white;
      border: 1px solid #dee2e6;
      border-radius: 8px;
      padding: 1rem;
      text-align: center;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
      position: relative;
    }
    .file-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }
    
    .icon { font-size: 2.5rem; margin-bottom: 0.5rem; }
    .name { 
      font-weight: 500; 
      white-space: nowrap; 
      overflow: hidden; 
      text-overflow: ellipsis; 
      font-size: 0.9rem;
    }
    .meta { font-size: 0.8rem; color: #6c757d; }
    
    .actions {
      position: absolute;
      top: 5px;
      right: 5px;
      display: none;
    }
    .file-card:hover .actions { display: block; }
    .btn-icon { text-decoration: none; font-size: 1.2rem; }
  `]
})
export class ProductFileListComponent implements OnInit {
    @Input() productId!: string;
    files: ProductAttachment[] = [];
    selectedFile: ProductAttachment | null = null;
    isUploading = false;

    constructor(private productService: ProductService) { }

    ngOnInit() {
        if (this.productId) this.loadFiles();
    }

    loadFiles() {
        this.productService.getFiles(this.productId).subscribe(files => this.files = files);
    }

    onFileSelected(event: any) {
        const file = event.target.files[0];
        if (file) {
            this.isUploading = true;
            this.productService.uploadFile(this.productId, file).subscribe({
                next: () => {
                    this.isUploading = false;
                    this.loadFiles();
                },
                error: () => this.isUploading = false
            });
        }
    }

    getViewLink(file: ProductAttachment): string {
        return this.productService.getViewUrl(this.productId, file.id);
    }

    getDownloadLink(file: ProductAttachment): string {
        return this.productService.getDownloadUrl(this.productId, file.id);
    }

    openViewer(file: ProductAttachment) {
        this.selectedFile = file;
    }
}
