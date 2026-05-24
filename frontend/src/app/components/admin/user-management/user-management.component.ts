import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="users-wrapper">
      <div class="glass-panel">
        <div class="header-section">
          <h2>Gestion des Utilisateurs</h2>
          <p class="subtitle">Créez et gérez les comptes pour les clercs et collaborateurs.</p>
        </div>

        <!-- Formulaire de création -->
        <div class="create-card">
          <h3>Créer un nouvel utilisateur</h3>
          <form (ngSubmit)="onCreateUser()" #createForm="ngForm" class="create-form">
            <div class="form-row">
              <div class="form-group">
                <input 
                  type="text" 
                  name="newUsername" 
                  [(ngModel)]="newUser.username" 
                  required
                  class="form-control"
                  placeholder="Nom d'utilisateur"
                >
              </div>
              <div class="form-group">
                <input 
                  type="password" 
                  name="newPassword" 
                  [(ngModel)]="newUser.password" 
                  required
                  class="form-control"
                  placeholder="Mot de passe"
                >
              </div>
              <button type="submit" class="btn-create" [disabled]="!createForm.form.valid || isCreating">
                <span *ngIf="!isCreating">Créer le compte (USER)</span>
                <span *ngIf="isCreating" class="spinner"></span>
              </button>
            </div>
            <div *ngIf="message" [class]="'alert ' + (isError ? 'alert-error' : 'alert-success')">
              {{ message }}
            </div>
          </form>
        </div>

        <!-- Liste des utilisateurs -->
        <div class="users-list-card">
          <h3>Comptes existants</h3>
          <div *ngIf="isLoading" class="loading-state">
            <span class="spinner"></span> Chargement...
          </div>
          <table class="table" *ngIf="!isLoading">
            <thead>
              <tr>
                <th>ID</th>
                <th>Nom d'utilisateur</th>
                <th>Rôle</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let user of users">
                <td>{{ user.id }}</td>
                <td class="font-bold">{{ user.username }}</td>
                <td>
                  <span class="badge" [class.badge-maitre]="user.role === 'ROLE_MAITRE'">
                    {{ user.role === 'ROLE_MAITRE' ? 'MAÎTRE' : 'UTILISATEUR' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap');
    
    :host { font-family: 'Outfit', sans-serif; }

    .users-wrapper {
      padding: 3rem 2rem;
      background: #0f1c15;
      min-height: calc(100vh - 80px);
      display: flex;
      justify-content: center;
      animation: fadeIn 0.5s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .glass-panel {
      width: 100%;
      max-width: 900px;
    }

    .header-section { margin-bottom: 2rem; }
    h2 { color: #fff; margin: 0 0 0.5rem; font-size: 2rem; }
    h3 { color: #4ecca3; margin: 0 0 1.5rem; font-size: 1.2rem; }
    .subtitle { color: rgba(255,255,255,0.6); margin: 0; }

    .create-card, .users-list-card {
      background: rgba(255,255,255,0.03);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 2rem;
    }

    .form-row {
      display: flex;
      gap: 1rem;
      align-items: center;
      flex-wrap: wrap;
    }

    .form-group { flex: 1; min-width: 200px; }

    .form-control {
      width: 100%;
      padding: 10px 14px;
      background: rgba(0,0,0,0.2);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 8px;
      color: #fff;
      font-family: inherit;
      transition: all 0.3s;
    }
    .form-control:focus { outline: none; border-color: #4ecca3; }

    .btn-create {
      padding: 10px 20px;
      background: #4ecca3;
      color: #0f1c15;
      border: none;
      border-radius: 8px;
      font-weight: bold;
      cursor: pointer;
      height: 42px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;
    }
    .btn-create:hover:not(:disabled) { background: #45b08c; }
    .btn-create:disabled { opacity: 0.6; cursor: not-allowed; }

    .alert { padding: 10px; border-radius: 8px; margin-top: 1rem; font-size: 0.9rem; }
    .alert-success { background: rgba(78,204,163,0.1); color: #4ecca3; border: 1px solid rgba(78,204,163,0.3); }
    .alert-error { background: rgba(255,77,77,0.1); color: #ff4d4d; border: 1px solid rgba(255,77,77,0.3); }

    .table { width: 100%; border-collapse: collapse; }
    .table th { text-align: left; padding: 1rem; color: rgba(255,255,255,0.5); font-weight: 600; border-bottom: 1px solid rgba(255,255,255,0.1); }
    .table td { padding: 1rem; color: #e2e8f0; border-bottom: 1px solid rgba(255,255,255,0.05); }
    .font-bold { font-weight: 600; color: #fff; }

    .badge {
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 0.8rem;
      font-weight: 600;
      background: rgba(255,255,255,0.1);
    }
    .badge-maitre {
      background: rgba(255,184,77,0.15);
      color: #ffb84d;
      border: 1px solid rgba(255,184,77,0.3);
    }

    .spinner {
      display: inline-block; width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.2);
      border-top-color: #fff; border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .loading-state { text-align: center; color: rgba(255,255,255,0.5); padding: 2rem; }
  `]
})
export class UserManagementComponent implements OnInit {
  users: any[] = [];
  newUser = { username: '', password: '' };
  isLoading = true;
  isCreating = false;
  message = '';
  isError = false;

  constructor(private authService: AuthService) {}

  ngOnInit() {
    this.loadUsers();
  }

  loadUsers() {
    this.isLoading = true;
    this.authService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  onCreateUser() {
    this.isCreating = true;
    this.message = '';
    
    this.authService.registerUser(this.newUser).subscribe({
      next: (res) => {
        this.message = "Utilisateur créé avec succès !";
        this.isError = false;
        this.newUser = { username: '', password: '' };
        this.isCreating = false;
        this.loadUsers(); // Recharger la liste
      },
      error: (err) => {
        this.message = err.error || "Erreur lors de la création de l'utilisateur.";
        this.isError = true;
        this.isCreating = false;
      }
    });
  }
}
