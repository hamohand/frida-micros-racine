import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ParametresService } from '../../../services/parametres.service';

@Component({
  selector: 'app-parametres',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="parametres-container">
      <h2>Paramètres</h2>

      <div *ngIf="chargement" class="info">Chargement...</div>

      <div *ngIf="!chargement" class="option">
        <label class="option-ligne">
          <input type="checkbox"
                 [(ngModel)]="verificationPhonetique"
                 (ngModelChange)="basculer($event)"
                 [disabled]="enregistrement" />
          <span class="option-titre">Vérification phonétique des noms</span>
        </label>
        <p class="option-aide">
          Compare le nom arabe lu sur le document à sa version latine (MRZ de la CNI ou du passeport,
          lecture NFC) et signale les écarts dans l'écran de revue.
        </p>
        <p class="option-aide">
          Inutile avec la lecture du QR code des extraits de naissance, dont le résultat est exact.
          À activer si les noms sont lus par OCR classique du texte.
        </p>
      </div>

      <div *ngIf="message" class="message" [class.erreur]="erreur">{{ message }}</div>
    </div>
  `,
  styles: [`
    .parametres-container { max-width: 760px; margin: 0 auto; padding: 2rem; color: var(--text-primary); }
    .option { margin-top: 1rem; padding: 1.25rem; border: 1px solid rgba(78, 204, 163, 0.25); border-radius: 8px; background: rgba(0, 0, 0, 0.2); }
    .option-ligne { display: flex; align-items: center; gap: 0.75rem; cursor: pointer; }
    .option-ligne input { width: 1.2rem; height: 1.2rem; cursor: pointer; }
    .option-titre { font-weight: 600; font-size: 1.05rem; }
    .option-aide { margin: 0.6rem 0 0 1.95rem; color: var(--text-secondary, #94a3b8); font-size: 0.9rem; line-height: 1.4; }
    .message { margin-top: 1rem; color: #4ecca3; }
    .message.erreur { color: #ff6b6b; }
  `]
})
export class ParametresComponent implements OnInit {
  verificationPhonetique = false;
  chargement = true;
  enregistrement = false;
  message = '';
  erreur = false;

  constructor(private parametresService: ParametresService) {}

  ngOnInit(): void {
    this.parametresService.lire().subscribe({
      next: (p) => {
        this.verificationPhonetique = p.verificationPhonetique;
        this.chargement = false;
      },
      error: () => {
        this.afficher('Impossible de charger les paramètres.', true);
        this.chargement = false;
      }
    });
  }

  basculer(active: boolean): void {
    this.enregistrement = true;
    this.parametresService.modifier({ verificationPhonetique: active }).subscribe({
      next: (p) => {
        this.verificationPhonetique = p.verificationPhonetique;
        this.enregistrement = false;
        this.afficher(p.verificationPhonetique
          ? 'Vérification phonétique activée.'
          : 'Vérification phonétique désactivée.', false);
      },
      error: () => {
        // La case revient à son état précédent : le réglage n'a pas été enregistré
        this.verificationPhonetique = !active;
        this.enregistrement = false;
        this.afficher("Impossible d'enregistrer le paramètre.", true);
      }
    });
  }

  private afficher(message: string, erreur: boolean): void {
    this.message = message;
    this.erreur = erreur;
  }
}
