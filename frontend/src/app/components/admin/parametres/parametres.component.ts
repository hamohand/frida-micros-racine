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
        <span class="option-titre">Adresse réseau locale du poste</span>
        <p class="option-aide">
          Pour que le scan d'une pièce d'identité par smartphone (bouton « Scanner via Mobile ») fonctionne :
          le téléphone, sur le même Wi-Fi que ce poste, ne peut pas le joindre via « localhost », qui ne désigne
          que lui-même une fois sur le réseau. Il faut donner l'adresse de ce poste sur le réseau local.
        </p>
        <p class="option-aide">
          Pour la trouver : sur ce poste, ouvrez une invite de commandes et tapez <code>ipconfig</code> —
          c'est la ligne « Adresse IPv4 » (ex. 192.168.1.50) de la carte Wi-Fi ou Ethernet active.
        </p>
        <div class="adresse-ligne">
          <input type="text"
                 class="adresse-champ"
                 [(ngModel)]="adresseReseauLocale"
                 [disabled]="enregistrementAdresse"
                 placeholder="ex. 192.168.1.50"
                 (keydown.enter)="enregistrerAdresseReseauLocale()" />
          <button class="btn-enregistrer" (click)="enregistrerAdresseReseauLocale()" [disabled]="enregistrementAdresse">
            Enregistrer
          </button>
        </div>
        <p class="option-aide" *ngIf="!adresseReseauLocale">
          ⚠️ Non configurée : le bouton « Scanner via Mobile » ne fonctionnera pas tant que cette adresse n'est pas renseignée.
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
    .option-aide code { background: rgba(255, 255, 255, 0.1); padding: 0.1rem 0.4rem; border-radius: 4px; }
    .adresse-ligne { display: flex; gap: 0.6rem; margin: 0.75rem 0 0 1.95rem; }
    .adresse-champ {
      flex: 1; max-width: 260px; padding: 0.5rem 0.75rem; border-radius: 6px;
      border: 1px solid rgba(255, 255, 255, 0.2); background: rgba(255, 255, 255, 0.05);
      color: var(--text-primary); font-size: 0.95rem;
    }
    .btn-enregistrer {
      padding: 0.5rem 1rem; border-radius: 6px; border: none; cursor: pointer;
      background: #4ecca3; color: #0f2027; font-weight: 600;
    }
    .btn-enregistrer:disabled { opacity: 0.6; cursor: not-allowed; }
    .message { margin-top: 1rem; color: #4ecca3; }
    .message.erreur { color: #ff6b6b; }
  `]
})
export class ParametresComponent implements OnInit {
  verificationPhonetique = false;
  adresseReseauLocale = '';
  chargement = true;
  enregistrement = false;
  enregistrementAdresse = false;
  message = '';
  erreur = false;

  constructor(private parametresService: ParametresService) {}

  ngOnInit(): void {
    this.parametresService.lire().subscribe({
      next: (p) => {
        this.verificationPhonetique = p.verificationPhonetique;
        this.adresseReseauLocale = p.adresseReseauLocale || '';
        this.chargement = false;
      },
      error: () => {
        this.afficher('Impossible de charger les paramètres.', true);
        this.chargement = false;
      }
    });
  }

  basculerVerificationPhonetique(active: boolean): void {
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

  enregistrerAdresseReseauLocale(): void {
    this.enregistrementAdresse = true;
    const valeur = this.adresseReseauLocale.trim();
    this.parametresService.modifier({ adresseReseauLocale: valeur }).subscribe({
      next: (p) => {
        this.adresseReseauLocale = p.adresseReseauLocale || '';
        this.enregistrementAdresse = false;
        this.afficher(this.adresseReseauLocale
          ? 'Adresse réseau enregistrée : ' + this.adresseReseauLocale
          : 'Adresse réseau effacée.', false);
      },
      error: (err) => {
        this.enregistrementAdresse = false;
        this.afficher(err?.error?.message || "Impossible d'enregistrer l'adresse.", true);
      }
    });
  }

  private afficher(message: string, erreur: boolean): void {
    this.message = message;
    this.erreur = erreur;
  }
}
