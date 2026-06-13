import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <div class="home-wrapper">
      <section class="hero-section glass-panel">
        <div class="hero-content">
          <h1 class="hero-title">Ustadh-a</h1>
          <p class="hero-subtitle">
            Système Avancé de Gestion Notariale<br />
            <span class="highlight">Intelligence Artificielle & Traitement de Fridas</span>
          </p>

          <!-- Section Utilisation -->
          <div class="section-label">
            <span class="section-icon">📋</span>
            <span>Utilisation</span>
          </div>
          <div class="action-grid">
            <a routerLink="/create" class="action-card card-green">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160Zm40 200q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z"/></svg>
              </div>
              <h3>Nouveau Dossier</h3>
              <p>Ouvrir une nouvelle instruction Frida</p>
            </a>

            <a routerLink="/batch-review" class="action-card card-purple">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm40-80h200v-80H240v80Zm280-80h200v-80H520v80ZM240-440h200v-80H240v80Zm280 0h200v-80H520v80ZM240-600h200v-80H240v80Zm280 0h200v-80H520v80ZM200-200v-560 560Z"/></svg>
              </div>
              <h3>Batch</h3>
              <p>Traitement en lot de plusieurs dossiers</p>
            </a>

            <a routerLink="/search" class="action-card card-blue">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z"/></svg>
              </div>
              <h3>Rechercher / Archive</h3>
              <p>Consulter, rechercher et trier les dossiers</p>
            </a>

            <a routerLink="/simulateur" class="action-card card-teal">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M280-400q-33 0-56.5-23.5T200-480q0-33 23.5-56.5T280-560q33 0 56.5 23.5T360-480q0 33-23.5 56.5T280-400Zm400 0q-33 0-56.5-23.5T600-480q0-33 23.5-56.5T680-560q33 0 56.5 23.5T760-480q0 33-23.5 56.5T680-400ZM480-240q-33 0-56.5-23.5T400-320q0-33 23.5-56.5T480-400q33 0 56.5 23.5T560-320q0 33-23.5 56.5T480-240ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z"/></svg>
              </div>
              <h3>Simulateur de parts</h3>
              <p>Calculez instantanément un héritage</p>
            </a>
          </div>

          <!-- Section Administration (Maître uniquement) -->
          <ng-container *ngIf="authService.isMaitre()">
            <div class="section-divider"></div>
            <div class="section-label admin-label">
              <span class="section-icon">⚙️</span>
              <span>Administration</span>
            </div>
            <div class="action-grid admin-grid">
              <a routerLink="/users" class="action-card card-amber">
                <div class="card-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M40-160v-112q0-34 17.5-62.5T104-378q62-31 126-46.5T360-440q66 0 130 15.5T616-378q29 15 46.5 43.5T680-272v112H40Zm720 0v-120q0-44-24.5-84.5T666-434q51 6 96 20.5t84 35.5q36 20 55 44.5t19 53.5v120H760ZM360-480q-66 0-113-47t-47-113q0-66 47-113t113-47q66 0 113 47t47 113q0 66-47 113t-113 47Zm400-160q0 66-47 113t-113 47q-11 0-28-2.5t-28-5.5q27-32 41.5-71t14.5-81q0-42-14.5-81T544-792q14-5 28-6.5t28-1.5q66 0 113 47t47 113ZM120-240h480v-32q0-11-5.5-20T580-306q-54-27-109-40.5T360-360q-56 0-111 13.5T140-306q-9 5-14.5 14t-5.5 20v32Zm240-320q33 0 56.5-23.5T440-640q0-33-23.5-56.5T360-720q-33 0-56.5 23.5T280-640q0 33 23.5 56.5T360-560Zm0 320Zm0-400Z"/></svg>
                </div>
                <h3>Utilisateurs</h3>
                <p>Gérer les comptes et les rôles</p>
              </a>

              <a routerLink="/backups" class="action-card card-slate">
                <div class="card-icon">
                  <svg xmlns="http://www.w3.org/2000/svg" height="36" viewBox="0 -960 960 960" width="36" fill="currentColor"><path d="M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z"/></svg>
                </div>
                <h3>Sauvegardes</h3>
                <p>Gérer les backups de la base de données</p>
              </a>
            </div>
          </ng-container>

        </div>
      </section>
    </div>
  `,
  styles: [`
    .home-wrapper {
      min-height: calc(100vh - 80px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: radial-gradient(circle at center, #112d1b 0%, #0a1f0f 100%);
      animation: fadeIn 0.8s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .glass-panel {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(16px);
      -webkit-backdrop-filter: blur(16px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 24px;
      padding: 3rem 3.5rem;
      max-width: 960px;
      width: 100%;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      text-align: center;
    }

    .hero-title {
      font-size: 3rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 0.5rem;
      letter-spacing: -0.02em;
    }

    .hero-subtitle {
      font-size: 1.15rem;
      color: #a0aec0;
      line-height: 1.6;
      margin-bottom: 2rem;
    }

    .highlight {
      color: #38a169;
      font-weight: 600;
    }

    /* Section labels */
    .section-label {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      font-size: 1.1rem;
      font-weight: 600;
      color: #cbd5e0;
      margin-bottom: 1.2rem;
      text-align: left;
      padding-left: 4px;
    }

    .section-icon {
      font-size: 1.3rem;
    }

    .admin-label {
      color: #fbd38d;
    }

    .section-divider {
      height: 1px;
      background: linear-gradient(to right, transparent, rgba(255,255,255,0.12), transparent);
      margin: 2rem 0 1.8rem;
    }

    /* Card grid */
    .action-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1.2rem;
    }

    .admin-grid {
      grid-template-columns: repeat(2, 1fr);
    }

    .action-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 2rem 1.5rem;
      border-radius: 16px;
      text-decoration: none;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid rgba(255, 255, 255, 0.06);
      background: rgba(0, 0, 0, 0.2);
    }

    .action-card:hover {
      transform: translateY(-4px);
    }

    /* Color variants */
    .card-green .card-icon { color: #48bb78; }
    .card-green:hover {
      background: rgba(56, 161, 105, 0.15);
      border-color: rgba(56, 161, 105, 0.4);
      box-shadow: 0 0 24px rgba(56, 161, 105, 0.15);
    }

    .card-purple .card-icon { color: #b794f4; }
    .card-purple:hover {
      background: rgba(159, 122, 234, 0.15);
      border-color: rgba(159, 122, 234, 0.4);
      box-shadow: 0 0 24px rgba(159, 122, 234, 0.15);
    }

    .card-blue .card-icon { color: #63b3ed; }
    .card-blue:hover {
      background: rgba(66, 153, 225, 0.15);
      border-color: rgba(66, 153, 225, 0.4);
      box-shadow: 0 0 24px rgba(66, 153, 225, 0.15);
    }

    .card-teal .card-icon { color: #4fd1c5; }
    .card-teal:hover {
      background: rgba(56, 178, 172, 0.15);
      border-color: rgba(56, 178, 172, 0.4);
      box-shadow: 0 0 24px rgba(56, 178, 172, 0.15);
    }

    .card-amber .card-icon { color: #f6ad55; }
    .card-amber:hover {
      background: rgba(237, 137, 54, 0.15);
      border-color: rgba(237, 137, 54, 0.4);
      box-shadow: 0 0 24px rgba(237, 137, 54, 0.15);
    }

    .card-slate .card-icon { color: #a0aec0; }
    .card-slate:hover {
      background: rgba(160, 174, 192, 0.12);
      border-color: rgba(160, 174, 192, 0.35);
      box-shadow: 0 0 24px rgba(160, 174, 192, 0.1);
    }

    .card-icon {
      margin-bottom: 1rem;
      opacity: 0.9;
    }

    .action-card h3 {
      font-size: 1.25rem;
      color: #ffffff;
      margin-bottom: 0.4rem;
    }

    .action-card p {
      color: #a0aec0;
      font-size: 0.9rem;
      margin: 0;
      line-height: 1.4;
    }

    /* Responsive */
    @media (max-width: 640px) {
      .action-grid {
        grid-template-columns: 1fr;
      }
      .glass-panel {
        padding: 2rem 1.5rem;
      }
      .hero-title {
        font-size: 2.2rem;
      }
    }
  `]
})
export class HomeComponent {
  authService = inject(AuthService);
}