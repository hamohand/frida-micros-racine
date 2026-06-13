import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
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
      sexeParentPredecede: ['M'],
      tombes: this.fb.array([])
    });
  }

  get tombes(): FormArray {
    return this.simForm.get('tombes') as FormArray;
  }

  addTombe(): void {
    const tombeGroup = this.fb.group({
      lienParente: ['enfant'],
      sexeParentPredecede: ['M'],
      nbDescendantsMales: [0, Validators.min(0)],
      nbDescendantesFemelles: [0, Validators.min(0)]
    });
    this.tombes.push(tombeGroup);
  }

  removeTombe(index: number): void {
    this.tombes.removeAt(index);
  }

  incrementTombe(index: number, controlName: string): void {
    const group = this.tombes.at(index) as FormGroup;
    const current = group.get(controlName)?.value || 0;
    group.get(controlName)?.setValue(current + 1);
  }

  decrementTombe(index: number, controlName: string): void {
    const group = this.tombes.at(index) as FormGroup;
    const current = group.get(controlName)?.value || 0;
    if (current > 0) {
      group.get(controlName)?.setValue(current - 1);
    }
  }

  private setupAutoCalculate(): void {
    this.simForm.valueChanges
      .pipe(
        takeUntil(this.destroy$),
        debounceTime(300)
      )
      .subscribe(() => {
        if (this.simForm.valid) {
          this.calculerParts();
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
    const current = this.simForm.get(controlName)?.value || 0;
    this.simForm.get(controlName)?.setValue(current + 1);
  }

  decrement(controlName: string): void {
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
