import { Injectable, signal } from '@angular/core';
import { UploadWindowState } from '../components/dossier/upload-windows/upload-window.interface';

/** Dossier en cours de création, pour pouvoir y revenir après avoir quitté le parcours. */
export interface DossierEnCours {
  libelle: string;
  /** Dernière étape visitée du parcours (URL complète, paramètres compris). */
  url: string;
  /** Renseigné dès que l'analyse a créé la fiche. */
  numFrida?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UploadStateService {
  private windowsState: Record<string, UploadWindowState> | null = null;
  private ocrMode: 'rapide' | 'approfondi' | 'batch' | null = null;

  readonly dossierEnCours = signal<DossierEnCours | null>(null);

  saveState(windows: Record<string, UploadWindowState>, mode: 'rapide' | 'approfondi' | 'batch') {
    this.windowsState = windows;
    this.ocrMode = mode;
  }

  getState() {
    return { windows: this.windowsState, ocrMode: this.ocrMode };
  }

  clearState() {
    this.windowsState = null;
    this.ocrMode = null;
    this.dossierEnCours.set(null);
  }

  demarrerDossier(libelle: string, url = '/upload') {
    this.dossierEnCours.set({ libelle, url });
  }

  lierFiche(numFrida: string) {
    this.dossierEnCours.update(d => d && { ...d, numFrida });
  }

  /**
   * Mémorise l'étape visitée si elle concerne le dossier en cours :
   * Documents dans tous les cas, Vérification et Héritiers seulement pour sa fiche
   * (pas pour un autre dossier ouvert depuis Batch ou Recherche).
   */
  noterEtape(page: string, url: string, numFrida?: string) {
    const d = this.dossierEnCours();
    if (!d) return;
    if (page === 'upload' || (numFrida && numFrida === d.numFrida)) {
      this.dossierEnCours.set({ ...d, url });
    }
  }

  /** Le parcours est terminé : la fiche du dossier en cours est affichée. */
  terminerSi(numFrida?: string) {
    const d = this.dossierEnCours();
    if (d?.numFrida && d.numFrida === numFrida) {
      this.clearState();
    }
  }
}
