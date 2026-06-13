import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, takeUntil, switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { SimulateurService, HeritageResponse } from '../../services/simulateur.service';

@Component({
  selector: 'app-simulateur',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './simulateur.component.html',
  styleUrls: ['./simulateur.component.css']
})
export class SimulateurComponent implements OnInit, OnDestroy {
  simForm!: FormGroup;
  resultats: HeritageResponse | null = null;
  erreur: string | null = null;
  enChargement = false;
  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private simulateurService: SimulateurService
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.setupAutoCalculate();
    // Déclencher un premier calcul
    this.calculerParts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initForm(): void {
    this.simForm = this.fb.group({
      sexeDefunt: ['M', Validators.required],
      nbConjoints: [0, [Validators.min(0), Validators.max(4)]],
      pereVivant: [false],
      mereVivante: [false],
      grandPerePaternelVivant: [false],
      grandMerePaternelleVivante: [false],
      nbFilles: [0, [Validators.min(0)]],
      nbGarcons: [0, [Validators.min(0)]],
      nbSoeurs: [0, [Validators.min(0)]],
      nbFreres: [0, [Validators.min(0)]],
      nbOncles: [0, [Validators.min(0)]],
      nbCousins: [0, [Validators.min(0)]],
      nbPetitsFils: [0, [Validators.min(0)]],
      nbPetitesFilles: [0, [Validators.min(0)]],
      sexeParentPredecede: ['M']
    });
  }

  private setupAutoCalculate(): void {
    this.simForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300)
      )
      .subscribe(() => {
        this.applyEliminations();
        if (this.simForm.valid) {
          this.calculerParts();
        }
      });
  }

  private applyEliminations(): void {
    // Les valeurs brutes pour inspecter l'état même des champs désactivés
    const valeurs = this.simForm.getRawValue();
    const patch: any = {};
    const disableList: string[] = [];
    const enableList: string[] = [
      'grandPerePaternelVivant', 'grandMerePaternelleVivante',
      'nbFreres', 'nbSoeurs', 'nbOncles', 'nbCousins', 'nbPetitsFils', 'nbPetitesFilles'
    ];

    // 1. La mère élimine les grands-mères
    if (valeurs.mereVivante) {
      patch.grandMerePaternelleVivante = false;
      disableList.push('grandMerePaternelleVivante');
    }

    // 2. Le père élimine le grand-père, les frères/sœurs, oncles, cousins
    if (valeurs.pereVivant) {
      patch.grandPerePaternelVivant = false;
      patch.nbFreres = 0; patch.nbSoeurs = 0; patch.nbOncles = 0; patch.nbCousins = 0;
      disableList.push('grandPerePaternelVivant', 'nbFreres', 'nbSoeurs', 'nbOncles', 'nbCousins');
    }

    // 3. Le fils (garçon) élimine les petits-enfants, frères/sœurs, oncles, cousins
    if (valeurs.nbGarcons > 0) {
      patch.nbPetitsFils = 0; patch.nbPetitesFilles = 0;
      patch.nbFreres = 0; patch.nbSoeurs = 0; patch.nbOncles = 0; patch.nbCousins = 0;
      disableList.push('nbPetitsFils', 'nbPetitesFilles', 'nbFreres', 'nbSoeurs', 'nbOncles', 'nbCousins');
    }

    // 4. Le petit-fils élimine frères/sœurs, oncles, cousins
    if (valeurs.nbPetitsFils > 0 && !disableList.includes('nbFreres')) {
      patch.nbFreres = 0; patch.nbSoeurs = 0; patch.nbOncles = 0; patch.nbCousins = 0;
      disableList.push('nbFreres', 'nbSoeurs', 'nbOncles', 'nbCousins');
    }

    // 5. Le frère élimine oncles, cousins
    if (valeurs.nbFreres > 0 && !disableList.includes('nbOncles')) {
      patch.nbOncles = 0; patch.nbCousins = 0;
      disableList.push('nbOncles', 'nbCousins');
    }

    // 6. L'oncle élimine cousins
    if (valeurs.nbOncles > 0 && !disableList.includes('nbCousins')) {
      patch.nbCousins = 0;
      disableList.push('nbCousins');
    }

    // Application des états (désactiver/activer)
    enableList.forEach(ctrl => {
      const control = this.simForm.get(ctrl);
      if (disableList.includes(ctrl)) {
        if (control?.enabled) {
          control.setValue(patch[ctrl], { emitEvent: false });
          control.disable({ emitEvent: false });
        }
      } else {
        if (control?.disabled) {
          control.enable({ emitEvent: false });
        }
      }
    });
  }

  calculerParts(): void {
    this.enChargement = true;
    this.erreur = null;
    
    // Si aucun héritier n'est sélectionné, on ne simule pas (le backend renverrait une erreur logique)
    // On utilise getRawValue pour inclure les champs qui ont été désactivés (ex: grand-père = false)
    const valeurs = this.simForm.getRawValue();
    const hasAtLeastOneHeir = valeurs.nbConjoints > 0 || valeurs.pereVivant || valeurs.mereVivante || 
                              valeurs.grandPerePaternelVivant || valeurs.grandMerePaternelleVivante ||
                              valeurs.nbFilles > 0 || valeurs.nbGarcons > 0 || 
                              valeurs.nbSoeurs > 0 || valeurs.nbFreres > 0 || 
                              valeurs.nbOncles > 0 || valeurs.nbCousins > 0 ||
                              valeurs.nbPetitsFils > 0 || valeurs.nbPetitesFilles > 0;

    if (!hasAtLeastOneHeir) {
      this.resultats = null;
      this.enChargement = false;
      return;
    }

    this.simulateurService.simulerCalcul(valeurs).subscribe({
      next: (response: HeritageResponse) => {
        this.resultats = response;
        this.enChargement = false;
      },
      error: (err: any) => {
        console.error('Erreur de simulation', err);
        this.erreur = err.error?.message || "Erreur lors du calcul des parts.";
        this.resultats = null;
        this.enChargement = false;
      }
    });
  }

  getPourcentage(h: any): number {
    if (!h.part || !h.part.denominateur || h.part.denominateur === 0) {
      return 0;
    }
    return (h.part.numerateur / h.part.denominateur) * 100;
  }

  // Helper pour les compteurs dans le template
  increment(controlName: string): void {
    if (this.simForm.get(controlName)?.disabled) return;
    const current = this.simForm.get(controlName)?.value || 0;
    this.simForm.get(controlName)?.setValue(current + 1);
  }

  decrement(controlName: string): void {
    if (this.simForm.get(controlName)?.disabled) return;
    const current = this.simForm.get(controlName)?.value || 0;
    if (current > 0) {
      this.simForm.get(controlName)?.setValue(current - 1);
    }
  }

  setSexeDefunt(sexe: string): void {
    this.simForm.get('sexeDefunt')?.setValue(sexe);
    // Si on passe d'un homme à une femme, le nombre de conjoints max passe de 4 à 1.
    // L'ajustement est purement logique, mais si c'est une femme, on reset à 1 si > 1
    if (sexe === 'F') {
      const conjoints = this.simForm.get('nbConjoints')?.value;
      if (conjoints > 1) {
        this.simForm.get('nbConjoints')?.setValue(1);
      }
    }
  }
}
