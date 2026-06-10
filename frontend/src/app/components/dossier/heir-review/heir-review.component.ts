import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FridaService } from '../../../services/frida.service';
import { OcrPipelineService } from '../../../services/ocr-pipeline.service';
import { UploadStateService } from '../../../services/upload-state.service';
import { AuthService } from '../../../services/auth.service';

interface Personne {
  id?: number;
  nom: string;
  prenom: string;
  dateNaissance: string;
  sexe: string;
  numParente: string;
  nin?: string;
}

@Component({
  selector: 'app-heir-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="review-container">
      <h2>Fiche Familiale Interactive</h2>
      <p class="subtitle">Complétez ou corrigez les informations de la famille avant de lancer le calcul définitif.</p>
      
      <div *ngIf="isLoading" class="loading-state">
        <span class="spinner"></span> Chargement du dossier...
      </div>

      <div class="form-card" *ngIf="!isLoading && frida">
        <!-- Section Défunt -->
        <div class="section-header">
          <h3>Le Défunt</h3>
        </div>
        <div class="person-card defunt">
          <div class="info">
            <span class="name"><span class="demo-blur">{{ defunt.nom }}</span> {{ defunt.prenom }}</span>
            <span class="badge">{{ defunt.sexe === 'M' ? 'Homme' : 'Femme' }}</span>
            <span class="details">Né(e) le {{ defunt.dateNaissance || 'Inconnue' }} | NIN: <span class="demo-blur">{{ defunt.nin || '-' }}</span></span>
          </div>
          <button class="btn-icon" (click)="editDefunt()" title="Modifier">✏️</button>
        </div>

        <!-- Mode Édition Défunt -->
        <div class="edit-form" *ngIf="editingDefunt">
          <h4>Modifier le Défunt</h4>
          <div class="form-grid">
            <input type="text" [(ngModel)]="defunt.nom" placeholder="Nom">
            <input type="text" [(ngModel)]="defunt.prenom" placeholder="Prénom">
            <input type="date" [(ngModel)]="defunt.dateNaissance">
            <input type="text" [(ngModel)]="defunt.nin" placeholder="NIN">
            <select [(ngModel)]="defunt.sexe">
              <option value="M">Homme</option>
              <option value="F">Femme</option>
            </select>
          </div>
          <div class="form-actions">
            <button class="btn btn-sm btn-primary" (click)="saveDefuntEdit()">Valider</button>
          </div>
        </div>

        <!-- Section Héritiers -->
        <div class="section-header mt-4">
          <h3>Les Héritiers</h3>
          <button class="btn btn-sm btn-outline" (click)="startAddingHeir()">+ Ajouter manuellement</button>
        </div>

        <!-- Formulaire Ajout/Modif Héritier -->
        <div class="edit-form highlight" *ngIf="isAddingHeir || editingIndex !== null">
          <h4>{{ isAddingHeir ? 'Nouvel Héritier' : 'Modifier Héritier' }}</h4>
          <div class="form-grid">
            <input type="text" [(ngModel)]="currentHeir.nom" placeholder="Nom">
            <input type="text" [(ngModel)]="currentHeir.prenom" placeholder="Prénom">
            <input type="date" [(ngModel)]="currentHeir.dateNaissance">
            <input type="text" [(ngModel)]="currentHeir.nin" placeholder="NIN">
            <select [(ngModel)]="currentHeir.sexe">
              <option value="M">Homme</option>
              <option value="F">Femme</option>
            </select>
            <select [(ngModel)]="currentHeir.numParente">
              <option value="02">Conjoint (Époux/Épouse)</option>
              <option value="03">Enfant (Fils/Fille)</option>
              <option value="04">Parent (Père/Mère)</option>
              <option value="08">Grand-Père paternel</option>
              <option value="11">Grand-Mère paternelle</option>
              <option value="09">Petit-fils</option>
              <option value="10">Petite-fille</option>
              <option value="05">Frère / Sœur</option>
              <option value="06">Oncle paternel</option>
              <option value="07">Cousin paternel</option>
            </select>
          </div>
          <div class="form-actions">
            <button class="btn btn-sm btn-secondary" (click)="cancelEdit()">Annuler</button>
            <button class="btn btn-sm btn-primary" (click)="saveHeir()">Enregistrer dans la fiche</button>
          </div>
        </div>

        <!-- Liste des héritiers -->
        <div class="persons-list">
          <div class="person-card" *ngFor="let h of heritiers; let i = index">
              <div class="info">
                <span class="name"><span class="demo-blur">{{ h.nom }}</span> {{ h.prenom }}</span>
                <span class="badge">{{ h.sexe === 'M' ? 'Homme' : 'Femme' }}</span>
                <span class="badge role">{{ getRoleLabel(h.numParente, h.sexe) }}</span>
                <span class="details">Né(e) le {{ h.dateNaissance || 'Inconnue' }} | NIN: <span class="demo-blur">{{ h.nin || '-' }}</span></span>
              </div>
             <div class="card-actions">
               <button class="btn-icon" (click)="editHeir(i)" title="Modifier">✏️</button>
               <button class="btn-icon text-danger" (click)="deleteHeir(i)" title="Retirer de la fiche">🗑️</button>
             </div>
          </div>
        </div>
        <div *ngIf="heritiers.length === 0" class="empty-state">
          Aucun héritier enregistré. Cliquez sur "Ajouter manuellement" pour en créer un.
        </div>

        <!-- Section Wasiyya Wajiba (Petits-enfants) -->
        <div class="edit-form highlight" *ngIf="hasGrandchildren()" style="margin-top: 1.5rem;">
          <h4 style="color: var(--accent-color);">ℹ️ Précision pour les Petits-Enfants (Wasiyya Wajiba)</h4>
          <p style="font-size: 0.9rem; margin-bottom: 1rem;">Les petits-enfants héritent de la part de leur parent pré-décédé. Veuillez préciser le sexe de ce parent :</p>
          <div class="form-grid">
            <select [(ngModel)]="sexeParentPredecede" style="width: 100%; max-width: 300px;">
              <option value="M">Leur père est pré-décédé (Fils du défunt)</option>
              <option value="F">Leur mère est pré-décédée (Fille du défunt)</option>
            </select>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-secondary" (click)="goBack()">Retourner au carrousel</button>
          <button class="btn btn-danger" (click)="annulerTout()">Tout annuler</button>
          <div class="validation-wrapper" style="display: flex; flex-direction: column; align-items: flex-end; gap: 5px;">
            <button class="btn btn-primary" (click)="validateAndCalculate()" 
                    [disabled]="isCalculating || isAddingHeir || editingIndex !== null || editingDefunt || !authService.isMaitre()">
               <span *ngIf="!isCalculating">💾 Sauvegarder et Calculer les Parts</span>
               <span *ngIf="isCalculating"><span class="spinner"></span> Sauvegarde et calcul en cours...</span>
            </button>
            <span *ngIf="!authService.isMaitre()" style="font-size: 0.8rem; color: #ffb84d;">
              ⚠️ Seul un compte Maître peut valider définitivement la Frida.
            </span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .review-container { max-width: 900px; margin: 0 auto; padding: 2rem; color: var(--text-primary); }
    h2 { text-align: center; color: var(--accent-color); margin-bottom: 0.5rem; text-transform: uppercase; font-weight: bold; letter-spacing: 1px;}
    .subtitle { text-align: center; color: var(--text-secondary); margin-bottom: 2rem; }
    .form-card { background: rgba(78, 204, 163, 0.05); border: 1px solid rgba(78, 204, 163, 0.2); border-radius: 12px; padding: 2rem; }
    
    .section-header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(78, 204, 163, 0.2); padding-bottom: 0.5rem; margin-bottom: 1rem; }
    .section-header h3 { font-size: 1.2rem; font-weight: bold; color: var(--text-primary); margin: 0; }
    .mt-4 { margin-top: 2rem; }
    
    .persons-list { display: flex; flex-direction: column; gap: 1rem; margin-top: 1rem;}
    .person-card { display: flex; justify-content: space-between; align-items: center; padding: 1rem; border: 1px solid rgba(78, 204, 163, 0.2); border-radius: 8px; background: rgba(0, 0, 0, 0.2); }
    .person-card.defunt { background: rgba(78, 204, 163, 0.1); border-color: var(--accent-color); }
    
    .info { display: flex; align-items: center; flex: 1; flex-wrap: wrap; gap: 10px; }
    .name { font-weight: bold; font-size: 1.1rem; min-width: 150px; color: var(--text-primary); }
    .badge { padding: 4px 12px; border-radius: 20px; font-size: 0.85rem; font-weight: bold; background: rgba(255,255,255,0.1); color: var(--text-primary); }
    .badge.role { background: var(--accent-color); color: var(--background-gradient-start); }
    .details { color: var(--text-secondary); font-size: 0.9rem; margin-left: auto; margin-right: 1rem;}
    
    .card-actions { display: flex; gap: 0.5rem; }
    .btn-icon { background: none; border: none; font-size: 1.2rem; cursor: pointer; padding: 4px; border-radius: 4px; transition: background 0.2s;}
    .btn-icon:hover { background: rgba(255,255,255,0.1); }
    .text-danger { color: #ff6b6b; }
    
    .edit-form { background: rgba(0, 0, 0, 0.3); padding: 1.5rem; border-radius: 8px; margin-bottom: 1rem; border-left: 4px solid var(--text-secondary);}
    .edit-form.highlight { border-left-color: var(--accent-color); }
    .edit-form h4 { margin-top: 0; margin-bottom: 1rem; color: var(--text-primary); font-size: 1rem;}
    .form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 1rem; margin-bottom: 1rem; }
    .form-grid input, .form-grid select { padding: 8px 12px; background: rgba(0,0,0,0.2); border: 1px solid rgba(78, 204, 163, 0.3); color: var(--text-primary); border-radius: 4px; font-size: 0.95rem; }
    .form-actions { display: flex; justify-content: flex-end; gap: 0.5rem; }
    
    .btn-sm { padding: 0.25rem 0.5rem; font-size: 0.875rem; border-radius: 0.2rem; }
    .btn-outline { border: 1px solid var(--accent-color); color: var(--accent-color); background: transparent; cursor: pointer; border-radius: var(--border-radius); padding: 8px 16px; font-weight: 500;}
    .btn-outline:hover { background: var(--accent-color); color: var(--background-gradient-start); }
    
    .actions { display: flex; justify-content: space-between; margin-top: 2.5rem; padding-top: 1.5rem; border-top: 1px solid rgba(78, 204, 163, 0.2); }
    
    .btn-danger { background: #ff4757; color: white; border: none; padding: 10px 20px; border-radius: var(--border-radius); cursor: pointer; font-weight: 600; }
    .btn-danger:hover { background: #ff6b81; }
    
    .spinner { display: inline-block; width: 16px; height: 16px; border: 2px solid #f3f3f3; border-top: 2px solid var(--accent-color); border-radius: 50%; animation: spin 1s linear infinite; margin-right: 8px; }
    @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
    .loading-state { text-align: center; padding: 3rem; font-size: 1.2rem; color: var(--text-secondary); }
    .empty-state { padding: 2rem; text-align: center; color: var(--text-secondary); font-style: italic; background: rgba(0,0,0,0.2); border-radius: 8px; margin-top: 1rem; }
  `]
})
export class HeirReviewComponent implements OnInit {
  numFrida: string = '';
  frida: any = null;
  isLoading = true;
  isCalculating = false;

  // Modèles de données
  sexeParentPredecede: string = 'M';
  defunt: Personne = { nom: '', prenom: '', dateNaissance: '', sexe: 'M', numParente: '01', nin: '' };
  heritiers: Personne[] = [];

  // États d'édition
  editingDefunt = false;
  isAddingHeir = false;
  editingIndex: number | null = null;
  currentHeir: Personne = this.getEmptyHeir();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fridaService: FridaService,
    private ocrPipelineService: OcrPipelineService,
    private uploadStateService: UploadStateService,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.numFrida = params['numFrida'];
      if (this.numFrida) {
        this.loadData();
      } else {
        this.isLoading = false;
        alert("Aucun numéro de dossier fourni.");
      }
    });
  }

  loadData() {
    this.fridaService.lancerApi('/api/frida/' + this.numFrida).subscribe({
      next: (data) => {
        this.frida = data;
        if (data.defunt && data.defunt.identite) {
           const s = (data.defunt.identite.sexe || '').trim();
           const isFemale = s.includes('أنثى') || s.includes('انثى') || s.includes('أنث') || s.includes('انث') || s === 'ا' || s === 'أ' || s.toUpperCase() === 'F';
           this.defunt = {
             nom: data.defunt.identite.nom || '',
             prenom: data.defunt.identite.prenom || '',
             dateNaissance: data.defunt.identite.dateNaissance || '',
             sexe: isFemale ? 'F' : 'M',
             numParente: '01',
             nin: data.defunt.identite.nin || ''
           };
        }
        if (data.sexeParentPredecede) {
            this.sexeParentPredecede = data.sexeParentPredecede;
        }
        this.loadHeritiers();
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
        alert("Erreur de chargement du dossier");
      }
    });
  }

  loadHeritiers() {
    this.fridaService.lancerApi('/api/frida/listeHeritiers/' + this.numFrida).subscribe({
      next: (data) => {
        if (data && Array.isArray(data)) {
           this.heritiers = data.map(h => {
              const s = (h.identite?.sexe || '').trim();
              const isFemale = s.includes('أنثى') || s.includes('انثى') || s.includes('أنث') || s.includes('انث') || s === 'ا' || s === 'أ' || s.toUpperCase() === 'F';
              return {
                nom: h.identite?.nom || '',
                prenom: h.identite?.prenom || '',
                dateNaissance: h.identite?.dateNaissance || '',
                sexe: isFemale ? 'F' : 'M',
                numParente: h.numParente || '03',
                nin: h.identite?.nin || ''
              };
           });
           
           // Appliquer l'unicité (Père, Mère, Grand-père)
           const uniqueHeirs: Personne[] = [];
           const seenRoles = new Set<string>();
           
           for (const h of this.heritiers) {
               if (h.numParente === '04' || h.numParente === '08' || h.numParente === '11') {
                   const key = h.numParente + '_' + (h.numParente === '04' ? h.sexe : '');
                   if (seenRoles.has(key)) {
                       continue; // Ignorer le doublon
                   }
                   seenRoles.add(key);
               }
               uniqueHeirs.push(h);
           }
           this.heritiers = uniqueHeirs;
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  // ==== Méthodes CRUD ====

  editDefunt() {
    this.editingDefunt = true;
  }
  
  saveDefuntEdit() {
    this.editingDefunt = false;
  }

  getEmptyHeir(): Personne {
    return { nom: '', prenom: '', dateNaissance: '', sexe: 'M', numParente: '03', nin: '' };
  }

  startAddingHeir() {
    this.isAddingHeir = true;
    this.editingIndex = null;
    this.currentHeir = this.getEmptyHeir();
  }

  editHeir(index: number) {
    this.isAddingHeir = false;
    this.editingIndex = index;
    // Clone l'objet pour éviter la modification en temps réel sans sauvegarde
    this.currentHeir = { ...this.heritiers[index] };
  }

  deleteHeir(index: number) {
    if (confirm("Voulez-vous vraiment retirer cet héritier de la fiche ?")) {
      this.heritiers.splice(index, 1);
    }
  }

  cancelEdit() {
    this.isAddingHeir = false;
    this.editingIndex = null;
  }

  saveHeir() {
    // Vérification de l'unicité
    const num = this.currentHeir.numParente;
    const sexe = this.currentHeir.sexe;

    if (num === '04' || num === '08' || num === '11') {
       const exists = this.heritiers.findIndex((h, index) => {
           if (this.editingIndex === index) return false;
           if (num === '04') {
              return h.numParente === '04' && h.sexe === sexe;
           }
           if (num === '08' || num === '11') {
              return h.numParente === num;
           }
           return false;
       });

       if (exists !== -1) {
          const role = this.getRoleLabel(num, sexe);
          alert(`Il y a déjà un(e) ${role} dans la liste. L'unicité doit être respectée.`);
          return;
       }
    }

    if (this.isAddingHeir) {
      this.heritiers.push({ ...this.currentHeir });
    } else if (this.editingIndex !== null) {
      this.heritiers[this.editingIndex] = { ...this.currentHeir };
    }
    this.cancelEdit();
  }

  // ==== Utilitaires ====

  getRoleLabel(numParente: string, sexe: string): string {
    const isMale = sexe === 'M';
    switch (numParente) {
      case '02': return isMale ? 'Époux' : 'Épouse';
      case '03': return isMale ? 'Fils' : 'Fille';
      case '04': return isMale ? 'Père' : 'Mère';
      case '05': return isMale ? 'Frère' : 'Sœur';
      case '06': return 'Oncle paternel';
      case '07': return 'Cousin paternel';
      case '08': return 'Grand-père paternel';
      case '11': return 'Grand-mère paternelle';
      case '09': return 'Petit-fils';
      case '10': return 'Petite-fille';
      default: return isMale ? 'Masculin' : 'Féminin';
    }
  }

  hasGrandchildren(): boolean {
    return this.heritiers.some(h => h.numParente === '09' || h.numParente === '10');
  }

  goBack() {
    this.router.navigate(['/upload']);
  }

  annulerTout() {
    if (confirm("Voulez-vous vraiment tout annuler ? L'état de ce dossier sera perdu.")) {
      this.uploadStateService.clearState();
      this.router.navigate(['/']);
    }
  }

  validateAndCalculate() {
    this.isCalculating = true;

    // Le backend (HeirPartCalculatorService.java et EcrireBdService.java)
    // base sa logique conditionnelle sur le sexe en Arabe ("ذكر").
    // On convertit donc nos "M" / "F" en Arabe avant de les envoyer !
    const formatPerson = (p: Personne) => ({
      nom: p.nom,
      prenom: p.prenom,
      dateNaissance: p.dateNaissance,
      sexe: p.sexe === 'M' ? 'ذكر' : 'أنثى', 
      numParente: p.numParente,
      nin: p.nin
    });

    const payload = {
      defunt: formatPerson(this.defunt),
      heritiers: this.heritiers.map(formatPerson),
      sexeParentPredecede: this.hasGrandchildren() ? this.sexeParentPredecede : null
    };

    // On envoie le payload au backend pour remplacer la liste en base
    this.ocrPipelineService.sauvegarderFiche(this.numFrida, payload).subscribe({
      next: () => {
        // Une fois sauvegardé, on lance le calcul
        this.ocrPipelineService.lancerCalcul(this.numFrida).subscribe({
          next: () => {
             this.isCalculating = false;
             this.router.navigate(['/frida'], { queryParams: { numFrida: this.numFrida } });
          },
          error: (err) => {
             console.error("Erreur de calcul", err);
             this.isCalculating = false;
             alert("Une erreur s'est produite lors du calcul des parts.");
          }
        });
      },
      error: (err) => {
        console.error("Erreur de sauvegarde", err);
        this.isCalculating = false;
        alert("Impossible de sauvegarder la fiche modifiée.");
      }
    });
  }
}
