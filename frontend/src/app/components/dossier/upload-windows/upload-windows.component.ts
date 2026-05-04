import { Component, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule, FileUploadComponent],
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
          <app-file-upload *ngIf="!windows['f2'].isUploading"
              [config]="getUploadConfig('2', 'Conjoint')"
              [initialFiles]="windows['f2'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f2', $event)"
              (previousClicked)="moveToPreviousWindow('f2')"
              (uploadCancelled)="onUploadCancelled('f2')"
          ></app-file-upload>
          <div *ngIf="windows['f2'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f2'].hasFiles && !windows['f2'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f2')"
          >
            Continuer s'il n'y a pas de conjoint
          </button>
        </div>

        <!-- Fenêtre Enfants -->
        <div class="window-section">
          <app-file-upload *ngIf="!windows['f3'].isUploading"
              [config]="getUploadConfig('3', 'Enfants')"
              [initialFiles]="windows['f3'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f3', $event)"
              (previousClicked)="moveToPreviousWindow('f3')"
              (uploadCancelled)="onUploadCancelled('f3')"
          ></app-file-upload>
          <div *ngIf="windows['f3'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f3'].hasFiles && !windows['f3'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f3')"
          >
            Continuer s'il n'y a pas d'enfants
          </button>
        </div>

        <!-- Fenêtre Parents du défunt -->
        <div class="window-section">
          <app-file-upload *ngIf="!windows['f4'].isUploading"
              [config]="getUploadConfig('4', 'Parents du défunt')"
              [initialFiles]="windows['f4'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f4', $event)"
              (previousClicked)="moveToPreviousWindow('f4')"
              (uploadCancelled)="onUploadCancelled('f4')"
          ></app-file-upload>
          <div *ngIf="windows['f4'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f4'].hasFiles && !windows['f4'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f4')"
          >
            Continuer s'il n'y a pas de parents
          </button>
        </div>

        <!-- Fenêtre Frères et sœurs du défunt -->
        <div class="window-section">
          <app-file-upload *ngIf="!windows['f5'].isUploading"
              [config]="getUploadConfig('5', 'Frères et sœurs du défunt')"
              [initialFiles]="windows['f5'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f5', $event)"
              (previousClicked)="moveToPreviousWindow('f5')"
              (uploadCancelled)="onUploadCancelled('f5')"
          ></app-file-upload>
          <div *ngIf="windows['f5'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f5'].hasFiles && !windows['f5'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f5')"
          >
            Continuer s'il n'y a pas de frères et sœurs
          </button>
        </div>

        <!-- Fenêtre témoins -->
        <div class="window-section">
          <app-file-upload *ngIf="!windows['f_temoins'].isUploading"
              [config]="getUploadConfig('11', 'Témoins')"
              [initialFiles]="windows['f_temoins'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f_temoins', $event)"
              (previousClicked)="moveToPreviousWindow('f_temoins')"
              (uploadCancelled)="onUploadCancelled('f_temoins')"
          ></app-file-upload>
          <div *ngIf="windows['f_temoins'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f_temoins'].hasFiles && !windows['f_temoins'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f_temoins')"
          >
            Continuer s'il n'y a pas de temoin
          </button>
        </div>

        <!-- Fenêtre Lecture AI ---------------- -->
        <div class="window-section">
          <div class="drop-zone">
            <div class="upload-container">
              <h2>Validation finale du dossier</h2>
              <div class="drop-zone">
                <button *ngIf="!endReading && !isReading"
                    class="btn btn-secondary continue-btn"
                    (click)="moveToPreviousWindow('f_ai')" [disabled]="isUploadingFiles"
                    style="margin-right: 10px;"
                >
                  <span>Précédent</span>
                </button>

                <!-- Bouton de lancement -->
                <button *ngIf="!endReading && !isReading"
                    class="btn btn-primary continue-btn"
                    (click)="onLireAiEcrireBd()" [disabled]="isUploadingFiles"
                >
                  <span *ngIf="!isUploadingFiles">Transférer les documents et Lancer la lecture</span>
                  <span *ngIf="isUploadingFiles"><span class="spinner"></span> Envoi des fichiers en cours...</span>
                </button>

                <!-- Vue en mode asynchrone (Pendant l'OCR) -->
                <div *ngIf="isReading && !endReading" style="margin-top: 15px; text-align: center; width: 100%;">
                   <p style="color: springgreen; font-weight: bold; margin-bottom: 5px;"><span class="spinner" style="margin-right: 8px;"></span> L'Intelligence Artificielle analyse vos documents en arrière-plan...</p>
                   <div style="display: flex; gap: 10px; justify-content: center;">
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
    f3: { isVisible: false, hasFiles: false, isUploading: false, path: '3' },  // Enfants
    f4: { isVisible: false, hasFiles: false, isUploading: false, path: '4' },  // Parents du défunt
    f5: { isVisible: false, hasFiles: false, isUploading: false, path: '5' },  // Frères et sœurs
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, isUploading: false, path: '11' },
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, isUploading: false, path: '' }
  };

  isUploadingFiles = false;
  isReading = false;
  endReading = false;
  numFrida: String = "1956010320250116";

  getCurrentIndex(): number {
    const windowKeys = ['f1', 'f2', 'f3', 'f4', 'f5', 'f_temoins', 'f_ai'];
    const activeKey = windowKeys.find(key => this.windows[key].isVisible);
    return windowKeys.indexOf(activeKey || 'f1');
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

  getUploadConfig(path: string, title: string, allowPrevious: boolean = true): UploadConfig & { allowPrevious?: boolean } {
    return {
      maxFileSize: 5 * 1024 * 1024,
      allowedTypes: ['image/jpeg', 'image/png', 'application/pdf'],
      uploadPath: path,
      title: title,
      docTypes: this.docTypeOptions,
      allowPrevious: allowPrevious
    };
  }

  onFilesConfirmed(window: string, events: { rawFiles: UploadedFile[], groupedFiles: { files: File[], docType: string }[] }) {
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

  continueToNext(window: string) {
    this.moveToNextWindow(window);
  }

  // Permet de reculer d'une fenêtre
  moveToPreviousWindow(currentWindow: string) {
    const windowKeys = ['f1', 'f2', 'f3', 'f4', 'f5', 'f_temoins', 'f_ai'];
    const currentIndex = windowKeys.indexOf(currentWindow);

    if (currentIndex > 0) {
      this.windows[currentWindow].isVisible = false;
      this.windows[windowKeys[currentIndex - 1]].isVisible = true;
    }
  }

  private moveToNextWindow(currentWindow: string) {
    const windowKeys = ['f1', 'f2', 'f3', 'f4', 'f5', 'f_temoins', 'f_ai'];
    const currentIndex = windowKeys.indexOf(currentWindow);

    if (currentIndex < windowKeys.length - 1) {
      this.windows[currentWindow].isVisible = false;
      this.windows[windowKeys[currentIndex + 1]].isVisible = true;
    }
  }

  onLireAiEcrireBd(): void {
    // Étape 1 : Upload de TOUS les fichiers accumulés (Asynchrone Backend)
    this.isUploadingFiles = true;

    const allUploadObservables: Observable<any>[] = [];

    // On parcourt toutes les fenêtres pour récupérer les événements "groupedFiles"
    Object.keys(this.windows).forEach(key => {
      const win = this.windows[key];
      if (win.groupedFiles && win.groupedFiles.length > 0) {
        win.groupedFiles.forEach(group => {
          const uploadPath = win.path + '_' + group.docType;
          allUploadObservables.push(this.fileUploadService.uploadFiles(group.files, uploadPath));
        });
      }
    });

    if (allUploadObservables.length > 0) {
      forkJoin(allUploadObservables).subscribe({
        next: () => {
          // Tous les uploads sont terminés avec succès
          this.isUploadingFiles = false;
          this.launchOcrProcess();
        },
        error: (err) => {
          console.error('Erreur lors du transfert des dossiers au serveur :', err);
          this.isUploadingFiles = false;
          alert("Une erreur de réseau empêche le transfert des fichiers.");
        }
      });
    } else {
      // Aucun fichier à uploader (impossible en théorie vu que f1 est requis)
      this.isUploadingFiles = false;
      this.launchOcrProcess();
    }
  }

  private launchOcrProcess() {
    // Étape 2 : Lancement officiel de la lecture OCR + Traitement Frida
    this.isReading = true;
    this.lireaiEcrirebdService.lireAiEcrireBd().subscribe({
      next: (data) => {
        console.log('Réponse du serveur UploadWindowsComponent: ', data);
        console.log('numFrida: ', data.numFrida);
        this.numFrida = data.numFrida;
        this.isReading = false;
        this.endReading = true;
      },
      error: (error) => {
        console.error('Erreur lors de l’écriture UploadWindowsComponent:', error);
        this.isReading = false;
        alert("L'analyse a échouée.");
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