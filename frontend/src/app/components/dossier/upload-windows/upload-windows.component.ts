import { NotificationService } from '../../../services/notification.service';
import { Component, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FileUploadComponent } from '../file-upload/file-upload.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { UploadWindowState } from './upload-window.interface';
import { UploadConfig, DocTypeOption, UploadedFile } from '../file-upload/file-upload.interface';
import { Router, ActivatedRoute } from '@angular/router';
import { OcrPipelineService } from "../../../services/ocr-pipeline.service";
import { ConstitutionService } from '../../../services/constitution.service';
import { UploadStateService } from '../../../services/upload-state.service';
import { forkJoin, Observable, of } from 'rxjs';
import { BrouillonService, BrouillonFichiers } from '../../../services/brouillon.service';

@Component({
  selector: 'app-upload-windows',
  standalone: true,
  imports: [CommonModule, FormsModule, FileUploadComponent],
  template: `
    <div class="windows-container carousel-viewport">

      <!-- Écran récapitulatif du brouillon -->
      <div *ngIf="showBrouillonRecap" class="brouillon-recap" style="max-width: 700px; margin: 0 auto; padding: 20px;">
        <h2 style="text-align: center; color: #4ecca3; margin-bottom: 8px;">
          <span class="material-icons" style="vertical-align: middle; font-size: 2rem;">folder_open</span>
          Reprise du brouillon
        </h2>
        <p style="text-align: center; color: #94a3b8; margin-bottom: 24px;">
          {{ brouillonFolderName }}
        </p>

        <div style="background: rgba(255,255,255,0.03); border: 1px solid rgba(78,204,163,0.2); border-radius: 12px; padding: 16px; margin-bottom: 24px;">
          <h3 style="color: #e2e8f0; margin-bottom: 12px; font-size: 1rem;">
            <span class="material-icons" style="vertical-align: middle; font-size: 1.2rem; color: #4ecca3;">inventory_2</span>
            Documents déjà enregistrés ({{ getTotalExistingFiles() }})
          </h3>
          <div *ngIf="recapCategories.length === 0" style="color: #64748b; text-align: center; padding: 12px;">
            Aucun document enregistré pour le moment.
          </div>
          <div *ngFor="let cat of recapCategories" style="display: flex; align-items: center; justify-content: space-between; padding: 8px 12px; border-bottom: 1px solid rgba(255,255,255,0.05);">
            <span style="color: #e2e8f0;">
              <span class="material-icons" style="font-size: 1rem; vertical-align: middle; color: #4ecca3; margin-right: 6px;">check_circle</span>
              {{ cat.label }}
            </span>
            <span style="background: rgba(78,204,163,0.15); color: #4ecca3; padding: 2px 10px; border-radius: 12px; font-size: 0.85rem;">
              {{ cat.count }} fichier{{ cat.count > 1 ? 's' : '' }}
            </span>
          </div>
        </div>

        <div style="display: flex; flex-direction: column; gap: 12px; align-items: center;">
          <button class="btn btn-primary" style="width: 100%; max-width: 400px; padding: 12px; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 1rem;" (click)="showBrouillonRecap = false">
            <span class="material-icons">person_add</span> Ajouter des héritiers
          </button>
          <button class="btn btn-primary" style="width: 100%; max-width: 400px; padding: 12px; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 1rem; background: #f59e0b; border-color: #f59e0b;" 
            (click)="showBrouillonRecap = false; launchOcrAndReview()" [disabled]="getTotalExistingFiles() === 0">
            <span class="material-icons">play_arrow</span> Créer le document Frida
          </button>
          <button class="btn btn-secondary" style="width: 100%; max-width: 400px; padding: 12px; display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 1rem;" (click)="router.navigate(['/search'])">
            <span class="material-icons">close</span> Fermer
          </button>
        </div>
      </div>

      <!-- Carousel normal (masqué pendant le récap) -->
      <ng-container *ngIf="!showBrouillonRecap">

      <div class="carousel-track" [style.transform]="'translateX(-' + getCurrentIndex() * 100 + '%)'">
        
        <!-- Fenêtre Défunt -->
        <div class="window-section" *ngIf="isWindowActive('f1')" [class.active-slide]="windows['f1'].isVisible">
          <ng-container *ngIf="!windows['f1'].isUploading">
            <h2 class="window-title">1. Document du Défunt</h2>
            <app-file-upload #fileUploadF1
                [config]="getUploadConfig('01', 'Défunt', false, '', 4)"
                [initialFiles]="windows['f1'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f1', $event)"
                (uploadCancelled)="onUploadCancelled('f1')"
                (skipClicked)="continueToNext('f1')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f1'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Conjoint -->
        <div class="window-section" *ngIf="isWindowActive('f2')" [class.active-slide]="windows['f2'].isVisible">
          <ng-container *ngIf="!windows['f2'].isUploading">
            <h2 class="window-title">2. Document du Conjoint</h2>
            <app-file-upload #fileUploadF2
                [config]="getUploadConfig('02', 'Conjoint', true, 'Continuer s\\'il n\\'y a pas de conjoint', 4)"
                [initialFiles]="windows['f2'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f2', $event)"
                (previousClicked)="moveToPreviousWindow('f2')"
                (uploadCancelled)="onUploadCancelled('f2')"
                (pendingFilesChanged)="onPendingFilesChanged('f2', $event)"
                (skipClicked)="continueToNext('f2')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f2'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Fils (Garçons) -->
        <div class="window-section" *ngIf="isWindowActive('f_garcons')" [class.active-slide]="windows['f_garcons'].isVisible">
          <ng-container *ngIf="!windows['f_garcons'].isUploading">
            <h2 class="window-title">3. Documents des Fils</h2>
            <app-file-upload #fileUploadFGarcons
                [config]="getUploadConfig('03', 'Fils (Garçons)', true, 'Continuer s\\'il n\\'y a pas de fils', 10)"
                [initialFiles]="windows['f_garcons'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_garcons', $event)"
                (previousClicked)="moveToPreviousWindow('f_garcons')"
                (uploadCancelled)="onUploadCancelled('f_garcons')"
                (pendingFilesChanged)="onPendingFilesChanged('f_garcons', $event)"
                (skipClicked)="continueToNext('f_garcons')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_garcons'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Filles -->
        <div class="window-section" *ngIf="isWindowActive('f_filles')" [class.active-slide]="windows['f_filles'].isVisible">
          <ng-container *ngIf="!windows['f_filles'].isUploading">
            <h2 class="window-title">4. Documents des Filles</h2>
            <app-file-upload #fileUploadFFilles
                [config]="getUploadConfig('03', 'Filles', true, 'Continuer s\\'il n\\'y a pas de filles', 10)"
                [initialFiles]="windows['f_filles'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_filles', $event)"
                (previousClicked)="moveToPreviousWindow('f_filles')"
                (uploadCancelled)="onUploadCancelled('f_filles')"
                (pendingFilesChanged)="onPendingFilesChanged('f_filles', $event)"
                (skipClicked)="continueToNext('f_filles')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_filles'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Déclaration Tombes -->
        <div class="window-section" *ngIf="isWindowActive('f_tombes_declare')" [class.active-slide]="windows['f_tombes_declare'].isVisible">
          <div class="upload-container" style="max-width: 600px; margin: 0 auto; text-align: center;">
            <h2 class="window-title">Enfants prédécédés (Tombes)</h2>
            <p style="color: #64748b; margin-bottom: 2rem;">Avez-vous des enfants décédés avant le défunt (laissant des descendants) ?</p>
            
            <div style="display: flex; justify-content: center; gap: 3rem; margin-bottom: 2rem;">
              <div style="display: flex; flex-direction: column; align-items: center; gap: 1rem;">
                <label style="font-weight: bold; color: #475569;">Fils décédés</label>
                <div style="display: flex; align-items: center; gap: 1rem;">
                  <button type="button" (click)="nbFilsDecedes = Math.max(0, nbFilsDecedes - 1)" style="width: 40px; height: 40px; border-radius: 50%; border: none; background: #e2e8f0; font-size: 1.5rem; cursor: pointer; color: #475569;">-</button>
                  <span style="font-size: 1.5rem; font-weight: bold; min-width: 30px;">{{ nbFilsDecedes }}</span>
                  <button type="button" (click)="nbFilsDecedes = nbFilsDecedes + 1" style="width: 40px; height: 40px; border-radius: 50%; border: none; background: #e2e8f0; font-size: 1.5rem; cursor: pointer; color: #475569;">+</button>
                </div>
              </div>
              
              <div style="display: flex; flex-direction: column; align-items: center; gap: 1rem;">
                <label style="font-weight: bold; color: #475569;">Filles décédées</label>
                <div style="display: flex; align-items: center; gap: 1rem;">
                  <button type="button" (click)="nbFillesDecedees = Math.max(0, nbFillesDecedees - 1)" style="width: 40px; height: 40px; border-radius: 50%; border: none; background: #e2e8f0; font-size: 1.5rem; cursor: pointer; color: #475569;">-</button>
                  <span style="font-size: 1.5rem; font-weight: bold; min-width: 30px;">{{ nbFillesDecedees }}</span>
                  <button type="button" (click)="nbFillesDecedees = nbFillesDecedees + 1" style="width: 40px; height: 40px; border-radius: 50%; border: none; background: #e2e8f0; font-size: 1.5rem; cursor: pointer; color: #475569;">+</button>
                </div>
              </div>
            </div>

            <div style="display: flex; justify-content: center; gap: 1rem; margin-top: 2rem;">
              <button class="btn btn-outline" (click)="moveToPreviousWindow('f_tombes_declare')">Précédent</button>
              <button class="btn btn-primary" (click)="continueToNext('f_tombes_declare')">Continuer</button>
            </div>
          </div>
        </div>

        <!-- Fenêtres dynamiques Fils Prédécédés -->
        <ng-container *ngFor="let idx of getRange(nbFilsDecedes)">
          <div class="window-section" *ngIf="isWindowActive('tombe_M_' + (idx + 1))" [class.active-slide]="getWindow('tombe_M_' + (idx + 1)).isVisible">
            <ng-container *ngIf="!getWindow('tombe_M_' + (idx + 1)).isUploading">
              <h2 class="window-title">Tombe {{ idx + 1 }} (Fils prédécédé)</h2>
              <p style="text-align: center; color: #64748b; margin-bottom: 1rem;">Uploadez son acte de décès et les actes de naissance de ses enfants.</p>
              <app-file-upload 
                  [config]="getUploadConfig('09', 'Tombe ' + (idx + 1) + ' (Fils)', true, 'Continuer s\\'il n\\'y a pas de documents', 10)"
                  [initialFiles]="getWindow('tombe_M_' + (idx + 1)).rawFiles || []"
                  (filesConfirmed)="onFilesConfirmed('tombe_M_' + (idx + 1), $event)"
                  (previousClicked)="moveToPreviousWindow('tombe_M_' + (idx + 1))"
                  (uploadCancelled)="onUploadCancelled('tombe_M_' + (idx + 1))"
                  (pendingFilesChanged)="onPendingFilesChanged('tombe_M_' + (idx + 1), $event)"
                  (skipClicked)="continueToNext('tombe_M_' + (idx + 1))"
              >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
            </ng-container>
            <div *ngIf="getWindow('tombe_M_' + (idx + 1)).isUploading" class="drop-zone loading-zone">
              <span class="spinner"></span> Sauvegarde en cours...
            </div>
          </div>
        </ng-container>

        <!-- Fenêtres dynamiques Filles Prédécédées -->
        <ng-container *ngFor="let idx of getRange(nbFillesDecedees)">
          <div class="window-section" *ngIf="isWindowActive('tombe_F_' + (idx + 1))" [class.active-slide]="getWindow('tombe_F_' + (idx + 1)).isVisible">
            <ng-container *ngIf="!getWindow('tombe_F_' + (idx + 1)).isUploading">
              <h2 class="window-title">Tombe {{ idx + 1 }} (Fille prédécédée)</h2>
              <p style="text-align: center; color: #64748b; margin-bottom: 1rem;">Uploadez son acte de décès et les actes de naissance de ses enfants.</p>
              <app-file-upload 
                  [config]="getUploadConfig('10', 'Tombe ' + (idx + 1) + ' (Fille)', true, 'Continuer s\\'il n\\'y a pas de documents', 10)"
                  [initialFiles]="getWindow('tombe_F_' + (idx + 1)).rawFiles || []"
                  (filesConfirmed)="onFilesConfirmed('tombe_F_' + (idx + 1), $event)"
                  (previousClicked)="moveToPreviousWindow('tombe_F_' + (idx + 1))"
                  (uploadCancelled)="onUploadCancelled('tombe_F_' + (idx + 1))"
                  (pendingFilesChanged)="onPendingFilesChanged('tombe_F_' + (idx + 1), $event)"
                  (skipClicked)="continueToNext('tombe_F_' + (idx + 1))"
              >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
            </ng-container>
            <div *ngIf="getWindow('tombe_F_' + (idx + 1)).isUploading" class="drop-zone loading-zone">
              <span class="spinner"></span> Sauvegarde en cours...
            </div>
          </div>
        </ng-container>

        <!-- Fenêtre Père -->
        <div class="window-section" *ngIf="isWindowActive('f_pere')" [class.active-slide]="windows['f_pere'].isVisible">
          <ng-container *ngIf="!windows['f_pere'].isUploading">
            <h2 class="window-title">Le Défunt a-t-il un Père vivant ?</h2>
            <app-file-upload #fileUploadFPere
                [config]="getUploadConfig('04', 'Père', true, 'Continuer s\\'il n\\'y a pas de père', 2)"
                [initialFiles]="windows['f_pere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_pere', $event)"
                (previousClicked)="moveToPreviousWindow('f_pere')"
                (uploadCancelled)="onUploadCancelled('f_pere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_pere', $event)"
                (skipClicked)="continueToNext('f_pere')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_pere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Grand-père paternel -->
        <div class="window-section" *ngIf="isWindowActive('f_grand_pere')" [class.active-slide]="windows['f_grand_pere'].isVisible">
          <ng-container *ngIf="!windows['f_grand_pere'].isUploading">
            <h2 class="window-title">Le Défunt a-t-il un Grand-père paternel vivant ?</h2>
            <app-file-upload #fileUploadFGrandPere
                [config]="getUploadConfig('08', 'Grand-père paternel', true, 'Continuer s\\'il n\\'y a pas de grand-père paternel', 2)"
                [initialFiles]="windows['f_grand_pere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_grand_pere', $event)"
                (previousClicked)="moveToPreviousWindow('f_grand_pere')"
                (uploadCancelled)="onUploadCancelled('f_grand_pere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_grand_pere', $event)"
                (skipClicked)="continueToNext('f_grand_pere')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_grand_pere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Mère -->
        <div class="window-section" *ngIf="isWindowActive('f_mere')" [class.active-slide]="windows['f_mere'].isVisible">
          <ng-container *ngIf="!windows['f_mere'].isUploading">
            <h2 class="window-title">Le Défunt a-t-il une Mère vivante ?</h2>
            <app-file-upload #fileUploadFMere
                [config]="getUploadConfig('04', 'Mère', true, 'Continuer s\\'il n\\'y a pas de mère', 2)"
                [initialFiles]="windows['f_mere'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_mere', $event)"
                (previousClicked)="moveToPreviousWindow('f_mere')"
                (uploadCancelled)="onUploadCancelled('f_mere')"
                (pendingFilesChanged)="onPendingFilesChanged('f_mere', $event)"
                (skipClicked)="continueToNext('f_mere')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_mere'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Grand-mère paternelle -->
        <div class="window-section" *ngIf="isWindowActive('f_grand_mere_paternelle')" [class.active-slide]="windows['f_grand_mere_paternelle'].isVisible">
          <ng-container *ngIf="!windows['f_grand_mere_paternelle'].isUploading">
            <h2 class="window-title">Le Défunt a-t-il une Grand-mère paternelle vivante ?</h2>
            <app-file-upload #fileUploadFGrandMerePaternelle
                [config]="getUploadConfig('11', 'Grand-mère paternelle', true, 'Continuer s\\'il n\\'y a pas de grand-mère paternelle', 2)"
                [initialFiles]="windows['f_grand_mere_paternelle'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_grand_mere_paternelle', $event)"
                (previousClicked)="moveToPreviousWindow('f_grand_mere_paternelle')"
                (uploadCancelled)="onUploadCancelled('f_grand_mere_paternelle')"
                (pendingFilesChanged)="onPendingFilesChanged('f_grand_mere_paternelle', $event)"
                (skipClicked)="continueToNext('f_grand_mere_paternelle')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_grand_mere_paternelle'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Frères et sœurs du défunt -->
        <div class="window-section" *ngIf="isWindowActive('f5')" [class.active-slide]="windows['f5'].isVisible">
          <ng-container *ngIf="!windows['f5'].isUploading">
            <app-file-upload #fileUploadF5
                [config]="getUploadConfig('05', 'Frères et sœurs du défunt', true, 'Continuer s\\'il n\\'y a pas de frères et sœurs', 10)"
                [initialFiles]="windows['f5'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f5', $event)"
                (previousClicked)="moveToPreviousWindow('f5')"
                (uploadCancelled)="onUploadCancelled('f5')"
                (pendingFilesChanged)="onPendingFilesChanged('f5', $event)"
                (skipClicked)="continueToNext('f5')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f5'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Oncles paternels -->
        <div class="window-section" *ngIf="isWindowActive('f6')" [class.active-slide]="windows['f6'].isVisible">
          <ng-container *ngIf="!windows['f6'].isUploading">
            <app-file-upload #fileUploadF6
                [config]="getUploadConfig('06', 'Oncles paternels', true, 'Continuer s\\'il n\\'y a pas d\\'oncles paternels', 10)"
                [initialFiles]="windows['f6'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f6', $event)"
                (previousClicked)="moveToPreviousWindow('f6')"
                (uploadCancelled)="onUploadCancelled('f6')"
                (pendingFilesChanged)="onPendingFilesChanged('f6', $event)"
                (skipClicked)="continueToNext('f6')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f6'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Cousins paternels -->
        <div class="window-section" *ngIf="isWindowActive('f7')" [class.active-slide]="windows['f7'].isVisible">
          <ng-container *ngIf="!windows['f7'].isUploading">
            <app-file-upload #fileUploadF7
                [config]="getUploadConfig('07', 'Cousins paternels', true, 'Continuer s\\'il n\\'y a pas de cousins paternels', 10)"
                [initialFiles]="windows['f7'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f7', $event)"
                (previousClicked)="moveToPreviousWindow('f7')"
                (uploadCancelled)="onUploadCancelled('f7')"
                (pendingFilesChanged)="onPendingFilesChanged('f7', $event)"
                (skipClicked)="continueToNext('f7')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f7'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre témoins -->
        <div class="window-section" *ngIf="isWindowActive('f_temoins')" [class.active-slide]="windows['f_temoins'].isVisible">
          <ng-container *ngIf="!windows['f_temoins'].isUploading">
            <app-file-upload #fileUploadFTemoins
                [config]="getUploadConfig('00', 'Témoins', true, 'Continuer s\\'il n\\'y a pas de temoin', 2)"
                [initialFiles]="windows['f_temoins'].rawFiles || []"
                (filesConfirmed)="onFilesConfirmed('f_temoins', $event)"
                (previousClicked)="moveToPreviousWindow('f_temoins')"
                (uploadCancelled)="onUploadCancelled('f_temoins')"
                (pendingFilesChanged)="onPendingFilesChanged('f_temoins', $event)"
                (skipClicked)="continueToNext('f_temoins')"
            >
              <div global-actions style="display: flex; flex-direction: column; gap: 12px; width: 100%;">
                <button *ngIf="isHeirWindowActive()" class="btn btn-secondary" style="border-color: #ffb84d; color: #ffb84d; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="skipToTemoins()">
                  Il n'y a plus d'héritiers
                </button>
                <button class="btn btn-outline" style="border-color: #4ecca3; color: #4ecca3; width: 100%; justify-content: center; padding: 12px; display: flex; align-items: center;" (click)="sauvegarderBrouillon()">
                  <span class="material-icons" style="font-size: 1.2rem; margin-right: 6px;">save</span> Sauvegarder brouillon ✓
                </button>
              </div>
            </app-file-upload>
          </ng-container>
          <div *ngIf="windows['f_temoins'].isUploading" class="drop-zone loading-zone">
            <span class="spinner"></span> Sauvegarde en cours...
          </div>
        </div>

        <!-- Fenêtre Lecture AI ---------------- -->
        <div class="window-section" *ngIf="isWindowActive('f_ai')" [class.active-slide]="windows['f_ai'].isVisible">
          <div class="drop-zone">
            <div class="upload-container">
              <h2>Validation finale du dossier</h2>
              <div class="drop-zone">
                <button *ngIf="!endReading && !isReading && !isBatchUploaded && !ficheCreee"
                    class="btn btn-secondary continue-btn"
                    (click)="moveToPreviousWindow('f_ai')" [disabled]="isUploadingFiles"
                    style="margin-right: 10px;"
                >
                  <span>Précédent</span>
                </button>

                <!-- Bouton de lancement -->
                <button *ngIf="!endReading && !isReading && !isBatchUploaded && !ficheCreee"
                    class="btn btn-primary continue-btn"
                    (click)="onReviewFamily()" [disabled]="isUploadingFiles"
                >
                  <span *ngIf="!isUploadingFiles">Vérifier la constitution de la famille</span>
                  <span *ngIf="isUploadingFiles"><span class="spinner"></span> Préparation...</span>
                </button>

                <!-- Fiche déjà créée : pas de second lancement -->
                <div *ngIf="ficheCreee" style="margin-top: 15px; text-align: center; width: 100%;">
                  <p style="color: var(--primary); font-weight: 600; margin-bottom: 15px;">Le dossier n° {{ ficheCreee.numFrida }} a été créé.</p>
                  <button class="btn btn-primary continue-btn" (click)="ouvrirFicheCreee()">Ouvrir le dossier</button>
                </div>

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
                
              </div>
            </div>
          </div>
        </div>

      </div> <!-- End Carousel Track -->

      <!-- Message d'information global -->
      <div class="global-info-message" style="margin: var(--spacing-md) auto 0 auto; max-width: 800px; padding: var(--spacing-sm); background: rgba(78, 204, 163, 0.05); border: 1px solid var(--accent-color); border-radius: var(--border-radius); font-size: 0.9rem; text-align: center; color: var(--text-secondary);">
        <span class="material-icons" style="font-size: 1.2rem; vertical-align: middle; color: #ffb84d; margin-right: 5px;">info</span>
        Utiliser uniquement les actes de naissance et de décès numériques, à demander sur le site : <a href="https://dzds.dz" target="_blank" style="color:#4ecca3;">dzds.dz</a>
      </div>

      </ng-container> <!-- End !showBrouillonRecap -->
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
    .window-title { font-size: 1.2rem; margin-bottom: 15px; }

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
      height: auto;
      max-height: 90vh;
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
      transition: opacity 0.3s ease, height 0.3s ease;
    }

    .window-section:not(.active-slide) {
      height: 0;
      overflow: hidden;
      opacity: 0;
      padding: 0;
      margin: 0;
      border: none;
    }

    .continue-btn {
      margin-top: var(--spacing-md);
    }
    
    .global-skip-action {
      display: flex;
      justify-content: center;
      margin-bottom: 15px;
      padding: 0 20px;
    }
    .skip-all-btn {
      border: 1px solid #ffb84d;
      color: #ffb84d;
      font-size: 1rem;
      font-weight: bold;
      padding: 8px 20px;
      border-radius: 20px;
      background: rgba(0, 0, 0, 0.2);
      cursor: pointer;
      transition: all 0.3s;
    }
    .skip-all-btn:hover {
      background: rgba(255, 184, 77, 0.1);
      transform: scale(1.02);
    }
  `]
})
export class UploadWindowsComponent implements OnInit, OnDestroy {
  private notif = inject(NotificationService);
  windows: { [key: string]: UploadWindowState } = {
    // Défunt
    f1: { isVisible: true, hasFiles: false, isUploading: false, path: '01' },
    // Héritiers
    f2: { isVisible: false, hasFiles: false, isUploading: false, path: '02' },  // Conjoint
    f_garcons: { isVisible: false, hasFiles: false, isUploading: false, path: '03' }, // Fils
    f_filles: { isVisible: false, hasFiles: false, isUploading: false, path: '03' }, // Filles
    f_petits_fils: { isVisible: false, hasFiles: false, isUploading: false, path: '09' }, // Petits-fils
    f_petites_filles: { isVisible: false, hasFiles: false, isUploading: false, path: '10' }, // Petites-filles
    f_pere: { isVisible: false, hasFiles: false, isUploading: false, path: '04' }, // Père
    f_grand_pere: { isVisible: false, hasFiles: false, isUploading: false, path: '08' }, // Grand-père paternel
    f_mere: { isVisible: false, hasFiles: false, isUploading: false, path: '04' }, // Mère
    f_grand_mere_paternelle: { isVisible: false, hasFiles: false, isUploading: false, path: '11' }, // Grand-mère paternelle
    f5: { isVisible: false, hasFiles: false, isUploading: false, path: '05' },  // Frères et sœurs
    f6: { isVisible: false, hasFiles: false, isUploading: false, path: '06' },  // Oncles paternels
    f7: { isVisible: false, hasFiles: false, isUploading: false, path: '07' },  // Cousins paternels
    // Témoins
    f_temoins: { isVisible: false, hasFiles: false, isUploading: false, path: '00' },
    // Lecture AI
    f_ai: { isVisible: false, hasFiles: false, isUploading: false, path: '' },
    // Fenêtre déclaration tombes
    f_tombes_declare: { isVisible: false, hasFiles: false, isUploading: false, path: '' }
  };

