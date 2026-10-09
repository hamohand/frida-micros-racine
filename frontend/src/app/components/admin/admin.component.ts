import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, Router, NavigationEnd } from "@angular/router";
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { filter } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';
import { UploadStateService } from '../../services/upload-state.service';
import { NotificationsComponent } from '../../shared/notifications/notifications.component';
import { DossierStepperComponent, ETAPES_DOSSIER } from '../../shared/dossier-stepper/dossier-stepper.component';
import { BilabelComponent } from '../../shared/bilabel/bilabel.component';

interface LienNav {
  route: string;
  libelle: string;
  arabe?: string;
  icone: string;
  exact?: boolean;
}

interface Miette {
  libelle: string;
  route?: string;
  arabe?: string;
}

/** Tracés d'icônes Material Symbols (viewBox 0 -960 960 960). */
const ICONES = {
  accueil: 'M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Zm320-350Z',
  nouveau: 'M440-280h80v-160h160v-80H520v-160h-80v160H280v80h160v160Zm40 200q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z',
  batch: 'M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm40-80h200v-80H240v80Zm280-80h200v-80H520v80ZM240-440h200v-80H240v80Zm280 0h200v-80H520v80ZM240-600h200v-80H240v80Zm280 0h200v-80H520v80ZM200-200v-560 560Z',
  recherche: 'M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z',
  simulateur: 'M280-400q-33 0-56.5-23.5T200-480q0-33 23.5-56.5T280-560q33 0 56.5 23.5T360-480q0 33-23.5 56.5T280-400Zm400 0q-33 0-56.5-23.5T600-480q0-33 23.5-56.5T680-560q33 0 56.5 23.5T760-480q0 33-23.5 56.5T680-400ZM480-240q-33 0-56.5-23.5T400-320q0-33 23.5-56.5T480-400q33 0 56.5 23.5T560-320q0 33-23.5 56.5T480-240ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z',
  utilisateurs: 'M40-160v-112q0-34 17.5-62.5T104-378q62-31 126-46.5T360-440q66 0 130 15.5T616-378q29 15 46.5 43.5T680-272v112H40Zm720 0v-120q0-44-24.5-84.5T666-434q51 6 96 20.5t84 35.5q36 20 55 44.5t19 53.5v120H760ZM360-480q-66 0-113-47t-47-113q0-66 47-113t113-47q66 0 113 47t47 113q0 66-47 113t-113 47Zm400-160q0 66-47 113t-113 47q-11 0-28-2.5t-28-5.5q27-32 41.5-71t14.5-81q0-42-14.5-81T544-792q14-5 28-6.5t28-1.5q66 0 113 47t47 113ZM120-240h480v-32q0-11-5.5-20T580-306q-54-27-109-40.5T360-360q-56 0-111 13.5T140-306q-9 5-14.5 14t-5.5 20v32Zm240-320q33 0 56.5-23.5T440-640q0-33-23.5-56.5T360-720q-33 0-56.5 23.5T280-640q0 33 23.5 56.5T360-560Zm0 320Zm0-400Z',
  sauvegardes: 'M480-320 280-520l56-58 104 104v-326h80v326l104-104 56 58-200 200ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z',
  parametres: 'M440-120v-240h80v80h320v80H520v80h-80Zm-320-80v-80h240v80H120Zm160-160v-80H120v-80h160v-80h80v240h-80Zm160-80v-80h400v80H440Zm160-160v-240h80v80h160v80H680v80h-80Zm-480-80v-80h400v80H120Z',
  menu: 'M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z',
  replier: 'M560-240 320-480l240-240 56 56-184 184 184 184-56 56Z',
  reprendre: 'M320-200v-560l440 280-440 280Zm80-280Zm0 134 210-134-210-134v268Z',
  deconnexion: 'M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h280v80H200v560h280v80H200Zm440-160-55-58 102-102H360v-80h327L585-622l55-58 200 200-200 200Z',
};

/** Libellé de chaque page, pour le fil d'Ariane. */
const LIBELLES: Record<string, string> = {
  'simulateur': 'Simulateur de parts',
  'batch-review': 'Dossiers à réviser',
  'search': 'Rechercher',
  'users': 'Utilisateurs',
  'backups': 'Sauvegardes',
  'parametres': 'Paramètres',
  'license': 'Licence',
  'login': 'Connexion',
  'about': 'À propos',
  'create': 'Défunt',
  'upload': 'Documents',
  'correction': 'Vérification',
  'review-family': 'Héritiers',
  'frida': 'Fiche',
  'edit': 'Modification',
};

