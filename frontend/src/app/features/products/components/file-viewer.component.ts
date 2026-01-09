import { Component, Input, Output, EventEmitter, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductAttachment } from '../../../core/services/product.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
    selector: 'app-file-viewer',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="viewer-backdrop" (click)="close.emit()">
      <div class="viewer-content" (click)="$event.stopPropagation()">
        <button class="close-btn" (click)="close.emit()">×</button>
        
        <div class="file-container" (contextmenu)="preventEvent($event)">
          
          <!-- Image Viewer -->
          <img *ngIf="file.fileType.includes('image')" 
               [src]="viewUrl" 
               class="preview-img">

          <!-- PDF Viewer (iframe) -->
          <iframe *ngIf="file.fileType.includes('pdf')" 
                  [src]="safeUrl" 
                  class="preview-iframe">
          </iframe>

          <!-- Fallback -->
          <div *ngIf="!file.fileType.includes('image') && !file.fileType.includes('pdf')" class="fallback">
            <p>Preview not available for this file type.</p>
            <p>{{ file.fileName }}</p>
          </div>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .viewer-backdrop {
      position: fixed;
      top: 0; left: 0;
      width: 100vw; height: 100vh;
      background: rgba(0,0,0,0.85);
      z-index: 1000;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .viewer-content {
      position: relative;
      width: 90%;
      height: 90%;
      background: #222;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .close-btn {
      position: absolute;
      top: 10px; right: 15px;
      background: none;
      border: none;
      color: white;
      font-size: 2rem;
      cursor: pointer;
      z-index: 10;
    }
    .file-container {
      width: 100%;
      height: 100%;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    .preview-img {
      max-width: 100%;
      max-height: 100%;
      object-fit: contain;
    }
    .preview-iframe {
      width: 100%;
      height: 100%;
      border: none;
      background: white;
    }
    .fallback {
      color: white;
      text-align: center;
    }
  `]
})
export class FileViewerComponent {
    @Input() file!: ProductAttachment;
    @Input() viewUrl!: string;
    @Output() close = new EventEmitter<void>();

    constructor(private sanitizer: DomSanitizer) { }

    get safeUrl(): SafeResourceUrl {
        return this.sanitizer.bypassSecurityTrustResourceUrl(this.viewUrl);
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            this.close.emit();
        }
        // Prevent Save Shortcut (Ctrl+S / Cmd+S)
        if ((event.ctrlKey || event.metaKey) && event.key === 's') {
            event.preventDefault();
            console.log('Saving is disabled in View Mode');
        }
    }

    preventEvent(event: Event) {
        event.preventDefault();
        return false;
    }
}