  Math = Math;
  nbFilsDecedes = 0;
  nbFillesDecedees = 0;

  getRange(n: number): number[] {
    return Array.from({length: n}, (_, i) => i);
  }

  getWindow(key: string): UploadWindowState {
    if (!this.windows[key]) {
      this.windows[key] = { isVisible: false, hasFiles: false, isUploading: false, path: key.startsWith('tombe_M') ? '09' : '10' };
    }
    return this.windows[key];
  }

  isUploadingFiles = false;
  isReading = false;
  /** Renseignée dès que l'analyse a créé la fiche ; interdit de relancer depuis cet écran. */
  ficheCreee: { numFrida: string; requiresCorrection: boolean } | null = null;
  /** Dossier passé en batch ou enregistré en brouillon : plus rien à reprendre ici. */
  private etatClos = false;
  endReading = false;
  isBatchUploaded = false;
  numFrida: String = "";
  ocrMode: 'rapide' | 'approfondi' | 'batch' = 'rapide';

  brouillonId: number | null = null;
  brouillonFolderName: string | null = null;
  existingFiles: { [key: string]: string[] } = {};
  showBrouillonRecap = false;
  recapCategories: { label: string, count: number }[] = [];

  private folderCodeLabels: { [key: string]: string } = {
    '01': 'Défunt', '02': 'Conjoint', '03': 'Fils / Filles',
    '04': 'Père / Mère', '05': 'Frères et sœurs', '06': 'Oncles paternels',
    '07': 'Cousins paternels', '08': 'Grand-père paternel',
    '09': 'Petits-fils / Tombes', '10': 'Petites-filles', 
    '11': 'Grand-mère paternelle', '00': 'Témoins'
  };