const LIBELLES_AR: Record<string, string> = {
  'simulateur': 'محاكي الحصص',
  'batch-review': 'ملفات للمراجعة',
  'search': 'بحث',
  'users': 'المستخدمون',
  'backups': 'النسخ الاحتياطية',
  'parametres': 'الإعدادات',
  'license': 'الترخيص',
  'login': 'تسجيل الدخول',
  'about': 'حول التطبيق',
  'create': 'المتوفى',
  'upload': 'الوثائق',
  'correction': 'التحقق',
  'review-family': 'الورثة',
  'frida': 'البطاقة',
  'edit': 'تعديل',
};

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, NotificationsComponent, DossierStepperComponent, BilabelComponent],
  template: `
    <div class="app-shell"
         [class.avec-sidebar]="afficherSidebar"
         [class.sidebar-reduite]="sidebarReduite"
         [class.menu-mobile-ouvert]="menuMobileOuvert"
         [class.sans-navigation]="isStandaloneSimulator">

      <header class="app-topbar" *ngIf="!isStandaloneSimulator">
        <button *ngIf="afficherSidebar" type="button" class="icone-btn bouton-menu"
                (click)="basculerMenu()" aria-label="Afficher ou masquer le menu">
          <svg viewBox="0 -960 960 960" width="24" height="24" fill="currentColor"><path [attr.d]="icones.menu"/></svg>
        </button>
        <a routerLink="/" class="nav-logo"><app-bilabel fr="Ustadh-a" ar="أستاذ-ة" /></a>

        <nav class="fil-ariane" aria-label="Fil d'Ariane" *ngIf="miettes.length">
          <ng-container *ngFor="let m of miettes; let dernier = last">
            <span class="separateur">›</span>
            <a *ngIf="m.route && !dernier; else texte" [routerLink]="m.route"><app-bilabel [fr]="m.libelle" [ar]="m.arabe ?? ''" /></a>
            <ng-template #texte><span [class.courant]="dernier"><app-bilabel [fr]="m.libelle" [ar]="m.arabe ?? ''" /></span></ng-template>
          </ng-container>
        </nav>

        <div class="topbar-droite" *ngIf="authService.isLoggedIn()">
          <button type="button" class="pastille-demo" (click)="langService.toggleLanguage()" title="Changer de langue" style="border-color: #4ecca3; color: #4ecca3; margin-right: 8px; display: flex; align-items: center; justify-content: center; width: auto; padding: 0 12px; font-weight: bold;">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" style="margin-right: 6px;"><path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95c-.32-1.25-.78-2.45-1.38-3.56 1.84.63 3.37 1.91 4.33 3.56zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56-1.84-.63-3.37-1.9-4.33-3.56zm2.95-8H5.08c1.96-1.66 3.49-2.93 5.33-3.56C9.81 5.55 9.35 6.75 9.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95c-.96 1.65-2.49 2.93-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/></svg>
            {{ langService.currentLangSignal() === 'fr' ? 'عربي' : 'Français' }}
          </button>
          <button type="button" class="pastille-demo" [class.actif]="isDemoMode" (click)="toggleDemoMode()"
                  title="Flouter les données personnelles pour une présentation">
            <app-bilabel [fr]="'Démo ' + (isDemoMode ? 'activée' : 'désactivée')" [ar]="isDemoMode ? 'العرض مفعّل' : 'العرض معطّل'" />
          </button>
          <div class="utilisateur">
            <span class="avatar">{{ initiale }}</span>
            <span class="utilisateur-texte">
              <span class="utilisateur-nom">{{ nomUtilisateur }}</span>
              <span class="utilisateur-role"><app-bilabel [fr]="authService.isMaitre() ? 'Maître' : 'Collaborateur'" [ar]="authService.isMaitre() ? 'أستاذ' : 'متعاون'" /></span>
            </span>
          </div>
          <button type="button" class="icone-btn bouton-deconnexion" (click)="logout()" title="Déconnexion" aria-label="Déconnexion">
            <svg viewBox="0 -960 960 960" width="22" height="22" fill="currentColor"><path [attr.d]="icones.deconnexion"/></svg>
          </button>
        </div>
      </header>

      <aside class="app-sidebar" *ngIf="afficherSidebar">
        <nav class="sidebar-nav">
          <a *ngIf="uploadState.dossierEnCours() as d" [routerLink]="lienReprise(d.url).chemin" [queryParams]="lienReprise(d.url).params"
             class="sidebar-lien sidebar-reprise" [class.actif]="etapeDossier && etapeDossier !== 'frida'"
             [title]="'Reprendre le dossier en cours : ' + d.libelle">
            <svg viewBox="0 -960 960 960" width="22" height="22" fill="currentColor"><path [attr.d]="icones.reprendre"/></svg>
            <span class="sidebar-libelle">
              <span class="reprise-titre"><app-bilabel fr="Reprendre" ar="متابعة" [wrap]="true" /></span>
              <span class="reprise-nom">{{ d.libelle }}</span>
            </span>
          </a>
          <a *ngFor="let l of liensPrincipaux" [routerLink]="l.route" routerLinkActive="actif"
             [routerLinkActiveOptions]="{ exact: !!l.exact }" class="sidebar-lien" [title]="l.libelle">
            <svg viewBox="0 -960 960 960" width="22" height="22" fill="currentColor"><path [attr.d]="l.icone"/></svg>
            <span class="sidebar-libelle"><app-bilabel [fr]="l.libelle" [ar]="l.arabe ?? ''" [wrap]="true" /></span>
          </a>

          <!-- Fiches issues de l'ancien mode batch : le lien disparaît une fois toutes révisées -->
          <a *ngIf="nbARevoir > 0" routerLink="/batch-review" routerLinkActive="actif" class="sidebar-lien" title="Dossiers à réviser">
            <svg viewBox="0 -960 960 960" width="22" height="22" fill="currentColor"><path [attr.d]="icones.batch"/></svg>
            <span class="sidebar-libelle"><app-bilabel fr="À réviser" ar="للمراجعة" [wrap]="true" /></span>
            <span class="sidebar-compteur">{{ nbARevoir }}</span>
          </a>

          <ng-container *ngIf="authService.isMaitre()">
            <div class="sidebar-section"><app-bilabel fr="Administration" ar="الإدارة" [wrap]="true" /></div>
            <a *ngFor="let l of liensAdmin" [routerLink]="l.route" routerLinkActive="actif" class="sidebar-lien" [title]="l.libelle">
              <svg viewBox="0 -960 960 960" width="22" height="22" fill="currentColor"><path [attr.d]="l.icone"/></svg>
              <span class="sidebar-libelle"><app-bilabel [fr]="l.libelle" [ar]="l.arabe ?? ''" [wrap]="true" /></span>
            </a>
          </ng-container>
        </nav>

        <button type="button" class="sidebar-replier" (click)="basculerReduction()"
                [attr.aria-label]="sidebarReduite ? 'Déplier le menu' : 'Replier le menu'">
          <svg viewBox="0 -960 960 960" width="20" height="20" fill="currentColor"><path [attr.d]="icones.replier"/></svg>
          <span class="sidebar-libelle"><app-bilabel fr="Replier" ar="طيّ القائمة" [wrap]="true" /></span>
        </button>
      </aside>
      <div class="sidebar-voile" *ngIf="afficherSidebar" (click)="menuMobileOuvert = false"></div>

      <main class="app-main">
        <app-dossier-stepper *ngIf="etapeDossier" [etapeActive]="etapeDossier"></app-dossier-stepper>
        <router-outlet></router-outlet>
      </main>
    </div>
    <app-notifications></app-notifications>
  `,
})
export class AdminComponent {
  authService = inject(AuthService);
  langService = inject(LanguageService);
  router = inject(Router);
  uploadState = inject(UploadStateService);
  private http = inject(HttpClient);
  nbARevoir = 0;
  isDemoMode = false;
  isStandaloneSimulator = false;

