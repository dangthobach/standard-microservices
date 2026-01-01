import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { ThemeService } from './shared/services/theme.service';
import { LayoutComponent } from './shared/components/layout/layout.component';
import { Observable } from 'rxjs';

/**
 * Root Application Component
 *
 * Features:
 * - Layout with header, sidebar, and main content
 * - Authentication temporarily disabled for demo
 * - Dark/Light theme support
 * - Responsive design
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    LayoutComponent
  ],
  template: `
    <div class="app-container">
      <!-- Layout always shown (auth disabled for demo) -->
      <app-layout></app-layout>
    </div>
  `,
  styles: [`
    .app-container {
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: hidden;
    }
  `]
})
export class AppComponent implements OnInit {
  isAuthenticated$: Observable<boolean>;

  constructor(
    private authService: AuthService,
    private themeService: ThemeService,
    private router: Router
  ) {
    this.isAuthenticated$ = this.authService.isAuthenticated$;
  }

  ngOnInit(): void {
    // AuthService and ThemeService automatically handle initialization
    console.log('App initialized with theme:', this.themeService.theme());
  }
}
