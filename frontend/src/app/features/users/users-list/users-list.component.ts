import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-users-list',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Users Management</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <p>Users list will be implemented here.</p>
      </mat-card-content>
    </mat-card>
  `
})
export class UsersListComponent {}
