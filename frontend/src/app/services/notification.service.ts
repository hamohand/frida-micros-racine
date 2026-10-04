import { Injectable, signal } from '@angular/core';

export type ToastType = 'succes' | 'erreur' | 'info' | 'avertissement';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
}

export interface DemandeConfirmation {
  titre: string;
  message: string;
  libelleConfirmer: string;
  danger: boolean;
  resoudre: (ok: boolean) => void;
}

/**
 * Remplace alert() et confirm() natifs : messages non bloquants (toasts)
 * et boîte de confirmation stylée, affichés par NotificationsComponent.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly toasts = signal<Toast[]>([]);
  readonly confirmation = signal<DemandeConfirmation | null>(null);

  private prochainId = 1;

  succes(message: string) { this.afficher('succes', message, 4000); }
  info(message: string) { this.afficher('info', message, 5000); }
  avertissement(message: string) { this.afficher('avertissement', message, 7000); }
  erreur(message: string) { this.afficher('erreur', message, 8000); }

  fermer(id: number) {
    this.toasts.update(liste => liste.filter(t => t.id !== id));
  }

  /** Résout à true si l'utilisateur confirme, false s'il annule ou ferme. */
  confirmer(message: string, options: { titre?: string; libelleConfirmer?: string; danger?: boolean } = {}): Promise<boolean> {
    // Une confirmation déjà ouverte est considérée comme annulée
    this.confirmation()?.resoudre(false);
    return new Promise(resolve => {
      this.confirmation.set({
        titre: options.titre ?? 'Confirmation',
        message,
        libelleConfirmer: options.libelleConfirmer ?? 'Confirmer',
        danger: options.danger ?? false,
        resoudre: (ok: boolean) => {
          this.confirmation.set(null);
          resolve(ok);
        }
      });
    });
  }

  private afficher(type: ToastType, message: string, duree: number) {
    const id = this.prochainId++;
    this.toasts.update(liste => [...liste, { id, type, message }]);
    setTimeout(() => this.fermer(id), duree);
  }
}
