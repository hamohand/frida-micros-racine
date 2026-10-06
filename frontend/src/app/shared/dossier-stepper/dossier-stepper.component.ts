import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BilabelComponent } from '../bilabel/bilabel.component';

/** Étapes du parcours de création d'un dossier, dans l'ordre. */
export const ETAPES_DOSSIER = [
  { route: 'create', libelle: 'Défunt', arabe: 'المتوفى' },
  { route: 'upload', libelle: 'Documents', arabe: 'الوثائق' },
  { route: 'correction', libelle: 'Vérification', arabe: 'التحقق' },
  { route: 'review-family', libelle: 'Héritiers', arabe: 'الورثة' },
  { route: 'frida', libelle: 'Fiche', arabe: 'البطاقة' },
];

@Component({
  selector: 'app-dossier-stepper',
  standalone: true,
  imports: [CommonModule, BilabelComponent],
  template: `
    <ol class="stepper" aria-label="Étapes du dossier">
      <li *ngFor="let e of etapes; let i = index"
          class="etape"
          [class.faite]="i < indexActif"
          [class.active]="i === indexActif"
          [attr.aria-current]="i === indexActif ? 'step' : null">
        <span class="pastille">{{ i < indexActif ? '✓' : i + 1 }}</span>
        <span class="libelle"><app-bilabel [fr]="e.libelle" [ar]="e.arabe" /></span>
      </li>
    </ol>
  `,
  styles: [`
    .stepper {
      list-style: none;
      display: flex;
      align-items: center;
      gap: var(--space-2);
      max-width: 860px;
      margin: 0 auto;
      padding: var(--space-4) var(--space-4) 0;
    }
    .etape {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      flex: 1;
      color: var(--text-muted);
      font-size: 0.9rem;
      white-space: nowrap;
    }
    .etape:not(:last-child)::after {
      content: '';
      flex: 1;
      height: 2px;
      min-width: var(--space-4);
      background: var(--border-subtle);
      border-radius: 1px;
    }
    .etape:last-child { flex: 0; }
    .etape.faite::after { background: var(--primary); }
    .pastille {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      flex-shrink: 0;
      border-radius: 50%;
      border: 2px solid var(--border-subtle);
      font-weight: 600;
      font-size: 0.85rem;
    }
    .faite { color: var(--text-2); }
    .faite .pastille { background: var(--primary-soft); border-color: var(--primary); color: var(--primary); }
    .active { color: var(--text-1); font-weight: 600; }
    .active .pastille { background: var(--primary); border-color: var(--primary); color: var(--surface-0); }

    @media (max-width: 700px) {
      .etape:not(.active) .libelle { display: none; }
    }
  `]
})
export class DossierStepperComponent {
  @Input({ required: true }) etapeActive!: string;

  etapes = ETAPES_DOSSIER;

  get indexActif(): number {
    return this.etapes.findIndex(e => e.route === this.etapeActive);
  }
}