  buildRecapCategories() {
    const catMap: { [label: string]: number } = {};
    for (const [folder, files] of Object.entries(this.existingFiles)) {
      const code = folder.substring(0, 2);
      const label = this.folderCodeLabels[code] || 'Autre (' + code + ')';
      catMap[label] = (catMap[label] || 0) + files.length;
    }
    this.recapCategories = Object.entries(catMap).map(([label, count]) => ({ label, count }));
  }

  getTotalExistingFiles(): number {
    return Object.values(this.existingFiles).reduce((sum, files) => sum + files.length, 0);
  }

  getActiveWindowKeys(): string[] {
    const fiche = this.constitutionService.currentFiche;
    const keys = ['f1', 'f2', 'f_garcons', 'f_filles', 'f_tombes_declare'];

    for (let i = 1; i <= this.nbFilsDecedes; i++) keys.push(`tombe_M_${i}`);
    for (let i = 1; i <= this.nbFillesDecedees; i++) keys.push(`tombe_F_${i}`);

    keys.push('f_pere');

    // Exclusion du Grand-père paternel (exclu si le Père est vivant)
    if (!fiche.pereVivant) {
      keys.push('f_grand_pere');
    }

    keys.push('f_mere');

    // Exclusion de la Grand-mère paternelle (exclue par le Père ou la Mère)
    if (!fiche.pereVivant && !fiche.mereVivante) {
      keys.push('f_grand_mere_paternelle');
    }

    // Exclusion des Frères et sœurs (exclus si Père vivant ou présence de Garçons)
    const aDesGarcons = fiche.nbGarcons > 0;
    if (!fiche.pereVivant && !aDesGarcons) {
      keys.push('f5');
    }

    // Exclusion des Oncles paternels (exclus si Père, Garçons, Grand-père ou Frères)
    const aDesFreres = fiche.nbFreres > 0;
    if (!fiche.pereVivant && !aDesGarcons && !fiche.grandPerePaternelVivant && !aDesFreres) {
      keys.push('f6');
    }

    // Exclusion des Cousins paternels (exclus si Père, Garçons, Grand-père, Frères ou Oncles)
    const aDesOncles = fiche.nbOnclesPaternels > 0;
    if (!fiche.pereVivant && !aDesGarcons && !fiche.grandPerePaternelVivant && !aDesFreres && !aDesOncles) {
      keys.push('f7');
    }

    // Témoins et Validation finale
    keys.push('f_temoins', 'f_ai');

    return keys;
  }

