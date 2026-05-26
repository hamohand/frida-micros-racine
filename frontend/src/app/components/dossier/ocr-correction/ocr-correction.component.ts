import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LireaiEcrirebdService } from '../../../services/lireai-ecrirebd.service';

interface ChampSuspect {
  personneId: string | null;
  personneLabel: string;
  personneNom: string;
  personnePrenom: string;
  personneNin: string;
  champ: string;
  champLabel: string;
  valeurOcr: string;
  confiance: number;
  numParente: string | null;
  // Valeur corrigée par l'utilisateur (initialisée avec valeurOcr)
  valeurCorrigee: string;
}

@Component({
  selector: 'app-ocr-correction',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="correction-container">
      <div class="correction-header">
        <div class="header-icon">⚠️</div>
        <h2>Vérification requise</h2>
        <p class="subtitle">
          L'IA a lu <strong>{{ champs.length }} champ(s)</strong> avec un niveau de confiance inférieur à 75%.
          <br>Veuillez vérifier et corriger si nécessaire avant de continuer.
        </p>
      </div>

      <div *ngIf="isLoading" class="loading-state">
        <span class="spinner"></span> Chargement des champs suspects...
      </div>

      <div *ngIf="!isLoading && champs.length === 0" class="empty-state">
        <span class="check-icon">✅</span>
        <p>Tous les champs ont été reconnus avec une confiance suffisante.</p>
        <button class="btn btn-primary" (click)="continuer()">Continuer vers la validation →</button>
      </div>

      <div class="champs-list" *ngIf="!isLoading && champs.length > 0">
        <div class="champ-card" *ngFor="let c of champs; let i = index" [class.corrected]="c.valeurCorrigee !== c.valeurOcr">
          <!-- En-tête de la carte -->
          <div class="champ-card-header">
            <div class="personne-info">
              <span class="personne-badge">{{ c.personneLabel }}</span>
              <span class="personne-identite">
                {{ c.personneNom }} {{ c.personnePrenom }}
                <span class="personne-nin" *ngIf="c.personneNin">— NIN : {{ c.personneNin }}</span>
              </span>
            </div>
            <span class="champ-name">{{ c.champLabel }}</span>
          </div>

          <!-- Barre de confiance -->
          <div class="confidence-bar-wrapper">
            <span class="confidence-label" [class.low]="c.confiance < 0.5" [class.medium]="c.confiance >= 0.5 && c.confiance < 0.75">
              Confiance : {{ (c.confiance * 100).toFixed(0) }}%
            </span>
            <div class="confidence-bar">
              <div class="confidence-fill"
                [style.width]="(c.confiance * 100) + '%'"
                [class.fill-low]="c.confiance < 0.5"
                [class.fill-medium]="c.confiance >= 0.5 && c.confiance < 0.75">
              </div>
            </div>
          </div>

          <!-- Valeur OCR et correction -->
          <div class="correction-row">
            <div class="ocr-value">
              <label>Lu par l'IA :</label>
              <span class="ocr-text" [class.suspicious]="true">{{ c.valeurOcr || '(vide)' }}</span>
            </div>
            <div class="correction-input">
              <label>Correction :</label>
              <input
                type="text"
                [(ngModel)]="c.valeurCorrigee"
                [placeholder]="c.valeurOcr || 'Saisir la valeur correcte'"
                [class.modified]="c.valeurCorrigee !== c.valeurOcr"
              >
            </div>
          </div>
        </div>
      </div>

      <div class="actions" *ngIf="!isLoading && champs.length > 0">
        <button class="btn btn-secondary" (click)="toutAccepter()" [disabled]="isSubmitting">
          Tout accepter tel quel
        </button>
        <button class="btn btn-secondary" (click)="mettreEnAttente()" [disabled]="isSubmitting" style="border-color: #ecc94b; color: #ecc94b;">
          ⏳ Mettre en attente
        </button>
        <button class="btn btn-primary" (click)="validerCorrections()" [disabled]="isSubmitting">
          <span *ngIf="!isSubmitting">✅ Valider les corrections</span>
          <span *ngIf="isSubmitting"><span class="spinner"></span> Sauvegarde...</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    @import url('https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;700&display=swap');

    :host { font-family: 'Outfit', sans-serif; }

    .correction-container {
      max-width: 820px;
      margin: 0 auto;
      padding: 2rem;
      color: var(--text-primary);
    }

    .correction-header {
      text-align: center;
      margin-bottom: 2.5rem;
    }

    .header-icon { font-size: 3rem; margin-bottom: 0.5rem; }

    h2 {
      color: #ffb84d;
      font-size: 1.8rem;
      font-weight: 700;
      margin: 0 0 0.5rem;
      text-transform: uppercase;
      letter-spacing: 1px;
    }

    .subtitle {
      color: var(--text-secondary);
      font-size: 1rem;
      line-height: 1.6;
    }

    .champs-list {
      display: flex;
      flex-direction: column;
      gap: 1.2rem;
      margin-bottom: 2rem;
    }

    .champ-card {
      background: rgba(255, 184, 77, 0.05);
      border: 1px solid rgba(255, 184, 77, 0.3);
      border-radius: 12px;
      padding: 1.25rem 1.5rem;
      transition: border-color 0.2s, background 0.2s;
    }

    .champ-card.corrected {
      background: rgba(78, 204, 163, 0.06);
      border-color: rgba(78, 204, 163, 0.4);
    }

    .champ-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.75rem;
      margin-bottom: 1rem;
    }

    .personne-info {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      flex-wrap: wrap;
    }

    .personne-identite {
      font-weight: 600;
      font-size: 0.95rem;
      color: var(--text-primary);
    }

    .personne-nin {
      font-weight: 400;
      font-size: 0.82rem;
      color: var(--text-secondary);
    }

    .personne-badge {
      background: rgba(255, 184, 77, 0.15);
      color: #ffb84d;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 0.82rem;
      font-weight: 600;
    }

    .champ-card.corrected .personne-badge {
      background: rgba(78, 204, 163, 0.15);
      color: var(--accent-color);
    }

    .champ-name {
      font-weight: 600;
      font-size: 1rem;
      color: var(--text-primary);
    }

    .confidence-bar-wrapper { margin-bottom: 1rem; }

    .confidence-label {
      font-size: 0.82rem;
      font-weight: 600;
      margin-bottom: 4px;
      display: block;
      color: #ffb84d;
    }

    .confidence-label.low { color: #ff6b6b; }
    .confidence-label.medium { color: #ffb84d; }

    .confidence-bar {
      height: 6px;
      background: rgba(255,255,255,0.1);
      border-radius: 3px;
      overflow: hidden;
    }

    .confidence-fill {
      height: 100%;
      background: var(--accent-color);
      border-radius: 3px;
      transition: width 0.4s;
    }

    .confidence-fill.fill-medium { background: #ffb84d; }
    .confidence-fill.fill-low { background: #ff6b6b; }

    .correction-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;
    }

    .ocr-value label, .correction-input label {
      display: block;
      font-size: 0.78rem;
      color: var(--text-secondary);
      margin-bottom: 4px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .ocr-text {
      display: block;
      padding: 8px 12px;
      background: rgba(255,107,107,0.1);
      border: 1px solid rgba(255,107,107,0.3);
      border-radius: 6px;
      font-size: 0.95rem;
      color: #ff9f9f;
      font-style: italic;
    }

    .correction-input input {
      width: 100%;
      padding: 8px 12px;
      background: rgba(0,0,0,0.25);
      border: 1px solid rgba(78, 204, 163, 0.3);
      border-radius: 6px;
      color: var(--text-primary);
      font-size: 0.95rem;
      font-family: inherit;
      transition: border-color 0.2s;
      box-sizing: border-box;
    }

    .correction-input input:focus {
      outline: none;
      border-color: var(--accent-color);
      box-shadow: 0 0 0 2px rgba(78, 204, 163, 0.2);
    }

    .correction-input input.modified {
      border-color: var(--accent-color);
      background: rgba(78, 204, 163, 0.07);
    }

    .actions {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding-top: 1.5rem;
      border-top: 1px solid rgba(78, 204, 163, 0.2);
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 3rem;
      color: var(--text-secondary);
    }

    .empty-state .check-icon { font-size: 3rem; display: block; margin-bottom: 1rem; }

    .spinner {
      display: inline-block;
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-right: 6px;
      vertical-align: middle;
    }

    @keyframes spin { to { transform: rotate(360deg); } }

    .btn {
      padding: 10px 24px;
      border-radius: var(--border-radius, 8px);
      border: none;
      cursor: pointer;
      font-family: inherit;
      font-weight: 600;
      font-size: 0.95rem;
      transition: opacity 0.2s, background 0.2s;
    }
    .btn:disabled { opacity: 0.5; cursor: not-allowed; }
    .btn-primary { background: var(--accent-color, #4ecca3); color: #0a1f0f; }
    .btn-primary:hover:not(:disabled) { opacity: 0.85; }
    .btn-secondary { background: transparent; border: 1px solid rgba(255,255,255,0.3); color: var(--text-primary); }
    .btn-secondary:hover:not(:disabled) { background: rgba(255,255,255,0.05); }
  `]
})
export class OcrCorrectionComponent implements OnInit {
  numFrida = '';
  champs: ChampSuspect[] = [];
  isLoading = true;
  isSubmitting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private lireaiService: LireaiEcrirebdService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.numFrida = params['numFrida'];
      if (this.numFrida) {
        this.chargerChampsSuspects();
      } else {
        this.isLoading = false;
      }
    });
  }

  chargerChampsSuspects() {
    this.lireaiService.getChampsSuspects(this.numFrida).subscribe({
      next: (data) => {
        this.champs = data.map(c => ({ ...c, valeurCorrigee: c.valeurOcr || '' }));
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement champs suspects', err);
        this.isLoading = false;
        // En cas d'erreur, on passe directement à la validation
        this.continuer();
      }
    });
  }

  validerCorrections() {
    this.isSubmitting = true;
    const corrections = this.champs
      .filter(c => c.valeurCorrigee !== c.valeurOcr) // Seulement les champs modifiés
      .map(c => ({
        personneId: c.personneId,
        champ: c.champ,
        valeur: c.valeurCorrigee
      }));

    if (corrections.length === 0) {
      // Rien à corriger, passer directement
      this.continuer();
      return;
    }

    this.lireaiService.appliquerCorrections(this.numFrida, corrections).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.continuer();
      },
      error: (err) => {
        console.error('Erreur application corrections', err);
        this.isSubmitting = false;
        alert('Une erreur est survenue lors de la sauvegarde des corrections.');
      }
    });
  }

  mettreEnAttente() {
    this.isSubmitting = true;
    const corrections = this.champs
      .filter(c => c.valeurCorrigee !== c.valeurOcr)
      .map(c => ({
        personneId: c.personneId,
        champ: c.champ,
        valeur: c.valeurCorrigee
      }));

    // Même sans correction, on veut appeler le backend pour changer le statut à BROUILLON

    this.lireaiService.mettreEnAttenteOcr(this.numFrida, corrections).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/']); // Retour à la liste
      },
      error: (err) => {
        console.error('Erreur mise en attente', err);
        this.isSubmitting = false;
        alert('Une erreur est survenue lors de la mise en attente.');
      }
    });
  }

  toutAccepter() {
    // Passer directement à la validation sans rien changer
    this.continuer();
  }

  continuer() {
    this.router.navigate(['/review-family'], { queryParams: { numFrida: this.numFrida } });
  }
}
