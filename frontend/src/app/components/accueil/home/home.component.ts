import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="home-wrapper">
      <section class="hero-section glass-panel">
        <div class="hero-content">
          <h1 class="hero-title">Ustadh-a</h1>
          <p class="hero-subtitle">
            Système Avancé de Gestion Notariale<br />
            <span class="highlight">Intelligence Artificielle & Traitement de Fridas</span>
          </p>
          
          <div class="action-grid">
            <a routerLink="/create" class="action-card primary-card">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="40" viewBox="0 -960 960 960" width="40" fill="currentColor"><path d="M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160Zm40 200q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z"/></svg>
              </div>
              <h3>Nouveau Dossier</h3>
              <p>Ouvrir une nouvelle instruction Frida</p>
            </a>

            <a routerLink="/list" class="action-card secondary-card">
              <div class="card-icon">
                <svg xmlns="http://www.w3.org/2000/svg" height="40" viewBox="0 -960 960 960" width="40" fill="currentColor"><path d="M160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800h240l80 80h320q33 0 56.5 23.5T880-640v400q0 33-23.5 56.5T800-160H160Zm0-80h640v-400H447l-80-80H160v480Zm0 0v-480 480Z"/></svg>
              </div>
              <h3>Consulter l'Archive</h3>
              <p>Rechercher et afficher les actes existants</p>
            </a>
          </div>
          
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
      animation: fadeIn 1.2s ease-out;
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
      padding: 4rem;
      max-width: 900px;
      width: 100%;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
      text-align: center;
    }

    .hero-title {
      font-size: 3.5rem;
      font-weight: 700;
      color: #ffffff;
      margin-bottom: 1rem;
      letter-spacing: -0.02em;
    }

    .hero-subtitle {
      font-size: 1.25rem;
      color: #a0aec0;
      line-height: 1.6;
      margin-bottom: 3rem;
    }

    .highlight {
      color: #38a169;
      font-weight: 600;
    }

    .action-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 2rem;
      margin-top: 2rem;
    }

    .action-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 2.5rem;
      border-radius: 16px;
      text-decoration: none;
      transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
      border: 1px solid rgba(255, 255, 255, 0.05);
      background: rgba(0, 0, 0, 0.2);
    }

    .action-card:hover {
      transform: translateY(-5px);
    }

    .primary-card:hover {
      background: rgba(56, 161, 105, 0.2);
      border-color: rgba(56, 161, 105, 0.5);
      box-shadow: 0 0 20px rgba(56, 161, 105, 0.2);
    }

    .secondary-card:hover {
      background: rgba(66, 153, 225, 0.15);
      border-color: rgba(66, 153, 225, 0.4);
      box-shadow: 0 0 20px rgba(66, 153, 225, 0.15);
    }

    .card-icon {
      margin-bottom: 1.5rem;
      color: #ffffff;
      opacity: 0.9;
    }

    .primary-card .card-icon {
      color: #48bb78;
    }

    .secondary-card .card-icon {
      color: #63b3ed;
    }

    .action-card h3 {
      font-size: 1.5rem;
      color: #ffffff;
      margin-bottom: 0.5rem;
    }

    .action-card p {
      color: #a0aec0;
      font-size: 1rem;
      margin: 0;
    }
  `]
})
export class HomeComponent { }