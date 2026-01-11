import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Product } from '../../../core/models/product.model';

@Component({
    selector: 'app-product-form',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule],
    templateUrl: './product-form.component.html',
    styleUrls: ['./product-form.component.css']
})
export class ProductFormComponent implements OnInit {
    @Input() product: Product | null = null;
    @Input() isEditMode = false;
    @Output() formSubmit = new EventEmitter<Partial<Product>>();
    @Output() formCancel = new EventEmitter<void>();

    productForm!: FormGroup;
    isSubmitting = false;

    constructor(private fb: FormBuilder) { }

    ngOnInit(): void {
        this.initForm();
        if (this.product && this.isEditMode) {
            this.productForm.patchValue(this.product);
        }
    }

    private initForm(): void {
        this.productForm = this.fb.group({
            name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
            sku: ['', [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/)]],
            description: ['', [Validators.maxLength(500)]],
            price: [0, [Validators.required, Validators.min(0)]],
            category: [''],
            stockQuantity: [0, [Validators.required, Validators.min(0)]],
            active: [true]
        });
    }

    onSubmit(): void {
        if (this.productForm.valid && !this.isSubmitting) {
            this.isSubmitting = true;
            this.formSubmit.emit(this.productForm.value);
        }
    }

    onCancel(): void {
        this.formCancel.emit();
    }

    // Getters for form validation
    get name() { return this.productForm.get('name'); }
    get sku() { return this.productForm.get('sku'); }
    get price() { return this.productForm.get('price'); }
    get stockQuantity() { return this.productForm.get('stockQuantity'); }
}