  isWindowActive(key: string): boolean {
    return this.getActiveWindowKeys().includes(key);
  }

  getCurrentIndex(): number {
    const windowKeys = this.getActiveWindowKeys();
    const activeKey = windowKeys.find(key => this.windows[key].isVisible);
    return windowKeys.indexOf(activeKey || 'f1');
  }

  constructor(private fileUploadService: FileUploadService, public router: Router,
    private ocrPipelineService: OcrPipelineService, private constitutionService: ConstitutionService,
    private uploadStateService: UploadStateService, private route: ActivatedRoute,
    private brouillonService: BrouillonService) { }

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['brouillon']) {
        this.brouillonId = +params['brouillon'];
        this.brouillonFolderName = params['folderName'] || null;
        this.loadBrouillonFiles();
      }
    });

    // Un brouillon ne reprend l'état en mémoire que s'il s'agit du même brouillon
    const brouillonParam = this.route.snapshot.queryParamMap.get('brouillon');
    const enCours = this.uploadStateService.dossierEnCours();
    const memeDossier = !brouillonParam || !!enCours?.url.includes('brouillon=' + brouillonParam);
    if (!memeDossier) {
      this.uploadStateService.clearState();
    }
    if (brouillonParam && !this.uploadStateService.dossierEnCours()) {
      const nom = this.route.snapshot.queryParamMap.get('folderName');
      this.uploadStateService.demarrerDossier(nom ? 'Brouillon ' + nom : 'Brouillon', this.router.url);
    }

    const saved = this.uploadStateService.getState();
    if (saved.windows) {
      this.windows = saved.windows;
      // Le choix du mode a été retiré : la lecture des QR codes est la même quel que soit le mode
      this.ocrMode = 'rapide';
      
      // Resynchronisation forcée du ConstitutionService au cas où on revient de l'écran de synthèse
      ['f2', 'f_garcons', 'f_filles', 'f_petits_fils', 'f_petites_filles', 'f_pere', 'f_grand_pere', 'f_mere', 'f_grand_mere_paternelle', 'f5', 'f6', 'f7'].forEach(key => {
        if (this.windows[key]) {
           this.updateConstitutionState(key, this.windows[key].hasFiles, this.windows[key].rawFiles?.length || 0);
        }
      });
    }
  }

  ngOnDestroy() {
    // Quitter le carrousel (menu, accueil…) ne doit pas perdre les documents déjà choisis
    if (!this.etatClos) {
      this.uploadStateService.saveState(this.windows, this.ocrMode);
    }
  }

  loadBrouillonFiles() {
    if (!this.brouillonId) return;
    this.brouillonService.getFichiers(this.brouillonId).subscribe(data => {
      this.existingFiles = data.subfolders || {};
      this.buildRecapCategories();
      
      // Inject dummy files into windows so they appear in the carousels
      for (const [folder, files] of Object.entries(this.existingFiles)) {
        if (!files || files.length === 0) continue;
        
        const parts = folder.split('_');
        if (parts.length < 2) continue;
        const code = parts[0];
        const docType = parts[1];
        const entityName = parts.slice(2).join('_');
        
        // Map code to window key
        let windowKey = '';
        if (code === '01') windowKey = 'f1';
        else if (code === '02') windowKey = 'f2';
        else if (code === '03') windowKey = 'f_garcons'; // Group all children in garcons
        else if (code === '04') windowKey = 'f_pere'; // Or f_mere, using f_pere as fallback
        else if (code === '05') windowKey = 'f5';
        else if (code === '06') windowKey = 'f6';
        else if (code === '07') windowKey = 'f7';
        else if (code === '08') windowKey = 'f_grand_pere';
        else if (code === '09') windowKey = 'tombe_M_1'; // Simplified
        else if (code === '10') windowKey = 'tombe_F_1';
        else if (code === '11') windowKey = 'f_grand_mere_paternelle';
        else if (code === '00') windowKey = 'f_temoins';
        
        if (windowKey && this.windows[windowKey]) {
          this.windows[windowKey].hasFiles = true;
          this.windows[windowKey].rawFiles = this.windows[windowKey].rawFiles || [];
          
          files.forEach(filename => {
            // Create a dummy file object to satisfy the UploadedFile interface
            const dummyFile = new File([], filename, { type: 'application/octet-stream' });
            (dummyFile as any).isRemote = true; // Mark it so we don't re-upload it
            
            // Only add if not already present
            if (!this.windows[windowKey].rawFiles.some(f => f.file.name === filename)) {
              this.windows[windowKey].rawFiles.push({
                file: dummyFile,
                id: 'remote_' + Math.random().toString(36).substr(2, 9),
                progress: 100,
                docType: docType,
                entityName: entityName
              });
            }
          });
        }
      }
      
      this.showBrouillonRecap = true;
      console.log('Fichiers existants du brouillon:', this.existingFiles);
    });
  }

  getFiche() {
    return this.constitutionService.currentFiche;
  }

  /** Types de documents disponibles pour le sélecteur */
  get docTypeOptions(): DocTypeOption[] {
    const isBeta = window.location.hostname.includes('frida.enclume-numerique.com');
    if (isBeta) {
      return [
        { id: 'en', label: 'Extrait de naissance' },
        { id: 'ad', label: 'Acte de décès' }
      ];
    }
    return [
      { id: 'en', label: 'Extrait de naissance' },
      { id: 'ad', label: 'Acte de décès' },
      { id: 'cni', label: 'Carte d\'identité' },
      { id: 'passeport', label: 'Passeport' }
    ];
  }

  getUploadConfig(path: string, title: string, allowPrevious: boolean = true, skipText: string = '', maxFiles: number = 10): UploadConfig & { allowPrevious?: boolean } {
    return {
      maxFileSize: 5 * 1024 * 1024,
      allowedTypes: ['image/jpeg', 'image/png', 'application/pdf'],
      uploadPath: path,
      title: title,
      docTypes: this.docTypeOptions,
      allowPrevious: allowPrevious,
      allowSkip: skipText.length > 0 || this.brouillonId !== null,
      skipText: skipText || (this.brouillonId !== null ? 'Suivant' : ''),
      highlightSkip: path === '00',
      maxFiles: maxFiles
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
      this.updateConstitutionState(window, currentWindow.hasFiles, events.rawFiles.length);
      this.moveToNextWindow(window);
    }
  }

  onUploadCancelled(window: string) {
    const currentWindow = this.windows[window];
    if (currentWindow) {
      currentWindow.hasFiles = false;
      currentWindow.rawFiles = [];
      currentWindow.groupedFiles = [];
      this.updateConstitutionState(window, false, 0);
    }
  }

  private updateConstitutionState(window: string, hasFiles: boolean, count: number) {
    switch (window) {
      case 'f2': this.constitutionService.updateFiche({ nbConjoints: count }); break;
      case 'f_garcons': this.constitutionService.updateFiche({ nbGarcons: count }); break;
      case 'f_filles': this.constitutionService.updateFiche({ nbFilles: count }); break;
      case 'f_petits_fils': this.constitutionService.updateFiche({ nbPetitsFils: count }); break;
      case 'f_petites_filles': this.constitutionService.updateFiche({ nbPetitesFilles: count }); break;
      case 'f_pere': this.constitutionService.updateFiche({ pereVivant: hasFiles }); break;
      case 'f_grand_pere': this.constitutionService.updateFiche({ grandPerePaternelVivant: hasFiles }); break;
      case 'f_mere': this.constitutionService.updateFiche({ mereVivante: hasFiles }); break;
      case 'f_grand_mere_paternelle': this.constitutionService.updateFiche({ grandMerePaternelleVivante: hasFiles }); break;
      case 'f5': this.constitutionService.updateFiche({ nbFreres: count }); break; // Par défaut, on les met dans Frères
      case 'f6': this.constitutionService.updateFiche({ nbOnclesPaternels: count }); break;
      case 'f7': this.constitutionService.updateFiche({ nbCousinsPaternels: count }); break;
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

  isHeirWindowActive(): boolean {
    const activeKey = this.getActiveWindowKeys().find(key => this.windows[key].isVisible);
    if (!activeKey || activeKey === 'f1' || activeKey === 'f_temoins' || activeKey === 'f_ai') {
        return false;
    }
    return !this.windows[activeKey].isUploading;
  }

  skipToTemoins() {
    const windowKeys = this.getActiveWindowKeys();
    const activeKey = windowKeys.find(key => this.windows[key].isVisible);
    if (activeKey) {
      this.windows[activeKey].isVisible = false;
    }
    this.windows['f_temoins'].isVisible = true;
  }

  onReviewFamily(): void {
    // Chaque lancement crée une fiche : on refuse un second lancement (double clic, navigation échouée)
    if (this.isUploadingFiles || this.isReading || this.ficheCreee) return;
    this.isUploadingFiles = true;
    
    if (this.brouillonId) {
      // En mode brouillon, ne pas effacer les fichiers déjà sur le serveur
      this.startUploads();
    } else {
      // Nettoyage du dossier serveur pour éviter le dédoublement des fichiers
      this.ocrPipelineService.clearLatestFolder().subscribe({
        next: () => this.startUploads(),
        error: (err) => {
          console.warn("Erreur lors du nettoyage du dossier, continuation...", err);
          this.startUploads();
        }
      });
    }
  }

  private startUploads(): void {
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
          const realFiles = group.files.filter(f => !(f as any).isRemote); if (realFiles.length > 0) allUploadObservables.push(this.fileUploadService.uploadFiles(realFiles, uploadPath, this.brouillonFolderName || undefined));
        });
      }
    });

    if (allUploadObservables.length > 0) {
      forkJoin(allUploadObservables).subscribe({
        next: () => {
          this.isUploadingFiles = false;
          this.launchOcrAndReview();
        },
        error: (err) => {
          console.error('Erreur lors du transfert des dossiers au serveur :', err);
          this.isUploadingFiles = false;
          this.notif.erreur("Une erreur de réseau empêche le transfert des fichiers.");
        }
      });
    } else {
      this.isUploadingFiles = false;
      this.launchOcrAndReview();
    }
  }

  sauvegarderBrouillon() {
    const allUploadObservables: Observable<any>[] = [];

    // Uploader les fichiers de TOUTES les fenêtres, pas seulement la fenêtre active
    this.getActiveWindowKeys().forEach(key => {
      if (key === 'f_ai') return;
      const win = this.windows[key];
      if (win && win.groupedFiles && win.groupedFiles.length > 0) {
        win.groupedFiles.forEach(group => {
          let uploadPath = win.path + '_' + group.docType;
          if (group.entityName && group.entityName.trim() !== '') {
            uploadPath += '_' + group.entityName;
          }
          const realFiles = group.files.filter(f => !(f as any).isRemote); if (realFiles.length > 0) allUploadObservables.push(this.fileUploadService.uploadFiles(realFiles, uploadPath, this.brouillonFolderName || undefined));
        });
      }
    });

    if (allUploadObservables.length > 0) {
      forkJoin(allUploadObservables).subscribe({
        next: () => {
          this.notif.succes('Brouillon sauvegardé');
          this.fermerEtat();
          this.router.navigate(['/search']);
        },
        error: (err) => {
          console.error('Erreur lors de la sauvegarde du brouillon:', err);
          this.notif.erreur('Erreur lors de la sauvegarde. Veuillez réessayer.');
        }
      });
    } else {
      this.notif.succes('Brouillon sauvegardé (aucun nouveau fichier à uploader)');
      this.fermerEtat();
      this.router.navigate(['/search']);
    }
  }

  /** Le dossier est confié au batch ou au brouillon : il ne figure plus comme « en cours ». */
  private fermerEtat() {
    this.etatClos = true;
    this.uploadStateService.clearState();
  }

  /** Fiche créée par l'analyse : ouvre l'écran suivant (aussi appelé par le bouton si la navigation a échoué). */
  ouvrirFicheCreee() {
    if (!this.ficheCreee) return;
    const { numFrida, requiresCorrection } = this.ficheCreee;
    this.router.navigate([requiresCorrection ? '/correction' : '/review-family'], { queryParams: { numFrida } });
  }

  launchOcrAndReview() {
    if (this.isReading || this.ficheCreee) return;
    // Sauvegarde l'état du carrousel avant de partir
    this.uploadStateService.saveState(this.windows, this.ocrMode);
    
    // Si on est en mode batch, on s'arrête ici. Le planificateur (BatchJobScheduler)
    // prendra le relais plus tard pour traiter le dossier.
    if (this.ocrMode === 'batch') {
      this.isBatchUploaded = true;
      this.fermerEtat();
      return;
    }
    
    this.isReading = true;
    this.ocrPipelineService.lireAiEcrireBd(this.ocrMode, this.brouillonFolderName || undefined).subscribe({
      next: (data) => {
        this.isReading = false;
        if (data && data.numFrida) {
           this.ficheCreee = { numFrida: data.numFrida, requiresCorrection: !!data.requiresCorrection };
           this.uploadStateService.lierFiche(data.numFrida);
           // Si l'OCR a détecté des champs à faible confiance, passer par la fiche de correction
           this.ouvrirFicheCreee();
        } else {
           this.notif.avertissement("L'analyse n'a renvoyé aucun dossier valide (aucun document n'a été reconnu).");
        }
      },
      error: (error) => {
        console.error('Erreur lors de l\'analyse OCR:', error);
        this.isReading = false;
        this.notif.erreur("Une erreur est survenue pendant l'analyse des documents.");
      },
    });
  }

  pageCreation() {
    this.router.navigate(['/create']);
  }

  accueil() {
    this.router.navigate(['']);
  }
}
