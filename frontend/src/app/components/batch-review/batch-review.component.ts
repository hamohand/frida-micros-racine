import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FridaService } from '../../services/frida.service';
import { forkJoin } from 'rxjs';

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
              <h2>Suivi du mode Batch</h2>
              <p class="subtitle">
                Supervisez vos dossiers en attente de traitement, de correction OCR ou de validation familiale.
              </p>
            </div>
          </div>
        </div>

        <!-- Chargement -->
        <div *ngIf="isLoading" class="loading-state">
          <span class="spinner"></span> Chargement des dossiers...
        </div>

        <div *ngIf="!isLoading">
          <!-- 1. Travaux mis en batch -->
          <div class="section-container">
            <h3><span class="section-icon">⏳</span> Travaux mis en batch <span class="badge-count" *ngIf="pendingFolders.length > 0">{{ pendingFolders.length }}</span></h3>
            <div *ngIf="pendingFolders.length === 0" class="empty-state">
              <p>Aucun dossier en attente de traitement OCR.</p>
            </div>
            <div class="table-container" *ngIf="pendingFolders.length > 0">
              <table class="table">
                <thead>
                  <tr>
                    <th>Nom du dossier</th>
                    <th>État</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let folder of pendingFolders">
                    <td class="font-mono"><strong>{{ folder }}</strong></td>
                    <td><span class="badge-ocr warning">⏳ En attente de traitement batch</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 2. Révisions -->
          <div class="section-container">
            <h3><span class="section-icon">⚠️</span> Révisions <span class="badge-count" *ngIf="revisions.length > 0">{{ revisions.length }}</span></h3>
            <div *ngIf="revisions.length === 0" class="empty-state">
              <p>Aucune révision OCR requise pour le moment.</p>
            </div>
            <div class="table-container" *ngIf="revisions.length > 0">
              <table class="table">
                <thead>
                  <tr>
                    <th>N° Dossier</th>
                    <th>Défunt</th>
                    <th>Date</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let d of revisions" class="row-warning">
                    <td class="font-mono"><strong>{{ d.numFrida }}</strong></td>
                    <td>{{ d.nom }} {{ d.prenom }}</td>
                    <td><span class="badge-date">{{ d.dateCreation }}</span></td>
                    <td>
                      <button class="btn-reviser" (click)="reviser(d)">Réviser OCR</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- 3. Validations -->
          <div class="section-container">
            <h3><span class="section-icon">✅</span> Validations <span class="badge-count" *ngIf="validations.length > 0">{{ validations.length }}</span></h3>
            <div *ngIf="validations.length === 0" class="empty-state">
              <p>Aucun dossier en attente de validation familiale.</p>
            </div>
            <div class="table-container" *ngIf="validations.length > 0">
              <table class="table">
                <thead>
                  <tr>
                    <th>N° Dossier</th>
                    <th>Défunt</th>
                    <th>Date</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let d of validations">
                    <td class="font-mono"><strong>{{ d.numFrida }}</strong></td>
                    <td>{{ d.nom }} {{ d.prenom }}</td>
                    <td><span class="badge-date">{{ d.dateCreation }}</span></td>
                    <td>
                      <button class="btn-valider" (click)="reviser(d)">Valider la Famille</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
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
    
    .section-container {
      margin-bottom: 2.5rem;
      background: rgba(0,0,0,0.15);
      border-radius: 12px;
      padding: 1.5rem;
      border: 1px solid rgba(255,255,255,0.05);
    }

    .section-container h3 {
      color: #e2e8f0;
      margin-top: 0;
      margin-bottom: 1.2rem;
      font-size: 1.3rem;
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .badge-count {
      background: rgba(255, 184, 77, 0.15);
      color: #ffb84d;
      border: 1px solid rgba(255, 184, 77, 0.4);
      border-radius: 50%;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      font-weight: 700;
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 1.5rem;
      color: rgba(255,255,255,0.4);
    }

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
      text-align: left;
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

    .btn-reviser {
      display: inline-flex;
      align-items: center;
      background: rgba(255,184,77,0.12);
      color: #ffb84d;
      border: 1px solid rgba(255,184,77,0.3);
      padding: 7px 14px;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.88rem;
      font-family: inherit;
      transition: all 0.2s;
    }

    .btn-reviser:hover {
      background: rgba(255,184,77,0.25);
      color: #fff;
    }

    .btn-valider {
      display: inline-flex;
      align-items: center;
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
    
    .btn-valider:hover {
      background: rgba(78,204,163,0.25);
      color: #fff;
    }
  `]
})
export class BatchReviewComponent implements OnInit {
  pendingFolders: string[] = [];
  revisions: DossierEnAttente[] = [];
  validations: DossierEnAttente[] = [];
  isLoading = true;

  constructor(private fridaService: FridaService, private router: Router) {}

  ngOnInit() {
    forkJoin({
      pending: this.fridaService.lancerApi('/api/folders/pending-batch'),
      enAttente: this.fridaService.lancerApi('/api/frida/batch-en-attente')
    }).subscribe({
      next: (data) => {
        this.pendingFolders = Array.isArray(data.pending) ? data.pending : [];
        const dossiers = Array.isArray(data.enAttente) ? data.enAttente : [];
        this.revisions = dossiers.filter(d => d.requiresCorrection);
        this.validations = dossiers.filter(d => !d.requiresCorrection);
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  reviser(d: DossierEnAttente) {
    if (d.requiresCorrection) {
      this.router.navigate(['/correction'], { queryParams: { numFrida: d.numFrida } });
    } else {
      this.router.navigate(['/review-family'], { queryParams: { numFrida: d.numFrida } });
    }
  }
}
