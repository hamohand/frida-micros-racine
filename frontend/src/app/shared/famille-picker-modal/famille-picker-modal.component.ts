import { Component, ElementRef, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { BilabelComponent } from '../bilabel/bilabel.component';

export interface FichierFamilleInfo {
  nom: string;
  libelleFr: string;
  libelleAr: string;
  typeActe: 'ad' | 'en';
  categorie: string;
}

@Component({
  selector: 'app-famille-picker-modal',
  standalone: true,
  imports: [CommonModule, BilabelComponent],
  template: `
    <div class="famille-backdrop" (click)="fermer()">
      <div class="famille-modal" (click)="$event.stopPropagation()">
        <div class="famille-header">
          <div>
            <h3>
              📂 <app-bilabel fr="Dossier en ligne : « famille »" ar="المجلد الإلكتروني : « famille »" />
            </h3>
            <p class="famille-subtitle">
              <app-bilabel
                [fr]="multiple ? 'Sélectionnez un ou plusieurs actes fictifs dans le dossier « famille »' : 'Sélectionnez un acte fictif dans le dossier « famille »'"
                [ar]="multiple ? 'اختر عقداً أو عدة عقود افتراضية من مجلد « famille »' : 'اختر عقداً افتراضياً من مجلد « famille »'" />
            </p>
          </div>
          <button type="button" class="btn-close" (click)="fermer()" aria-label="Fermer">✕</button>
        </div>

        <div class="famille-body">
          <div class="famille-list">
            <div *ngIf="chargementListe" class="famille-empty">
              Chargement du dossier « famille »...
            </div>
            <div
              *ngFor="let f of fichiers"
              class="famille-item"
              [class.selected]="estSelectionne(f.nom)"
              (click)="basculerSelection(f.nom)"
              (dblclick)="confirmerUnique(f.nom)"
            >
              <input
                [type]="multiple ? 'checkbox' : 'radio'"
                [checked]="estSelectionne(f.nom)"
                (click)="$event.stopPropagation(); basculerSelection(f.nom)"
              />
              <div class="famille-item-info">
                <div class="famille-item-top">
                  <span class="famille-filename">{{ f.nom }}</span>
                  <span class="famille-badge" [class.badge-ad]="f.typeActe === 'ad'" [class.badge-en]="f.typeActe === 'en'">
                    {{ f.typeActe === 'ad' ? 'Acte de décès' : 'Acte de naissance' }}
                  </span>
                </div>
                <div class="famille-item-desc">
                  <span>{{ f.libelleFr }}</span>
                  <span dir="rtl" class="famille-ar">{{ f.libelleAr }}</span>
                </div>
              </div>
              <button
                type="button"
                class="btn-preview"
                [class.active]="apercuNom === f.nom"
                (click)="$event.stopPropagation(); afficherApercu(f.nom)"
                title="Aperçu du document"
              >
                👁
              </button>
            </div>
          </div>

          <div class="famille-preview" *ngIf="apercuNom">
            <div class="preview-header">
              <span>{{ apercuNom }}</span>
              <button type="button" class="btn-close-sm" (click)="apercuNom = null">✕</button>
            </div>
            <img [src]="'/api/demo/famille/' + apercuNom" [alt]="apercuNom" />
          </div>
        </div>

        <div *ngIf="erreur" class="famille-error">{{ erreur }}</div>

        <div class="famille-footer">
          <span class="selection-count">
            {{ selection.size }} fichier{{ selection.size > 1 ? 's' : '' }} sélectionné{{ selection.size > 1 ? 's' : '' }}
          </span>
          <div class="footer-actions">
            <button type="button" class="btn btn-secondary" (click)="fermer()" [disabled]="telechargement">
              <app-bilabel fr="Annuler" ar="إلغاء" />
            </button>
            <button
              type="button"
              class="btn btn-primary"
              (click)="valider()"
              [disabled]="selection.size === 0 || telechargement"
            >
              <app-bilabel
                [fr]="telechargement ? 'Chargement...' : 'Choisir (' + selection.size + ')'"
                [ar]="telechargement ? 'جارٍ التحميل...' : 'اختيار (' + selection.size + ')'" />
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .famille-backdrop {
      position: fixed;
      inset: 0;
      z-index: 2000;
      background: rgba(15, 23, 42, 0.8);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
    }
    .famille-modal {
      width: 100%;
      max-width: 860px;
      max-height: 88vh;
      background: #1e293b;
      border: 1px solid rgba(78, 204, 163, 0.35);
      border-radius: 14px;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
    }
    .famille-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 16px 20px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      background: rgba(15, 23, 42, 0.5);
    }
    .famille-header h3 {
      margin: 0;
      color: #4ecca3;
      font-size: 1.15rem;
    }
    .famille-subtitle {
      margin: 4px 0 0;
      color: #94a3b8;
      font-size: 0.85rem;
    }
    .btn-close {
      background: transparent;
      border: none;
      color: #94a3b8;
      font-size: 1.25rem;
      cursor: pointer;
      padding: 4px 8px;
    }
    .btn-close:hover { color: #fff; }
    .famille-body {
      display: flex;
      flex: 1;
      min-height: 0;
      overflow: hidden;
    }
    .famille-list {
      flex: 1;
      overflow-y: auto;
      padding: 12px 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .famille-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.07);
      cursor: pointer;
      transition: all 0.15s ease;
      text-align: left;
    }
    .famille-item:hover {
      background: rgba(78, 204, 163, 0.08);
      border-color: rgba(78, 204, 163, 0.3);
    }
    .famille-item.selected {
      background: rgba(78, 204, 163, 0.15);
      border-color: #4ecca3;
    }
    .famille-item-info {
      flex: 1;
      min-width: 0;
    }
    .famille-item-top {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }
    .famille-filename {
      font-family: monospace;
      font-weight: 700;
      color: #f8fafc;
      font-size: 0.95rem;
    }
    .famille-badge {
      font-size: 0.72rem;
      padding: 2px 8px;
      border-radius: 999px;
      font-weight: 600;
    }
    .badge-ad {
      background: rgba(245, 158, 11, 0.2);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.4);
    }
    .badge-en {
      background: rgba(78, 204, 163, 0.18);
      color: #4ecca3;
      border: 1px solid rgba(78, 204, 163, 0.35);
    }
    .famille-item-desc {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      font-size: 0.82rem;
      color: #cbd5e1;
      margin-top: 3px;
      flex-wrap: wrap;
    }
    .famille-ar { color: #94a3b8; }
    .btn-preview {
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid rgba(255, 255, 255, 0.12);
      color: #e2e8f0;
      border-radius: 6px;
      padding: 6px 10px;
      cursor: pointer;
    }
    .btn-preview:hover, .btn-preview.active {
      background: rgba(78, 204, 163, 0.25);
      border-color: #4ecca3;
    }
    .famille-preview {
      width: 300px;
      border-left: 1px solid rgba(255, 255, 255, 0.08);
      padding: 12px;
      background: rgba(15, 23, 42, 0.6);
      display: flex;
      flex-direction: column;
      gap: 8px;
      overflow-y: auto;
    }
    .preview-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-family: monospace;
      color: #4ecca3;
      font-size: 0.85rem;
    }
    .btn-close-sm {
      background: none;
      border: none;
      color: #94a3b8;
      cursor: pointer;
    }
    .famille-preview img {
      width: 100%;
      border-radius: 6px;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    .famille-error {
      padding: 8px 20px;
      color: #f87171;
      background: rgba(239, 68, 68, 0.12);
      font-size: 0.85rem;
    }
    .famille-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 20px;
      border-top: 1px solid rgba(255, 255, 255, 0.08);
      background: rgba(15, 23, 42, 0.5);
    }
    .selection-count {
      color: #94a3b8;
      font-size: 0.88rem;
    }
    .footer-actions {
      display: flex;
      gap: 10px;
    }
    @media (max-width: 700px) {
      .famille-body { flex-direction: column; }
      .famille-preview { width: 100%; border-left: none; border-top: 1px solid rgba(255,255,255,0.08); }
    }
  `]
})
export class FamillePickerModalComponent implements OnInit, OnDestroy {
  private http = inject(HttpClient);
  private el = inject(ElementRef<HTMLElement>);

