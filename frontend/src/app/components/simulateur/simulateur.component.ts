import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
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
    this.setupHajbRules();
    this.setupAutoCalculate();
    // Lancer un premier calcul
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
      tombesEnfants: this.fb.array([]),
      tombesFratrie: this.fb.array([])
    });
  }

  get tombesEnfants(): FormArray {
    return this.simForm.get('tombesEnfants') as FormArray;
  }

  get tombesFratrie(): FormArray {
    return this.simForm.get('tombesFratrie') as FormArray;
  }

  addTombeEnfant(): void {
    this.tombesEnfants.push(this.createTombeGroup('enfant'));
  }

  addTombeFratrie(): void {
    this.tombesFratrie.push(this.createTombeGroup('frere/soeur'));
  }

  private createTombeGroup(lien: string): FormGroup {
    return this.fb.group({
      lienParente: [lien],
      sexeParentPredecede: ['M'],
      nbDescendantsMales: [0, Validators.min(0)],
      nbDescendantesFemelles: [0, Validators.min(0)]
    });
  }

  removeTombeEnfant(index: number): void {
    this.tombesEnfants.removeAt(index);
  }

  removeTombeFratrie(index: number): void {
    this.tombesFratrie.removeAt(index);
  }

  incrementTombe(formArray: FormArray, index: number, controlName: string): void {
    const group = formArray.at(index) as FormGroup;
    const current = group.get(controlName)?.value || 0;
    group.get(controlName)?.setValue(current + 1);
  }

  decrementTombe(formArray: FormArray, index: number, controlName: string): void {
    const group = formArray.at(index) as FormGroup;
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

  private setupHajbRules(): void {
    this.simForm.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.applyHajbLogic();
      });
    // Appliquer au démarrage
    this.applyHajbLogic();
  }

  private applyHajbLogic(): void {
    const val = this.simForm.getRawValue();

    // 1. Le Père exclut Grand-père et Grand-mère paternelle
    if (val.pereVivant) {
      this.disableControl('grandPerePaternelVivant');
      this.disableControl('grandMerePaternelleVivante');
    } else {
      this.enableControl('grandPerePaternelVivant');
      if (!val.mereVivante) {
        this.enableControl('grandMerePaternelleVivante');
      }
    }

    // 2. La Mère exclut Grand-mère paternelle
    if (val.mereVivante) {
      this.disableControl('grandMerePaternelleVivante');
    } else if (!val.pereVivant) {
      this.enableControl('grandMerePaternelleVivante');
    }

    // 3. Père, Grand-père ou Fils excluent Frères, Sœurs (et Tombes Fratrie)
    const excludeFratrie = val.pereVivant || val.grandPerePaternelVivant || val.nbGarcons > 0;
    if (excludeFratrie) {
      this.disableControl('nbFreres');
      this.disableControl('nbSoeurs');
      if (!this.tombesFratrie.disabled) this.tombesFratrie.disable({ emitEvent: false });
    } else {
      this.enableControl('nbFreres');
      this.enableControl('nbSoeurs');
      if (this.tombesFratrie.disabled) this.tombesFratrie.enable({ emitEvent: false });
    }

    // 4. Père, Grand-père, Fils ou Frères excluent Oncles
    const excludeOncles = excludeFratrie || val.nbFreres > 0;
    if (excludeOncles) {
      this.disableControl('nbOncles');
    } else {
      this.enableControl('nbOncles');
    }

    // 5. Père, Grand-père, Fils, Frères ou Oncles excluent Cousins
    const excludeCousins = excludeOncles || val.nbOncles > 0;
    if (excludeCousins) {
      this.disableControl('nbCousins');
    } else {
      this.enableControl('nbCousins');
    }
  }

  private disableControl(name: string): void {
    const ctrl = this.simForm.get(name);
    if (ctrl && !ctrl.disabled) {
      ctrl.disable({ emitEvent: false });
      // Reset value visually if disabled to avoid confusion
      if (typeof ctrl.value === 'boolean') {
        ctrl.setValue(false, { emitEvent: false });
      }
    }
  }

  private enableControl(name: string): void {
    const ctrl = this.simForm.get(name);
    if (ctrl && ctrl.disabled) {
      ctrl.enable({ emitEvent: false });
    }
  }

  calculerParts(): void {
    this.enChargement = true;
    this.erreur = null;
    
    // Si aucun héritier n'est sélectionné, on ne simule pas (le backend renverrait une erreur logique)
    // On utilise getRawValue pour inclure les champs qui ont été désactivés (ex: grand-père = false)
    const valeurs = this.simForm.getRawValue();
    valeurs.nbPetitsFils = 0;
    valeurs.nbPetitesFilles = 0;
    valeurs.sexeParentPredecede = 'M';

    const hasAtLeastOneHeir = valeurs.nbConjoints > 0 || valeurs.pereVivant || valeurs.mereVivante || 
                              valeurs.grandPerePaternelVivant || valeurs.grandMerePaternelleVivante ||
                              valeurs.nbFilles > 0 || valeurs.nbGarcons > 0 || 
                              valeurs.nbSoeurs > 0 || valeurs.nbFreres > 0 || 
                              valeurs.nbOncles > 0 || valeurs.nbCousins > 0 ||
                              (valeurs.tombesEnfants && valeurs.tombesEnfants.length > 0) ||
                              (valeurs.tombesFratrie && valeurs.tombesFratrie.length > 0);

    // Fusionner les tombes pour le backend
    valeurs.tombes = [...(valeurs.tombesEnfants || []), ...(valeurs.tombesFratrie || [])];
    // Générer un identifiant unique pour chaque tombe au moment de l'envoi
    valeurs.tombes.forEach((t: any, i: number) => {
      t.identifiant = `T${i + 1}`;
    });
    
    // Supprimer les champs spécifiques au frontend
    delete valeurs.tombesEnfants;
    delete valeurs.tombesFratrie;

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
    const control = this.simForm.get(controlName);
    if (control?.disabled) return;
    const current = control?.value || 0;
    control?.setValue(current + 1);
  }

  decrement(controlName: string): void {
    const control = this.simForm.get(controlName);
    if (control?.disabled) return;
    const current = control?.value || 0;
    if (current > 0) {
      control?.setValue(current - 1);
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
