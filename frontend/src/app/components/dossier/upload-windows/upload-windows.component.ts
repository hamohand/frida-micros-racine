import { Component, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileUploadComponent } from '../file-upload/file-upload.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { UploadWindowState } from './upload-window.interface';
import { UploadConfig, DocTypeOption, UploadedFile } from '../file-upload/file-upload.interface';
import { Router } from '@angular/router';
import { LireaiEcrirebdService } from "../../../services/lireai-ecrirebd.service";
import { forkJoin, Observable, of } from 'rxjs';

@Component({
  selector: 'app-upload-windows',
  standalone: true,
  imports: [CommonModule, FormsModule, FileUploadComponent],
  template: `
    <div class="windows-container carousel-viewport">
      <div class="carousel-track" [style.transform]="'translateX(-' + getCurrentIndex() * 100 + '%)'">
        
        <!-- Fenêtre Défunt -->
        <div class="window-section">
          <app-file-upload *ngIf="!windows['f1'].isUploading"
              [config]="getUploadConfig('1', 'Défunt', false)"
              [initialFiles]="windows['f1'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f1', $event)"
              (uploadCancelled)="onUploadCancelled('f1')"
          ></app-file-upload>
          <div *ngIf="windows['f1'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Conjoint -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f2'].isUploading">
            <app-file-upload #fileUploadF2
                [config]="getUploadConfig('2', 'Conjoint', true, 'Continuer s\\'il n\\'y a pas de conjoint')"
                [initialFiles]="windows['f2'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f2', $event)"
                (previousClicked)="moveToPreviousWindow('f2')"
                (uploadCancelled)="onUploadCancelled('f2')"
                (pendingFilesChanged)="onPendingFilesChanged('f2', $event)"
                (skipClicked)="continueToNext('f2')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f2'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Fils (Garçons) -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f_garcons'].isUploading">
            <app-file-upload #fileUploadFGarcons
                [config]="getUploadConfig('3', 'Fils (Garçons)', true, 'Continuer s\\'il n\\'y a pas de fils')"
                [initialFiles]="windows['f_garcons'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_garcons', $event)"
                (previousClicked)="moveToPreviousWindow('f_garcons')"
                (uploadCancelled)="onUploadCancelled('f_garcons')"
                (pendingFilesChanged)="onPendingFilesChanged('f_garcons', $event)"
                (skipClicked)="continueToNext('f_garcons')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_garcons'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Filles -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f_filles'].isUploading">
            <app-file-upload #fileUploadFFilles
                [config]="getUploadConfig('3', 'Filles', true, 'Continuer s\\'il n\\'y a pas de filles')"
                [initialFiles]="windows['f_filles'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_filles', $event)"
                (previousClicked)="moveToPreviousWindow('f_filles')"
                (uploadCancelled)="onUploadCancelled('f_filles')"
                (pendingFilesChanged)="onPendingFilesChanged('f_filles', $event)"
                (skipClicked)="continueToNext('f_filles')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_filles'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Père -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f_pere'].isUploading">
            <app-file-upload #fileUploadFPere
                [config]="getUploadConfig('4', 'Père', true, 'Continuer s\\'il n\\'y a pas de père')"
                [initialFiles]="windows['f_pere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_pere', $event)"
                (previousClicked)="moveToPreviousWindow('f_pere')"
                (uploadCancelled)="onUploadCancelled('f_pere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_pere', $event)"
                (skipClicked)="continueToNext('f_pere')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_pere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Grand-père paternel -->
        <div class="window-section" *ngIf="!shouldHideGrandPere()">
          <ng-container *ngIf="!windows['f_grand_pere'].isUploading">
            <app-file-upload #fileUploadFGrandPere
                [config]="getUploadConfig('8', 'Grand-père paternel', true, 'Continuer s\\'il n\\'y a pas de grand-père paternel')"
                [initialFiles]="windows['f_grand_pere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_grand_pere', $event)"
                (previousClicked)="moveToPreviousWindow('f_grand_pere')"
                (uploadCancelled)="onUploadCancelled('f_grand_pere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_grand_pere', $event)"
                (skipClicked)="continueToNext('f_grand_pere')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_grand_pere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Mère -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f_mere'].isUploading">
            <app-file-upload #fileUploadFMere
                [config]="getUploadConfig('4', 'Mère', true, 'Continuer s\\'il n\\'y a pas de mère')"
                [initialFiles]="windows['f_mere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_mere', $event)"
                (previousClicked)="moveToPreviousWindow('f_mere')"
                (uploadCancelled)="onUploadCancelled('f_mere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_mere', $event)"
                (skipClicked)="continueToNext('f_mere')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_mere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Frères et sœurs du défunt -->
        <div class="window-section" *ngIf="!shouldHideSiblings()">
          <ng-container *ngIf="!windows['f5'].isUploading">
            <app-file-upload #fileUploadF5
                [config]="getUploadConfig('5', 'Frères et sœurs du défunt', true, 'Continuer s\\'il n\\'y a pas de frères et sœurs')"
                [initialFiles]="windows['f5'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f5', $event)"
                (previousClicked)="moveToPreviousWindow('f5')"
                (uploadCancelled)="onUploadCancelled('f5')"
                (pendingFilesChanged)="onPendingFilesChanged('f5', $event)"
                (skipClicked)="continueToNext('f5')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f5'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Oncles paternels -->
        <div class="window-section" *ngIf="!shouldHideUncles()">
          <ng-container *ngIf="!windows['f6'].isUploading">
            <app-file-upload #fileUploadF6
                [config]="getUploadConfig('6', 'Oncles paternels', true, 'Continuer s\\'il n\\'y a pas d\\'oncles paternels')"
                [initialFiles]="windows['f6'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f6', $event)"
                (previousClicked)="moveToPreviousWindow('f6')"
                (uploadCancelled)="onUploadCancelled('f6')"
                (pendingFilesChanged)="onPendingFilesChanged('f6', $event)"
                (skipClicked)="continueToNext('f6')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f6'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Cousins paternels -->
        <div class="window-section" *ngIf="!shouldHideCousins()">
          <ng-container *ngIf="!windows['f7'].isUploading">
            <app-file-upload #fileUploadF7
                [config]="getUploadConfig('7', 'Cousins paternels', true, 'Continuer s\\'il n\\'y a pas de cousins paternels')"
                [initialFiles]="windows['f7'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f7', $event)"
                (previousClicked)="moveToPreviousWindow('f7')"
                (uploadCancelled)="onUploadCancelled('f7')"
                (pendingFilesChanged)="onPendingFilesChanged('f7', $event)"
                (skipClicked)="continueToNext('f7')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f7'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre témoins -->
        <div class="window-section">
          <ng-container *ngIf="!windows['f_temoins'].isUploading">
            <app-file-upload #fileUploadFTemoins
                [config]="getUploadConfig('11', 'Témoins', true, 'Continuer s\\'il n\\'y a pas de temoin')"
                [initialFiles]="windows['f_temoins'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_temoins', $event)"
                (previousClicked)="moveToPreviousWindow('f_temoins')"
                (uploadCancelled)="onUploadCancelled('f_temoins')"
                (pendingFilesChanged)="onPendingFilesChanged('f_temoins', $event)"
                (skipClicked)="continueToNext('f_temoins')"
            ></app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_temoins'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Lecture AI ---------------- -->
        <div class="window-section">
          <div class="drop-zone">
            <div class="upload-container">
              <h2>Validation finale du dossier</h2>
              <div class="drop-zone">
                <button *ngIf="!endReading && !isReading && !isBatchUploaded"
                    class="btn btn-secondary continue-btn"
                    (click)="moveToPreviousWindow('f_ai')" [disabled]="isUploadingFiles"
                    style="margin-right: 10px;"
                >
                  <span>Précédent</span>
                </button>

                <!-- UI de sélection du mode -->
                <div *ngIf="!endReading && !isReading && !isBatchUploaded" class="mode-selector" style="margin-bottom: 20px; text-align: left; padding: 15px; border: 1px solid var(--accent-color); border-radius: 8px;">
                  <h3 style="margin-top: 0; font-size: 1.1rem; color: var(--accent-color);">Mode de traitement :</h3>
                  <div style="margin-bottom: 8px;">
                    <label style="cursor: pointer;">
                      <input type="radio" name="ocrMode" value="rapide" [(ngModel)]="ocrMode" [disabled]="isUploadingFiles">
                      <strong>Rapide</strong> - Lecture immédiate des données
                    </label>
                  </div>
                  <div style="margin-bottom: 8px;">
                    <label style="cursor: pointer;">
                      <input type="radio" name="ocrMode" value="approfondi" [(ngModel)]="ocrMode" [disabled]="isUploadingFiles">
                      <strong>Approfondi</strong> - Qualité maximale immédiate
                    </label>
                  </div>
                  <div>
                    <label style="cursor: pointer;">
                      <input type="radio" name="ocrMode" value="batch" [(ngModel)]="ocrMode" [disabled]="isUploadingFiles">
                      <strong>Batch (Différé)</strong> - Uploader seulement (pour traitement de nuit)
                    </label>
                  </div>
                </div>

                <!-- Bouton de lancement -->
                <button *ngIf="!endReading && !isReading && !isBatchUploaded"
                    class="btn btn-primary continue-btn"
                    (click)="onLireAiEcrireBd()" [disabled]="isUploadingFiles"
                >
                  <span *ngIf="!isUploadingFiles">Valider et Lancer</span>
                  <span *ngIf="isUploadingFiles"><span class="spinner"></span> Envoi des fichiers en cours...</span>
                </button>

                <!-- Vue en mode asynchrone (Pendant l'OCR) -->
                <div *ngIf="isReading && !endReading" style="margin-top: 15px; text-align: center; width: 100%;">
                   <p style="color: springgreen; font-weight: bold; margin-bottom: 5px;"><span class="spinner" style="margin-right: 8px;"></span> Analyse de vos documents en cours...</p>
                   <div style="display: flex; gap: 10px; justify-content: center;">
                       <button class="btn btn-secondary continue-btn" (click)="accueil()">
                         Retour à l'Accueil pour un nouveau dossier
                       </button>
                   </div>
                </div>
                
                <!-- Vue terminée - Batch -->
                <div *ngIf="isBatchUploaded" style="margin-top: 15px; text-align: center; width: 100%;">
                    <p style="color: springgreen; font-weight: bold; margin-bottom: 15px;">Les documents ont bien été sauvegardés pour un traitement par lot différé.</p>
                    <div style="display: flex; gap: 10px; justify-content: center;">
                       <button class="btn btn-primary continue-btn" (click)="pageCreation()">
                         Nouveau dossier
                       </button>
                       <button class="btn btn-secondary continue-btn" (click)="accueil()">
                         Retour à l'Accueil
                       </button>
                    </div>
                </div>
                
                <!-- Vue terminée -->
                <button *ngIf="endReading"
                    class="btn btn-primary continue-btn"
                    (click)="onAfficheFrida()" >
                    <span>Afficher la frida</span>
                </button>
                <button *ngIf="endReading"
                        class="btn btn-primary continue-btn"
                        (click)="pageCreation()" >
                  <span>Nouvelle frida</span>
                </button>
                <button *ngIf="endReading"
                        class="btn btn-primary continue-btn"
                        (click)="accueil()" >
                  <span>Accueil</span>
                </button>
              </div>
            </div>
          </div>
        </div>

      </div> <!-- End Carousel Track -->
    </div> <!-- End Windows Container -->
  `,
  styles: [`
    h2 {
      text-align: center;
      font-size: 1.8rem;
      color: var(--accent-color);
      margin-bottom: var(--spacing-sm);
      text-transform: uppercase;
      letter-spacing: 1px;
      font-weight: bold;
    }

    /* Ajout du style pour le spinner */
    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid #f3f3f3;
      border-top: 2px solid #3498db;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      display: inline-block;
      margin-right: 5px;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
    
    .upload-container {
      padding: var(--spacing-lg);
    }

    .drop-zone {
      border: 2px dashed var(--accent-color);
      border-radius: var(--border-radius);
      padding: var(--spacing-lg);
      text-align: center;
      transition: var(--transition);
      background: rgba(78, 204, 163, 0.05);
    }
    
    .loading-zone {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 40px;
      font-size: 1.2rem;
      border: 2px solid transparent;
    }
    
    .carousel-viewport {
      overflow-x: hidden; /* Hide horizontal overflow for sliding */
      overflow-y: auto;   /* Allow vertical scrolling if the content is too tall */
      width: 100%;
      height: 100%;
      max-height: 85vh;   /* Prevent the component from exceeding the screen height and being clipped by central flexbox */
      padding-top: 20px;
      padding-bottom: 20px;
    }
    
    .carousel-track {
      display: flex;
      flex-direction: row;
      flex-wrap: nowrap;
      transition: transform 0.5s cubic-bezier(0.25, 0.8, 0.25, 1);
      width: 100%;
      align-items: flex-start; /* Prevent children from stretching to match tallest item */
    }

    .window-section {
      min-width: 100%;
      flex: 0 0 100%;
      padding: 0 var(--spacing-sm);
      box-sizing: border-box;
    }

    .continue-btn {
      margin-top: var(--spacing-md);
    }
  `]
})
export class UploadWindowsComponent implements OnInit {
  windows: Record<string, UploadWindowState> = {
    // Défunt
    f1: { isVisible: true, hasFiles: false, isUploading: false, path: '1' },
    // Héritiers
    f2: { isVisible: false, hasFiles: false, isUploading: false, path: '2' },  // Conjoint
    f_garcons: { isVisible: false, hasFiles: false, isUploading: false, path: '3' }, // Fils
    f_filles: { isVisible: false, hasFiles: false, isUploading: false, path: '3' }, // Filles
    f_pere: { isVisible: false, hasFiles: false, isUploading: false, path: '4' }, // Père
    f_grand_pere: { isVisible: false, hasFiles: false, isUploading: false, path: '8' }, // Grand-père paternel
    f_mere: { isVisible: false, hasFiles: false, isUploading: false, path: '4' }, // Mère
    f5: { isVisible: false, hasFiles: false, isUploading: false, path: '5' },  // Frères et sœurs
    f6: { isVisible: false, hasFiles: false, isUploading: false, path: '6' },  // Oncles paternels
    f7: { isVisible: false, hasFiles: false, isUploading: false, path: '7' },  // Cousins paternels
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, isUploading: false, path: '11' },
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, isUploading: false, path: '' }
  };

  isUploadingFiles = false;
  isReading = false;
  endReading = false;
  isBatchUploaded = false;
  numFrida: String = "";
  ocrMode: 'rapide' | 'approfondi' | 'batch' = 'rapide';

  getActiveWindowKeys(): string[] {
    const keys = ['f1', 'f2', 'f_garcons', 'f_filles', 'f_pere'];
    if (!this.shouldHideGrandPere()) {
      keys.push('f_grand_pere');
    }
    keys.push('f_mere');
    if (!this.shouldHideSiblings()) {
      keys.push('f5');
    }
    if (!this.shouldHideUncles()) {
      keys.push('f6');
    }
    if (!this.shouldHideCousins()) {
      keys.push('f7');
    }
    keys.push('f_temoins', 'f_ai');
    return keys;
  }

  getCurrentIndex(): number {
    const windowKeys = this.getActiveWindowKeys();
    const activeKey = windowKeys.find(key => this.windows[key].isVisible);
    return windowKeys.indexOf(activeKey || 'f1');
  }

  shouldHideGrandPere(): boolean {
    const hide = this.windows['f_pere'].hasFiles;
    if (hide) {
      this.clearWindow('f_grand_pere');
    }
    return hide;
  }

  shouldHideSiblings(): boolean {
    const hide = this.windows['f_garcons'].hasFiles || this.windows['f_pere'].hasFiles || this.windows['f_grand_pere'].hasFiles;
    if (hide) {
      this.clearWindow('f5');
    }
    return hide;
  }

  shouldHideUncles(): boolean {
    const hide = this.shouldHideSiblings() || this.windows['f5'].hasFiles;
    if (hide) {
      this.clearWindow('f6');
    }
    return hide;
  }

  shouldHideCousins(): boolean {
    const hide = this.shouldHideUncles() || this.windows['f6'].hasFiles;
    if (hide) {
      this.clearWindow('f7');
    }
    return hide;
  }

  private clearWindow(key: string) {
    const win = this.windows[key];
    if (win && (win.hasFiles || win.rawFiles?.length || win.groupedFiles?.length)) {
      win.hasFiles = false;
      win.rawFiles = [];
      win.groupedFiles = [];
    }
  }

  constructor(private fileUploadService: FileUploadService, private router: Router,
    private lireaiEcrirebdService: LireaiEcrirebdService) { }

  ngOnInit() {
  }

  /** Types de documents disponibles pour le sélecteur */
  docTypeOptions: DocTypeOption[] = [
    { id: 'cni', label: 'Carte nationale d\'identité' },
    { id: 'en', label: 'Extrait de naissance' },
    { id: 'pp', label: 'Passeport' }
  ];

  getUploadConfig(path: string, title: string, allowPrevious: boolean = true, skipText: string = ''): UploadConfig & { allowPrevious?: boolean } {
    return {
      maxFileSize: 5 * 1024 * 1024,
      allowedTypes: ['image/jpeg', 'image/png', 'application/pdf'],
      uploadPath: path,
      title: title,
      docTypes: this.docTypeOptions,
      allowPrevious: allowPrevious,
      allowSkip: skipText.length > 0,
      skipText: skipText
    };
  }

  onFilesConfirmed(window: string, events: { rawFiles: UploadedFile[], groupedFiles: { files: File[], docType: string, entityName: string }[] }) {
    const currentWindow = this.windows[window];
    if (currentWindow) {
      if (events.rawFiles.length > 0) {
        currentWindow.hasFiles = true;
        currentWindow.rawFiles = events.rawFiles;
        currentWindow.groupedFiles = events.groupedFiles;
      } else {
        currentWindow.hasFiles = false;
        currentWindow.rawFiles = [];
        currentWindow.groupedFiles = [];
      }
      this.moveToNextWindow(window);
    }
  }

  onUploadCancelled(window: string) {
    const currentWindow = this.windows[window];
    if (currentWindow) {
      currentWindow.hasFiles = false;
      currentWindow.rawFiles = [];
      currentWindow.groupedFiles = [];
    }
  }

  onPendingFilesChanged(windowKey: string, count: number) {
    if (this.windows[windowKey]) {
      this.windows[windowKey].pendingFilesCount = count;
    }
  }

  continueToNext(window: string) {
    this.moveToNextWindow(window);
  }

  // Permet de reculer d'une fenêtre
  moveToPreviousWindow(currentWindow: string) {
    const windowKeys = this.getActiveWindowKeys();
    const currentIndex = windowKeys.indexOf(currentWindow);

    if (currentIndex > 0) {
      this.windows[currentWindow].isVisible = false;
      this.windows[windowKeys[currentIndex - 1]].isVisible = true;
    }
  }

  private moveToNextWindow(currentWindow: string) {
    const windowKeys = this.getActiveWindowKeys();
    const currentIndex = windowKeys.indexOf(currentWindow);

    if (currentIndex < windowKeys.length - 1) {
      this.windows[currentWindow].isVisible = false;
      this.windows[windowKeys[currentIndex + 1]].isVisible = true;
    }
  }

  onLireAiEcrireBd(): void {
    this.isUploadingFiles = true;

    const allUploadObservables: Observable<any>[] = [];

    this.getActiveWindowKeys().forEach(key => {
      if (key === 'f_ai') return;
      const win = this.windows[key];
      if (win && win.groupedFiles && win.groupedFiles.length > 0) {
        win.groupedFiles.forEach(group => {
          let uploadPath = win.path + '_' + group.docType;
          if (group.entityName && group.entityName.trim() !== '') {
            uploadPath += '_' + group.entityName;
          }
          allUploadObservables.push(this.fileUploadService.uploadFiles(group.files, uploadPath));
        });
      }
    });

    if (allUploadObservables.length > 0) {
      forkJoin(allUploadObservables).subscribe({
        next: () => {
          this.isUploadingFiles = false;
          if (this.ocrMode === 'batch') {
            this.isBatchUploaded = true;
          } else {
            this.launchOcrProcess();
          }
        },
        error: (err) => {
          console.error('Erreur lors du transfert des dossiers au serveur :', err);
          this.isUploadingFiles = false;
          alert("Une erreur de réseau empêche le transfert des fichiers.");
        }
      });
    } else {
      this.isUploadingFiles = false;
      if (this.ocrMode === 'batch') {
        this.isBatchUploaded = true;
      } else {
        this.launchOcrProcess();
      }
    }
  }

  private launchOcrProcess() {
    this.isReading = true;
    this.lireaiEcrirebdService.lireAiEcrireBd(this.ocrMode).subscribe({
      next: (data) => {
        console.log('Réponse du serveur UploadWindowsComponent: ', data);
        this.numFrida = data.numFrida;
        this.isReading = false;
        this.endReading = true;
      },
      error: (error) => {
        console.error('Erreur lors de l’écriture UploadWindowsComponent:', error);
        this.isReading = false;
        this.endReading = true; 
      },
    });
  }

  onAfficheFrida() {
    console.log('Processus onAfficheFrida !');
    this.router.navigate(['/frida'], { queryParams: { numFrida: this.numFrida } });
  }

  pageCreation() {
    this.router.navigate(['/create']);
  }

  accueil() {
    this.router.navigate(['']);
  }
}