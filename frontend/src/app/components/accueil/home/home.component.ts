import { Component, DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { forkJoin, of, catchError } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { FridaService } from '../../../services/frida.service';
import { Brouillon, BrouillonService } from '../../../services/brouillon.service';
import { BackupInfo, BackupService } from '../../../services/backup.service';
import { UploadStateService } from '../../../services/upload-state.service';

interface DossierResume {
  numFrida: string;
  dateCreation: string;
  nom: string;
  prenom: string;
  requiresCorrection: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <!-- Visiteur non connecté -->
    <section *ngIf="!authService.isLoggedIn(); else tableauDeBord" class="accueil-public">
      <h1 class="hero-title">Ustadh-a</h1>
      <p class="hero-subtitle">Système Avancé de Gestion Notariale</p>
      <div class="actions-publiques">
        <a routerLink="/login" class="btn btn-primary">Se connecter</a>
        <a routerLink="/simulateur" class="btn btn-secondary">Simulateur de parts</a>
      </div>
    </section>

    <ng-template #tableauDeBord>
      <div class="dashboard">
        <header class="dashboard-entete">
          <div>
            <h1>Bonjour{{ nomUtilisateur ? ', ' + nomUtilisateur : '' }}</h1>
            <p class="date-jour">{{ aujourdhui }}</p>
          </div>
          <div class="entete-actions">
            <form class="recherche-rapide" (submit)="rechercher($event, champ.value)">
              <svg viewBox="0 -960 960 960" width="20" height="20" fill="currentColor"><path d="M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z"/></svg>
              <input #champ type="search" placeholder="N° de dossier, nom du défunt…" aria-label="Rechercher un dossier" />
            </form>
            <a routerLink="/create" class="btn btn-primary bouton-nouveau">+ Nouveau dossier</a>
          </div>
        </header>

        <!-- Indicateurs -->
        <div class="tuiles">
          <a routerLink="/search" class="tuile">
            <span class="tuile-valeur">{{ chargement ? '–' : dossiers.length }}</span>
            <span class="tuile-libelle">Dossiers actifs</span>
          </a>
          <a routerLink="/search" class="tuile">
            <span class="tuile-valeur">{{ chargement ? '–' : dossiersDuMois }}</span>
            <span class="tuile-libelle">Créés ce mois-ci</span>
          </a>
          <a routerLink="/search" class="tuile" [class.tuile-attention]="brouillons.length > 0">
            <span class="tuile-valeur">{{ chargement ? '–' : brouillons.length }}</span>
            <span class="tuile-libelle">Brouillons en cours</span>
          </a>
          <a *ngIf="batchAReviser > 0" routerLink="/batch-review" class="tuile tuile-attention">
            <span class="tuile-valeur">{{ batchAReviser }}</span>
            <span class="tuile-libelle">Dossiers à réviser</span>
          </a>
        </div>

        <!-- Dossier en cours de création -->
        <button *ngIf="uploadState.dossierEnCours() as d" type="button" class="bandeau-reprise" (click)="router.navigateByUrl(d.url)">
          <span class="bandeau-icone">▶</span>
          <span>Dossier en cours : <strong class="demo-blur">{{ d.libelle }}</strong></span>
          <span class="bandeau-lien">Reprendre ›</span>
        </button>

        <!-- Alerte sauvegarde (Maître) -->
        <a *ngIf="authService.isMaitre() && !chargement && etatSauvegarde as s" routerLink="/backups"
           class="bandeau-sauvegarde" [class.en-retard]="s.enRetard">
          <span class="bandeau-icone">{{ s.enRetard ? '⚠' : '✓' }}</span>
          <span>{{ s.texte }}</span>
          <span class="bandeau-lien">Sauvegardes ›</span>
        </a>

        <div class="colonnes">
          <!-- Brouillons -->
          <section class="panneau">
            <div class="panneau-entete">
              <h2>Reprendre un brouillon</h2>
              <a *ngIf="brouillons.length > 5" routerLink="/search" class="voir-tout">Tout voir ({{ brouillons.length }})</a>
            </div>
            <div *ngIf="chargement" class="squelette-liste">
              <div class="squelette" *ngFor="let i of [1,2,3]"></div>
            </div>
            <ul *ngIf="!chargement && brouillons.length" class="liste">
              <li *ngFor="let b of brouillons.slice(0, 5)">
                <button type="button" class="ligne" (click)="reprendreBrouillon(b)">
                  <span class="ligne-principal demo-blur">{{ b.nomDefunt }} {{ b.prenomDefunt }}</span>
                  <span class="ligne-secondaire">Commencé le {{ formaterDate(b.dateCreation) }}</span>
                  <span class="ligne-action">Reprendre ›</span>
                </button>
              </li>
            </ul>
            <p *ngIf="!chargement && !brouillons.length" class="vide">
              Aucun brouillon en attente. Un dossier interrompu à l'étape Documents apparaîtra ici.
            </p>
          </section>

          <!-- Derniers dossiers -->
          <section class="panneau">
            <div class="panneau-entete">
              <h2>Derniers dossiers</h2>
              <a routerLink="/search" class="voir-tout">Tout voir</a>
            </div>
            <div *ngIf="chargement" class="squelette-liste">
              <div class="squelette" *ngFor="let i of [1,2,3]"></div>
            </div>
            <ul *ngIf="!chargement && derniersDossiers.length" class="liste">
              <li *ngFor="let d of derniersDossiers">
                <button type="button" class="ligne" (click)="ouvrirDossier(d)">
                  <span class="ligne-principal">
                    <span class="num">{{ d.numFrida }}</span>
                    <span class="demo-blur">{{ d.nom }} {{ d.prenom }}</span>
                  </span>
                  <span class="ligne-secondaire">{{ formaterDate(d.dateCreation) }}</span>
                  <span *ngIf="d.requiresCorrection" class="badge-correction">À corriger</span>
                </button>
              </li>
            </ul>
            <div *ngIf="!chargement && !derniersDossiers.length" class="vide">
              <p>Aucun dossier pour l'instant.</p>
              <a routerLink="/create" class="btn btn-secondary">Créer le premier dossier</a>
            </div>
          </section>
        </div>
      </div>
    </ng-template>
  `,
  styles: [`
    :host { display: block; }

    /* --- Visiteur --- */
    .accueil-public {
      min-height: calc(100vh - var(--nav-height));
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: var(--space-6);
    }
    .actions-publiques { display: flex; gap: var(--space-3); flex-wrap: wrap; justify-content: center; }

    /* --- Tableau de bord --- */
    .dashboard {
      max-width: 1180px;
      margin: 0 auto;
      padding: var(--space-6) var(--space-6) var(--space-7);
    }
    .dashboard-entete {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: var(--space-4);
      flex-wrap: wrap;
      margin-bottom: var(--space-6);
    }
    .dashboard-entete h1 { font-size: 1.75rem; color: var(--text-1); margin: 0; }
    .date-jour { color: var(--text-muted); margin: var(--space-1) 0 0; }
    .entete-actions { display: flex; gap: var(--space-3); align-items: center; flex-wrap: wrap; }

    .recherche-rapide {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      width: 300px;
      max-width: 100%;
      padding: 0 var(--space-3);
      height: 42px;
      background: var(--surface-1);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-md);
      color: var(--text-muted);
      transition: border-color var(--duree-rapide);
    }
    .recherche-rapide:focus-within { border-color: var(--primary); }
    .recherche-rapide input {
      flex: 1;
      min-width: 0;
      border: none;
      outline: none;
      background: transparent;
      color: var(--text-1);
      font: inherit;
    }
    .bouton-nouveau { padding: var(--space-2) var(--space-5); height: 42px; display: inline-flex; align-items: center; }

    /* Tuiles */
    .tuiles {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: var(--space-4);
      margin-bottom: var(--space-5);
    }
    .tuile {
      display: flex;
      flex-direction: column;
      gap: var(--space-1);
      padding: var(--space-4) var(--space-5);
      background: var(--surface-1);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      text-decoration: none;
      transition: border-color var(--duree-rapide), transform var(--duree-rapide);
    }
    .tuile:hover { border-color: var(--primary); transform: translateY(-2px); }
    .tuile-valeur { font-size: 2rem; font-weight: 700; color: var(--text-1); line-height: 1.1; }
    .tuile-libelle { color: var(--text-2); font-size: 0.9rem; }
    .tuile-attention .tuile-valeur { color: var(--warning); }

    /* Bandeau dossier en cours */
    .bandeau-reprise {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      width: 100%;
      padding: var(--space-3) var(--space-4);
      margin-bottom: var(--space-4);
      border: 1px solid var(--warning);
      border-radius: var(--radius-md);
      background: var(--warning-soft);
      color: var(--text-1);
      font: inherit;
      font-size: 0.95rem;
      text-align: left;
      cursor: pointer;
    }
    .bandeau-reprise .bandeau-icone, .bandeau-reprise .bandeau-lien { color: var(--warning); }
    .bandeau-reprise:hover { border-style: dashed; }

    /* Bandeau sauvegarde */
    .bandeau-sauvegarde {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      padding: var(--space-3) var(--space-4);
      margin-bottom: var(--space-5);
      border-radius: var(--radius-md);
      border: 1px solid var(--border-subtle);
      background: var(--primary-soft);
      color: var(--text-2);
      text-decoration: none;
      font-size: 0.92rem;
    }
    .bandeau-sauvegarde.en-retard { background: var(--warning-soft); border-color: var(--warning); color: var(--text-1); }
    .bandeau-icone { font-weight: 700; color: var(--primary); }
    .en-retard .bandeau-icone { color: var(--warning); }
    .bandeau-lien { margin-left: auto; color: var(--primary); white-space: nowrap; }
    .en-retard .bandeau-lien { color: var(--warning); }

    /* Panneaux */
    .colonnes { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-5); }
    .panneau {
      background: var(--surface-1);
      border: 1px solid var(--border-subtle);
      border-radius: var(--radius-lg);
      padding: var(--space-5);
      min-width: 0;
    }
    .panneau-entete { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: var(--space-3); }
    .panneau-entete h2 { font-size: 1.05rem; color: var(--text-1); margin: 0; }
    .voir-tout { color: var(--primary); text-decoration: none; font-size: 0.88rem; }
    .voir-tout:hover { text-decoration: underline; }

    .liste { list-style: none; display: flex; flex-direction: column; }
    .ligne {
      display: grid;
      grid-template-columns: 1fr auto;
      grid-template-areas: "principal action" "secondaire action";
      align-items: center;
      column-gap: var(--space-3);
      padding: var(--space-3);
      margin: 0 calc(-1 * var(--space-3));
      width: calc(100% + 2 * var(--space-3));
      border: none;
      border-radius: var(--radius-md);
      background: transparent;
      color: inherit;
      font: inherit;
      text-align: left;
      cursor: pointer;
      transition: background var(--duree-rapide);
    }
    .liste li + li .ligne { border-top: 1px solid rgba(255, 255, 255, 0.04); }
    .ligne:hover { background: var(--primary-soft); }
    .ligne-principal { grid-area: principal; color: var(--text-1); font-weight: 500; display: flex; gap: var(--space-2); min-width: 0; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
    .ligne-secondaire { grid-area: secondaire; color: var(--text-muted); font-size: 0.85rem; }
    .ligne-action { grid-area: action; color: var(--primary); font-size: 0.88rem; opacity: 0; transition: opacity var(--duree-rapide); }
    .ligne:hover .ligne-action, .ligne:focus-visible .ligne-action { opacity: 1; }
    .num { font-family: var(--font-mono); color: var(--primary); }
    .badge-correction {
      grid-area: action;
      padding: 2px var(--space-2);
      border-radius: 999px;
      background: var(--warning-soft);
      color: var(--warning);
      font-size: 0.75rem;
      font-weight: 600;
    }

    .vide { color: var(--text-muted); font-size: 0.92rem; padding: var(--space-4) 0; }
    .vide p { margin-bottom: var(--space-3); }

    /* Squelettes de chargement */
    .squelette-liste { display: flex; flex-direction: column; gap: var(--space-3); }
    .squelette {
      height: 44px;
      border-radius: var(--radius-md);
      background: linear-gradient(90deg, var(--surface-1) 25%, rgba(255,255,255,0.08) 50%, var(--surface-1) 75%);
      background-size: 200% 100%;
      animation: reflet 1.4s infinite;
    }
    @keyframes reflet { to { background-position: -200% 0; } }

    @media (max-width: 1000px) {
      .colonnes { grid-template-columns: 1fr; }
    }
    @media (max-width: 600px) {
      .dashboard { padding: var(--space-4); }
      .recherche-rapide { width: 100%; }
    }
    @media (prefers-reduced-motion: reduce) {
      .squelette { animation: none; }
    }
  `]
})
export class HomeComponent {
  authService = inject(AuthService);
  router = inject(Router);
  uploadState = inject(UploadStateService);
  private fridaService = inject(FridaService);
  private brouillonService = inject(BrouillonService);
  private backupService = inject(BackupService);
  private destroyRef = inject(DestroyRef);

  chargement = true;
  dossiers: DossierResume[] = [];
  brouillons: Brouillon[] = [];
  batchAReviser = 0;
  derniereSauvegarde: BackupInfo | null = null;

  get aujourdhui(): string {
    return new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  get nomUtilisateur(): string {
    return localStorage.getItem('username') || '';
  }

  get derniersDossiers(): DossierResume[] {
    return [...this.dossiers]
      .sort((a, b) => (b.dateCreation || '').localeCompare(a.dateCreation || '') || b.numFrida.localeCompare(a.numFrida))
      .slice(0, 6);
  }

  get dossiersDuMois(): number {
    const mois = new Date().toISOString().slice(0, 7);
    return this.dossiers.filter(d => (d.dateCreation || '').startsWith(mois)).length;
  }

  /** Alerte si aucune sauvegarde ou si la dernière a plus de 48 h (l'automatique tourne toutes les 24 h). */
  get etatSauvegarde(): { texte: string; enRetard: boolean } {
    if (!this.derniereSauvegarde) {
      return { texte: 'Aucune sauvegarde n\'a encore été faite.', enRetard: true };
    }
    const date = new Date(this.derniereSauvegarde.createdAt);
    const heures = (Date.now() - date.getTime()) / 3_600_000;
    const quand = date.toLocaleString('fr-FR', { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' });
    return heures > 48
      ? { texte: `Dernière sauvegarde le ${quand}, il y a plus de deux jours.`, enRetard: true }
      : { texte: `Dernière sauvegarde le ${quand}.`, enRetard: false };
  }

  ngOnInit() {
    if (window.location.hostname.includes('simul-frida')) {
      this.router.navigate(['/simulateur']);
      return;
    }
    // En mode démo, la connexion automatique peut arriver après l'affichage de la page
    this.authService.utilisateur$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(u => {
      if (u && !this.chargementLance) this.charger();
    });
  }

  private chargementLance = false;

  private charger() {
    this.chargementLance = true;
    const aucun = catchError(() => of([]));
    forkJoin({
      dossiers: this.fridaService.lancerApi('/api/frida/fridas').pipe(aucun),
      brouillons: this.brouillonService.list().pipe(catchError(() => of([] as Brouillon[]))),
      batch: this.fridaService.lancerApi('/api/frida/batch-en-attente').pipe(aucun),
      sauvegardes: this.authService.isMaitre() ? this.backupService.listBackups().pipe(catchError(() => of([] as BackupInfo[]))) : of([] as BackupInfo[]),
    }).subscribe(r => {
      this.dossiers = Array.isArray(r.dossiers) ? r.dossiers.filter((d: any) => d != null) : [];
      this.brouillons = r.brouillons || [];
      this.batchAReviser = Array.isArray(r.batch) ? r.batch.length : 0;
      this.derniereSauvegarde = [...(r.sauvegardes || [])]
        .filter(s => !s.avantRestauration)
        .sort((a, b) => b.createdAt.localeCompare(a.createdAt))[0] ?? null;
      this.chargement = false;
    });
  }

  formaterDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return isNaN(d.getTime()) ? iso : d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  rechercher(event: Event, terme: string) {
    event.preventDefault();
    this.router.navigate(['/search'], { queryParams: terme.trim() ? { q: terme.trim() } : {} });
  }

  reprendreBrouillon(b: Brouillon) {
    this.router.navigate(['/upload'], { queryParams: { brouillon: b.id, folderName: b.folderName } });
  }

  // Même règle que l'écran Recherche
  ouvrirDossier(d: DossierResume) {
    if (d.requiresCorrection) {
      this.router.navigate(['/edit', d.numFrida]);
    } else {
      this.router.navigate(['/frida'], { queryParams: { numFrida: d.numFrida } });
    }
  }
}
