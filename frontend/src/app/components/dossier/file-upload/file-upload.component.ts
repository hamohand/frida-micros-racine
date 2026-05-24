import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UploadedFile, UploadConfig } from './file-upload.interface';
import { v4 as uuidv4 } from 'uuid';
import { FileUploadService } from '../../../services/file-upload.service';

@Component({
  selector: 'app-file-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="upload-container">
      <h2>{{ config.title }}</h2>
      <div
        class="drop-zone"
        (dragover)="onDragOver($event)"
        (dragleave)="onDragLeave($event)"
        [class.disabled-zone]="uploadedFiles.length >= (config.maxFiles || 10)"
      >
        <div class="drop-message" *ngIf="uploadedFiles.length < (config.maxFiles || 10)">
          <span class="material-icons">cloud_upload</span>
          <p>Glissez-déposez vos fichiers ici ou</p>
          <button class="btn btn-secondary" (click)="fileInput.click()">
            Sélectionnez des fichiers
          </button>
        </div>
        <div class="drop-message" *ngIf="uploadedFiles.length >= (config.maxFiles || 10)">
          <span class="material-icons" style="color: #ffb84d;">lock</span>
          <p style="color: #ffb84d;">Limite atteinte ({{ config.maxFiles }} max).</p>
        </div>
        <input
          #fileInput
          type="file"
          multiple
          [disabled]="uploadedFiles.length >= (config.maxFiles || 10)"
          (change)="onFileSelected($event)"
        />
      </div>

      <div class="file-list" *ngIf="uploadedFiles.length > 0">
        <div class="file-item" *ngFor="let file of uploadedFiles">
          <div class="file-info">
            <span class="material-icons">Document :</span>
            <span class="file-name">{{ file.file.name }}</span>
            <select *ngIf="config.docTypes && config.docTypes.length > 0" [(ngModel)]="file.docType" (change)="onDocTypeChange(file)" class="select-doc-type file-select">
              <option *ngFor="let dt of config.docTypes" [value]="dt.id">{{ dt.label }}</option>
            </select>
            <select *ngIf="getEntitiesForDocType(file.docType).length > 0" [(ngModel)]="file.entityName" class="select-doc-type file-select">
              <option *ngFor="let ent of getEntitiesForDocType(file.docType)" [value]="ent">
                {{ ent }} {{ ent === file.docType + '_01' ? '(par défaut)' : '' }}
              </option>
            </select>
          </div>
          <button 
            class="btn-icon" 
            (click)="removeFile(file.id)"
            aria-label="Supprimer le fichier"
          >
            <svg xmlns="http://www.w3.org/2000/svg" height="20px" viewBox="0 -960 960 960" width="20px" fill="#D16D6A"><path d="M312-144q-29.7 0-50.85-21.15Q240-186.3 240-216v-480h-48v-72h192v-48h192v48h192v72h-48v479.57Q720-186 698.85-165T648-144H312Zm336-552H312v480h336v-480ZM384-288h72v-336h-72v336Zm120 0h72v-336h-72v336ZM312-696v480-480Z"/></svg>
          </button>
        </div>
      </div>

      <div class="button-group" *ngIf="uploadedFiles.length > 0 || config.allowPrevious || config.allowSkip">
        <button class="btn btn-secondary" *ngIf="config.allowPrevious" (click)="onPrevious()">
          Précédent
        </button>
        <button class="btn btn-secondary continue-btn" (click)="onSkip()" *ngIf="uploadedFiles.length === 0 && config.allowSkip" style="margin-top: 0;">
          {{ config.skipText || 'Continuer' }}
        </button>
        <button class="btn btn-secondary" (click)="onCancel()" *ngIf="uploadedFiles.length > 0">
          Vider
        </button>
        <button class="btn btn-primary" (click)="onUpload()" *ngIf="uploadedFiles.length > 0">
          Suivant
        </button>
      </div>
    </div>
  `,
  styles: [`
    .material-symbols-outlined {
      font-variation-settings:
          'FILL' 0,
          'wght' 400,
          'GRAD' 0,
          'opsz' 24
    }
    
    .upload-container {
      padding: var(--spacing-xs);
    }
    
    .upload-container h2 {
      text-align: center;
      font-size: 1.8rem;
      color: var(--accent-color);
      margin-bottom: var(--spacing-sm);
      text-transform: uppercase;
      letter-spacing: 1px;
      font-weight: bold;
    }

    .doc-type-selector {
      display: flex;
      align-items: center;
      gap: var(--spacing-xs);
      margin-bottom: var(--spacing-xs);
    }

    .select-doc-type {
      padding: 6px 12px;
      border: 1px solid var(--accent-color);
      border-radius: var(--border-radius);
      background: rgba(78, 204, 163, 0.05);
      color: inherit;
      font-size: 0.95rem;
    }

    /* Correction du bug d'affichage blanc sur blanc pour les options */
    .select-doc-type option {
      background-color: #2c3e50; /* Couleur foncée par défaut pour s'assortir au thème sombre probable */
      color: #ffffff;
    }
    
    .drop-zone {
      border: 2px dashed var(--accent-color);
      border-radius: var(--border-radius);
      padding: var(--spacing-xs);
      text-align: center;
      transition: var(--transition);
      background: rgba(78, 204, 163, 0.05);
    }
    
    .drop-zone.drag-over:not(.disabled-zone) {
      background: rgba(78, 204, 163, 0.1);
      border-color: var(--accent-color);
    }
    
    .drop-zone.disabled-zone {
      opacity: 0.6;
      pointer-events: none;
      background: rgba(0, 0, 0, 0.2);
      border-color: rgba(255, 255, 255, 0.1);
    }
    
    .file-list {
      margin-top: var(--spacing-xs);
    }
    
    .file-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: var(--spacing-xs);
      background: rgba(78, 204, 163, 0.05);
      border-radius: var(--border-radius);
      margin-bottom: var(--spacing-xs);
    }
    
    .file-info {
      display: flex;
      align-items: center;
      gap: var(--spacing-xs);
    }
    
    .button-group {
      display: flex;
      gap: var(--spacing-xs);
      margin-top: var(--spacing-xs);
      justify-content: flex-end;
    }
    
    .file-select {
      margin-left: var(--spacing-sm);
      padding: 4px 8px;
    }
  `]
})
export class FileUploadComponent implements OnInit {
  @Input() config!: UploadConfig & { allowPrevious?: boolean };
  @Input() initialFiles: UploadedFile[] = [];
  @Output() filesConfirmed = new EventEmitter<{rawFiles: UploadedFile[], groupedFiles: {files: File[], docType: string, entityName: string}[]}>();
  @Output() previousClicked = new EventEmitter<void>();
  @Output() uploadCancelled = new EventEmitter<void>();
  @Output() pendingFilesChanged = new EventEmitter<number>();
  @Output() skipClicked = new EventEmitter<void>();

  uploadedFiles: UploadedFile[] = [];

  availableEntities: Record<string, string[]> = {
    'cni': [],
    'en': [],
    'pp': []
  };

  constructor(private fileUploadService: FileUploadService) {}

  getEntitiesForDocType(docType: string): string[] {
    return this.availableEntities[docType] || [];
  }

  ngOnInit() {
    if (this.initialFiles && this.initialFiles.length > 0) {
      this.uploadedFiles = [...this.initialFiles];
      // Pour s'assurer que le parent a bien le compte initial quand on revient en arrière
      setTimeout(() => this.pendingFilesChanged.emit(this.uploadedFiles.length));
    }
    
    // Charger dynamiquement les entités OCR depuis l'API Python
    this.fileUploadService.getEntities().subscribe({
      next: (entities) => {
        const cni = entities.filter(e => e.nom && e.nom.startsWith('cni_')).map(e => e.nom);
        const en = entities.filter(e => e.nom && e.nom.startsWith('en_')).map(e => e.nom);
        const pp = entities.filter(e => e.nom && e.nom.startsWith('pp_')).map(e => e.nom);
        this.availableEntities = { 'cni': cni, 'en': en, 'pp': pp };
      },
      error: (err) => console.error("Erreur de chargement des entités OCR", err)
    });
  }

  getFileSize(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    const dropZone = event.currentTarget as HTMLElement;
    dropZone.classList.add('drag-over');
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    const dropZone = event.currentTarget as HTMLElement;
    dropZone.classList.remove('drag-over');
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    const dropZone = event.currentTarget as HTMLElement;
    dropZone.classList.remove('drag-over');

    const files = event.dataTransfer?.files;
    if (files) {
      this.addFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.addFiles(Array.from(input.files));
      input.value = ''; // Réinitialiser pour permettre de resélectionner le même fichier
    }
  }

  addFiles(newFiles: File[]) {
    const validFiles = newFiles.filter(file => this.validateFile(file));

    validFiles.forEach(file => {
      const docType = this.config.docTypes && this.config.docTypes.length > 0 ? this.config.docTypes[0].id : 'en';
      this.uploadedFiles.push({
        file,
        id: uuidv4(),
        progress: 0,
        docType: docType,
        entityName: docType + '_01'
      });
    });
    this.pendingFilesChanged.emit(this.uploadedFiles.length);
  }

  onDocTypeChange(file: UploadedFile) {
    file.entityName = file.docType + '_01';
  }

  validateFile(file: File): boolean {
    if (file.size > this.config.maxFileSize) {
      // Ajouter un message d'erreur
      return false;
    }

    if (!this.config.allowedTypes.includes(file.type)) {
      // Ajouter un message d'erreur
      return false;
    }

    return true;
  }

  removeFile(id: string) {
    this.uploadedFiles = this.uploadedFiles.filter(file => file.id !== id);
    this.pendingFilesChanged.emit(this.uploadedFiles.length);
    if (this.uploadedFiles.length === 0) {
      this.uploadCancelled.emit();
    }
  }

  onCancel() {
    this.uploadedFiles = [];
    this.pendingFilesChanged.emit(0);
    this.uploadCancelled.emit();
  }

  onPrevious() {
    this.previousClicked.emit();
  }

  onSkip() {
    this.skipClicked.emit();
  }

  onUpload() {
    if (this.uploadedFiles.length > 0) {
      // Group files by docType and entityName
      const groups = new Map<string, {files: File[], docType: string, entityName: string}>();

      this.uploadedFiles.forEach(f => {
        const key = f.docType + '_' + (f.entityName || '');
        if (!groups.has(key)) {
          groups.set(key, {files: [], docType: f.docType, entityName: f.entityName || ''});
        }
        groups.get(key)!.files.push(f.file);
      });

      // Emit array of groups
      const emitData: { files: File[], docType: string, entityName: string }[] = [];
      groups.forEach((groupData, key) => {
        emitData.push(groupData);
      });

      this.filesConfirmed.emit({
        rawFiles: [...this.uploadedFiles],
        groupedFiles: emitData
      });
    }
  }
}