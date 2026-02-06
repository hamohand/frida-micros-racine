import {Component, OnInit, Output} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileUploadComponent } from '../file-upload/file-upload.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { UploadWindowState } from './upload-window.interface';
import { UploadConfig } from '../file-upload/file-upload.interface';
import { Router } from '@angular/router';
import {LireaiEcrirebdService} from "../../../services/lireai-ecrirebd.service";

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

      <!-- Fenêtre témoins -->
      <div *ngIf="windows['f_temoins'].isVisible" class="window-section">
        <app-file-upload
            [config]="getUploadConfig('11', 'Témoins')"
            (filesUploaded)="onFilesUploaded('f_temoins', $event)"
            (uploadCancelled)="onUploadCancelled('f_temoins')"
        ></app-file-upload>
<!--        <button-->
<!--            *ngIf="!windows['f_temoins'].hasFiles"-->
<!--            class="btn btn-secondary continue-btn"-->
<!--            (click)="continueToNext('f_temoins')"-->
<!--        >-->
<!--          Continuer s'il n'y a pas d'enfants-->
<!--        </button>-->
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
    //Défunt
    f1: { isVisible: true, hasFiles: false, path: '1' }, // dossier "1"
    //Héritiers : 2-10
    f2: { isVisible: false, hasFiles: false, path: '2' }, // Conjoints dossier "2"
    f3: { isVisible: false, hasFiles: false, path: '3' }, // Enfants dossier "3"
    f4: { isVisible: false, hasFiles: false, path: '4' }, // Enfants dossier "3"
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, path: '11' }, // dossier "11"
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, path: '' } // Nouvelle fenêtre
  };

  isReading = false;
  endReading = false;
  numFrida: String = "1956010320250116";
  //numFrida: String = "19560103202501171733";
  //numFrida: String = "";

  constructor(private fileUploadService: FileUploadService, private router: Router,
              private lireaiEcrirebdService: LireaiEcrirebdService) {}

  ngOnInit() {
  }

  getUploadConfig(path: string, title: string): UploadConfig {
    return {
      maxFileSize: 5 * 1024 * 1024,
      allowedTypes: ['image/jpeg', 'image/png', 'application/pdf'],
      uploadPath: path,
      title: title
    };
  }

  onFilesUploaded(window: string, files: File[]) {
    const currentWindow = this.windows[window];
    if (currentWindow) {
      this.fileUploadService.uploadFiles(files, currentWindow.path).subscribe({
        next: (progress) => {
          if (progress === 100) {
            currentWindow.hasFiles = true;
            this.moveToNextWindow(window);
          }
        },
        error: (error) => {
          console.error('Erreur lors du téléversement:', error);
        }
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
    const windows = ['f1', 'f2', 'f3', 'f_temoins', 'f_ai']; // Ajoutez 'f_ai' à la liste des fenêtres
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
 pageCreation(){
   this.router.navigate(['/create']);
 }
  accueil(){
    this.router.navigate(['']);
  }
}