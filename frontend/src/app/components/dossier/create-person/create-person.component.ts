import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { FolderService } from '../../../services/folder.service';

@Component({
  selector: 'app-create-person',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="container form-container">
      <h1 class="form-title">Création du dossier</h1>
      <cite style="color: #2eaf7d">Tapez le nom de famille et le prénom du défunt</cite>
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

  constructor(
    private fb: FormBuilder,
    private folderService: FolderService,
    private router: Router
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

  onSubmit() {
    if (this.personForm.valid && !this.isSubmitting) {
      this.isSubmitting = true;
      this.submitSuccess = false;
      this.submitError = false;

      const request = {
        nom: this.personForm.value.lastName,
        prenom: this.personForm.value.firstName
      };

      this.folderService.createFolder(request).subscribe({
        next: (response) => {
          this.submitSuccess = true;
          this.personForm.reset();
          this.isSubmitting = false;
          setTimeout(() => {
            this.router.navigate(['/upload']);
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