import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { FolderService } from '../../../services/folder.service';
import { UploadStateService } from '../../../services/upload-state.service';
import { ConstitutionService } from '../../../services/constitution.service';

@Component({
  selector: 'app-create-person',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="container form-container">
      <h1 class="form-title">Création du dossier</h1>
      <cite style="color: #2eaf7d">Tapez le nom de famille et le prénom du défunt en lettres latines</cite>
      
      <div *ngIf="uploadStateService.dossierEnCours() as d" class="avis-en-cours">
        <span>Le dossier <strong>{{ d.libelle }}</strong> est en cours. En créer un nouveau l'abandonnera.</span>
        <button type="button" class="btn btn-secondary" (click)="router.navigateByUrl(d.url)">Reprendre ce dossier</button>
      </div>

      <div class="qr-action-container">
        <input type="file" #fileInput (change)="onFileSelected($event)" accept="image/*,.pdf" style="display: none;" />
        <button type="button" class="btn btn-secondary qr-btn" (click)="fileInput.click()" [disabled]="isScanningQr">
          <span class="icon">📷</span> {{ isScanningQr ? 'Analyse du QR Code...' : 'Remplir via QR Code (Acte de décès)' }}
        </button>
        <div *ngIf="qrError" class="error-message" style="margin-top: 5px;">{{ qrError }}</div>
      </div>
      
      <div class="divider">ou saisie manuelle</div>

      <form [formGroup]="personForm" (ngSubmit)="onSubmit()" class="person-form">
        <div class="form-group">
          <label for="lastName">Nom</label>
          <input
            id="lastName"
            type="text"
            placeholder="Entrez le nom en lettres latines"
            formControlName="lastName"
            class="form-input"
            [class.error]="isFieldInvalid('lastName')"
          />
          <span class="error-message" *ngIf="isFieldInvalid('lastName')">
            Le nom est requis et doit contenir uniquement des lettres
          </span>
        </div>

        <div class="form-group">
          <label for="firstName">Prénom</label>
          <input
            id="firstName"
            type="text"
            placeholder="Entrez le prénom en lettres latines"
            formControlName="firstName"
            class="form-input"
            [class.error]="isFieldInvalid('firstName')"
          />
          <span class="error-message" *ngIf="isFieldInvalid('firstName')">
            Le prénom est requis et doit contenir uniquement des lettres
          </span>
        </div>

        <div class="form-feedback" *ngIf="submitSuccess">
          Dossier créé avec succès !
        </div>
        <div class="form-error" *ngIf="submitError">
          {{ errorMessage }}
        </div>

        <button type="submit" class="btn btn-primary" [disabled]="personForm.invalid || isSubmitting">
          {{ isSubmitting ? 'Création...' : 'Enregistrer' }}
        </button>
      </form>
    </div>
  `,
  styles: [`
    .qr-action-container {
      margin: var(--space-4) 0;
      text-align: center;
    }
    .qr-btn {
      width: 100%;
      padding: var(--space-3);
      font-size: 1.05rem;
      border: 1px dashed var(--accent-color);
      background: rgba(78, 204, 163, 0.05);
      color: var(--accent-color);
      cursor: pointer;
      border-radius: var(--radius-md);
      transition: all 0.2s ease;
    }
    .qr-btn:hover:not(:disabled) {
      background: rgba(78, 204, 163, 0.15);
    }
    .qr-btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .divider {
      display: flex;
      align-items: center;
      text-align: center;
      color: var(--text-muted);
      margin: var(--space-4) 0;
      font-size: 0.85rem;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .divider::before, .divider::after {
      content: '';
      flex: 1;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }
    .divider:not(:empty)::before { margin-right: .5em; }
    .divider:not(:empty)::after { margin-left: .5em; }
    
    .avis-en-cours {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--space-3);
      flex-wrap: wrap;
      margin: var(--space-4) 0;
      padding: var(--space-3) var(--space-4);
      border: 1px solid var(--warning);
      border-radius: var(--radius-md);
      background: var(--warning-soft);
      color: var(--text-1);
      font-size: 0.92rem;
    }
    .avis-en-cours .btn { padding: var(--space-2) var(--space-4); border-color: var(--warning); color: var(--warning); }
    .form-feedback {
      padding: var(--spacing-xs);
      margin-bottom: var(--spacing-xs);
      background: rgba(78, 204, 163, 0.1);
      color: var(--accent-color);
      border-radius: var(--border-radius);
    }

    .form-error {
      padding: var(--spacing-sm);
      margin-bottom: var(--spacing-md);
      background: rgba(255, 68, 68, 0.1);
      color: #ff4444;
      border-radius: var(--border-radius);
    }
    .form-container {
      padding-top: calc(var(--nav-height) + var(--spacing-xs));
      max-width: 600px;
    }

    .form-title {
      margin-bottom: var(--spacing-xs);
      color: var(--text-primary);
    }

    .person-form {
      background: rgba(255, 255, 255, 0.05);
      padding: var(--spacing-md);
      border-radius: var(--border-radius);
      backdrop-filter: blur(10px);
    }

    .form-group {
      margin-bottom: var(--spacing-xs);
    }

    .form-group label {
      display: block;
      margin-bottom: var(--spacing-xs);
      color: var(--text-primary);
    }

    .form-input {
      width: 100%;
      padding: var(--spacing-xs);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: var(--border-radius);
      background: rgba(255, 255, 255, 0.05);
      color: var(--text-primary);
      transition: var(--transition);
    }

    .form-input:focus {
      outline: none;
      border-color: var(--accent-color);
      box-shadow: 0 0 0 2px rgba(78, 204, 163, 0.2);
    }

    .form-input.error {
      border-color: #ff4444;
    }

    .error-message {
      color: #ff4444;
      font-size: 0.875rem;
      margin-top: var(--spacing-xs);
    }
  `]
})
export class CreatePersonComponent {
  personForm: FormGroup;
  isSubmitting = false;
  submitSuccess = false;
  submitError = false;
  errorMessage = '';
  
  isScanningQr = false;
  qrError = '';
  
  selectedFile: File | null = null;

  constructor(
    private fb: FormBuilder,
    private folderService: FolderService,
    public router: Router,
    public uploadStateService: UploadStateService,
    private constitutionService: ConstitutionService,
    private http: HttpClient
  ) {
    this.personForm = this.fb.group({
      lastName: ['', [Validators.required, Validators.pattern('^[a-zA-Z]+$')]],
      firstName: ['', [Validators.required, Validators.pattern('^[a-zA-Z]+$')]]
    });
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.personForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
  
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;
    
    this.selectedFile = file;
    this.isScanningQr = true;
    this.qrError = '';
    
    const formData = new FormData();
    formData.append('file', file);
    
    this.http.post<any>(`/api/pdfs/extraire-noms-qr`, formData).subscribe({
      next: (res) => {
        this.isScanningQr = false;
        if (res.success) {
          // Retirer les éventuels accents et caractères non autorisés, et mettre en majuscule
          const cleanNom = (res.nom || '').normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-zA-Z]/g, "").toUpperCase();
          const cleanPrenom = (res.prenom || '').normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-zA-Z]/g, "").toUpperCase();
          
          this.personForm.patchValue({
            lastName: cleanNom,
            firstName: cleanPrenom
          });
          
          if (!cleanNom || !cleanPrenom) {
             this.qrError = "Le QR Code a été lu, mais les noms latins sont manquants.";
          }
        } else {
          this.qrError = res.message || "Impossible de trouver ou lire un QR Code sur ce document.";
        }
      },
      error: (err) => {
        this.isScanningQr = false;
        this.qrError = "Erreur de connexion au serveur OCR.";
        console.error(err);
      }
    });
  }

  onSubmit() {
    if (this.personForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.submitSuccess = false;
      this.submitError = false;

      const request = {
        nom: this.personForm.value.lastName.toUpperCase(),
        prenom: this.personForm.value.firstName.toUpperCase()
      };

      this.folderService.createFolder(request).subscribe({
        next: (response) => {
          this.submitSuccess = true;
          this.isSubmitting = false;
          // Nettoyer la mémoire de l'ancien dossier
          this.uploadStateService.clearState();
          this.uploadStateService.demarrerDossier(`${request.nom} ${request.prenom}`.trim());
          
          if (this.selectedFile) {
             const state = this.uploadStateService.getState();
             if (state && state.windows && state.windows['f1']) {
                state.windows['f1'].hasFiles = true;
                
                const uploadedFile = {
                   file: this.selectedFile,
                   id: Math.random().toString(36).substring(2, 9),
                   progress: 100,
                   docType: 'ad'
                };
                
                state.windows['f1'].rawFiles = [uploadedFile];
                state.windows['f1'].groupedFiles = [{
                   files: [this.selectedFile],
                   docType: 'ad',
                   entityName: ''
                }];
                this.uploadStateService.saveState(state.windows, state.ocrMode || 'rapide');
             }
          }
          
          this.constitutionService.resetFiche();
          this.personForm.reset();
          setTimeout(() => {
            if (this.selectedFile) {
               this.router.navigate(['/upload'], { state: { preloadedDefuntFile: this.selectedFile } });
            } else {
               this.router.navigate(['/upload']);
            }
          }, 1500);
        },
        error: (error) => {
          this.submitError = true;
          this.errorMessage = error.error.error || 'Une erreur est survenue';
          this.isSubmitting = false;
        }
      });
    }
  }
}
