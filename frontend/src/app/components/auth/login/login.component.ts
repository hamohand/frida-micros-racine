import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">
            <span class="logo-text">Frida</span><span class="logo-dot">.</span>
          </div>
          <h2>Connexion</h2>
          <p>Veuillez vous authentifier pour accéder à l'application.</p>
        </div>

        <div *ngIf="errorMessage" class="error-banner">
          {{ errorMessage }}
        </div>

        <form (ngSubmit)="onSubmit()" #loginForm="ngForm" class="login-form">
          <div class="form-group">
            <label for="username">Nom d'utilisateur</label>
            <input 
              type="text" 
              id="username" 
              name="username" 
              [(ngModel)]="credentials.username" 
              required
              class="form-control"
              placeholder="Ex: maitre"
            >
          </div>

          <div class="form-group">
            <label for="password">Mot de passe</label>
            <input 
              type="password" 
              id="password" 
              name="password" 
              [(ngModel)]="credentials.password" 
              required
              class="form-control"
              placeholder="••••••••"
            >
          </div>

          <button type="submit" [disabled]="!loginForm.form.valid || isLoading" class="btn-login">
            <span *ngIf="!isLoading">Se connecter</span>
            <span *ngIf="isLoading" class="spinner"></span>
          </button>

          <div *ngIf="isDemoMode" class="demo-section">
            <div class="divider"><span>OU</span></div>
            <button type="button" class="btn-demo" (click)="onDemoLogin()">
              🚀 Démarrer en Mode Démo
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap');
    
    :host { font-family: 'Outfit', sans-serif; display: block; height: 100vh; background: #0f1c15; }

    .login-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 2rem;
    }

    .login-card {
      background: rgba(255,255,255,0.03);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 16px;
      padding: 3rem;
      width: 100%;
      max-width: 450px;
      box-shadow: 0 15px 35px rgba(0,0,0,0.4);
      animation: fadeIn 0.5s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .logo {
      font-size: 2.5rem;
      font-weight: 700;
      margin-bottom: 1rem;
    }

    .logo-text { color: #fff; }
    .logo-dot { color: #4ecca3; }

    h2 { color: #fff; margin: 0 0 0.5rem; }
    p { color: rgba(255,255,255,0.5); font-size: 0.9rem; margin: 0; }

    .error-banner {
      background: rgba(255, 77, 77, 0.1);
      color: #ff4d4d;
      border: 1px solid rgba(255, 77, 77, 0.3);
      padding: 10px;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      font-size: 0.9rem;
      text-align: center;
    }

    .form-group {
      margin-bottom: 1.5rem;
    }

    label {
      display: block;
      color: #e2e8f0;
      margin-bottom: 0.5rem;
      font-size: 0.9rem;
      font-weight: 600;
    }

    .form-control {
      width: 100%;
      padding: 12px 16px;
      background: rgba(0,0,0,0.2);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 8px;
      color: #fff;
      font-family: inherit;
      font-size: 1rem;
      transition: all 0.3s;
      box-sizing: border-box;
    }

    .form-control:focus {
      outline: none;
      border-color: #4ecca3;
      background: rgba(0,0,0,0.4);
    }

    .btn-login {
      width: 100%;
      padding: 14px;
      background: #4ecca3;
      color: #0f1c15;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 700;
      font-family: inherit;
      cursor: pointer;
      transition: all 0.2s;
      margin-top: 1rem;
    }

    .btn-login:hover:not(:disabled) {
      background: #45b08c;
      transform: translateY(-2px);
    }

    .btn-login:disabled {
      background: rgba(78,204,163,0.5);
      cursor: not-allowed;
    }

    .spinner {
      display: inline-block;
      width: 20px; height: 20px;
      border: 3px solid rgba(15,28,21,0.3);
      border-top-color: #0f1c15;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .divider {
      text-align: center;
      margin: 1.5rem 0;
      position: relative;
    }
    .divider::before {
      content: '';
      position: absolute;
      left: 0; top: 50%; width: 40%; height: 1px;
      background: rgba(255,255,255,0.1);
    }
    .divider::after {
      content: '';
      position: absolute;
      right: 0; top: 50%; width: 40%; height: 1px;
      background: rgba(255,255,255,0.1);
    }
    .divider span { color: rgba(255,255,255,0.4); font-size: 0.8rem; font-weight: bold; }

    .btn-demo {
      width: 100%;
      padding: 14px;
      background: rgba(78,204,163,0.1);
      color: #4ecca3;
      border: 1px solid rgba(78,204,163,0.3);
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 700;
      font-family: inherit;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn-demo:hover {
      background: rgba(78,204,163,0.2);
    }
  `]
})
export class LoginComponent implements OnInit {
  credentials = { username: '', password: '' };
  isLoading = false;
  errorMessage = '';
  isDemoMode = false;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.authService.getSecurityStatus().subscribe({
      next: (res) => {
        this.isDemoMode = res.demoMode === true || res.demoMode === 'true';
        if (this.isDemoMode) {
          this.onDemoLogin(); // Redirection automatique !
        }
      },
      error: () => {
        this.isDemoMode = false; // Par défaut s'il n'arrive pas à lire
      }
    });
  }

  onDemoLogin() {
    this.authService.loginDemo();
    this.router.navigate(['/list']);
  }

  onSubmit() {
    this.isLoading = true;
    this.errorMessage = '';
    
    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.isLoading = false;
        // Rediriger vers la page d'accueil ou tableau de bord
        this.router.navigate(['/list']);
      },
      error: (err) => {
        this.isLoading = false;
        if (err.status === 401 || err.status === 403) {
          this.errorMessage = "Nom d'utilisateur ou mot de passe incorrect.";
        } else {
          this.errorMessage = "Une erreur est survenue lors de la connexion.";
        }
      }
    });
  }
}
