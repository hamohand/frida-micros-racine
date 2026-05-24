import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FridaService } from '../../services/frida.service';

interface DossierEnAttente {
  numFrida: string;
  dateCreation: string;
  nom: string;
  prenom: string;
  requiresCorrection: boolean;
}

@Component({
  selector: 'app-batch-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="batch-wrapper">
      <div class="glass-panel">

        <!-- En-tête -->
        <div class="header-section">
          <div class="header-left">
            <span class="header-icon">🌙</span>
            <div>
              <h2>Révision des dossiers batch</h2>
              <p class="subtitle">
                {{ dossiers.length }} dossier(s) traités automatiquement en attente de votre validation.
              </p>
            </div>
          </div>
          <span class="badge-count" *ngIf="dossiers.length > 0">{{ dossiers.length }}</span>
        </div>

        <!-- Chargement -->
        <div *ngIf="isLoading" class="loading-state">
          <span class="spinner"></span> Chargement des dossiers...
        </div>

        <!-- Liste vide -->
        <div *ngIf="!isLoading && dossiers.length === 0" class="empty-state">
          <span class="empty-icon">✅</span>
          <p>Aucun dossier en attente de révision.</p>
          <p class="empty-sub">Le traitement batch déposera ici les dossiers traités automatiquement.</p>
        </div>

        <!-- Tableau -->
        <div class="table-container" *ngIf="!isLoading && dossiers.length > 0">
          <table class="table">
            <thead>
              <tr>
                <th>N° Dossier</th>
                <th>Défunt</th>
                <th>Date de création</th>
                <th>État OCR</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let d of dossiers" [class.row-warning]="d.requiresCorrection">
                <td class="font-mono"><strong>{{ d.numFrida }}</strong></td>
                <td>{{ d.nom }} {{ d.prenom }}</td>
                <td><span class="badge-date">{{ d.dateCreation }}</span></td>
                <td>
                  <span *ngIf="d.requiresCorrection" class="badge-ocr warning">
                    ⚠️ Champs à vérifier
                  </span>
                  <span *ngIf="!d.requiresCorrection" class="badge-ocr ok">
                    ✅ OCR fiable
                  </span>
                </td>
                <td>
                  <button class="btn-reviser" (click)="reviser(d)">
                    <svg xmlns="http://www.w3.org/2000/svg" height="18" viewBox="0 -960 960 960" width="18" fill="currentColor">
                      <path d="M200-200h57l391-391-57-57-391 391v57Zm-80 80v-170l528-527q12-11 26.5-17t30.5-6q16 0 31 6t26 18l55 56q11 11 17 25.5t6 30.5q0 16-6 30.5t-17 25.5L290-120H120Zm640-584-56-56 56 56Zm-141 85-28-29 57 57-29-28Z"/>
                    </svg>
                    Réviser
                  </button>
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

    .batch-wrapper {
      min-height: calc(100vh - 80px);
      padding: 3rem 2rem;
      background: #0f1c15;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      animation: fadeIn 0.5s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to   { opacity: 1; transform: translateY(0); }
    }

    .glass-panel {
      background: rgba(255,255,255,0.03);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255,255,255,0.1);
      border-radius: 16px;
      padding: 2rem;
      width: 100%;
      max-width: 1100px;
      box-shadow: 0 15px 35px rgba(0,0,0,0.4);
    }

    .header-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .header-icon { font-size: 2.5rem; }

    h2 {
      color: #fff;
      margin: 0 0 0.2rem;
      font-size: 1.8rem;
    }

    .subtitle {
      color: rgba(255,255,255,0.5);
      margin: 0;
      font-size: 0.95rem;
    }

    .badge-count {
      background: rgba(255, 184, 77, 0.15);
      color: #ffb84d;
      border: 1px solid rgba(255, 184, 77, 0.4);
      border-radius: 50%;
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
      font-weight: 700;
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 3rem;
      color: rgba(255,255,255,0.4);
    }

    .empty-icon { font-size: 3rem; display: block; margin-bottom: 1rem; }
    .empty-sub { font-size: 0.85rem; opacity: 0.6; }

    .spinner {
      display: inline-block;
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.2);
      border-top-color: #4ecca3;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-right: 8px;
      vertical-align: middle;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .table-container { overflow-x: auto; }

    .table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 0;
    }

    .table th {
      padding: 1rem;
      color: #4ecca3;
      background: rgba(0,0,0,0.2);
      font-weight: 600;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }

    .table td {
      padding: 0.9rem 1rem;
      color: #e2e8f0;
      border-bottom: 1px solid rgba(255,255,255,0.05);
      vertical-align: middle;
    }

    .table tbody tr { transition: background 0.2s; }
    .table tbody tr:hover { background: rgba(255,255,255,0.04); }
    .table tbody tr.row-warning { background: rgba(255,184,77,0.04); }

    .font-mono {
      font-family: monospace;
      letter-spacing: 1px;
      color: #63b3ed;
    }

    .badge-date {
      background: rgba(78,204,163,0.1);
      color: #4ecca3;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 0.85rem;
    }

    .badge-ocr {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 3px 10px;
      border-radius: 6px;
      font-size: 0.82rem;
      font-weight: 600;
    }

    .badge-ocr.warning {
      background: rgba(255,184,77,0.12);
      color: #ffb84d;
      border: 1px solid rgba(255,184,77,0.3);
    }

    .badge-ocr.ok {
      background: rgba(78,204,163,0.1);
      color: #4ecca3;
      border: 1px solid rgba(78,204,163,0.25);
    }

    .btn-reviser {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(78,204,163,0.12);
      color: #4ecca3;
      border: 1px solid rgba(78,204,163,0.3);
      padding: 7px 14px;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.88rem;
      font-family: inherit;
      transition: all 0.2s;
    }

    .btn-reviser:hover {
      background: rgba(78,204,163,0.25);
      color: #fff;
    }
  `]
})
export class BatchReviewComponent implements OnInit {
  dossiers: DossierEnAttente[] = [];
  isLoading = true;

  constructor(private fridaService: FridaService, private router: Router) {}

  ngOnInit() {
    this.fridaService.lancerApi('/api/frida/batch-en-attente').subscribe({
      next: (data) => {
        this.dossiers = Array.isArray(data) ? data : [];
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  reviser(d: DossierEnAttente) {
    // Si l'OCR a détecté des champs suspects → passer par la fiche de correction
    // Sinon → aller directement à la fiche de validation familiale
    if (d.requiresCorrection) {
      this.router.navigate(['/correction'], { queryParams: { numFrida: d.numFrida } });
    } else {
      this.router.navigate(['/review-family'], { queryParams: { numFrida: d.numFrida } });
    }
  }
}
