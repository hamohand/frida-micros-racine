import { Component, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadComponent } from '../file-upload/file-upload.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { UploadWindowState } from './upload-window.interface';
import { UploadConfig, DocTypeOption } from '../file-upload/file-upload.interface';
import { Router } from '@angular/router';
import { LireaiEcrirebdService } from "../../../services/lireai-ecrirebd.service";
import { forkJoin, Observable } from 'rxjs';

@Component({
  selector: 'app-upload-windows',
  standalone: true,
  imports: [CommonModule, FileUploadComponent],
  template: `
    <div class="windows-container">
      <!-- Fenêtre Défunt -->
      <div *ngIf="windows['f1'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('1', 'Défunt')"
            (filesUploaded)="onFilesUploaded('f1', $event)"
            (uploadCancelled)="onUploadCancelled('f1')"
        ></app-file-upload>
      </div>

      <!-- Fenêtre Conjoint -->
      <div *ngIf="windows['f2'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('2', 'Conjoint')"
            (filesUploaded)="onFilesUploaded('f2', $event)"
            (uploadCancelled)="onUploadCancelled('f2')"
        ></app-file-upload>
        <button
            *ngIf="!windows['f2'].hasFiles"
            class="btn btn-secondary continue-btn"
            (click)="continueToNext('f2')"
        >
          Continuer s'il n'y a pas de conjoint
        </button>
      </div>

      <!-- Fenêtre Enfants -->
      <div *ngIf="windows['f3'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('3', 'Enfants')"
            (filesUploaded)="onFilesUploaded('f3', $event)"
            (uploadCancelled)="onUploadCancelled('f3')"
        ></app-file-upload>
        <button
            *ngIf="!windows['f3'].hasFiles"
            class="btn btn-secondary continue-btn"
            (click)="continueToNext('f3')"
        >
          Continuer s'il n'y a pas d'enfants
        </button>
      </div>

      <!-- Fenêtre Parents du défunt -->
      <div *ngIf="windows['f4'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('4', 'Parents du défunt')"
            (filesUploaded)="onFilesUploaded('f4', $event)"
            (uploadCancelled)="onUploadCancelled('f4')"
        ></app-file-upload>
        <button
            *ngIf="!windows['f4'].hasFiles"
            class="btn btn-secondary continue-btn"
            (click)="continueToNext('f4')"
        >
          Continuer s'il n'y a pas de parents
        </button>
      </div>

      <!-- Fenêtre Frères et sœurs du défunt -->
      <div *ngIf="windows['f5'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('5', 'Frères et sœurs du défunt')"
            (filesUploaded)="onFilesUploaded('f5', $event)"
            (uploadCancelled)="onUploadCancelled('f5')"
        ></app-file-upload>
        <button
            *ngIf="!windows['f5'].hasFiles"
            class="btn btn-secondary continue-btn"
            (click)="continueToNext('f5')"
        >
          Continuer s'il n'y a pas de frères et sœurs
        </button>
      </div>

      <!-- Fenêtre témoins -->
      <div *ngIf="windows['f_temoins'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('11', 'Témoins')"
            (filesUploaded)="onFilesUploaded('f_temoins', $event)"
            (uploadCancelled)="onUploadCancelled('f_temoins')"
        ></app-file-upload>
        <button
            *ngIf="!windows['f_temoins'].hasFiles"
            class="btn btn-secondary continue-btn"
            (click)="continueToNext('f_temoins')"
        >
          Continuer s'il n'y a pas de temoin
        </button>
      </div>
    </div>

    <!-- Fenêtre Lecture AI ---------------- -->
    <div *ngIf="windows['f_ai'].isVisible" class="window-section"
    >
      <div class="drop-zone">
        <div class="upload-container">
          <h2>Lecture des documents</h2>
          <div class="drop-zone">
            
            <button *ngIf="!endReading"
                class="btn btn-primary continue-btn"
                (click)="onLireAiEcrireBd()" [disabled]="isReading"
            >
<!--               <button *ngIf="!endReading"
                          class="btn btn-primary continue-btn"
                   (click)="onAfficheFrida()"
                   >-->
              <span *ngIf="!isReading">Lancer la lecture des documents</span>
              <span *ngIf="isReading"><span class="spinner"></span> Lecture en cours...</span>
            </button>
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
  `,
  styles: [`
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
    .windows-container {
      padding: var(--spacing-lg);
    }

    .window-section {
      margin-bottom: var(--spacing-lg);
    }

    .continue-btn {
      margin-top: var(--spacing-md);
    }
  `]
})
export class UploadWindowsComponent implements OnInit {
  windows: Record<string, UploadWindowState> = {
    // Défunt
    f1: { isVisible: true, hasFiles: false, path: '1' },
    // Héritiers
    f2: { isVisible: false, hasFiles: false, path: '2' },  // Conjoint
    f3: { isVisible: false, hasFiles: false, path: '3' },  // Enfants
    f4: { isVisible: false, hasFiles: false, path: '4' },  // Parents du défunt
    f5: { isVisible: false, hasFiles: false, path: '5' },  // Frères et sœurs
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, path: '11' },
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, path: '' }
  };

  isReading = false;
  endReading = false;
  numFrida: String = "1956010320250116";
  //numFrida: String = "19560103202501171733";
  //numFrida: String = "";

  constructor(private fileUploadService: FileUploadService, private router: Router,
    private lireaiEcrirebdService: LireaiEcrirebdService) { }

  ngOnInit() {
  }

  /** Types de documents disponibles pour le sélecteur */
  docTypeOptions: DocTypeOption[] = [
    { id: 'en', label: 'Extrait de naissance' },
    { id: 'cni', label: 'Carte nationale d\'identité' },
    { id: 'pp', label: 'Passeport' }
  ];

  getUploadConfig(path: string, title: string): UploadConfig {
    return {
      maxFileSize: 5 * 1024 * 1024,
      allowedTypes: ['image/jpeg', 'image/png', 'application/pdf'],
      uploadPath: path,
      title: title,
      docTypes: this.docTypeOptions
    };
  }

  onFilesUploaded(window: string, events: { files: File[], docType: string }[]) {
    const currentWindow = this.windows[window];
    if (currentWindow && events.length > 0) {
      currentWindow.hasFiles = true;

      // Créer une liste de requêtes HTTP
      // On utilise `uploadFiles` qui retourne un flux avec les events de progression.
      // Pour attendre la fin de tous les flux avec `forkJoin`, nous devons capturer le résultat final.
      // Le composant fileUploadService retourne un Observable<number> (progression de 0 à 100).
      const uploadObservables = events.map(event => {
        const uploadPath = currentWindow.path + '_' + event.docType;
        return this.fileUploadService.uploadFiles(event.files, uploadPath);
      });

      // Lancer toutes les requêtes en parallèle et s'abonner pour suivre leur avancée
      let completedUploads = 0;
      let hasError = false;

      uploadObservables.forEach(obs => {
        obs.subscribe({
          next: (progress) => {
            if (progress === 100) {
              completedUploads++;
              // Vérifier si toutes les requêtes sont terminées
              if (completedUploads === uploadObservables.length && !hasError) {
                this.moveToNextWindow(window);
              }
            }
          },
          error: (err) => {
            console.error('Erreur lors du téléversement d\'un groupe:', err);
            hasError = true;
          }
        });
      });
    }
  }

  onUploadCancelled(window: string) {
    const currentWindow = this.windows[window];
    if (currentWindow) {
      currentWindow.hasFiles = false;
    }
  }

  continueToNext(window: string) {
    this.moveToNextWindow(window);
  }

  private moveToNextWindow(currentWindow: string) {
    const windows = ['f1', 'f2', 'f3', 'f4', 'f5', 'f_temoins', 'f_ai'];
    const currentIndex = windows.indexOf(currentWindow);

    if (currentIndex < windows.length - 1) {
      this.windows[currentWindow].isVisible = false;
      this.windows[windows[currentIndex + 1]].isVisible = true;
    }
  }

  onLireAiEcrireBd(): void {
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
      },
    });
  }

  onAfficheFrida() {
    console.log('Processus onAfficheFrida !');
    //Avec passage de paramètre 'numFrida
    this.router.navigate(['/frida'], { queryParams: { numFrida: this.numFrida } });
    // Construire l'URL à partir du router
    /*const url = this.router.serializeUrl(
        this.router.createUrlTree(['/frida'], { queryParams: { numFrida: this.numFrida } }) // Chemin de destination
    );
    // Ouvrir dans un nouvel onglet
    window.open(url, '_blank');*/
  }
  pageCreation() {
    this.router.navigate(['/create']);
  }
  accueil() {
    this.router.navigate(['']);
  }
}