import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet, Router } from "@angular/router";
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  template: `
    <nav class="nav-bar">
      <a routerLink="/" class="nav-logo">Ustadh-a</a>
      <div class="nav-links">
        <a routerLink="/" class="nav-link">
          <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#e8eaed"><path d="M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Zm320-350Z"/></svg>
        </a>
        <ng-container *ngIf="authService.isLoggedIn()">
          <a (click)="toggleDemoMode()" class="nav-link" style="cursor: pointer;" [ngStyle]="{'color': isDemoMode ? '#ffb84d' : 'inherit'}">
            {{ isDemoMode ? '🎭 Démo On' : '🎭 Démo Off' }}
          </a>
          <a (click)="logout()" class="nav-link logout-btn" style="cursor: pointer; color: #ff4d4d;">Déconnexion</a>
        </ng-container>
      </div>
    </nav>
    <main style="padding-top: var(--nav-height);">
      <router-outlet></router-outlet>
    </main>
  `,
})
export class AdminComponent {
  authService = inject(AuthService);
  router = inject(Router);
  isDemoMode = false;

  toggleDemoMode() {
    this.isDemoMode = !this.isDemoMode;
    if (this.isDemoMode) {
      document.body.classList.add('demo-mode');
    } else {
      document.body.classList.remove('demo-mode');
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
