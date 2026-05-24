import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface ConstitutionFiche {
  sexeDefunt: 'M' | 'F' | null;
  nbConjoints: number;
  pereVivant: boolean;
  mereVivante: boolean;
  grandPerePaternelVivant: boolean;
  nbFilles: number;
  nbGarcons: number;
  nbSoeurs: number;
  nbFreres: number;
  nbOncles: number;
  nbCousins: number;
}

@Injectable({
  providedIn: 'root'
})
export class ConstitutionService {
  private initialFiche: ConstitutionFiche = {
    sexeDefunt: 'M', // Valeur par défaut
    nbConjoints: 0,
    pereVivant: false,
    mereVivante: false,
    grandPerePaternelVivant: false,
    nbFilles: 0,
    nbGarcons: 0,
    nbSoeurs: 0,
    nbFreres: 0,
    nbOncles: 0,
    nbCousins: 0
  };

  private ficheSubject = new BehaviorSubject<ConstitutionFiche>({ ...this.initialFiche });

  constructor() {}

  /**
   * Retourne l'état de la fiche sous forme d'Observable (pour s'y abonner dans les composants)
   */
  get fiche$(): Observable<ConstitutionFiche> {
    return this.ficheSubject.asObservable();
  }

  /**
   * Retourne la valeur actuelle de la fiche (snapshot)
   */
  get currentFiche(): ConstitutionFiche {
    return this.ficheSubject.getValue();
  }

  /**
   * Met à jour une ou plusieurs valeurs de la fiche
   * @param partialFiche Un objet contenant les champs à modifier
   */
  updateFiche(partialFiche: Partial<ConstitutionFiche>) {
    this.ficheSubject.next({ ...this.currentFiche, ...partialFiche });
  }

  /**
   * Remet la fiche à zéro
   */
  resetFiche() {
    this.ficheSubject.next({ ...this.initialFiche });
  }
}