  @Input() multiple = false;
  @Input() maxFiles = 10;
  @Output() filesSelected = new EventEmitter<File[]>();
  @Output() closed = new EventEmitter<void>();

  fichiers: FichierFamilleInfo[] = [];
  selection = new Set<string>();
  apercuNom: string | null = null;
  chargementListe = true;
  telechargement = false;
  erreur = '';

  ngOnInit() {
    // Monter la modale directement dans document.body pour éviter qu'un parent avec
    // transform: translateX(...) (comme le carrousel d'upload) ne décale position: fixed hors écran.
    if (this.el?.nativeElement && typeof document !== 'undefined') {
      document.body.appendChild(this.el.nativeElement);
    }

    this.http.get<FichierFamilleInfo[]>('/api/demo/famille').subscribe({
      next: (data) => {
        this.fichiers = data || [];
        this.chargementListe = false;
      },
      error: () => {
        this.chargementListe = false;
        this.erreur = 'Impossible de charger la liste du dossier « famille ».';
      }
    });
  }

  ngOnDestroy() {
    if (this.el?.nativeElement?.parentNode === document.body) {
      document.body.removeChild(this.el.nativeElement);
    }
  }

  estSelectionne(nom: string): boolean {
    return this.selection.has(nom);
  }

  basculerSelection(nom: string) {
    this.erreur = '';
    if (!this.multiple) {
      this.selection.clear();
      this.selection.add(nom);
      return;
    }
    if (this.selection.has(nom)) {
      this.selection.delete(nom);
    } else {
      if (this.selection.size >= this.maxFiles) {
        this.erreur = `Vous pouvez sélectionner au maximum ${this.maxFiles} fichier(s).`;
        return;
      }
      this.selection.add(nom);
    }
  }

  afficherApercu(nom: string) {
    this.apercuNom = this.apercuNom === nom ? null : nom;
  }

  confirmerUnique(nom: string) {
    if (!this.multiple) {
      this.selection.clear();
    }
    this.selection.add(nom);
    this.valider();
  }

  valider() {
    if (this.selection.size === 0 || this.telechargement) return;
    this.telechargement = true;
    this.erreur = '';

    const noms = Array.from(this.selection);
    const requetes = noms.map(nom =>
      this.http.get(`/api/demo/famille/${encodeURIComponent(nom)}`, { responseType: 'blob' })
    );

    forkJoin(requetes).subscribe({
      next: (blobs) => {
        const files = blobs.map((blob, i) => new File([blob], noms[i], { type: 'image/png' }));
        this.telechargement = false;
        this.filesSelected.emit(files);
      },
      error: () => {
        this.telechargement = false;
        this.erreur = 'Erreur lors de la récupération du fichier dans le dossier « famille ».';
      }
    });
  }

  fermer() {
    this.closed.emit();
  }
}