  icones = ICONES;
  sidebarReduite = localStorage.getItem('sidebarReduite') === 'true';
  menuMobileOuvert = false;
  miettes: Miette[] = [];
  etapeDossier: string | null = null;

  liensPrincipaux: LienNav[] = [
    { route: '/', libelle: 'Accueil', arabe: 'الرئيسية', icone: ICONES.accueil, exact: true },
    { route: '/create', libelle: 'Nouveau dossier', arabe: 'ملف جديد', icone: ICONES.nouveau },
    { route: '/search', libelle: 'Rechercher', arabe: 'بحث', icone: ICONES.recherche },
    { route: '/simulateur', libelle: 'Simulateur', arabe: 'المحاكي', icone: ICONES.simulateur },
  ];

  liensAdmin: LienNav[] = [
    { route: '/users', libelle: 'Utilisateurs', arabe: 'المستخدمون', icone: ICONES.utilisateurs },
    { route: '/backups', libelle: 'Sauvegardes', arabe: 'النسخ الاحتياطية', icone: ICONES.sauvegardes },
    { route: '/parametres', libelle: 'Paramètres', arabe: 'الإعدادات', icone: ICONES.parametres },
  ];

  private routePrecedente = '';

  get afficherSidebar(): boolean {
    return !this.isStandaloneSimulator && this.authService.isLoggedIn();
  }

