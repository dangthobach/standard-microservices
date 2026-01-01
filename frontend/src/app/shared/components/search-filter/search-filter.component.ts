import { Component, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FilterOptions } from '../../models/customer.model';

@Component({
  selector: 'app-search-filter',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  templateUrl: './search-filter.component.html',
  styleUrls: ['./search-filter.component.css']
})
export class SearchFilterComponent {
  searchText = signal<string>('');
  dateFrom = signal<Date | null>(null);
  dateTo = signal<Date | null>(null);

  filterChange = output<FilterOptions>();

  onSearch(): void {
    this.emitFilters();
  }

  onDateChange(): void {
    this.emitFilters();
  }

  clearFilters(): void {
    this.searchText.set('');
    this.dateFrom.set(null);
    this.dateTo.set(null);
    this.emitFilters();
  }

  hasActiveFilters(): boolean {
    return !!(this.searchText() || this.dateFrom() || this.dateTo());
  }

  private emitFilters(): void {
    const filters: FilterOptions = {
      search: this.searchText() || undefined,
      dateFrom: this.dateFrom() || undefined,
      dateTo: this.dateTo() || undefined
    };
    this.filterChange.emit(filters);
  }
}
