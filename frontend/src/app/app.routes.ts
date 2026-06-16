import { Routes } from '@angular/router';
import { authGuard, maitreGuard } from './services/auth.guard';

export const routes: Routes = [
  { 
    path: '', 
    loadComponent: () => import('./components/accueil/home/home.component').then(m => m.HomeComponent) 
  },
  { 
    path: 'about', 
    loadComponent: () => import('./components/accueil/about/about.component').then(m => m.AboutComponent) 
  },
  { 
    path: 'simulateur', 
    loadComponent: () => import('./components/simulateur/simulateur.component').then(m => m.SimulateurComponent) 
  },
  { 
    path: 'login', 
    loadComponent: () => import('./components/auth/login/login.component').then(m => m.LoginComponent) 
  },
  { 
    path: 'users', 
    canActivate: [maitreGuard],
    loadComponent: () => import('./components/admin/user-management/user-management.component').then(m => m.UserManagementComponent) 
  },
  { 
    path: 'create', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/dossier/create-person/create-person.component').then(m => m.CreatePersonComponent) 
  },
  { 
    path: 'upload', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/dossier/upload-windows/upload-windows.component').then(m => m.UploadWindowsComponent) 
  },
  { 
    path: 'review-family', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/dossier/heir-review/heir-review.component').then(m => m.HeirReviewComponent) 
  },
  { 
    path: 'correction', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/dossier/ocr-correction/ocr-correction.component').then(m => m.OcrCorrectionComponent) 
  },
  {
    path: 'batch-review',
    canActivate: [authGuard],
    loadComponent: () => import('./components/batch-review/batch-review.component').then(m => m.BatchReviewComponent)
  },
  { 
    path: 'frida', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/frida/frida.component').then(m => m.FridaComponent)
  },
  {
    path: 'search', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/search/search.component').then(m => m.SearchComponent)
  },
  { 
    path: 'list', 
    redirectTo: 'search',
    pathMatch: 'full'
  },
  { 
    path: 'edit/:numFrida', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/frida-edit/frida-edit.component').then(m => m.FridaEditComponent) 
  },
  { 
    path: 'backups', 
    canActivate: [authGuard],
    loadComponent: () => import('./components/backup/backup.component').then(m => m.BackupComponent) 
  },
  {
    path: 'license',
    loadComponent: () => import('./components/license/license.component').then(m => m.LicenseComponent)
  },
  { path: '**', redirectTo: '' }
];