  get nomUtilisateur(): string {
    return localStorage.getItem('username') || 'Utilisateur';
  }

  get initiale(): string {
    return this.nomUtilisateur.charAt(0).toUpperCase();
  }

  ngOnInit() {
    if (window.location.hostname.includes('simul-frida')) {
      this.isStandaloneSimulator = true;
    }

    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => {
      this.menuMobileOuvert = false;
      this.majNavigation();
      this.majARevoir();
    });
    this.majNavigation();

    // Auto-login if demo mode is enabled on startup
    if (!this.authService.isLoggedIn()) {
      this.authService.getSecurityStatus().subscribe({
        next: (res) => {
          if (res.demoMode === true || res.demoMode === 'true') {
            this.authService.loginDemo();
          }
        },
        error: () => {}
      });
    }
  }

  basculerMenu() {
    // Sur petit écran le menu s'ouvre par-dessus la page ; sur grand écran il se replie
    if (window.matchMedia('(max-width: 900px)').matches) {
      this.menuMobileOuvert = !this.menuMobileOuvert;
    } else {
      this.basculerReduction();
    }
  }

  basculerReduction() {
    this.sidebarReduite = !this.sidebarReduite;
    localStorage.setItem('sidebarReduite', String(this.sidebarReduite));
  }

  toggleDemoMode() {
    this.isDemoMode = !this.isDemoMode;
    if (this.isDemoMode) {
      document.body.classList.add('demo-mode');
    } else {
      document.body.classList.remove('demo-mode');
    }
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  /** Compte les fiches en attente de révision (ancien mode batch). */
  private majARevoir() {
    if (!this.authService.isLoggedIn()) {
      this.nbARevoir = 0;
      return;
    }
    this.http.get<unknown[]>('/api/frida/batch-en-attente').subscribe({
      next: liste => this.nbARevoir = Array.isArray(liste) ? liste.length : 0,
      error: () => this.nbARevoir = 0
    });
  }

  /** Découpe une URL mémorisée en chemin + paramètres pour routerLink. */
  lienReprise(url: string): { chemin: string; params: Record<string, string> } {
    const arbre = this.router.parseUrl(url);
    const chemin = '/' + (arbre.root.children['primary']?.segments.map(s => s.path).join('/') ?? '');
    return { chemin, params: arbre.queryParams };
  }

  /** Recalcule le fil d'Ariane et l'étape du dossier à partir de l'URL. */
  private majNavigation() {
    const arbre = this.router.parseUrl(this.router.url);
    const segments = arbre.root.children['primary']?.segments.map(s => s.path) ?? [];
    const page = segments[0] ?? '';
    const numFrida: string | undefined = arbre.queryParams['numFrida'] ?? (page === 'edit' ? segments[1] : undefined);
    const estEtape = ETAPES_DOSSIER.some(e => e.route === page);

    // Sur la fiche, l'indicateur n'apparaît qu'en fin de parcours, pas en simple consultation
    const finDeParcours = page === 'frida' && this.routePrecedente === 'review-family';
    this.etapeDossier = estEtape && (page !== 'frida' || finDeParcours) ? page : null;

    // Mémorise l'étape du dossier en cours pour le lien « Reprendre »
    if (page === 'upload' || page === 'correction' || page === 'review-family') {
      this.uploadState.noterEtape(page, this.router.url, numFrida);
    } else if (finDeParcours) {
      this.uploadState.terminerSi(numFrida);
    }
    this.routePrecedente = page;

    this.miettes = [];
    if (!page) return;
    if (estEtape || page === 'edit') {
      this.miettes.push(numFrida
        ? { libelle: 'Dossier n° ' + numFrida, arabe: 'ملف رقم ' + numFrida }
        : { libelle: 'Nouveau dossier', route: '/create', arabe: 'ملف جديد' });
    }
    this.miettes.push({ libelle: LIBELLES[page] ?? page, arabe: LIBELLES_AR[page] ?? '' });
  }
}
