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
          <app-file-upload *ngIf="!windows['f4'].isUploading && !isAnalyzing"
              [config]="getUploadConfig('4', 'Parents du défunt')"
              [initialFiles]="windows['f4'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f4', $event)"
              (previousClicked)="moveToPreviousWindow('f4')"
              (uploadCancelled)="onUploadCancelled('f4')"
          ></app-file-upload>
          <div *ngIf="windows['f4'].isUploading || isAnalyzing" class="drop-zone loading-zone">
            <span class="spinner"></span> {{ isAnalyzing ? 'Analyse de la composition en cours...' : 'Sauvegarde en cours...' }}
          </div>
          <button
              *ngIf="!windows['f4'].hasFiles && !windows['f4'].isUploading && !isAnalyzing"
              class="btn btn-secondary continue-btn"
              (click)="analyzeAndContinue()"
          >
            Continuer s'il n'y a pas de parents
          </button>
        </div>

        <!-- Fenêtre Frères et sœurs du défunt -->
        <div class="window-section" *ngIf="!shouldHideSiblings()">
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

        <!-- Fenêtre Autres héritiers -->
        <div class="window-section" *ngIf="hasRemainingPartOcr">
          <app-file-upload *ngIf="!windows['f_autres'].isUploading"
              [config]="getUploadConfig('6', 'Autres éventuels héritiers (oncles, cousins, etc.)')"
              [initialFiles]="windows['f_autres'].rawFiles || []"
              (filesConfirmed)="onFilesConfirmed('f_autres', $event)"
              (previousClicked)="moveToPreviousWindow('f_autres')"
              (uploadCancelled)="onUploadCancelled('f_autres')"
          ></app-file-upload>
          <div *ngIf="windows['f_autres'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
          <button
              *ngIf="!windows['f_autres'].hasFiles && !windows['f_autres'].isUploading"
              class="btn btn-secondary continue-btn"
              (click)="continueToNext('f_autres')"
          >
            Continuer s'il n'y a pas d'autres héritiers
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
    f3: { isVisible: false, hasFiles: false, isUploading: false, path: '3' },  // Enfants
    f4: { isVisible: false, hasFiles: false, isUploading: false, path: '4' },  // Parents du défunt
    f5: { isVisible: false, hasFiles: false, isUploading: false, path: '5' },  // Frères et sœurs
    f_autres: { isVisible: false, hasFiles: false, isUploading: false, path: '6' }, // Autres héritiers
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, isUploading: false, path: '11' },
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, isUploading: false, path: '' }
  };

  isUploadingFiles = false;
  isReading = false;
  endReading = false;
  isBatchUploaded = false;
  numFrida: String = "1956010320250116";
  ocrMode: 'rapide' | 'approfondi' | 'batch' = 'rapide';

  hasBoyOcr = false;
  hasFatherOcr = false;
  hasRemainingPartOcr = false;
  isAnalyzing = false;
  hasAnalyzedComposition = false;

  getActiveWindowKeys(): string[] {
    const keys = ['f1', 'f2', 'f3', 'f4'];
    if (!this.shouldHideSiblings()) {
      keys.push('f5');
    }
    if (this.hasRemainingPartOcr) {
      keys.push('f_autres');
    }
    keys.push('f_temoins', 'f_ai');
    return keys;
  }

  getCurrentIndex(): number {
    const windowKeys = this.getActiveWindowKeys();
    const activeKey = windowKeys.find(key => this.windows[key].isVisible);
    return windowKeys.indexOf(activeKey || 'f1');
  }

  hasBoyEnfant(): boolean {
    const enfantsWindow = this.windows['f3'];
    if (!enfantsWindow || !enfantsWindow.rawFiles) return false;
    
    const boyKeywords = ['garcon', 'garçon', 'fils', 'boy', 'ذكر', 'masculin', 'ibn', 'ولد', 'ابن'];
    return enfantsWindow.rawFiles.some(file => {
      const fileName = file.file?.name?.toLowerCase() || '';
      const entityName = file.entityName?.toLowerCase() || '';
      
      const checkKeyword = (text: string) => {
        return boyKeywords.some(keyword => {
          if (keyword === 'ابن') {
            return text.includes(keyword) && !text.includes('ابنة');
          }
          return text.includes(keyword);
        });
      };
      
      return checkKeyword(fileName) || checkKeyword(entityName);
    });
  }

  hasFatherParent(): boolean {
    const parentsWindow = this.windows['f4'];
    if (!parentsWindow || !parentsWindow.rawFiles) return false;
    
    const fatherKeywords = ['pere', 'père', 'father', 'ذكر', 'masculin', 'والد', 'أب', 'اب'];
    return parentsWindow.rawFiles.some(file => {
      const fileName = file.file?.name?.toLowerCase() || '';
      const entityName = file.entityName?.toLowerCase() || '';
      
      const checkKeyword = (text: string) => {
        return fatherKeywords.some(keyword => {
          if (keyword === 'والد') {
            return text.includes(keyword) && !text.includes('والدة');
          }
          if (keyword === 'أب' || keyword === 'اب') {
            return text.includes(keyword) && !text.includes('ابن') && !text.includes('ابنة');
          }
          return text.includes(keyword);
        });
      };
      
      return checkKeyword(fileName) || checkKeyword(entityName);
    });
  }

  shouldHideSiblings(): boolean {
    const hide = this.hasAnalyzedComposition ? (this.hasBoyOcr || this.hasFatherOcr) : (this.hasBoyEnfant() || this.hasFatherParent());
    if (hide) {
      const siblingWindow = this.windows['f5'];
      if (siblingWindow && (siblingWindow.hasFiles || siblingWindow.rawFiles?.length || siblingWindow.groupedFiles?.length)) {
        siblingWindow.hasFiles = false;
        siblingWindow.rawFiles = [];
        siblingWindow.groupedFiles = [];
      }
    }
    return hide;
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
      if (window === 'f4') {
        this.analyzeAndContinue();
      } else {
        this.moveToNextWindow(window);
      }
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

  analyzeAndContinue() {
    this.isAnalyzing = true;
    const uploadObservables: Observable<any>[] = [];

    ['f1', 'f2', 'f3', 'f4'].forEach(key => {
      const win = this.windows[key];
      if (win.groupedFiles && win.groupedFiles.length > 0) {
        win.groupedFiles.forEach(group => {
          let uploadPath = win.path + '_' + group.docType;
          if (group.entityName && group.entityName.trim() !== '') {
            uploadPath += '_' + group.entityName;
          }
          uploadObservables.push(this.fileUploadService.uploadFiles(group.files, uploadPath));
        });
      }
    });

    if (uploadObservables.length > 0) {
      forkJoin(uploadObservables).subscribe({
        next: () => {
          this.callAnalyzeEndpoint();
        },
        error: (err) => {
          console.error('Erreur upload partiel :', err);
          this.isAnalyzing = false;
          this.moveToNextWindow('f4');
        }
      });
    } else {
      this.callAnalyzeEndpoint();
    }
  }

  private callAnalyzeEndpoint() {
    this.lireaiEcrirebdService.analyzeComposition(this.ocrMode).subscribe({
      next: (res: any) => {
        this.hasAnalyzedComposition = true;
        if (res.numFrida) this.numFrida = res.numFrida;
        this.hasBoyOcr = res.hasBoy;
        this.hasFatherOcr = res.hasFather;
        this.hasRemainingPartOcr = res.hasRemainingPart;
        this.isAnalyzing = false;
        this.moveToNextWindow('f4');
      },
      error: (err) => {
        console.error('Erreur analyze-composition :', err);
        this.isAnalyzing = false;
        this.moveToNextWindow('f4');
      }
    });
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
    // Étape finale : Upload de f5, f_autres et témoins (Asynchrone Backend)
    this.isUploadingFiles = true;

    const allUploadObservables: Observable<any>[] = [];

    const keysToUpload = [];
    if (!this.shouldHideSiblings()) keysToUpload.push('f5');
    if (this.hasRemainingPartOcr) keysToUpload.push('f_autres');
    keysToUpload.push('f_temoins');

    keysToUpload.forEach(key => {
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
    this.lireaiEcrirebdService.updateFrida(this.numFrida as string, this.ocrMode).subscribe({
      next: (data) => {
        console.log('Réponse du serveur UploadWindowsComponent (update): ', data);
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