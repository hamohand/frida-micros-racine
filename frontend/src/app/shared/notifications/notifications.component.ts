import { Component, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-pile" aria-live="polite">
      <div *ngFor="let t of notif.toasts()" class="toast toast-{{ t.type }}" role="status">
        <span class="toast-icone">{{ icones[t.type] }}</span>
        <span class="toast-message">{{ t.message }}</span>
        <button type="button" class="toast-fermer" (click)="notif.fermer(t.id)" aria-label="Fermer">×</button>
      </div>
    </div>

    <div *ngIf="notif.confirmation() as c" class="confirm-fond" (click)="c.resoudre(false)">
      <div class="confirm-boite" role="alertdialog" aria-modal="true" (click)="$event.stopPropagation()">
        <h3 class="confirm-titre">{{ c.titre }}</h3>
        <p class="confirm-message">{{ c.message }}</p>
        <div class="confirm-actions">
          <button type="button" class="btn btn-secondary" (click)="c.resoudre(false)">Annuler</button>
          <button type="button" class="btn" [class.btn-primary]="!c.danger" [class.btn-danger]="c.danger"
                  (click)="c.resoudre(true)" autofocus>{{ c.libelleConfirmer }}</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .toast-pile {
      position: fixed;
      top: calc(var(--nav-height) + var(--space-3));
      right: var(--space-4);
      z-index: 2000;
      display: flex;
      flex-direction: column;
      gap: var(--space-2);
      max-width: min(420px, calc(100vw - 2 * var(--space-4)));
    }
    .toast {
      display: flex;
      align-items: flex-start;
      gap: var(--space-2);
      padding: var(--space-3) var(--space-4);
      background: var(--surface-2);
      border: 1px solid var(--border-subtle);
      border-left: 4px solid var(--toast-couleur);
      border-radius: var(--radius-md);
      box-shadow: var(--shadow-2);
      color: var(--text-1);
      animation: toast-entree var(--duree-rapide) ease-out;
    }
    .toast-succes { --toast-couleur: var(--success); }
    .toast-erreur { --toast-couleur: var(--danger); }
    .toast-info { --toast-couleur: var(--info); }
    .toast-avertissement { --toast-couleur: var(--warning); }
    .toast-message { flex: 1; white-space: pre-line; line-height: 1.4; }
    .toast-fermer {
      background: none;
      border: none;
      color: var(--text-muted);
      font-size: 1.25rem;
      line-height: 1;
      cursor: pointer;
    }
    .toast-fermer:hover { color: var(--text-1); }

    .confirm-fond {
      position: fixed;
      inset: 0;
      z-index: 2100;
      background: rgba(0, 0, 0, 0.55);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-4);
      animation: fondu var(--duree-rapide) ease-out;
    }
    .confirm-boite {
      width: min(480px, 100%);
      background: var(--surface-2);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      box-shadow: var(--shadow-2);
      padding: var(--space-5);
    }
    .confirm-titre { color: var(--text-1); font-size: 1.15rem; }
    .confirm-message { color: var(--text-2); white-space: pre-line; margin: var(--space-3) 0 var(--space-5); }
    .confirm-actions { display: flex; justify-content: flex-end; gap: var(--space-2); }
    .confirm-actions .btn { padding: var(--space-2) var(--space-4); }

    @keyframes toast-entree { from { opacity: 0; transform: translateX(16px); } }
    @keyframes fondu { from { opacity: 0; } }
  `]
})
export class NotificationsComponent {
  notif = inject(NotificationService);

  icones: Record<string, string> = { succes: '✓', erreur: '✕', info: 'ℹ', avertissement: '⚠' };

  @HostListener('document:keydown.escape')
  annulerParEchap() {
    this.notif.confirmation()?.resoudre(false);
  }
}
